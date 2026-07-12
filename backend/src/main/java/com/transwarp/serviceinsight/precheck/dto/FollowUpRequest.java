package com.transwarp.serviceinsight.precheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FollowUpRequest(
    @NotNull(message = "precheckId 不能为空") UUID precheckId,
    @NotBlank(message = "message 不能为空") @Size(max = 5000, message = "message 长度不能超过 5000 个字符")
        String message) {}
