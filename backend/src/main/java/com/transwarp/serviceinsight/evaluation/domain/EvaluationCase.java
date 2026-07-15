package com.transwarp.serviceinsight.evaluation.domain;

import java.util.List;

public record EvaluationCase(
    String caseId,
    String datasetVersion,
    List<String> languageTags,
    List<String> scenarioTags,
    List<EvaluationTurn> turns,
    String executionIdentityCode,
    List<String> allowedProductLineCodes,
    List<String> expectedEvidenceIds,
    List<String> forbiddenEvidenceIds,
    String expectedRetrievalMode,
    String expectedDegradation,
    List<String> expectedMissingFieldCodes,
    boolean mockData) {
  public EvaluationCase {
    languageTags = List.copyOf(languageTags);
    scenarioTags = List.copyOf(scenarioTags);
    turns = List.copyOf(turns);
    allowedProductLineCodes = List.copyOf(allowedProductLineCodes);
    expectedEvidenceIds = List.copyOf(expectedEvidenceIds);
    forbiddenEvidenceIds = List.copyOf(forbiddenEvidenceIds);
    expectedMissingFieldCodes = List.copyOf(expectedMissingFieldCodes);
  }

  public record EvaluationTurn(int runSequence, String descriptionPlainText) {}
}
