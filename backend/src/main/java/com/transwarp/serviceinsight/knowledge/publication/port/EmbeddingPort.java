package com.transwarp.serviceinsight.knowledge.publication.port;

import java.util.List;

public interface EmbeddingPort {
  List<float[]> embedPassages(List<String> texts);

  List<float[]> embedQueries(List<String> texts);
}
