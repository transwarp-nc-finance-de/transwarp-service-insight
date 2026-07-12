package com.transwarp.serviceinsight.audit.infrastructure.memory;

import com.transwarp.serviceinsight.audit.domain.AuditEvent;
import com.transwarp.serviceinsight.audit.port.AuditPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuditAdapter implements AuditPort {
  private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

  @Override
  public void record(AuditEvent event) {
    events.add(event);
  }

  public List<AuditEvent> snapshot() {
    return List.copyOf(events);
  }

  public void clear() {
    events.clear();
  }
}
