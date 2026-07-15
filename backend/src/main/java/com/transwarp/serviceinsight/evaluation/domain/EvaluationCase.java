package com.transwarp.serviceinsight.evaluation.domain;

import java.util.List;

public record EvaluationCase(
    String caseId,
    String datasetVersion,
    List<String> languageTags,
    List<String> scenarioTags,
    PrecheckInput precheckInput,
    List<EvaluationTurn> turns,
    String executionIdentityCode,
    List<String> allowedProductLineCodes,
    List<String> expectedEvidenceIds,
    List<String> forbiddenEvidenceIds,
    RetrievalMode expectedRetrievalMode,
    Degradation expectedDegradation,
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

  public record PrecheckInput(
      String sourceSystem,
      String hostRequestId,
      String formSchemaVersion,
      String issueType,
      String productLineCode,
      String productCode,
      String componentCode,
      String version,
      String severity,
      String serviceType,
      String title,
      String descriptionPlainText,
      List<AdditionalInformation> additionalInformation,
      String impactScope,
      List<AttachmentMetadata> attachments) {
    public PrecheckInput {
      additionalInformation = List.copyOf(additionalInformation);
      attachments = List.copyOf(attachments);
    }
  }

  public record AdditionalInformation(String fieldCode, String displayName, String value) {}

  public record AttachmentMetadata(
      String attachmentId, String fileName, String mediaType, long sizeBytes) {}

  public record EvaluationTurn(int runSequence, String descriptionPlainText) {}

  public enum RetrievalMode {
    HYBRID,
    FTS_ONLY,
    NONE
  }

  public enum Degradation {
    NONE,
    FTS_ONLY,
    UNAVAILABLE
  }
}
