package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;
import java.util.UUID;

public interface ContinueSessionPrecheckUseCase {
  FollowUpResult continueSession(UUID sessionId, String message);
}
