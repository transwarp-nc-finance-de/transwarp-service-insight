package com.transwarp.serviceinsight.evaluation.port;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationSet;
import com.transwarp.serviceinsight.evaluation.domain.EvidenceFixtureManifest;

public interface EvaluationSetCatalog {
  EvaluationSet load(String version);

  EvidenceFixtureManifest loadEvidenceManifest(String version);
}
