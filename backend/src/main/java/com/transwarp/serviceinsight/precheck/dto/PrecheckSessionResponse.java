package com.transwarp.serviceinsight.precheck.dto;

import java.util.List;

public record PrecheckSessionResponse(
    String sessionId, String precheckId, String status, List<RunSummaryResponse> runs) {}
