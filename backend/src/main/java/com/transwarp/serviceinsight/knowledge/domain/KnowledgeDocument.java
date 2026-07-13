package com.transwarp.serviceinsight.knowledge.domain;

import java.util.UUID;

public record KnowledgeDocument(
    UUID id,
    String title,
    DataClassification dataClassification,
    SourceType sourceType,
    AuthorizationStatus authorizationStatus,
    SensitivityLevel sensitivityLevel) {
  public KnowledgeDocument {
    if (id == null || title == null || title.isBlank())
      throw new IllegalArgumentException("知识文档标识和标题不能为空");
  }

  public KnowledgeDocument(UUID id, String title, boolean mockData) {
    this(
        id,
        title,
        mockData ? DataClassification.MOCK : DataClassification.INTERNAL,
        SourceType.MOCK,
        mockData ? AuthorizationStatus.APPROVED : AuthorizationStatus.PENDING,
        SensitivityLevel.PUBLIC);
  }

  public enum DataClassification {
    MOCK,
    PUBLIC,
    INTERNAL
  }

  public enum SourceType {
    PRODUCT_MANUAL,
    WIKI,
    TRAINING_MATERIAL,
    USER_MANUAL,
    HISTORICAL_SLA,
    OPEN_SOURCE_REFERENCE,
    AIOPS_CONTEXT,
    MOCK
  }

  public enum AuthorizationStatus {
    PENDING,
    APPROVED,
    REJECTED
  }

  public enum SensitivityLevel {
    PUBLIC,
    DESENSITIZED,
    RESTRICTED
  }
}
