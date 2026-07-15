package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.knowledge.ingestion.application.ParseTaskQueryService;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/parse-tasks")
public class ParseTaskController {
  private final ParseTaskQueryService service;

  public ParseTaskController(ParseTaskQueryService service) {
    this.service = service;
  }

  @GetMapping("/{taskId}")
  public ParseTask get(@PathVariable UUID taskId, HttpServletRequest request) {
    return service.get(sessionCookie(request), taskId);
  }

  private String sessionCookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
        .filter(cookie -> "SESSION".equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
