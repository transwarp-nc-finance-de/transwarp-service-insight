package com.transwarp.serviceinsight.feedback.infrastructure.memory;

import com.transwarp.serviceinsight.feedback.domain.PrecheckFeedback;
import com.transwarp.serviceinsight.feedback.port.FeedbackRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryFeedbackRepository implements FeedbackRepository {
  private final List<PrecheckFeedback> feedback = new CopyOnWriteArrayList<>();

  @Override
  public void save(PrecheckFeedback item) {
    feedback.add(item);
  }

  public List<PrecheckFeedback> snapshot() {
    return List.copyOf(feedback);
  }

  public void clear() {
    feedback.clear();
  }
}
