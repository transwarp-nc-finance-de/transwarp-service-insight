package com.transwarp.serviceinsight.knowledge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeGovernanceControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private KnowledgePublicationRepository publicationRepository;
  @MockitoBean private EmbeddingPort embeddingPort;

  @Test
  void editorSubmitsAndDifferentReviewerApprovesWithIdempotentReplay() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "governance-submit-001", "# 模拟数据\n\n审核内容");

    var first =
        command(
                editor,
                uploaded.versionId(),
                "review-submissions",
                "submit-command-001",
                "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
            .andExpect(status().isOk())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.version.status").value("IN_REVIEW"))
            .andExpect(jsonPath("$.version.submittedBy").value("mock-knowledge-editor"))
            .andExpect(jsonPath("$.auditEventId").isNotEmpty())
            .andReturn();

    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-command-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.auditEventId").value(json(first).path("auditEventId").asText()));
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-command-001",
            "{\"parseResultHash\":\"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));

    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "approve-command-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version.status").value("APPROVED"))
        .andExpect(jsonPath("$.version.approvedBy").value("mock-knowledge-reviewer"));

    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_governance_audit WHERE subject_id = ?",
                Integer.class,
                uploaded.versionId()))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_review_history WHERE version_id = ?",
                Integer.class,
                uploaded.versionId()))
        .isEqualTo(2);
  }

  @Test
  void adminCannotApproveAndStaleParseHashCannotBeSubmitted() throws Exception {
    var editor = login("mock-knowledge-editor");
    var admin = login("mock-admin");
    var uploaded = uploadAndAwait(editor, "governance-admin-001", "# 模拟数据\n\n不可绕过");

    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-admin-001",
            "{\"parseResultHash\":\"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_STATE_TRANSITION"));

    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-admin-002",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    command(
            admin,
            uploaded.versionId(),
            "approvals",
            "approve-admin-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));
  }

  @Test
  void reviewerReturnsThenEditorCreatesImmutableRevisionAndReparses() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "governance-revise-001", "# 模拟数据\n\n旧内容");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-revise-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    command(
            reviewer,
            uploaded.versionId(),
            "draft-returns",
            "return-revise-001",
            "{\"reason\":\"模拟数据：请补充适用范围\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version.status").value("DRAFT"));

    var revisionJson =
        """
        {"title":"修订标题（模拟数据）","productLine":{"code":"TDH","displayName":"TDH（模拟数据）"},"cleanedText":"# 模拟数据\\n\\n新修订内容","parseWarningNotes":["模拟数据：人工清洗"]}
        """;
    var revisionPart =
        new MockMultipartFile(
            "revision", "", MediaType.APPLICATION_JSON_VALUE, revisionJson.getBytes());
    var revised =
        mockMvc
            .perform(
                multipart("/api/v2/knowledge-versions/{versionId}/revisions", uploaded.versionId())
                    .file(revisionPart)
                    .cookie(editor.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", editor.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "revision-command-001"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.revision.revisionNumber").value(2))
            .andExpect(
                jsonPath("$.revision.cleanedTextHash")
                    .value(org.hamcrest.Matchers.startsWith("sha256:")))
            .andExpect(jsonPath("$.parseTask.status").value("PENDING"))
            .andReturn();
    var revisedTaskId = json(revised).at("/parseTask/taskId").asText();
    awaitTask(editor, revisedTaskId);
    var newHash = previewHash(editor, uploaded.versionId());

    assertThat(newHash).isNotEqualTo(uploaded.parseResultHash());
    mockMvc
        .perform(
            get("/api/v2/parse-tasks/{taskId}", uploaded.taskId())
                .cookie(editor.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_draft_revision WHERE version_id = ?",
                Integer.class,
                uploaded.versionId()))
        .isEqualTo(2);
  }

  @Test
  void approvalRequiresEveryWarningCodeFromTheCurrentParseResult() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded =
        uploadAndAwaitFile(
            editor,
            "governance-warning-001",
            new MockMultipartFile("file", "warning.pdf", "application/pdf", tableLikePdf()));
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-warning-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());

    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "approve-warning-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_STATE_TRANSITION"));
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "approve-warning-002",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[\"TABLE_STRUCTURE_FLATTENED\",\"READING_ORDER_UNCERTAIN\"]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version.status").value("APPROVED"));
  }

  @Test
  void submitterCannotApproveEvenWhenTheIdentityAlsoHasReviewerRole() throws Exception {
    var editor = login("mock-knowledge-editor");
    var uploaded = uploadAndAwait(editor, "governance-self-review-001", "# 模拟数据\n\n职责分离");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-self-review-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    jdbc.update(
        "INSERT INTO local_identity_role(user_code, role_code) VALUES ('mock-knowledge-editor', 'KNOWLEDGE_REVIEWER')");
    try {
      var editorReviewer = login("mock-knowledge-editor");
      command(
              editorReviewer,
              uploaded.versionId(),
              "approvals",
              "approve-self-review-001",
              "{\"parseResultHash\":\""
                  + uploaded.parseResultHash()
                  + "\",\"acknowledgedWarningCodes\":[]}")
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("DUTY_SEPARATION_VIOLATION"));
    } finally {
      jdbc.update(
          "DELETE FROM local_identity_role WHERE user_code = 'mock-knowledge-editor' AND role_code = 'KNOWLEDGE_REVIEWER'");
    }
  }

  @Test
  void serializesTheSameCommandIdempotencyKeyAcrossDifferentVersions() throws Exception {
    var editor = login("mock-knowledge-editor");
    var first = uploadAndAwait(editor, "governance-race-upload-001", "# 模拟数据\n\n第一份资料");
    var second = uploadAndAwait(editor, "governance-race-upload-002", "# 模拟数据\n\n第二份资料");
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var firstRequest =
          executor.submit(
              () -> concurrentSubmit(editor, first, "submit-race-shared-key", ready, start));
      var secondRequest =
          executor.submit(
              () -> concurrentSubmit(editor, second, "submit-race-shared-key", ready, start));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(List.of(firstRequest.get(), secondRequest.get()))
          .containsExactlyInAnyOrder(200, 409);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void databaseRejectsMutationOfDraftRevisionsAndReviewHistory() throws Exception {
    var editor = login("mock-knowledge-editor");
    var uploaded = uploadAndAwait(editor, "governance-immutable-001", "# 模拟数据\n\n不可变记录");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "submit-immutable-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE knowledge_draft_revision SET title = 'changed' WHERE version_id = ?",
                    uploaded.versionId()))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM knowledge_review_history WHERE version_id = ?",
                    uploaded.versionId()))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void approvedVersionPublicationImmediatelyReturnsPendingIndexTask() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "publication-upload-001", "# 模拟数据\n\n待发布索引内容");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "publication-submit-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    var previousVersionId = seedPreviousPublishedVersion(uploaded.versionId());
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "publication-approve-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk());

    when(embeddingPort.embedPassages(anyList()))
        .thenAnswer(
            invocation ->
                ((List<?>) invocation.getArgument(0))
                    .stream().map(ignored -> new float[768]).toList());
    var publication =
        command(reviewer, uploaded.versionId(), "publications", "publication-command-001", "{}")
            .andExpect(status().isAccepted())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.resourceId").value(uploaded.versionId()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.attempt").value(0))
            .andExpect(jsonPath("$.maxAttempts").value(3))
            .andExpect(jsonPath("$.ftsStatus").value("PENDING"))
            .andExpect(jsonPath("$.embeddingStatus").value("PENDING"))
            .andReturn();

    var taskId = json(publication).path("taskId").asText();
    awaitIndexTask(reviewer, taskId, "SUCCEEDED");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk_index WHERE version_id = ? AND fts_document IS NOT NULL",
                Integer.class,
                java.util.UUID.fromString(uploaded.versionId())))
        .isGreaterThan(0);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk_fts_stage WHERE task_id = ?",
                Integer.class,
                java.util.UUID.fromString(taskId)))
        .isZero();
    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview", uploaded.versionId())
                .cookie(reviewer.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionStatus").value("PUBLISHED"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM knowledge_version_v2 WHERE version_id = ?",
                String.class,
                previousVersionId))
        .isEqualTo("DEPRECATED");
    assertThat(
            jdbc.queryForObject(
                "SELECT current_published_version_id FROM knowledge_document WHERE document_id = (SELECT document_id FROM knowledge_version_v2 WHERE version_id = ?)",
                java.util.UUID.class,
                java.util.UUID.fromString(uploaded.versionId())))
        .isEqualTo(java.util.UUID.fromString(uploaded.versionId()));
  }

  @Test
  void retryableEmbeddingFailureStopsAfterThreeAttemptsAndKeepsVersionApproved() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "publication-retry-upload-001", "# 模拟数据\n\n向量服务超时");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "publication-retry-submit-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    var previousVersionId = seedPreviousPublishedVersion(uploaded.versionId());
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "publication-retry-approve-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk());
    when(embeddingPort.embedPassages(anyList()))
        .thenThrow(new EmbeddingException("EMBEDDING_TIMEOUT", "模拟数据：向量超时", true));

    var publication =
        command(
                reviewer,
                uploaded.versionId(),
                "publications",
                "publication-retry-command-001",
                "{}")
            .andExpect(status().isAccepted())
            .andReturn();

    var taskId = json(publication).path("taskId").asText();
    var failed = awaitIndexTask(reviewer, taskId, "FAILED");
    assertThat(json(failed).path("attempt").asInt()).isEqualTo(3);
    assertThat(json(failed).path("error").path("code").asText()).isEqualTo("EMBEDDING_TIMEOUT");
    assertThat(json(failed).path("ftsStatus").asText()).isEqualTo("SUCCEEDED");
    assertThat(json(failed).path("embeddingStatus").asText()).isEqualTo("FAILED");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk_fts_stage WHERE task_id = ? AND fts_document IS NOT NULL",
                Integer.class,
                java.util.UUID.fromString(taskId)))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk_index WHERE version_id = ?",
                Integer.class,
                java.util.UUID.fromString(uploaded.versionId())))
        .isZero();
    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview", uploaded.versionId())
                .cookie(reviewer.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionStatus").value("APPROVED"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM knowledge_version_v2 WHERE version_id = ?",
                String.class,
                previousVersionId))
        .isEqualTo("PUBLISHED");
    assertThat(
            jdbc.queryForObject(
                "SELECT current_published_version_id FROM knowledge_document WHERE document_id = (SELECT document_id FROM knowledge_version_v2 WHERE version_id = ?)",
                java.util.UUID.class,
                java.util.UUID.fromString(uploaded.versionId())))
        .isEqualTo(previousVersionId);
  }

  @Test
  void concurrentPublicationCreatesOnlyOneActiveTask() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "publication-concurrent-upload-001", "# 模拟数据\n\n并发发布");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "publication-concurrent-submit-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "publication-concurrent-approve-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk());
    var embeddingStarted = new CountDownLatch(1);
    var releaseEmbedding = new CountDownLatch(1);
    when(embeddingPort.embedPassages(anyList()))
        .thenAnswer(
            invocation -> {
              embeddingStarted.countDown();
              if (!releaseEmbedding.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("embedding release timed out");
              }
              return ((List<?>) invocation.getArgument(0))
                  .stream().map(ignored -> new float[768]).toList();
            });

    var first =
        command(
                reviewer,
                uploaded.versionId(),
                "publications",
                "publication-concurrent-command-001",
                "{}")
            .andExpect(status().isAccepted())
            .andReturn();
    assertThat(embeddingStarted.await(5, TimeUnit.SECONDS)).isTrue();
    command(
            reviewer,
            uploaded.versionId(),
            "publications",
            "publication-concurrent-command-002",
            "{}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INDEX_TASK_ALREADY_ACTIVE"));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM index_task WHERE resource_id = ? AND status IN ('PENDING', 'RUNNING')",
                Integer.class,
                java.util.UUID.fromString(uploaded.versionId())))
        .isEqualTo(1);
    releaseEmbedding.countDown();
    awaitIndexTask(reviewer, json(first).path("taskId").asText(), "SUCCEEDED");
  }

  @Test
  void deterministicEmbeddingFailureIsNotRetried() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "publication-invalid-upload-001", "# 模拟数据\n\n非法向量响应");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "publication-invalid-submit-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "publication-invalid-approve-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk());
    when(embeddingPort.embedPassages(anyList()))
        .thenThrow(new EmbeddingException("EMBEDDING_INVALID_RESPONSE", "模拟数据：向量维度不符合契约", false));
    var publication =
        command(
                reviewer,
                uploaded.versionId(),
                "publications",
                "publication-invalid-command-001",
                "{}")
            .andExpect(status().isAccepted())
            .andReturn();

    var taskId = json(publication).path("taskId").asText();
    var failed = awaitIndexTask(reviewer, taskId, "FAILED");
    assertThat(json(failed).path("attempt").asInt()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM index_task_attempt WHERE task_id = ?",
                Integer.class,
                java.util.UUID.fromString(taskId)))
        .isEqualTo(1);
  }

  @Test
  void restartRecoveryAppendsInterruptedAttemptBeforeRescheduling() throws Exception {
    var editor = login("mock-knowledge-editor");
    var reviewer = login("mock-knowledge-reviewer");
    var uploaded = uploadAndAwait(editor, "publication-recovery-upload-001", "# 模拟数据\n\n重启恢复");
    command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            "publication-recovery-submit-001",
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andExpect(status().isOk());
    command(
            reviewer,
            uploaded.versionId(),
            "approvals",
            "publication-recovery-approve-001",
            "{\"parseResultHash\":\""
                + uploaded.parseResultHash()
                + "\",\"acknowledgedWarningCodes\":[]}")
        .andExpect(status().isOk());
    var taskId = java.util.UUID.randomUUID();
    var versionId = java.util.UUID.fromString(uploaded.versionId());
    var revisionId =
        jdbc.queryForObject(
            "SELECT current_draft_revision_id FROM knowledge_version_v2 WHERE version_id = ?",
            java.util.UUID.class,
            versionId);
    var startedAt = Instant.now().minusSeconds(10);
    jdbc.update(
        "INSERT INTO index_task(task_id, resource_id, revision_id, actor_user_code, status, fts_status, embedding_status, attempt, max_attempts, created_at, started_at) VALUES (?, ?, ?, 'mock-knowledge-reviewer', 'RUNNING', 'SUCCEEDED', 'RUNNING', 1, 3, ?, ?)",
        taskId,
        versionId,
        revisionId,
        Timestamp.from(startedAt.minusSeconds(1)),
        Timestamp.from(startedAt));

    publicationRepository.recoverIncompleteTasks(Instant.now());

    assertThat(
            jdbc.queryForObject(
                "SELECT error_code FROM index_task_attempt WHERE task_id = ? AND attempt = 1",
                String.class,
                taskId))
        .isEqualTo("INDEX_INTERRUPTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM index_task WHERE task_id = ?", String.class, taskId))
        .isEqualTo("PENDING");
  }

  private int concurrentSubmit(
      MvcResult editor, Uploaded uploaded, String key, CountDownLatch ready, CountDownLatch start)
      throws Exception {
    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent start timed out");
    return command(
            editor,
            uploaded.versionId(),
            "review-submissions",
            key,
            "{\"parseResultHash\":\"" + uploaded.parseResultHash() + "\"}")
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private java.util.UUID seedPreviousPublishedVersion(String successorVersionId) {
    var successorId = java.util.UUID.fromString(successorVersionId);
    var previousId = java.util.UUID.randomUUID();
    var previousRevisionId = java.util.UUID.randomUUID();
    var documentId =
        jdbc.queryForObject(
            "SELECT document_id FROM knowledge_version_v2 WHERE version_id = ?",
            java.util.UUID.class,
            successorId);
    var originalFileId =
        jdbc.queryForObject(
            "SELECT original_file_id FROM knowledge_version_v2 WHERE version_id = ?",
            java.util.UUID.class,
            successorId);
    jdbc.update(
        "INSERT INTO knowledge_version_v2(version_id, document_id, revision_number, status, created_by, submitted_by, approved_by, original_file_id, created_at, updated_at, mock_data) VALUES (?, ?, 0, 'PUBLISHED', 'mock-knowledge-editor', 'mock-knowledge-editor', 'mock-knowledge-reviewer', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE)",
        previousId,
        documentId,
        originalFileId);
    jdbc.update(
        "INSERT INTO knowledge_draft_revision(revision_id, version_id, revision_number, title, product_line_code, created_at, cleaned_text_hash, product_line_display_name, created_by, mock_data) VALUES (?, ?, 1, '模拟数据：旧发布版本', 'TDH', CURRENT_TIMESTAMP, 'sha256:0000000000000000000000000000000000000000000000000000000000000000', 'TDH（模拟数据）', 'mock-knowledge-editor', TRUE)",
        previousRevisionId,
        previousId);
    jdbc.update(
        "UPDATE knowledge_version_v2 SET current_draft_revision_id = ? WHERE version_id = ?",
        previousRevisionId,
        previousId);
    jdbc.update(
        "UPDATE knowledge_document SET current_published_version_id = ? WHERE document_id = ?",
        previousId,
        documentId);
    return previousId;
  }

  private org.springframework.test.web.servlet.ResultActions command(
      MvcResult login, String versionId, String command, String key, String body) throws Exception {
    return mockMvc.perform(
        post("/api/v2/knowledge-versions/{versionId}/{command}", versionId, command)
            .cookie(login.getResponse().getCookie("SESSION"))
            .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private Uploaded uploadAndAwait(MvcResult login, String key, String content) throws Exception {
    return uploadAndAwaitFile(
        login,
        key,
        new MockMultipartFile("file", "review.md", "text/markdown", content.getBytes()));
  }

  private Uploaded uploadAndAwaitFile(MvcResult login, String key, MockMultipartFile file)
      throws Exception {
    var metadata =
        new MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            "{\"title\":\"审核资料（模拟数据）\",\"productLine\":{\"code\":\"TDH\",\"displayName\":\"TDH（模拟数据）\"},\"sourceType\":\"MOCK\",\"mockData\":true}"
                .getBytes());
    var upload =
        mockMvc
            .perform(
                multipart("/api/v2/knowledge-documents")
                    .file(metadata)
                    .file(file)
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", key))
            .andExpect(status().isCreated())
            .andReturn();
    var body = json(upload);
    var versionId = body.at("/version/versionId").asText();
    var taskId = body.at("/parseTask/taskId").asText();
    awaitTask(login, taskId);
    return new Uploaded(versionId, taskId, previewHash(login, versionId));
  }

  private byte[] tableLikePdf() throws Exception {
    try (var document = new PDDocument();
        var output = new ByteArrayOutputStream()) {
      var page = new PDPage();
      document.addPage(page);
      try (var content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(72, 720);
        content.showText("Column A    Column B");
        content.endText();
      }
      document.save(output);
      return output.toByteArray();
    }
  }

  private void awaitTask(MvcResult login, String taskId) throws Exception {
    for (int attempt = 0; attempt < 40; attempt++) {
      var response =
          mockMvc
              .perform(
                  get("/api/v2/parse-tasks/{taskId}", taskId)
                      .cookie(login.getResponse().getCookie("SESSION")))
              .andExpect(status().isOk())
              .andReturn();
      if ("SUCCEEDED".equals(json(response).path("status").asText())) return;
      Thread.sleep(50);
    }
    throw new AssertionError("Parse task did not complete");
  }

  private MvcResult awaitIndexTask(MvcResult login, String taskId, String expectedStatus)
      throws Exception {
    for (int attempt = 0; attempt < 40; attempt++) {
      var response =
          mockMvc
              .perform(
                  get("/api/v2/index-tasks/{taskId}", taskId)
                      .cookie(login.getResponse().getCookie("SESSION")))
              .andExpect(status().isOk())
              .andReturn();
      if (expectedStatus.equals(json(response).path("status").asText())) return response;
      Thread.sleep(50);
    }
    throw new AssertionError("Index task did not reach " + expectedStatus);
  }

  private String previewHash(MvcResult login, String versionId) throws Exception {
    var preview =
        mockMvc
            .perform(
                get("/api/v2/knowledge-versions/{versionId}/parse-preview", versionId)
                    .cookie(login.getResponse().getCookie("SESSION")))
            .andExpect(status().isOk())
            .andReturn();
    return json(preview).path("parseResultHash").asText();
  }

  private JsonNode json(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }

  private MvcResult login(String userCode) throws Exception {
    return mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"" + userCode + "\",\"mockData\":true}"))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private record Uploaded(String versionId, String taskId, String parseResultHash) {}
}
