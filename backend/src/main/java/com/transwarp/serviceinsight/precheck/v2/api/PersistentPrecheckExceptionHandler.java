package com.transwarp.serviceinsight.precheck.v2.api;

import com.transwarp.serviceinsight.identity.api.V2ApiError;
import com.transwarp.serviceinsight.identity.api.V2FieldError;
import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.application.UnauthenticatedException;
import com.transwarp.serviceinsight.precheck.retrieval.api.EvidenceController;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    assignableTypes = {
      PersistentPrecheckController.class,
      CompletenessPolicyController.class,
      EvidenceController.class
    })
public class PersistentPrecheckExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<V2ApiError> validation(MethodArgumentNotValidException exception) {
    var errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new V2FieldError(error.getField(), "INVALID", "字段不合法"))
            .toList();
    return response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        errors,
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<V2ApiError> unreadable(HttpMessageNotReadableException exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        List.of(),
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
        "CSRF 校验失败",
        List.of(),
        false,
        Map.of("mockData", true));
  }

  @ExceptionHandler(PrecheckV2Exception.class)
  ResponseEntity<V2ApiError> precheck(PrecheckV2Exception exception) {
    return response(
        exception.status(),
        exception.code(),
        exception.getMessage(),
        List.of(),
        false,
        exception.safeDetails());
  }

  private ResponseEntity<V2ApiError> response(
      HttpStatus status,
      String code,
      String message,
      List<V2FieldError> errors,
      boolean retryable,
      Map<String, Object> details) {
    return ResponseEntity.status(status)
        .body(
            new V2ApiError(
                code,
                message,
                UUID.randomUUID().toString(),
                Instant.now(),
                errors,
                retryable,
                details));
  }
}
