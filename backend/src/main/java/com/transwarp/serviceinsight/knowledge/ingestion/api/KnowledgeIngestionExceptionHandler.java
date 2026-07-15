package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.identity.api.V2ApiError;
import com.transwarp.serviceinsight.identity.api.V2FieldError;
import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.application.UnauthenticatedException;
import com.transwarp.serviceinsight.knowledge.governance.api.KnowledgeGovernanceController;
import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    assignableTypes = {
      KnowledgeIngestionController.class,
      ParseTaskController.class,
      ParsePreviewController.class,
      KnowledgeGovernanceController.class
    })
public class KnowledgeIngestionExceptionHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(KnowledgeIngestionExceptionHandler.class);

  @ExceptionHandler(KnowledgeApiException.class)
  ResponseEntity<V2ApiError> knowledge(KnowledgeApiException exception) {
    return response(
        exception.status(),
        exception.code(),
        exception.getMessage(),
        exception.fieldErrors(),
        exception.retryable(),
        exception.safeDetails());
  }

  @ExceptionHandler(UnauthenticatedException.class)
  ResponseEntity<V2ApiError> unauthenticated() {
    return response(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHENTICATED",
        "请重新建立本地模拟会话",
        List.of(),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(CsrfValidationFailedException.class)
  ResponseEntity<V2ApiError> csrf() {
    return response(
        HttpStatus.FORBIDDEN,
        "CSRF_VALIDATION_FAILED",
        "CSRF 校验失败，请恢复当前会话 Token 后重试",
        List.of(),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<V2ApiError> validation(MethodArgumentNotValidException exception) {
    var fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new V2FieldError(error.getField(), "INVALID", safe(error.getDefaultMessage())))
            .toList();
    return response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        fieldErrors,
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<V2ApiError> unavailable(Exception exception) {
    var requestId = UUID.randomUUID().toString();
    LOGGER.error(
        "Knowledge ingestion failed safely, requestId={}, type={}",
        requestId,
        exception.getClass().getSimpleName());
    return response(
        HttpStatus.SERVICE_UNAVAILABLE,
        "SERVICE_UNAVAILABLE",
        "知识处理服务暂时不可用",
        List.of(),
        true,
        Map.of("mockData", true),
        requestId);
  }

  private ResponseEntity<V2ApiError> response(
      HttpStatus status,
      String code,
      String message,
      List<V2FieldError> fieldErrors,
      boolean retryable,
      Map<String, Object> safeDetails) {
    return response(
        status, code, message, fieldErrors, retryable, safeDetails, UUID.randomUUID().toString());
  }

  private ResponseEntity<V2ApiError> response(
      HttpStatus status,
      String code,
      String message,
      List<V2FieldError> fieldErrors,
      boolean retryable,
      Map<String, Object> safeDetails,
      String requestId) {
    return ResponseEntity.status(status)
        .body(
            new V2ApiError(
                code, message, requestId, Instant.now(), fieldErrors, retryable, safeDetails));
  }

  private String safe(String message) {
    return message == null ? "字段不合法" : message;
  }
}
