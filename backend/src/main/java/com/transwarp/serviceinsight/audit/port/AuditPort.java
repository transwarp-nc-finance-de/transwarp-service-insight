package com.transwarp.serviceinsight.audit.port;

import com.transwarp.serviceinsight.audit.domain.AuditEvent;

public interface AuditPort {
  void record(AuditEvent event);
}
