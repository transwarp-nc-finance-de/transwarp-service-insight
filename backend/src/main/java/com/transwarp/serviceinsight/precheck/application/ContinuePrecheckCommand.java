package com.transwarp.serviceinsight.precheck.application;

import java.util.UUID;

public record ContinuePrecheckCommand(UUID precheckId, String message) {}
