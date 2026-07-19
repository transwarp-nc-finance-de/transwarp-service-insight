package com.transwarp.serviceinsight.knowledge.publication.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.identity.api.V2FieldError;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeApiException;
import com.transwarp.serviceinsight.knowledge.publication.api.CommandReasonRequest;
import com.transwarp.serviceinsight.knowledge.publication.domain.IndexTask;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class KnowledgePublicationService {
  private final KnowledgePublicationRepository repository;
  private final AuthSessionApplicationService authSessions;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final KnowledgeIndexProcessor processor;

  public KnowledgePublicationService(
      KnowledgePublicationRepository repository,
      AuthSessionApplicationService authSessions,
      ObjectMapper objectMapper,
      Clock clock,
      KnowledgeIndexProcessor processor) {
    this.repository = repository;
    this.authSessions = authSessions;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.processor = processor;
  }

  @Transactional
  public PublicationResponse publish(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      CommandReasonRequest request) {
    var session = authSessions.current(sessionCookie);
    requireCsrf(session.csrfToken(), csrfToken);
    var identity = session.identity();
    if (!identity.hasRole(Role.KNOWLEDGE_REVIEWER)) {
      throw error(HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "当前身份无权发布知识版本");
    }
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("PUBLISH", idempotencyKey);
    var requestHash = requestHash(versionId, identity.userCode(), request.reason());
    var existing = repository.findIdempotency("PUBLISH", idempotencyKey);
    if (existing.isPresent()) {
      if (!existing.get().requestHash().equals(requestHash)) {
        throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求");
      }
      return new PublicationResponse(existing.get().task(), true);
    }
    var version =
        repository
            .lockApprovedVersion(versionId)
            .orElseThrow(
                () ->
                    error(
                        HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION", "仅 APPROVED 知识版本可创建发布任务"));
    if (!identity.canAccessProductLine(version.productLineCode())) {
      throw error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在");
    }
    if (repository.hasActiveTask(versionId)) {
      throw error(HttpStatus.CONFLICT, "INDEX_TASK_ALREADY_ACTIVE", "该知识版本已有进行中的索引任务");
    }
    var task =
        repository.createTask(
            UUID.randomUUID(),
            version,
            identity.userCode(),
            idempotencyKey,
            requestHash,
            clock.instant());
    enqueueAfterCommit(task.taskId());
    return new PublicationResponse(task, false);
  }

  private void requireCsrf(String expected, String actual) {
    if (actual == null
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
      throw new CsrfValidationFailedException();
    }
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw new KnowledgeApiException(
          HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "请求字段校验失败",
          List.of(new V2FieldError("Idempotency-Key", "INVALID", "幂等键长度必须为 8 到 128")),
          false,
          Map.of("mockData", true));
    }
  }

  private String requestHash(Object... values) {
    try {
      return "sha256:"
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(objectMapper.writeValueAsBytes(values)));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("无法规范化发布请求", exception);
    }
  }

  private KnowledgeApiException error(HttpStatus status, String code, String message) {
    return new KnowledgeApiException(
        status, code, message, List.of(), false, Map.of("mockData", true));
  }

  private void enqueueAfterCommit(UUID taskId) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            processor.enqueue(taskId);
          }
        });
  }

  public record PublicationResponse(IndexTask task, boolean replayed) {}
}
