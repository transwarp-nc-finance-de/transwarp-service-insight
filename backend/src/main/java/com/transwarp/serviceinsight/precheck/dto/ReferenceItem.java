package com.transwarp.serviceinsight.precheck.dto;

public record ReferenceItem(
    ReferenceSourceType sourceType, String title, String excerpt, String url, boolean mockData) {}
