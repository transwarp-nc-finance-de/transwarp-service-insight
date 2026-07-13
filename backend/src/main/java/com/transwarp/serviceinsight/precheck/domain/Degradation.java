package com.transwarp.serviceinsight.precheck.domain;

public record Degradation(boolean degraded, String code, String message) {
  public static Degradation mock(String message) {
    return new Degradation(true, "DETERMINISTIC_MOCK", message);
  }
}
