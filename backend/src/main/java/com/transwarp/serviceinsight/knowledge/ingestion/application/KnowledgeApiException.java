package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.identity.api.V2FieldError;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class KnowledgeApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;
  private final List<V2FieldError> fieldErrors;
  private final boolean retryable;
  private final Map<String, Object> safeDetails;

  public KnowledgeApiException(
      HttpStatus status,
      String code,
      String message,
      List<V2FieldError> fieldErrors,
      boolean retryable,
      Map<String, Object> safeDetails) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = List.copyOf(fieldErrors);
    this.retryable = retryable;
    this.safeDetails = Map.copyOf(safeDetails);
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public List<V2FieldError> fieldErrors() {
    return fieldErrors;
  }

  public boolean retryable() {
    return retryable;
  }

  public Map<String, Object> safeDetails() {
    return safeDetails;
  }
}
