package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ChunkPage;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsePreview;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedBlockPage;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ParsePreviewQueryService {
  private final AuthSessionApplicationService authSessions;
  private final KnowledgeIngestionRepository repository;

  public ParsePreviewQueryService(
      AuthSessionApplicationService authSessions, KnowledgeIngestionRepository repository) {
    this.authSessions = authSessions;
    this.repository = repository;
  }

  public ParsePreview summary(String cookie, UUID versionId) {
    var identity = authorizedIdentity(cookie);
    var task = repository.findVisibleTaskByVersion(versionId, identity).orElseThrow(this::notFound);
    if (!"SUCCEEDED".equals(task.status())) {
      var failed = "FAILED".equals(task.status());
      throw new KnowledgeApiException(
          HttpStatus.CONFLICT,
          failed ? "TASK_FAILED" : "PARSE_PREVIEW_NOT_READY",
          failed ? "The parse task failed." : "The parse preview is not ready.",
          List.of(),
          !failed || (task.error() != null && task.error().retryable()),
          Map.of("taskId", task.taskId(), "status", task.status(), "mockData", true));
    }
    return repository.findPreview(versionId, identity).orElseThrow(this::notFound);
  }

  public ParsedBlockPage blocks(String cookie, UUID versionId, int page, int size) {
    summary(cookie, versionId);
    validatePage(page, size);
    return repository.findBlocks(versionId, page, size);
  }

  public ChunkPage chunks(String cookie, UUID versionId, int page, int size) {
    summary(cookie, versionId);
    validatePage(page, size);
    return repository.findChunks(versionId, page, size);
  }

  private com.transwarp.serviceinsight.identity.domain.IdentityContext authorizedIdentity(
      String cookie) {
    var identity = authSessions.current(cookie).identity();
    if (!identity.hasRole(Role.KNOWLEDGE_EDITOR) && !identity.hasRole(Role.KNOWLEDGE_REVIEWER)) {
      throw new KnowledgeApiException(
          HttpStatus.FORBIDDEN,
          "INSUFFICIENT_ROLE",
          "The current identity cannot read parse content.",
          List.of(),
          false,
          Map.of("mockData", true));
    }
    return identity;
  }

  private void validatePage(int page, int size) {
    if (page < 1 || size < 1 || size > 100) {
      throw new KnowledgeApiException(
          HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "Page parameters are invalid.",
          List.of(),
          false,
          Map.of("mockData", true));
    }
  }

  private KnowledgeApiException notFound() {
    return new KnowledgeApiException(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "The resource does not exist.",
        List.of(),
        false,
        Map.of("mockData", true));
  }
}
