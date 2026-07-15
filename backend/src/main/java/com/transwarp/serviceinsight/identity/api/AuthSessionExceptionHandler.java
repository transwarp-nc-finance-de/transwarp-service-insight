package com.transwarp.serviceinsight.identity.api;

import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.application.UnauthenticatedException;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthSessionController.class)
public class AuthSessionExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthSessionExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<V2ApiError> validation(MethodArgumentNotValidException exception) {
    var fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new V2FieldError(
                        error.getField(), "INVALID", safeMessage(error.getDefaultMessage())))
            .toList();
    return response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        fieldErrors,
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<V2ApiError> unreadable(HttpMessageNotReadableException exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        List.of(new V2FieldError("request", "INVALID", "请求格式或字段类型不正确")),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(UnauthenticatedException.class)
  ResponseEntity<V2ApiError> unauthenticated(UnauthenticatedException exception) {
    return response(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHENTICATED",
        "请重新建立本地模拟会话",
        List.of(),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(CsrfValidationFailedException.class)
  ResponseEntity<V2ApiError> csrf(CsrfValidationFailedException exception) {
    return response(
        HttpStatus.FORBIDDEN,
        "CSRF_VALIDATION_FAILED",
        "CSRF 校验失败，请恢复当前会话 Token 后重试",
        List.of(),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<V2ApiError> unavailable(Exception exception) {
    var requestId = UUID.randomUUID().toString();
    LOGGER.error("Local identity service unavailable, requestId={}", requestId, exception);
    return response(
        HttpStatus.SERVICE_UNAVAILABLE,
        "SERVICE_UNAVAILABLE",
        "本地身份服务暂时不可用",
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

  private String safeMessage(String message) {
    return message == null ? "字段不合法" : message;
  }
}
