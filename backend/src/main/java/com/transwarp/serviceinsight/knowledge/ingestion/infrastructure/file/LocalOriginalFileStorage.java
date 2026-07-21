package com.transwarp.serviceinsight.knowledge.ingestion.infrastructure.file;

import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalOriginalFileStorage implements OriginalFileStorage {
  private static final String STORAGE_SENTINEL = ".service-insight-knowledge-storage";
  private final Path root;

  public LocalOriginalFileStorage(
      @Value("${app.knowledge.storage-path:${java.io.tmpdir}/service-insight-knowledge}")
          String storagePath) {
    root = Path.of(storagePath).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
      var sentinel = root.resolve(STORAGE_SENTINEL);
      if (!Files.exists(sentinel)) Files.createFile(sentinel);
    } catch (IOException exception) {
      throw new IllegalStateException("无法初始化本地知识文件存储", exception);
    }
  }

  @Override
  public String store(UUID fileId, String originalName, byte[] content) throws IOException {
    var storageKey = "original/" + fileId + extension(originalName);
    var target = resolve(storageKey);
    Files.createDirectories(target.getParent());
    Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    return storageKey;
  }

  @Override
  public byte[] read(String storageKey) throws IOException {
    return Files.readAllBytes(resolve(storageKey));
  }

  @Override
  public void deleteIfPresent(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException ignored) {
      // Best-effort cleanup is used only when the database transaction did not persist metadata.
    }
  }

  private Path resolve(String storageKey) {
    var resolved = root.resolve(storageKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("非法存储键");
    }
    return resolved;
  }

  private String extension(String originalName) {
    if (originalName == null) return "";
    var normalized = originalName.toLowerCase(Locale.ROOT);
    if (normalized.endsWith(".md")) return ".md";
    if (normalized.endsWith(".txt")) return ".txt";
    if (normalized.endsWith(".pdf")) return ".pdf";
    return "";
  }
}
