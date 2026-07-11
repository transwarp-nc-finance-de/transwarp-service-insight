package com.transwarp.serviceinsight.precheck.api;

import com.transwarp.serviceinsight.precheck.MockPrecheckService;
import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.PrecheckResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PrecheckController implements PrecheckApi {
  private final MockPrecheckService service;

  public PrecheckController(MockPrecheckService service) {
    this.service = service;
  }

  @Override
  public PrecheckResponse precheck(PrecheckRequest request) {
    return service.precheck(request);
  }
}
