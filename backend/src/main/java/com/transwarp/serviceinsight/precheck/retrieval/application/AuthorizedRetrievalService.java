package com.transwarp.serviceinsight.precheck.retrieval.application;

import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import com.transwarp.serviceinsight.precheck.retrieval.domain.HybridRetrievalPolicy;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalCandidate;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.Capability;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceSnapshot;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalSearchPort;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizedRetrievalService {
  private final RetrievalSearchPort search;
  private final EmbeddingPort embedding;
  private final HybridRetrievalPolicy fusion = new HybridRetrievalPolicy();

  public AuthorizedRetrievalService(RetrievalSearchPort search, EmbeddingPort embedding) {
    this.search = search;
    this.embedding = embedding;
  }

  public RetrievalOutcome retrieve(IdentityContext identity, PrecheckContext context) {
    return retrieve(identity, context, EvaluationFault.NONE);
  }

  public RetrievalOutcome retrieveForEvaluation(
      IdentityContext identity, PrecheckContext context, EvaluationFault fault) {
    return retrieve(identity, context, fault);
  }

  private RetrievalOutcome retrieve(
      IdentityContext identity, PrecheckContext context, EvaluationFault fault) {
    var retrievalStarted = System.nanoTime();
    if (!identity.hasRole(Role.PRECHECK_USER)
        || !identity.canAccessProductLine(context.productLine().code())) {
      throw new IllegalArgumentException("identity is not authorized for retrieval context");
    }
    var query = query(context);
    var productLineScope = List.of(context.productLine().code());
    if (fault == EvaluationFault.ALL_RETRIEVAL_UNAVAILABLE) {
      return unavailable("FTS_UNAVAILABLE", "模拟数据：评估场景注入全文检索不可用", elapsed(retrievalStarted));
    }
    final List<RetrievalCandidate> ftsCandidates;
    try {
      ftsCandidates = search.searchFts(query, productLineScope);
    } catch (DataAccessException exception) {
      return unavailable("FTS_UNAVAILABLE", "模拟数据：全文检索暂时不可用", elapsed(retrievalStarted));
    }
    var embeddingStarted = System.nanoTime();
    if (fault == EvaluationFault.EMBEDDING_UNAVAILABLE) {
      return outcome(
          "FTS_ONLY",
          new Capability(true, "FTS_AVAILABLE", "模拟数据：全文检索可用"),
          new Capability(false, "EMBEDDING_UNAVAILABLE", "模拟数据：评估场景注入本地向量检索不可用"),
          fusion.fuse(ftsCandidates, List.of()),
          elapsed(retrievalStarted),
          elapsed(embeddingStarted));
    }
    try {
      var queryVector = embedding.embedQueries(List.of(query)).getFirst();
      var vectorCandidates = search.searchVector(queryVector, productLineScope);
      var result = fusion.fuse(ftsCandidates, vectorCandidates);
      return outcome(
          "HYBRID",
          new Capability(true, "FTS_AVAILABLE", "模拟数据：全文检索可用"),
          new Capability(true, "EMBEDDING_AVAILABLE", "模拟数据：本地向量检索可用"),
          result,
          elapsed(retrievalStarted),
          elapsed(embeddingStarted));
    } catch (EmbeddingException | DataAccessException exception) {
      var result = fusion.fuse(ftsCandidates, List.of());
      return outcome(
          "FTS_ONLY",
          new Capability(true, "FTS_AVAILABLE", "模拟数据：全文检索可用"),
          new Capability(false, "EMBEDDING_UNAVAILABLE", "模拟数据：本地向量检索暂时不可用"),
          result,
          elapsed(retrievalStarted),
          elapsed(embeddingStarted));
    }
  }

  private RetrievalOutcome unavailable(String code, String message, long retrievalDurationMs) {
    return new RetrievalOutcome(
        "UNAVAILABLE",
        new Capability(false, code, message),
        new Capability(false, "EMBEDDING_NOT_ATTEMPTED", "模拟数据：全文检索不可用时不执行向量召回"),
        fusion.fuse(List.of(), List.of()),
        List.of(),
        retrievalDurationMs,
        0);
  }

  private RetrievalOutcome outcome(
      String mode,
      Capability fts,
      Capability embedding,
      HybridRetrievalPolicy.FusionResult result,
      long retrievalDurationMs,
      long embeddingDurationMs) {
    var evidence = new ArrayList<EvidenceSnapshot>();
    for (var selected : result.selected()) {
      var candidate = selected.candidate();
      evidence.add(
          new EvidenceSnapshot(
              UUID.randomUUID(),
              candidate.documentId(),
              candidate.versionId(),
              candidate.chunkId(),
              candidate.productLineCode(),
              candidate.title(),
              excerpt(candidate.text()),
              candidate.contentHash(),
              mode,
              selected.ftsRank(),
              selected.vectorRank(),
              selected.rrfScore(),
              selected.selectedRank()));
    }
    return new RetrievalOutcome(
        mode,
        fts,
        embedding,
        result,
        List.copyOf(evidence),
        retrievalDurationMs,
        embeddingDurationMs);
  }

  private long elapsed(long started) {
    return Math.max(
        0, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
  }

  public enum EvaluationFault {
    NONE,
    EMBEDDING_UNAVAILABLE,
    ALL_RETRIEVAL_UNAVAILABLE
  }

  private String query(PrecheckContext context) {
    var values = new ArrayList<String>();
    values.add(context.title());
    values.add(context.descriptionPlainText());
    if (context.product() != null) values.add(context.product().code());
    if (context.component() != null) values.add(context.component().code());
    if (context.version() != null) values.add(context.version());
    context.additionalInformation().forEach(item -> values.add(item.value()));
    return String.join(" ", values).trim();
  }

  private String excerpt(String text) {
    var normalized = text.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
  }
}
