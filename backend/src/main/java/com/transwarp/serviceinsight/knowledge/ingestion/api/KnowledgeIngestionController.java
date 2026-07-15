package com.transwarp.serviceinsight.knowledge.ingestion.api;

import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/knowledge-documents")
public class KnowledgeIngestionController {
  private final KnowledgeIngestionService service;

  public KnowledgeIngestionController(KnowledgeIngestionService service) {
    this.service = service;
  }

  @PostMapping(consumes = "multipart/form-data")
  ResponseEntity<?> create(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestPart("metadata") KnowledgeDocumentMetadataRequest metadata,
      @RequestPart("file") MultipartFile file) {
    var result = service.upload(sessionCookie, csrfToken, idempotencyKey, metadata, file);
    return ResponseEntity.status(201)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.created());
  }
}
