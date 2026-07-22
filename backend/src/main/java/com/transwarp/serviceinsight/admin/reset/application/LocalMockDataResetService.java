package com.transwarp.serviceinsight.admin.reset.application;

import com.transwarp.serviceinsight.admin.reset.port.LocalMockDataResetPort;
import com.transwarp.serviceinsight.evaluation.port.EvaluationFixturePort;
import com.transwarp.serviceinsight.evaluation.port.EvaluationSetCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalMockDataResetService {
  private final LocalMockDataResetPort resetPort;
  private final EvaluationSetCatalog evaluationCatalog;
  private final EvaluationFixturePort evaluationFixtures;

  public LocalMockDataResetService(
      LocalMockDataResetPort resetPort,
      EvaluationSetCatalog evaluationCatalog,
      EvaluationFixturePort evaluationFixtures) {
    this.resetPort = resetPort;
    this.evaluationCatalog = evaluationCatalog;
    this.evaluationFixtures = evaluationFixtures;
  }

  @Transactional
  public void rebuildDatabase() {
    resetPort.resetBusinessData();
    evaluationFixtures.publish(evaluationCatalog.loadEvidenceManifest("mock-eval-v1"));
  }
}
