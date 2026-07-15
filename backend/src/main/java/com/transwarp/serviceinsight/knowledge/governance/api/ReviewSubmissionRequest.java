package com.transwarp.serviceinsight.knowledge.governance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewSubmissionRequest(
    @NotBlank @Pattern(regexp = "^sha256:[0-9a-f]{64}$") String parseResultHash,
    @Size(max = 1000) String reason) {}
