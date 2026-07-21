package com.transwarp.serviceinsight.evaluation.application;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.Metrics;
import com.transwarp.serviceinsight.evaluation.port.MetricsRepository;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
  private final MetricsRepository repository;
  private final AuthSessionApplicationService authSessions;

  public MetricsService(MetricsRepository repository, AuthSessionApplicationService authSessions) {
    this.repository = repository;
    this.authSessions = authSessions;
  }

  public Metrics get(Instant from, Instant to, String productLineCode, String sessionCookie) {
    if (from == null || to == null || !from.isBefore(to)) {
      throw new PrecheckV2Exception(
          "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "统计时间范围不合法", Map.of("mockData", true));
    }
    var identity = authSessions.current(sessionCookie).identity();
    List<String> scope = identity.productLineCodes();
    if (productLineCode != null && !productLineCode.isBlank()) {
      if (!identity.canAccessProductLine(productLineCode)) {
        throw new PrecheckV2Exception(
            "INSUFFICIENT_PRODUCT_SCOPE",
            HttpStatus.FORBIDDEN,
            "当前身份无权读取该产品线指标",
            Map.of("mockData", true));
      }
      scope = List.of(productLineCode);
    }
    return repository.calculate(from, to, scope);
  }
}
