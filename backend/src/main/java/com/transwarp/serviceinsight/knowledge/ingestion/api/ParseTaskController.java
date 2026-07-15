package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.knowledge.ingestion.application.ParseTaskQueryService;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
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
  public ParseTask get(
      @PathVariable UUID taskId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.get(sessionCookie, taskId);
  }
}
