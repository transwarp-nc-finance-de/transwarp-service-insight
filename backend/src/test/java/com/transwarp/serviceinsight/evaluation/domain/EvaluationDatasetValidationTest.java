package com.transwarp.serviceinsight.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EvaluationDatasetValidationTest {
  private static final String VERSION = "mock-eval-v1";
  private static final String ROOT = "evaluation/mock-eval-v1/";
  private static final Set<String> REQUIRED_LANGUAGE_TAGS =
      Set.of("LANG_ZH", "LANG_EN", "LANG_MIXED");
  private static final Set<String> REQUIRED_SCENARIO_TAGS =
      Set.of(
          "EXACT_TERM",
          "SEMANTIC_REWRITE",
          "INSUFFICIENT_EVIDENCE",
          "PERMISSION_ISOLATION",
          "EMBEDDING_DEGRADATION",
          "FTS_ONLY",
          "UNAVAILABLE",
          "MULTI_RUN",
          "CITATION_VERIFICATION",
          "CROSS_LANGUAGE");
  private static final Pattern ABSOLUTE_PATH =
      Pattern.compile("(?i)([a-z]:[/\\\\]|/(home|users|var|etc|opt)/)");
  private static final Pattern SECRET =
      Pattern.compile("(?i)(password|secret|api[_-]?key|private[_-]?key|access[_-]?token)\\s*[:=]");

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static EvaluationSet dataset;
  private static EvidenceFixtureManifest manifest;
  private static JsonNode datasetTree;

  @BeforeAll
  static void loadAssets() throws Exception {
    var catalog = new ClasspathEvaluationSetCatalog(MAPPER);
    dataset = catalog.load(VERSION);
    manifest = catalog.loadEvidenceManifest(VERSION);
    datasetTree = readTree("dataset.json");
  }

  @Test
  void datasetHasStableUniqueCasesAndRequiredMockMarkers() {
    assertThat(dataset.datasetVersion()).isEqualTo(VERSION);
    assertThat(dataset.mockData()).isTrue();
    assertThat(dataset.cases()).hasSizeGreaterThanOrEqualTo(30);

    var caseIds = dataset.cases().stream().map(EvaluationCase::caseId).toList();
    assertThat(caseIds).doesNotHaveDuplicates().isSorted();
    assertThat(caseIds).allMatch(caseId -> caseId.matches("mock-eval-v1-\\d{3}"));
    assertThat(dataset.cases())
        .allSatisfy(
            item -> {
              assertThat(item.datasetVersion()).isEqualTo(VERSION);
              assertThat(item.mockData()).isTrue();
              assertThat(item.turns()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
              assertThat(item.turns().stream().map(EvaluationCase.EvaluationTurn::runSequence))
                  .containsExactlyElementsOf(
                      java.util.stream.IntStream.rangeClosed(1, item.turns().size())
                          .boxed()
                          .toList());
              assertThat(item.turns())
                  .allSatisfy(turn -> assertThat(turn.descriptionPlainText()).startsWith("模拟数据："));
            });
  }

  @Test
  void requiredLanguageScenarioAndDegradationCoverageIsPresent() {
    var languageTags =
        dataset.cases().stream()
            .flatMap(item -> item.languageTags().stream())
            .collect(Collectors.toSet());
    var scenarioTags =
        dataset.cases().stream()
            .flatMap(item -> item.scenarioTags().stream())
            .collect(Collectors.toSet());

    assertThat(languageTags).containsAll(REQUIRED_LANGUAGE_TAGS);
    assertThat(scenarioTags).containsAll(REQUIRED_SCENARIO_TAGS);
    assertThat(dataset.cases().stream().map(EvaluationCase::expectedDegradation))
        .contains("FTS_ONLY", "UNAVAILABLE");
    assertThat(dataset.cases().stream().filter(item -> item.scenarioTags().contains("MULTI_RUN")))
        .allMatch(item -> item.turns().size() > 1);
  }

  @Test
  void identitiesProductLinesAndEvidenceReferencesResolve() {
    var identities =
        manifest.executionIdentities().stream()
            .collect(
                Collectors.toMap(
                    EvidenceFixtureManifest.ExecutionIdentityFixture::identityCode,
                    Function.identity()));
    var evidence =
        manifest.evidenceFixtures().stream()
            .collect(
                Collectors.toMap(
                    EvidenceFixtureManifest.EvidenceFixture::evidenceId, Function.identity()));

    assertThat(manifest.datasetVersion()).isEqualTo(VERSION);
    assertThat(manifest.mockData()).isTrue();
    assertThat(identities.keySet()).doesNotHaveDuplicates();
    assertThat(evidence.keySet()).doesNotHaveDuplicates();
    assertThat(manifest.executionIdentities())
        .allSatisfy(identity -> assertThat(identity.mockData()).isTrue());
    assertThat(manifest.evidenceFixtures())
        .allSatisfy(
            fixture -> {
              assertThat(fixture.mockData()).isTrue();
              assertThat(fixture.title()).startsWith("模拟数据：");
              assertThat(fixture.excerpt()).startsWith("模拟数据：");
            });

    assertThat(dataset.cases())
        .allSatisfy(
            item -> {
              assertThat(identities).containsKey(item.executionIdentityCode());
              var identity = identities.get(item.executionIdentityCode());
              assertThat(identity.productLineCodes()).containsAll(item.allowedProductLineCodes());
              var overlappingEvidence = new HashSet<>(item.expectedEvidenceIds());
              overlappingEvidence.retainAll(item.forbiddenEvidenceIds());
              assertThat(overlappingEvidence).isEmpty();
              item.expectedEvidenceIds()
                  .forEach(evidenceId -> assertThat(evidence).containsKey(evidenceId));
              item.forbiddenEvidenceIds()
                  .forEach(evidenceId -> assertThat(evidence).containsKey(evidenceId));
              item.expectedEvidenceIds()
                  .forEach(
                      evidenceId ->
                          assertThat(item.allowedProductLineCodes())
                              .contains(evidence.get(evidenceId).productLineCode()));
            });
  }

  @Test
  void permissionIsolationCasesContainEvidenceThatTheIdentityMustReject() {
    var identities =
        manifest.executionIdentities().stream()
            .collect(
                Collectors.toMap(
                    EvidenceFixtureManifest.ExecutionIdentityFixture::identityCode,
                    Function.identity()));
    var evidence =
        manifest.evidenceFixtures().stream()
            .collect(
                Collectors.toMap(
                    EvidenceFixtureManifest.EvidenceFixture::evidenceId, Function.identity()));

    var permissionCases =
        dataset.cases().stream()
            .filter(item -> item.scenarioTags().contains("PERMISSION_ISOLATION"))
            .toList();
    assertThat(permissionCases).isNotEmpty();
    assertThat(permissionCases)
        .allSatisfy(
            item -> {
              assertThat(item.forbiddenEvidenceIds()).isNotEmpty();
              var allowedByIdentity =
                  identities.get(item.executionIdentityCode()).productLineCodes();
              item.forbiddenEvidenceIds()
                  .forEach(
                      evidenceId ->
                          assertThat(allowedByIdentity)
                              .doesNotContain(evidence.get(evidenceId).productLineCode()));
            });
  }

  @Test
  void checkedInCoverageMatrixExactlyMatchesDatasetTags() throws Exception {
    var actual = new LinkedHashMap<String, Map<String, List<String>>>();
    actual.put("language", groupByTags(EvaluationCase::languageTags));
    actual.put("scenario", groupByTags(EvaluationCase::scenarioTags));
    actual.put(
        "degradation",
        dataset.cases().stream()
            .filter(item -> !"NONE".equals(item.expectedDegradation()))
            .collect(
                Collectors.groupingBy(
                    EvaluationCase::expectedDegradation,
                    TreeMap::new,
                    Collectors.mapping(EvaluationCase::caseId, Collectors.toList()))));

    var matrix = readTree("coverage-matrix.json");
    assertThat(matrix.path("datasetVersion").asText()).isEqualTo(VERSION);
    assertThat(matrix.path("mockData").asBoolean()).isTrue();
    assertThat(matrix.path("dimensions")).isEqualTo(MAPPER.valueToTree(actual));
  }

  @Test
  void schemasDeclareStrictVersionedBoundaries() throws Exception {
    var datasetSchema = readTree("dataset.schema.json");
    var manifestSchema = readTree("evidence-fixture-manifest.schema.json");

    assertThat(datasetSchema.path("additionalProperties").asBoolean()).isFalse();
    assertThat(datasetSchema.at("/properties/datasetVersion/const").asText()).isEqualTo(VERSION);
    assertThat(datasetSchema.at("/properties/cases/minItems").asInt()).isEqualTo(30);
    assertThat(datasetSchema.at("/$defs/case/additionalProperties").asBoolean()).isFalse();
    assertThat(datasetSchema.at("/$defs/case/properties/turns/maxItems").asInt()).isEqualTo(3);
    assertThat(manifestSchema.path("additionalProperties").asBoolean()).isFalse();
    assertThat(manifestSchema.at("/properties/datasetVersion/const").asText()).isEqualTo(VERSION);
    assertThat(manifestSchema.at("/$defs/evidence/additionalProperties").asBoolean()).isFalse();
  }

  @Test
  void assetsContainNoAbsolutePathsCredentialsOrUnmarkedFixtureText() throws Exception {
    var assetText =
        String.join(
            "\n",
            readText("dataset.json"),
            readText("evidence-fixture-manifest.json"),
            readText("coverage-matrix.json"));

    assertThat(ABSOLUTE_PATH.matcher(assetText).find()).isFalse();
    assertThat(SECRET.matcher(assetText).find()).isFalse();
    assertThat(assetText).doesNotContain("BEGIN PRIVATE KEY", "真实客户", "真实工单");
  }

  @Test
  void canonicalSerializationAndChecksumAreStable() throws Exception {
    var canonicalBytes = MAPPER.writeValueAsBytes(canonicalize(datasetTree));
    var secondSerialization = MAPPER.writeValueAsBytes(canonicalize(readTree("dataset.json")));
    var actualChecksum =
        "sha256:"
            + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalBytes));
    var expectedChecksum = readText("dataset.sha256").trim();

    assertThat(secondSerialization).containsExactly(canonicalBytes);
    assertThat(actualChecksum).isEqualTo(expectedChecksum);
    assertThat(manifest.executionIdentities().stream().map(item -> item.identityCode()).toList())
        .isSorted();
    assertThat(manifest.evidenceFixtures().stream().map(item -> item.evidenceId()).toList())
        .isSorted();
  }

  private static Map<String, List<String>> groupByTags(
      Function<EvaluationCase, List<String>> tags) {
    var result = new TreeMap<String, List<String>>();
    dataset
        .cases()
        .forEach(
            item ->
                tags.apply(item)
                    .forEach(
                        tag ->
                            result
                                .computeIfAbsent(tag, ignored -> new ArrayList<>())
                                .add(item.caseId())));
    return result;
  }

  private static JsonNode canonicalize(JsonNode node) {
    if (node.isObject()) {
      var sorted = MAPPER.createObjectNode();
      var fields = new TreeMap<String, JsonNode>();
      node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
      fields.forEach((key, value) -> sorted.set(key, canonicalize(value)));
      return sorted;
    }
    if (node.isArray()) {
      var array = MAPPER.createArrayNode();
      node.forEach(item -> array.add(canonicalize(item)));
      return array;
    }
    return node;
  }

  private static JsonNode readTree(String fileName) throws Exception {
    return MAPPER.readTree(readText(fileName));
  }

  private static String readText(String fileName) throws Exception {
    try (var input = new ClassPathResource(ROOT + fileName).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
