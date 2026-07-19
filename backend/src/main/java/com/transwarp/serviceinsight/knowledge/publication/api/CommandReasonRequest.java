package com.transwarp.serviceinsight.knowledge.publication.api;

import jakarta.validation.constraints.Size;

public record CommandReasonRequest(@Size(max = 1000) String reason) {}
