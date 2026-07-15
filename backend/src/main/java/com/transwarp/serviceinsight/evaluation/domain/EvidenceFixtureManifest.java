package com.transwarp.serviceinsight.evaluation.domain;

import java.util.List;

public record EvidenceFixtureManifest(
    String manifestVersion,
    String datasetVersion,
    boolean mockData,
    List<ExecutionIdentityFixture> executionIdentities,
    List<EvidenceFixture> evidenceFixtures) {
  public EvidenceFixtureManifest {
    executionIdentities = List.copyOf(executionIdentities);
    evidenceFixtures = List.copyOf(evidenceFixtures);
  }

  public record ExecutionIdentityFixture(
      String identityCode, List<String> roles, List<String> productLineCodes, boolean mockData) {
    public ExecutionIdentityFixture {
      roles = List.copyOf(roles);
      productLineCodes = List.copyOf(productLineCodes);
    }
  }

  public record EvidenceFixture(
      String evidenceId,
      String productLineCode,
      String language,
      String title,
      String excerpt,
      boolean mockData) {}
}
