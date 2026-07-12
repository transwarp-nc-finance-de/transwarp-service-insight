package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.precheck.domain.PrecheckResult;

public interface CreatePrecheckUseCase {
  PrecheckResult create(CreatePrecheckCommand command);
}
