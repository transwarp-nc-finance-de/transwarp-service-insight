package com.transwarp.serviceinsight.feedback.port;

import com.transwarp.serviceinsight.feedback.domain.PrecheckFeedback;

public interface FeedbackRepository {
  void save(PrecheckFeedback feedback);
}
