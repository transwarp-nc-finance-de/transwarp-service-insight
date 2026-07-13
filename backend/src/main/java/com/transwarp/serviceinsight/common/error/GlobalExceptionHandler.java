package com.transwarp.serviceinsight.common.error;

import com.transwarp.serviceinsight.precheck.application.PrecheckSessionIdNotFoundException;
import com.transwarp.serviceinsight.precheck.application.PrecheckSessionNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadableRequest(HttpMessageNotReadableException exception) {
    var traceId = UUID.randomUUID().toString();
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                "VALIDATION_ERROR",
                "请求参数校验失败",
                Map.of("request", "请求格式或字段类型不正确"),
                Instant.now(),
                traceId));
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

  @ExceptionHandler({
    PrecheckSessionNotFoundException.class,
    PrecheckSessionIdNotFoundException.class
  })
  ResponseEntity<ApiError> sessionNotFound(RuntimeException exception) {
    var traceId = UUID.randomUUID().toString();
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new ApiError(
                "SESSION_NOT_FOUND",
                "预诊会话不存在或已失效，可重新发起预诊或继续人工提交",
                Map.of(),
                Instant.now(),
                traceId));
  }
}
