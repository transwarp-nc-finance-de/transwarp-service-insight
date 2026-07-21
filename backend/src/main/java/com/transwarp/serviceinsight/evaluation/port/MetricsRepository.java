package com.transwarp.serviceinsight.evaluation.port;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.Metrics;
import java.time.Instant;
import java.util.List;

public interface MetricsRepository {
  Metrics calculate(Instant from, Instant to, List<String> productLineScope);
}
