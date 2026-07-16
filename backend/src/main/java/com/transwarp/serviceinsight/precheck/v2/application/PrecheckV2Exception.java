package com.transwarp.serviceinsight.precheck.v2.application;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class PrecheckV2Exception extends RuntimeException {
  private final String code;
  private final HttpStatus status;
  private final Map<String, Object> safeDetails;

  public PrecheckV2Exception(
      String code, HttpStatus status, String message, Map<String, Object> safeDetails) {
    super(message);
    this.code = code;
    this.status = status;
    this.safeDetails = Map.copyOf(safeDetails);
  }

  public String code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public Map<String, Object> safeDetails() {
    return safeDetails;
  }
}
