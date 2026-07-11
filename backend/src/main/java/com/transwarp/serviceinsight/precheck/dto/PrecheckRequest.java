package com.transwarp.serviceinsight.precheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrecheckRequest(
    @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题不能超过 200 个字符") String title,
    @NotBlank(message = "问题描述不能为空") @Size(max = 10000, message = "问题描述不能超过 10000 个字符")
        String description,
    @Size(max = 100, message = "产品不能超过 100 个字符") String product,
    @Size(max = 100, message = "模块不能超过 100 个字符") String module,
    @Size(max = 100, message = "版本不能超过 100 个字符") String version,
    @Size(max = 100, message = "紧急程度不能超过 100 个字符") String severity,
    @Size(max = 2000, message = "影响范围不能超过 2000 个字符") String impactScope,
    @Size(max = 5000, message = "附件摘要不能超过 5000 个字符") String attachmentsSummary) {}
