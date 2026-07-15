package com.transwarp.serviceinsight.identity.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAuthSessionRequest(
    @NotBlank @Size(max = 100) String userCode, @NotNull @AssertTrue Boolean mockData) {}
