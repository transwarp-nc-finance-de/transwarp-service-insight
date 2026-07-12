package com.transwarp.serviceinsight.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
  private static final Map<String, String> HEALTH = Map.of("status", "UP");

  @GetMapping("/health")
  public Map<String, String> health() {
    return HEALTH;
  }
}
