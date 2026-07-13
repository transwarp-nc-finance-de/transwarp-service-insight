package com.transwarp.serviceinsight.precheck.domain;

public record ExecutionMetadata(
    String policyVersion, String promptVersion, String modelVersion, String indexVersion) {}
