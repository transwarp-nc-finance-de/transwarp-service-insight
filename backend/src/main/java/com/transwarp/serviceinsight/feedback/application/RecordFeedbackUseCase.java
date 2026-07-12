package com.transwarp.serviceinsight.feedback.application;

import com.transwarp.serviceinsight.feedback.domain.PrecheckFeedback;

public interface RecordFeedbackUseCase {
  PrecheckFeedback record(RecordFeedbackCommand command);
}
