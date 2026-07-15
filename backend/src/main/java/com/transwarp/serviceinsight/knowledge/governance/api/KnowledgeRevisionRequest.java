package com.transwarp.serviceinsight.knowledge.governance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeRevisionRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull @Valid CatalogValueRequest productLine,
    @NotBlank @Size(max = 5_000_000) String cleanedText,
    @NotNull @Size(max = 100) List<@Size(max = 1000) String> parseWarningNotes) {
  public record CatalogValueRequest(
      @NotBlank @Size(max = 100) String code, @NotBlank @Size(max = 200) String displayName) {}
}
