package com.transwarp.serviceinsight.precheck.port;

import com.transwarp.serviceinsight.precheck.domain.Evidence;
import java.util.List;

public interface KnowledgeSearchPort {
  List<Evidence> search(String query);
}
