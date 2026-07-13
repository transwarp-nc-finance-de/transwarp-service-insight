package com.transwarp.serviceinsight.precheck.api;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.ContinueSessionPrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.GetPrecheckSessionUseCase;
import com.transwarp.serviceinsight.precheck.dto.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PrecheckController implements PrecheckApi {
  private final CreatePrecheckUseCase create;
  private final ContinuePrecheckUseCase followUp;
  private final ContinueSessionPrecheckUseCase sessionRuns;
  private final GetPrecheckSessionUseCase sessions;
  private final PrecheckApiMapper mapper;

  public PrecheckController(
      CreatePrecheckUseCase create,
      ContinuePrecheckUseCase followUp,
      ContinueSessionPrecheckUseCase sessionRuns,
      GetPrecheckSessionUseCase sessions,
      PrecheckApiMapper mapper) {
    this.create = create;
    this.followUp = followUp;
    this.sessionRuns = sessionRuns;
    this.sessions = sessions;
    this.mapper = mapper;
  }

  @Override
  public PrecheckResponse precheck(PrecheckRequest request) {
    return mapper.toResponse(create.create(mapper.toCommand(request)));
  }

  @Override
  public FollowUpResponse followUp(FollowUpRequest request) {
    return mapper.toResponse(followUp.continuePrecheck(mapper.toCommand(request)));
  }

  @PostMapping("/precheck-sessions")
  public PrecheckResponse createSession(@Valid @RequestBody PrecheckRequest request) {
    return precheck(request);
  }

  @PostMapping("/precheck-sessions/{sessionId}/runs")
  public FollowUpResponse createRun(
      @PathVariable UUID sessionId, @Valid @RequestBody SessionRunRequest request) {
    return mapper.toResponse(sessionRuns.continueSession(sessionId, request.message()));
  }

  @GetMapping("/precheck-sessions/{sessionId}")
  public PrecheckSessionResponse getSession(@PathVariable UUID sessionId) {
    return mapper.toResponse(sessions.getSession(sessionId));
  }
}
