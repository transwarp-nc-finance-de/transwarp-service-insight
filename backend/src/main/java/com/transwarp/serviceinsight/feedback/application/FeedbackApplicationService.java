package com.transwarp.serviceinsight.feedback.application;

import com.transwarp.serviceinsight.audit.domain.AuditEvent;
import com.transwarp.serviceinsight.audit.domain.BusinessEventType;
import com.transwarp.serviceinsight.audit.port.AuditPort;
import com.transwarp.serviceinsight.feedback.domain.PrecheckFeedback;
import com.transwarp.serviceinsight.feedback.port.FeedbackRepository;
import com.transwarp.serviceinsight.policy.port.PolicyContextPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FeedbackApplicationService implements RecordFeedbackUseCase {
  private final FeedbackRepository repository;
  private final PolicyContextPort policyContext;
  private final AuditPort audit;

  public FeedbackApplicationService(
      FeedbackRepository repository, PolicyContextPort policyContext, AuditPort audit) {
    this.repository = repository;
    this.policyContext = policyContext;
    this.audit = audit;
  }

  @Override
  public PrecheckFeedback record(RecordFeedbackCommand command) {
    var policy = policyContext.currentSnapshot();
    var feedback =
        new PrecheckFeedback(
            UUID.randomUUID(),
            command.precheckId(),
            command.adoptionStatus(),
            command.feedbackReason(),
            command.continuedSubmission(),
            policy.version(),
            Instant.now());
    repository.save(feedback);
    audit.record(
        new AuditEvent(
            UUID.randomUUID(),
            BusinessEventType.FEEDBACK_RECORDED,
            command.precheckId(),
            policy.subjectId(),
            policy.version(),
            Instant.now(),
            Map.of(
                "adoptionStatus", command.adoptionStatus().name(),
                "continuedSubmission", Boolean.toString(command.continuedSubmission()))));
    if (command.continuedSubmission()) {
      audit.record(
          new AuditEvent(
              UUID.randomUUID(),
              BusinessEventType.TICKET_SUBMISSION_CONTINUED,
              command.precheckId(),
              policy.subjectId(),
              policy.version(),
              Instant.now(),
              Map.of("humanConfirmed", "true")));
    }
    return feedback;
  }
}
