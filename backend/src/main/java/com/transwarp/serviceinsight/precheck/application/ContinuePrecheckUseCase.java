package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;

public interface ContinuePrecheckUseCase {
  FollowUpResult continuePrecheck(ContinuePrecheckCommand command);
}
