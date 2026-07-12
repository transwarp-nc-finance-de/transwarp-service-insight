package com.transwarp.serviceinsight.policy.infrastructure.mock;

import com.transwarp.serviceinsight.policy.domain.PolicySnapshot;
import com.transwarp.serviceinsight.policy.port.PolicyContextPort;
import org.springframework.stereotype.Component;

@Component
public class MockPolicyContextAdapter implements PolicyContextPort {
  public static final String POLICY_VERSION = "mock-policy-v1";

  @Override
  public PolicySnapshot currentSnapshot() {
    return new PolicySnapshot("mock-user", POLICY_VERSION, true);
  }
}
