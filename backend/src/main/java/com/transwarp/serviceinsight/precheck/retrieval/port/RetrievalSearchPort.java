package com.transwarp.serviceinsight.precheck.retrieval.port;

import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalCandidate;
import java.util.List;

public interface RetrievalSearchPort {
  List<RetrievalCandidate> searchFts(String query, List<String> productLineCodes);

  List<RetrievalCandidate> searchVector(float[] queryEmbedding, List<String> productLineCodes);
}
