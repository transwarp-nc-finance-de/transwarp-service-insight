package com.transwarp.serviceinsight.policy.port;

import com.transwarp.serviceinsight.policy.domain.PolicySnapshot;

public interface PolicyContextPort {
  PolicySnapshot currentSnapshot();
}
