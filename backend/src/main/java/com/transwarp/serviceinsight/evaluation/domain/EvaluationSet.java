package com.transwarp.serviceinsight.evaluation.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record EvaluationSet(
    String datasetVersion, String owner, boolean mockData, List<EvaluationCase> cases) {
  public EvaluationSet {
    cases = List.copyOf(cases);
  }

  public Set<String> coveredTags() {
    var tags = new LinkedHashSet<String>();
    cases.forEach(
        item -> {
          tags.addAll(item.languageTags());
          tags.addAll(item.scenarioTags());
        });
    return Set.copyOf(tags);
  }
}
