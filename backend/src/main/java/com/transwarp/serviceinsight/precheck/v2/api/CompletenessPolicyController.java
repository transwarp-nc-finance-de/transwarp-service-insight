package com.transwarp.serviceinsight.precheck.v2.api;

import com.transwarp.serviceinsight.precheck.v2.application.PersistentPrecheckService;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicy;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicyPage;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/completeness-policies")
public class CompletenessPolicyController {
  private final PersistentPrecheckService service;

  public CompletenessPolicyController(PersistentPrecheckService service) {
    this.service = service;
  }

  @GetMapping
  CompletenessPolicyPage list(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestParam(required = false) String issueTypeCode,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {
    return service.listPolicies(sessionCookie, issueTypeCode, page, size, sortBy, sortDirection);
  }

  @GetMapping("/{policyVersion}")
  CompletenessPolicy get(
      @PathVariable String policyVersion,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.getPolicy(sessionCookie, policyVersion);
  }
}
