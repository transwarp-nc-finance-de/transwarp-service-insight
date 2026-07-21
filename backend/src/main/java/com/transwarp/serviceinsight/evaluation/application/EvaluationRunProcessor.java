package com.transwarp.serviceinsight.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationCase;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationCaseResult;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationSummary;
import com.transwarp.serviceinsight.evaluation.domain.EvidenceFixtureManifest.EvidenceFixture;
import com.transwarp.serviceinsight.evaluation.port.EvaluationFixturePort;
import com.transwarp.serviceinsight.evaluation.port.EvaluationRetrievalPort;
import com.transwarp.serviceinsight.evaluation.port.EvaluationRunRepository;
import com.transwarp.serviceinsight.evaluation.port.EvaluationSetCatalog;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalAuditPort;
import com.transwarp.serviceinsight.precheck.v2.application.DeterministicPrecheckPolicy;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckContextNormalizer;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.*;
import com.transwarp.serviceinsight.precheck.v2.port.PersistentPrecheckRepository;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EvaluationRunProcessor {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(EvaluationRunProcessor.class);
  private final EvaluationRunRepository runs;
  private final EvaluationSetCatalog catalog;
  private final PersistentPrecheckRepository prechecks;
  private final EvaluationRetrievalPort retrieval;
  private final RetrievalAuditPort retrievalAudit;
  private final PrecheckContextNormalizer normalizer;
  private final ObjectMapper json;
  private final Clock clock;
  private final Executor executor;
  private final EvaluationFixturePort fixtures;
  private final StructuredAuditPort audit;
  private final DeterministicPrecheckPolicy policy = new DeterministicPrecheckPolicy();

  public EvaluationRunProcessor(
      EvaluationRunRepository runs,
      EvaluationSetCatalog catalog,
      PersistentPrecheckRepository prechecks,
      EvaluationRetrievalPort retrieval,
      RetrievalAuditPort retrievalAudit,
      PrecheckContextNormalizer normalizer,
      ObjectMapper json,
      Clock clock,
      EvaluationFixturePort fixtures,
      StructuredAuditPort audit,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.runs = runs;
    this.catalog = catalog;
    this.prechecks = prechecks;
    this.retrieval = retrieval;
    this.retrievalAudit = retrievalAudit;
    this.normalizer = normalizer;
    this.json = json;
    this.clock = clock;
    this.fixtures = fixtures;
    this.audit = audit;
    this.executor = executor;
  }

  public void enqueue(UUID taskId) {
    executor.execute(() -> process(taskId));
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recoverIncompleteRuns() {
    runs.recoverIncomplete().forEach(this::enqueue);
  }

  void process(UUID taskId) {
    var claimed = runs.claim(taskId, clock.instant());
    if (claimed.isEmpty()) return;
    try {
      var set = catalog.load(claimed.get().evaluationSetVersion());
      var manifest = catalog.loadEvidenceManifest(claimed.get().evaluationSetVersion());
      fixtures.publish(manifest);
      var fixturesByChunk = new HashMap<UUID, EvidenceFixture>();
      manifest
          .evidenceFixtures()
          .forEach(item -> fixturesByChunk.put(UUID.fromString(item.chunkId()), item));
      var results = new ArrayList<EvaluationCaseResult>();
      var counters = new Counters();
      for (var evaluationCase : set.cases()) {
        results.add(execute(taskId, evaluationCase, fixturesByChunk, counters));
      }
      var sampleCount = results.size();
      var permissionRate = ratio(counters.permissionFailures, sampleCount);
      var citationRate = ratio(counters.citationErrors, counters.evidenceCount);
      var degradationRate = ratio(counters.degradationPasses, counters.degradationCases);
      var recall = ratio(counters.expectedHits, counters.expectedEvidence);
      var summary =
          new EvaluationSummary(
              sampleCount,
              permissionRate == 0 && citationRate == 0 && degradationRate == 1 && recall >= .8,
              permissionRate,
              citationRate,
              degradationRate,
              recall,
              com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.DISCLAIMER);
      runs.complete(taskId, summary, results, clock.instant());
      record(
          taskId,
          "EVALUATION_RUN_COMPLETED",
          "SUCCEEDED",
          Map.of(
              "sampleCount",
              sampleCount,
              "gatePassed",
              summary.gatePassed(),
              "evaluationSetVersion",
              claimed.get().evaluationSetVersion(),
              "mockData",
              true));
    } catch (Exception failure) {
      LOG.error("Evaluation run {} failed", taskId, failure);
      runs.fail(taskId, "EVALUATION_EXECUTION_FAILED", "固定模拟评估执行失败，请人工检查本地依赖。", clock.instant());
      record(
          taskId,
          "EVALUATION_RUN_FAILED",
          "FAILED",
          Map.of("errorCode", "EVALUATION_EXECUTION_FAILED", "mockData", true));
    }
  }

  private EvaluationCaseResult execute(
      UUID taskId,
      EvaluationCase evaluationCase,
      Map<UUID, EvidenceFixture> fixturesByChunk,
      Counters counters)
      throws JsonProcessingException {
    var identity =
        new IdentityContext(
            "mock-admin",
            evaluationCase.executionIdentityCode(),
            List.of(Role.PRECHECK_USER),
            evaluationCase.allowedProductLineCodes(),
            true);
    var sessionId = UUID.randomUUID();
    RetrievalOutcome lastRetrieval = null;
    PrecheckResult lastResult = null;
    UUID lastRunId = null;
    for (var turn : evaluationCase.turns()) {
      var context = context(taskId, evaluationCase.caseId(), turn.contextSnapshot());
      var currentPolicy = prechecks.findPolicy(context.issueType().code());
      var now = clock.instant();
      lastRetrieval = retrieval.retrieve(identity, context, evaluationCase.expectedDegradation());
      lastResult = policy.evaluate(context, currentPolicy, turn.runSequence(), lastRetrieval);
      lastRunId = UUID.randomUUID();
      var run =
          new PrecheckRun(
              lastRunId,
              sessionId,
              turn.runSequence(),
              status(lastResult),
              context,
              lastResult,
              now,
              now);
      if (turn.runSequence() == 1) {
        prechecks.create(
            new PrecheckSession(sessionId, "mock-admin", "ACTIVE", null, run, 1, 3, now, now, true),
            run,
            normalizer.contextHash(context));
      } else {
        prechecks.appendRun(
            run,
            "evaluation-" + taskId + "-" + evaluationCase.caseId() + "-" + turn.runSequence(),
            normalizer.runCommandHash(sessionId, context));
      }
      retrievalAudit.save(lastRunId, "mock-admin", lastRetrieval, now);
    }

    var actualIds = new LinkedHashSet<String>();
    var productLines = new LinkedHashSet<String>();
    int unauthorized = 0;
    int citationErrors = 0;
    for (var evidence : lastRetrieval.evidence()) {
      counters.evidenceCount++;
      productLines.add(evidence.productLineCode());
      var fixture = fixturesByChunk.get(evidence.chunkId());
      if (fixture != null) actualIds.add(fixture.evidenceId());
      if (!evaluationCase.allowedProductLineCodes().contains(evidence.productLineCode())
          || (fixture != null
              && evaluationCase.forbiddenEvidenceIds().contains(fixture.evidenceId())))
        unauthorized++;
      if (fixture != null
          && (!fixture.versionId().equals(evidence.versionId().toString())
              || !fixture.contentHash().equals(evidence.contentHash()))) citationErrors++;
    }
    if (unauthorized > 0) counters.permissionFailures++;
    counters.citationErrors += citationErrors;
    counters.expectedEvidence += evaluationCase.expectedEvidenceIds().size();
    counters.expectedHits +=
        evaluationCase.expectedEvidenceIds().stream().filter(actualIds::contains).count();
    var degradationExpected =
        evaluationCase.expectedDegradation() != EvaluationCase.Degradation.NONE;
    if (degradationExpected) {
      counters.degradationCases++;
      if (degradationMatches(evaluationCase, lastRetrieval)) counters.degradationPasses++;
    }

    var failed = new LinkedHashSet<String>();
    var codes = new LinkedHashSet<String>();
    check(unauthorized == 0, "PERMISSION_LEAKAGE", "UNAUTHORIZED_EVIDENCE", failed, codes);
    check(citationErrors == 0, "CITATION_ERROR", "EVIDENCE_SNAPSHOT_MISMATCH", failed, codes);
    check(
        degradationMatches(evaluationCase, lastRetrieval),
        "DEGRADATION_BEHAVIOR",
        "DEGRADATION_MISMATCH",
        failed,
        codes);
    check(
        actualIds.containsAll(evaluationCase.expectedEvidenceIds()),
        "EVIDENCE_EXPECTATION",
        "EXPECTED_EVIDENCE_MISSING",
        failed,
        codes);
    check(
        mode(lastRetrieval).equals(evaluationCase.expectedRetrievalMode().name()),
        "RETRIEVAL_MODE",
        "RETRIEVAL_MODE_MISMATCH",
        failed,
        codes);
    check(
        new LinkedHashSet<>(lastResult.completeness().missingFieldCodes())
            .equals(new LinkedHashSet<>(evaluationCase.expectedMissingFieldCodes())),
        "MISSING_INFORMATION",
        "MISSING_INFORMATION_MISMATCH",
        failed,
        codes);

    var expected =
        Map.of(
            "evidenceCount", evaluationCase.expectedEvidenceIds().size(),
            "productLineCodes", evaluationCase.allowedProductLineCodes(),
            "retrievalMode", evaluationCase.expectedRetrievalMode().name(),
            "degraded", degradationExpected,
            "missingFieldCodes", evaluationCase.expectedMissingFieldCodes());
    var actual = new LinkedHashMap<String, Object>();
    actual.put("evidenceCount", lastRetrieval.evidence().size());
    actual.put("productLineCodes", List.copyOf(productLines));
    actual.put("retrievalMode", mode(lastRetrieval));
    actual.put("degraded", lastRetrieval.degraded());
    actual.put("missingFieldCodes", lastResult.completeness().missingFieldCodes());
    actual.put("unauthorizedEvidenceCount", unauthorized);
    actual.put("citationErrorCount", citationErrors);
    return new EvaluationCaseResult(
        evaluationCase.caseId(),
        evaluationCase.scenarioTags(),
        List.copyOf(failed),
        List.copyOf(codes),
        json.writeValueAsString(expected),
        json.writeValueAsString(actual),
        failed.isEmpty(),
        sessionId,
        lastRunId);
  }

  private PrecheckContext context(UUID taskId, String caseId, EvaluationCase.PrecheckInput input) {
    return new PrecheckContext(
        input.sourceSystem(),
        "evaluation-" + taskId + "-" + caseId,
        input.formSchemaVersion(),
        new IssueTypeValue(input.issueType(), input.issueType()),
        value(input.productLineCode()),
        value(input.productCode()),
        value(input.componentCode()),
        input.version(),
        value(input.severity()),
        value(input.serviceType()),
        input.title(),
        input.descriptionPlainText(),
        input.additionalInformation().stream()
            .map(
                item ->
                    new AdditionalInformationItem(
                        item.fieldCode(), item.displayName(), item.value()))
            .toList(),
        input.impactScope(),
        input.attachments().stream()
            .map(
                item ->
                    new AttachmentMetadata(
                        item.attachmentId(), item.fileName(), item.mediaType(), item.sizeBytes()))
            .toList());
  }

  private CatalogValue value(String value) {
    return value == null ? null : new CatalogValue(value, value);
  }

  private String status(PrecheckResult result) {
    return result.retrieval().degraded()
        ? "DEGRADED"
        : result.completeness().complete() ? "COMPLETED" : "NEED_MORE_INFORMATION";
  }

  private String mode(RetrievalOutcome result) {
    return "UNAVAILABLE".equals(result.mode()) ? "NONE" : result.mode();
  }

  private boolean degradationMatches(EvaluationCase item, RetrievalOutcome result) {
    return switch (item.expectedDegradation()) {
      case NONE -> !result.degraded();
      case FTS_ONLY -> "FTS_ONLY".equals(result.mode());
      case UNAVAILABLE -> "UNAVAILABLE".equals(result.mode());
    };
  }

  private void check(boolean ok, String check, String code, Set<String> checks, Set<String> codes) {
    if (!ok) {
      checks.add(check);
      codes.add(code);
    }
  }

  private double ratio(long numerator, long denominator) {
    return denominator == 0 ? 1.0 : (double) numerator / denominator;
  }

  private void record(UUID taskId, String action, String outcome, Map<String, Object> metadata) {
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                UUID.randomUUID(),
                "mock-admin",
                action,
                "EvaluationRun",
                taskId,
                outcome,
                metadata,
                clock.instant(),
                true),
            null,
            null));
  }

  private static final class Counters {
    long permissionFailures;
    long citationErrors;
    long evidenceCount;
    long degradationPasses;
    long degradationCases;
    long expectedHits;
    long expectedEvidence;
  }
}
