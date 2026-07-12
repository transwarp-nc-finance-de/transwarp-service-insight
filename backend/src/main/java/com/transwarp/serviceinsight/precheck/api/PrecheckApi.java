package com.transwarp.serviceinsight.precheck.api;

import com.transwarp.serviceinsight.precheck.dto.FollowUpRequest;
import com.transwarp.serviceinsight.precheck.dto.FollowUpResponse;
import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.PrecheckResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface PrecheckApi {
  @PostMapping("/precheck")
  PrecheckResponse precheck(@Valid @RequestBody PrecheckRequest request);

  @PostMapping("/precheck/follow-up")
  FollowUpResponse followUp(@Valid @RequestBody FollowUpRequest request);
}
