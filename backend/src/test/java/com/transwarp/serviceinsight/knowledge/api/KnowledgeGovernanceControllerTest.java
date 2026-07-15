package com.transwarp.serviceinsight.knowledge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeGovernanceControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;

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
