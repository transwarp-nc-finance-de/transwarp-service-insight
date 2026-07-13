package com.transwarp.serviceinsight.precheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionRunRequest(
    @NotBlank(message = "补充信息不能为空") @Size(max = 5000, message = "补充信息不能超过 5000 个字符")
        String message) {}
