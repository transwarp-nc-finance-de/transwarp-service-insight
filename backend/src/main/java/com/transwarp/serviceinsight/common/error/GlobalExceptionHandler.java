package com.transwarp.serviceinsight.common.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
    var traceId = UUID.randomUUID().toString();
    var errors = new LinkedHashMap<String, String>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "请求参数校验失败", errors, Instant.now(), traceId));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception exception) {
    var traceId = UUID.randomUUID().toString();
    LOGGER.error("Unexpected precheck failure, traceId={}", traceId, exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ApiError(
                "INTERNAL_ERROR", "预诊服务暂时不可用，请稍后重试或继续人工提交", Map.of(), Instant.now(), traceId));
  }
}
