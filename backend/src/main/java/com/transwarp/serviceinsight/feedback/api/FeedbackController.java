package com.transwarp.serviceinsight.feedback.api;

import com.transwarp.serviceinsight.feedback.application.RecordFeedbackCommand;
import com.transwarp.serviceinsight.feedback.application.RecordFeedbackUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/precheck")
public class FeedbackController {
  private final RecordFeedbackUseCase recordFeedback;

  public FeedbackController(RecordFeedbackUseCase recordFeedback) {
    this.recordFeedback = recordFeedback;
  }

  @PostMapping("/feedback")
  public FeedbackResponse record(@Valid @RequestBody FeedbackRequest request) {
    var feedback =
        recordFeedback.record(
            new RecordFeedbackCommand(
                request.precheckId(),
                request.adoptionStatus(),
                request.feedbackReason(),
                request.continuedSubmission()));
    return new FeedbackResponse(
        feedback.id().toString(),
        feedback.precheckId().toString(),
        feedback.adoptionStatus().name(),
        feedback.continuedSubmission(),
        feedback.policyVersion(),
        true,
        true);
  }
}
