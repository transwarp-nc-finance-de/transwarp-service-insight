package com.transwarp.serviceinsight.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record V2ApiError(
    String code,
    String message,
    String requestId,
    Instant timestamp,
    List<V2FieldError> fieldErrors,
    boolean retryable,
    Map<String, Object> safeDetails) {}
