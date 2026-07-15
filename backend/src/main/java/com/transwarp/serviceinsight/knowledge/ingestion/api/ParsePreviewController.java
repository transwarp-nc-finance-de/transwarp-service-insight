package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.knowledge.ingestion.application.ParsePreviewQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/knowledge-versions/{versionId}/parse-preview")
public class ParsePreviewController {
  private final ParsePreviewQueryService service;

  public ParsePreviewController(ParsePreviewQueryService service) {
    this.service = service;
  }

  @GetMapping
  public Object summary(
      @PathVariable UUID versionId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.summary(sessionCookie, versionId);
  }

  @GetMapping("/blocks")
  public Object blocks(
      @PathVariable UUID versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.blocks(sessionCookie, versionId, page, size);
  }

  @GetMapping("/chunks")
  public Object chunks(
      @PathVariable UUID versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.chunks(sessionCookie, versionId, page, size);
  }
}
