package com.transwarp.serviceinsight.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeParseProcessor;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.UploadAggregate;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class KnowledgeParseRecoveryTest {
  @Autowired private KnowledgeIngestionRepository repository;
  @Autowired private OriginalFileStorage storage;
  @Autowired private KnowledgeParseProcessor processor;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void resumesPendingAndInterruptedTasksAfterApplicationStartup() throws Exception {
    var pending = createPending("pending");
    var interrupted = createPending("interrupted");
    jdbc.update(
        "UPDATE parse_task SET status = 'RUNNING', attempt = 1 WHERE task_id = ?",
        interrupted.taskId());

    try {
      processor.recoverIncompleteTasks();

      assertSucceeded(pending.taskId(), 1);
      assertSucceeded(interrupted.taskId(), 2);
    } finally {
      storage.deleteIfPresent(pending.storageKey());
      storage.deleteIfPresent(interrupted.storageKey());
    }
  }

  @Test
  void doesNotRunRecoveredRetryBeforeItsBackoffDeadline() throws Exception {
    var delayed = createPending("delayed");
    jdbc.update(
        "UPDATE parse_task SET next_retry_at = ? WHERE task_id = ?",
        java.sql.Timestamp.from(Instant.now().plusMillis(600)),
        delayed.taskId());

    try {
      processor.recoverIncompleteTasks();
      Thread.sleep(150);

      var beforeDeadline =
          jdbc.queryForMap(
              "SELECT status, attempt FROM parse_task WHERE task_id = ?", delayed.taskId());
      assertThat(beforeDeadline.get("status")).isEqualTo("PENDING");
      assertThat(beforeDeadline.get("attempt")).isEqualTo(0);
      assertSucceeded(delayed.taskId(), 1);
    } finally {
      storage.deleteIfPresent(delayed.storageKey());
    }
  }

  private Scenario createPending(String suffix) throws Exception {
    var documentId = UUID.randomUUID();
    var versionId = UUID.randomUUID();
    var taskId = UUID.randomUUID();
    var fileId = UUID.randomUUID();
    var content = ("模拟数据：恢复解析 " + suffix).getBytes(StandardCharsets.UTF_8);
    var contentHash = sha256(content);
    var storageKey = storage.store(fileId, suffix + ".txt", content);
    repository.create(
        new UploadAggregate(
            documentId,
            versionId,
            UUID.randomUUID(),
            taskId,
            fileId,
            "恢复解析（模拟数据）",
            new CatalogValue("TDH", "TDH（模拟数据）"),
            "MOCK",
            "mock-knowledge-editor",
            storageKey,
            contentHash,
            content.length,
            "text/plain",
            suffix + ".txt",
            "recovery-" + UUID.randomUUID(),
            contentHash,
            Instant.now()));
    return new Scenario(taskId, storageKey);
  }

  private void assertSucceeded(UUID taskId, int expectedAttempt) throws InterruptedException {
    for (var poll = 0; poll < 40; poll++) {
      var task =
          jdbc.queryForMap("SELECT status, attempt FROM parse_task WHERE task_id = ?", taskId);
      if ("SUCCEEDED".equals(task.get("status"))) {
        assertThat(task.get("attempt")).isEqualTo(expectedAttempt);
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("Recovered parse task did not complete: " + taskId);
  }

  private String sha256(byte[] value) throws Exception {
    return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }

  private record Scenario(UUID taskId, String storageKey) {}
}
