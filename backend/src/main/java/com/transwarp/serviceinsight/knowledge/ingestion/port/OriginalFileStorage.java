package com.transwarp.serviceinsight.knowledge.ingestion.port;

import java.io.IOException;
import java.util.UUID;

public interface OriginalFileStorage {
  String store(UUID fileId, String originalName, byte[] content) throws IOException;

  byte[] read(String storageKey) throws IOException;

  void deleteIfPresent(String storageKey);

  void clearAll() throws IOException;
}
