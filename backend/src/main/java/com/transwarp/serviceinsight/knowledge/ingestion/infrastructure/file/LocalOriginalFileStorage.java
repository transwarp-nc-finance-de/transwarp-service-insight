package com.transwarp.serviceinsight.knowledge.ingestion.infrastructure.file;

import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalOriginalFileStorage implements OriginalFileStorage {
  private final Path root;

  public LocalOriginalFileStorage(
      @Value("${app.knowledge.storage-path:${java.io.tmpdir}/service-insight-knowledge}")
          String storagePath) {
    root = Path.of(storagePath).toAbsolutePath().normalize();
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

  @Override
  public void clearAll() throws IOException {
    if (!Files.exists(root)) return;
    if (root.getParent() == null) throw new IOException("拒绝清理文件系统根目录");
    try (var paths = Files.walk(root)) {
      for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
        if (!path.equals(root)) Files.deleteIfExists(path);
      }
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
