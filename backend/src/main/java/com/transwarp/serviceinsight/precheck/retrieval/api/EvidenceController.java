package com.transwarp.serviceinsight.precheck.retrieval.api;

import com.transwarp.serviceinsight.precheck.retrieval.application.EvidenceQueryService;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceView;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/evidence")
public class EvidenceController {
  private final EvidenceQueryService service;

  public EvidenceController(EvidenceQueryService service) {
    this.service = service;
  }

  @GetMapping("/{evidenceId}")
  EvidenceView get(
      @PathVariable UUID evidenceId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.get(evidenceId, sessionCookie);
  }
}
