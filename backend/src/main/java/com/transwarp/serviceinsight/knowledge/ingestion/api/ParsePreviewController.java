package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.knowledge.ingestion.application.ParsePreviewQueryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;
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
  public Object summary(@PathVariable UUID versionId, HttpServletRequest request) {
    return service.summary(cookie(request), versionId);
  }

  @GetMapping("/blocks")
  public Object blocks(
      @PathVariable UUID versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest request) {
    return service.blocks(cookie(request), versionId, page, size);
  }

  @GetMapping("/chunks")
  public Object chunks(
      @PathVariable UUID versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest request) {
    return service.chunks(cookie(request), versionId, page, size);
  }

  private String cookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
        .filter(value -> "SESSION".equals(value.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
