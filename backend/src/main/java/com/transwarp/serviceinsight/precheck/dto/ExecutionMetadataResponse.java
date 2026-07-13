package com.transwarp.serviceinsight.precheck.dto;

public record ExecutionMetadataResponse(
    String policyVersion, String promptVersion, String modelVersion, String indexVersion) {}
