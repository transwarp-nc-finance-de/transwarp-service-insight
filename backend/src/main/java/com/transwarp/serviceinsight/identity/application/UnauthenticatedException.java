package com.transwarp.serviceinsight.identity.application;

public class UnauthenticatedException extends RuntimeException {
  public UnauthenticatedException() {
    super("Local mock session is missing or expired");
  }
}
