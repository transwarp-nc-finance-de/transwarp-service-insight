package com.transwarp.serviceinsight.admin.reset.infrastructure.file;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalResetFileStoreAdapterTest {
  @TempDir Path temporaryDirectory;

  @Test
  void refusesToClearAnUnmarkedDirectory() throws Exception {
    Files.writeString(temporaryDirectory.resolve("keep.txt"), "模拟数据");

    var adapter = new LocalResetFileStoreAdapter(temporaryDirectory.toString());

    assertThrows(IOException.class, adapter::clearOriginalFiles);
    assertTrue(Files.exists(temporaryDirectory.resolve("keep.txt")));
  }

  @Test
  void clearsOnlyInsideAMarkedKnowledgeStorageRoot() throws Exception {
    var sentinel = temporaryDirectory.resolve(".service-insight-knowledge-storage");
    Files.createFile(sentinel);
    var nested = Files.createDirectories(temporaryDirectory.resolve("original"));
    Files.writeString(nested.resolve("old.txt"), "模拟数据");

    new LocalResetFileStoreAdapter(temporaryDirectory.toString()).clearOriginalFiles();

    assertTrue(Files.isRegularFile(sentinel));
    assertTrue(Files.notExists(nested));
  }
}
