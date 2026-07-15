package com.transwarp.serviceinsight.knowledge.governance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequiredCommandReasonRequest(@NotBlank @Size(max = 1000) String reason) {}
