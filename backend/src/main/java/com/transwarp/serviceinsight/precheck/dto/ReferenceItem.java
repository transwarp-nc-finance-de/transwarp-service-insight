package com.transwarp.serviceinsight.precheck.dto;

public record ReferenceItem(
    String sourceType, String title, String excerpt, String url, boolean mockData) {}
