package com.transwarp.serviceinsight.precheck.domain;

public record Evidence(
    SourceType sourceType, String title, String excerpt, String url, boolean mockData) {
  public enum SourceType {
    PRODUCT_MANUAL,
    HISTORICAL_SLA
  }
}
