package com.transwarp.serviceinsight.precheck.dto;

public record RunSummaryResponse(String runId, int sequence, String status, String inputSummary) {}
