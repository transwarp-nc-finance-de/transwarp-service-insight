package com.transwarp.serviceinsight.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EvaluationSetCatalogTest {
  private final ClasspathEvaluationSetCatalog catalog =
      new ClasspathEvaluationSetCatalog(new ObjectMapper());

  @Test
  void loadsTheVersionedDatasetAndFixtureManifest() {
    var evaluationSet = catalog.load("mock-eval-v1");
    var manifest = catalog.loadEvidenceManifest("mock-eval-v1");

    assertThat(evaluationSet.datasetVersion()).isEqualTo("mock-eval-v1");
    assertThat(evaluationSet.mockData()).isTrue();
    assertThat(evaluationSet.cases()).hasSize(30);
    assertThat(manifest.datasetVersion()).isEqualTo(evaluationSet.datasetVersion());
    assertThat(manifest.evidenceFixtures()).isNotEmpty();
  }

  @Test
  void rejectsUnknownVersionsInsteadOfSilentlyLoadingTheDefault() {
    assertThatThrownBy(() -> catalog.load("mock-eval-v2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mock-eval-v2");
  }
}
