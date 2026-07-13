package com.transwarp.serviceinsight.precheck.dto;

public record DegradationResponse(boolean degraded, String code, String message) {}
