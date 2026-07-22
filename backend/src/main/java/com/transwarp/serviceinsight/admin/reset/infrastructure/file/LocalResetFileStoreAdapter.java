package com.transwarp.serviceinsight.admin.reset.infrastructure.file;

import com.transwarp.serviceinsight.admin.reset.port.LocalResetFileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalResetFileStoreAdapter implements LocalResetFileStore {
  private static final String STORAGE_SENTINEL = ".service-insight-knowledge-storage";
  private final Path root;

  public LocalResetFileStoreAdapter(
      @Value("${app.knowledge.storage-path:${java.io.tmpdir}/service-insight-knowledge}")
          String storagePath) {
    root = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Override
  public void clearOriginalFiles() throws IOException {
    var sentinel = root.resolve(STORAGE_SENTINEL);
    if (!Files.isRegularFile(sentinel)) {
      throw new IOException("拒绝清理未标记的知识文件存储目录");
    }
    try (var paths = Files.walk(root)) {
      for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
        if (!path.equals(root) && !path.equals(sentinel)) Files.deleteIfExists(path);
      }
    }
  }
}
