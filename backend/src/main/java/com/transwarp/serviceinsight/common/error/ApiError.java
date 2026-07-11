package com.transwarp.serviceinsight.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    String code,
    String message,
    Map<String, String> fieldErrors,
    Instant timestamp,
    String traceId) {}
