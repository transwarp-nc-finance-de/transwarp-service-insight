package com.transwarp.serviceinsight.identity.application;

public class CsrfValidationFailedException extends RuntimeException {
  public CsrfValidationFailedException() {
    super("CSRF validation failed");
  }
}
