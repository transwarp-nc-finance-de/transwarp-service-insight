package com.transwarp.serviceinsight.evaluation.port;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationCase.Degradation;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;

public interface EvaluationRetrievalPort {
  RetrievalOutcome retrieve(
      IdentityContext identity, PrecheckContext context, Degradation degradationScenario);
}
