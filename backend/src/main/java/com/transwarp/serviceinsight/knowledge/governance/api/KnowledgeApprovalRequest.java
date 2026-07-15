package com.transwarp.serviceinsight.knowledge.governance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record KnowledgeApprovalRequest(
    @NotBlank @Pattern(regexp = "^sha256:[0-9a-f]{64}$") String parseResultHash,
    @NotNull @Size(max = 4)
        Set<
                @Pattern(
                    regexp =
                        "^(IMAGE_CONTENT_IGNORED|TABLE_STRUCTURE_FLATTENED|READING_ORDER_UNCERTAIN|CONTENT_LOSS_SUSPECTED)$")
                String>
            acknowledgedWarningCodes,
    @Size(max = 1000) String reason) {}
