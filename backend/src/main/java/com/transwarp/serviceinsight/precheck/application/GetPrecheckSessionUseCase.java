package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.precheck.domain.PrecheckSession;
import java.util.UUID;

public interface GetPrecheckSessionUseCase {
  PrecheckSession getSession(UUID sessionId);
}
