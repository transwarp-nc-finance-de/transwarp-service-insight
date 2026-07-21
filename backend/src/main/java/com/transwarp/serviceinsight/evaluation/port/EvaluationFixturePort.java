package com.transwarp.serviceinsight.evaluation.port;

import com.transwarp.serviceinsight.evaluation.domain.EvidenceFixtureManifest;

public interface EvaluationFixturePort {
  void publish(EvidenceFixtureManifest manifest);
}
