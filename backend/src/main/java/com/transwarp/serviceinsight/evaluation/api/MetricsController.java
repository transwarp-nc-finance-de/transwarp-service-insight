package com.transwarp.serviceinsight.evaluation.api;

import com.transwarp.serviceinsight.evaluation.application.MetricsService;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.Metrics;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/metrics")
public class MetricsController {
  private final MetricsService service;

  public MetricsController(MetricsService service) {
    this.service = service;
  }

  @GetMapping
  Metrics get(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(required = false) String productLineCode,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.get(from, to, productLineCode, sessionCookie);
  }
}
