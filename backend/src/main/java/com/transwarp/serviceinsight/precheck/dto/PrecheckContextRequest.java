package com.transwarp.serviceinsight.precheck.dto;

import jakarta.validation.constraints.Size;

public record PrecheckContextRequest(
    @Size(max = 32) String sourceSystem,
    @Size(max = 100) String hostRequestId,
    @Size(max = 32) String formSchemaVersion) {}
