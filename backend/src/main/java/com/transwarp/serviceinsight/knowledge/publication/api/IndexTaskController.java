package com.transwarp.serviceinsight.knowledge.publication.api;

import com.transwarp.serviceinsight.knowledge.publication.application.IndexTaskQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/index-tasks")
public class IndexTaskController {
  private final IndexTaskQueryService service;

  public IndexTaskController(IndexTaskQueryService service) {
    this.service = service;
  }

  @GetMapping("/{taskId}")
  Object get(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @PathVariable UUID taskId) {
    return service.get(sessionCookie, taskId);
  }

  @GetMapping
  Object list(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "DESC") String sortDirection,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID versionId) {
    return service.list(sessionCookie, page, size, sortDirection, sortBy, status, versionId);
  }
}
