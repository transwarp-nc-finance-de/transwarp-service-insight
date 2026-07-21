package com.transwarp.serviceinsight.evaluation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationSet;
import com.transwarp.serviceinsight.evaluation.domain.EvidenceFixtureManifest;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathEvaluationSetCatalog
    implements com.transwarp.serviceinsight.evaluation.port.EvaluationSetCatalog {
  static final String SUPPORTED_VERSION = "mock-eval-v1";

  private final ObjectMapper objectMapper;

  public ClasspathEvaluationSetCatalog(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public EvaluationSet load(String version) {
    return read(version, "evaluation/mock-eval-v1/dataset.json", EvaluationSet.class);
  }

  public EvidenceFixtureManifest loadEvidenceManifest(String version) {
    return read(
        version,
        "evaluation/mock-eval-v1/evidence-fixture-manifest.json",
        EvidenceFixtureManifest.class);
  }

  private <T> T read(String version, String path, Class<T> valueType) {
    if (!SUPPORTED_VERSION.equals(version)) {
      throw new IllegalArgumentException("Unsupported evaluation set version: " + version);
    }
    try (var input = new ClassPathResource(path).getInputStream()) {
      return objectMapper.readValue(input, valueType);
    } catch (IOException exception) {
      throw new IllegalStateException("Evaluation asset is unavailable: " + path, exception);
    }
  }
}
