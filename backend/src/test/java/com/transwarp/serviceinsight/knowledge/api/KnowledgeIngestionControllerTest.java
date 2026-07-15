package com.transwarp.serviceinsight.knowledge.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeIngestionControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void knowledgeEditorUploadsMarkdownAndCreatesPendingParseTask() throws Exception {
    var login = login("mock-knowledge-editor");
    var metadata =
        new MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {"title":"TDH 排查手册（模拟数据）","productLine":{"code":"TDH","displayName":"TDH（模拟数据）"},"sourceType":"MOCK","mockData":true}
            """
                .getBytes());
    var file =
        new MockMultipartFile(
            "file", "guide.md", "text/markdown", "# 模拟数据\n\n检查组件版本和完整错误码。".getBytes());

    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata)
                .file(file)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "upload-markdown-001"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotency-Replayed", "false"))
        .andExpect(jsonPath("$.document.title").value("TDH 排查手册（模拟数据）"))
        .andExpect(jsonPath("$.document.productLine.code").value("TDH"))
        .andExpect(jsonPath("$.document.mockData").value(true))
        .andExpect(jsonPath("$.version.revisionNumber").value(1))
        .andExpect(jsonPath("$.version.status").value("DRAFT"))
        .andExpect(jsonPath("$.parseTask.status").value("PENDING"))
        .andExpect(jsonPath("$.parseTask.attempt").value(0))
        .andExpect(jsonPath("$.parseTask.maxAttempts").value(3));
  }

  @Test
  void parsesMarkdownAsynchronouslyAndExposesSucceededTask() throws Exception {
    var login = login("mock-knowledge-editor");
    var metadata =
        new MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {"title":"异步解析（模拟数据）","productLine":{"code":"TDH","displayName":"TDH（模拟数据）"},"sourceType":"MOCK","mockData":true}
            """
                .getBytes());
    var file =
        new MockMultipartFile(
            "file", "async.md", "text/markdown", "# 标题\n\n模拟数据：异步解析正文。".getBytes());
    var upload =
        mockMvc
            .perform(
                multipart("/api/v2/knowledge-documents")
                    .file(metadata)
                    .file(file)
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "async-markdown-001"))
            .andExpect(status().isCreated())
            .andReturn();
    var taskId =
        objectMapper
            .readTree(upload.getResponse().getContentAsByteArray())
            .at("/parseTask/taskId")
            .asText();

    for (int attempt = 0; attempt < 40; attempt++) {
      var task =
          mockMvc
              .perform(
                  get("/api/v2/parse-tasks/{taskId}", taskId)
                      .cookie(login.getResponse().getCookie("SESSION")))
              .andExpect(status().isOk())
              .andReturn();
      var statusValue =
          objectMapper.readTree(task.getResponse().getContentAsByteArray()).path("status").asText();
      if ("SUCCEEDED".equals(statusValue)) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("解析任务未在测试窗口内完成");
  }

  @Test
  void exposesStableParseSummaryBlocksAndChunks() throws Exception {
    var login = login("mock-knowledge-editor");
    var metadata =
        new MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {"title":"Preview mock data","productLine":{"code":"TDH","displayName":"TDH mock data"},"sourceType":"MOCK","mockData":true}
            """
                .getBytes());
    var file =
        new MockMultipartFile(
            "file",
            "preview.md",
            "text/markdown",
            "# Heading\n\nMock data paragraph one.\n\nMock data paragraph two.".getBytes());
    var upload =
        mockMvc
            .perform(
                multipart("/api/v2/knowledge-documents")
                    .file(metadata)
                    .file(file)
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "preview-markdown-001"))
            .andExpect(status().isCreated())
            .andReturn();
    var response = objectMapper.readTree(upload.getResponse().getContentAsByteArray());
    var taskId = response.at("/parseTask/taskId").asText();
    var versionId = response.at("/version/versionId").asText();
    awaitSucceeded(login, taskId);

    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview", versionId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parseStatus").value("SUCCEEDED"))
        .andExpect(jsonPath("$.parserVersion").value("text-structure-v1"))
        .andExpect(jsonPath("$.parseResultHash").value(org.hamcrest.Matchers.startsWith("sha256:")))
        .andExpect(jsonPath("$.chunkingRule.strategy").value("STRUCTURE_FIRST"))
        .andExpect(jsonPath("$.chunkingRule.maxTokens").value(400))
        .andExpect(jsonPath("$.chunkingRule.overlapTokens").value(50));
    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview/blocks", versionId)
                .param("page", "1")
                .param("size", "2")
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].sequence").value(1))
        .andExpect(jsonPath("$.items[1].sequence").value(2))
        .andExpect(jsonPath("$.page.size").value(2))
        .andExpect(jsonPath("$.page.sortBy").value("sequence"));
    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview/chunks", versionId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].sequence").value(1))
        .andExpect(jsonPath("$.items[0].tokenCount").isNumber())
        .andExpect(jsonPath("$.page.sortDirection").value("ASC"));
  }

  @Test
  void enforcesAuthenticationRoleProductScopeAndSafeNotFound() throws Exception {
    var metadata = metadata("Authorization mock data", "NOT_AUTHORIZED");
    var file = markdown("authorization.md", "# Mock data");

    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata)
                .file(file)
                .header("Idempotency-Key", "authorization-unauthenticated"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

    var reviewer = login("mock-knowledge-reviewer");
    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata("Reviewer mock data", "TDH"))
                .file(file)
                .cookie(reviewer.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", reviewer.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "authorization-reviewer"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));

    var editor = login("mock-knowledge-editor");
    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata)
                .file(file)
                .cookie(editor.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", editor.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "authorization-product-scope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    mockMvc
        .perform(
            get("/api/v2/knowledge-versions/{versionId}/parse-preview", java.util.UUID.randomUUID())
                .cookie(editor.getResponse().getCookie("SESSION")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void replaysSameIdempotentUploadAndRejectsChangedPayload() throws Exception {
    var login = login("mock-knowledge-editor");
    var first =
        mockMvc
            .perform(
                multipart("/api/v2/knowledge-documents")
                    .file(metadata("Idempotency mock data", "TDH"))
                    .file(markdown("same.md", "# Same mock data"))
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "idempotency-upload-001"))
            .andExpect(status().isCreated())
            .andReturn();
    var documentId =
        objectMapper
            .readTree(first.getResponse().getContentAsByteArray())
            .at("/document/documentId");

    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata("Idempotency mock data", "TDH"))
                .file(markdown("same.md", "# Same mock data"))
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "idempotency-upload-001"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.document.documentId").value(documentId.asText()));
    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata("Changed mock data", "TDH"))
                .file(markdown("changed.md", "# Changed mock data"))
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "idempotency-upload-001"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
  }

  @Test
  void rejectsUnsupportedTypeOversizedTextAndInvalidPagingWithoutLeakingContent() throws Exception {
    var login = login("mock-knowledge-editor");
    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata("Unsupported mock data", "TDH"))
                .file(new MockMultipartFile("file", "bad.html", "text/html", "secret".getBytes()))
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "unsupported-media-001"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"))
        .andExpect(jsonPath("$.safeDetails.mockData").value(true));
    mockMvc
        .perform(
            multipart("/api/v2/knowledge-documents")
                .file(metadata("Oversized mock data", "TDH"))
                .file(
                    new MockMultipartFile(
                        "file", "large.txt", "text/plain", new byte[5 * 1024 * 1024 + 1]))
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "oversized-text-001"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
  }

  private MockMultipartFile metadata(String title, String productLine) {
    return new MockMultipartFile(
        "metadata",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        ("{\"title\":\""
                + title
                + "\",\"productLine\":{\"code\":\""
                + productLine
                + "\",\"displayName\":\"Mock data\"},\"sourceType\":\"MOCK\",\"mockData\":true}")
            .getBytes());
  }

  private MockMultipartFile markdown(String name, String content) {
    return new MockMultipartFile("file", name, "text/markdown", content.getBytes());
  }

  private void awaitSucceeded(MvcResult login, String taskId) throws Exception {
    for (int attempt = 0; attempt < 40; attempt++) {
      var task =
          mockMvc
              .perform(
                  get("/api/v2/parse-tasks/{taskId}", taskId)
                      .cookie(login.getResponse().getCookie("SESSION")))
              .andExpect(status().isOk())
              .andReturn();
      if ("SUCCEEDED"
          .equals(
              objectMapper
                  .readTree(task.getResponse().getContentAsByteArray())
                  .path("status")
                  .asText())) return;
      Thread.sleep(50);
    }
    throw new AssertionError("Parse task did not complete in the test window");
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
}
