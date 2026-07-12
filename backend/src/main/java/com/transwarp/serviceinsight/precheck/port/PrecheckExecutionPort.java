package com.transwarp.serviceinsight.precheck.port;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckCommand;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;
import com.transwarp.serviceinsight.precheck.domain.PrecheckResult;

public interface PrecheckExecutionPort {
  PrecheckResult create(CreatePrecheckCommand command);

  FollowUpResult continuePrecheck(ContinuePrecheckCommand command);
}
