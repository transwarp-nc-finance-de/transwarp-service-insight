package com.transwarp.serviceinsight.knowledge.ingestion.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentMetadataRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull @Valid CatalogValueRequest productLine,
    @NotBlank String sourceType,
    @NotNull Boolean mockData) {
  @AssertTrue(message = "仅允许模拟数据")
  public boolean isMockDataConfirmed() {
    return Boolean.TRUE.equals(mockData);
  }

  public record CatalogValueRequest(
      @NotBlank @Size(max = 100) String code, @NotBlank @Size(max = 200) String displayName) {}
}
