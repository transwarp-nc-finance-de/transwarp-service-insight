package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.identity.api.V2FieldError;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.ingestion.api.KnowledgeDocumentMetadataRequest;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Created;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.UploadAggregate;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeIngestionService {
  private static final long TEXT_MAX_BYTES = 5L * 1024 * 1024;
  private static final long PDF_MAX_BYTES = 20L * 1024 * 1024;
  private static final Set<String> SOURCE_TYPES =
      Set.of("PRODUCT_MANUAL", "TRAINING_MATERIAL", "USER_MANUAL", "OPEN_SOURCE_REFERENCE", "MOCK");
  private final AuthSessionApplicationService authSessions;
  private final KnowledgeIngestionRepository repository;
  private final OriginalFileStorage fileStorage;
  private final KnowledgeParseProcessor parseProcessor;
  private final StructuredAuditPort audit;
  private final Clock clock;

  public KnowledgeIngestionService(
      AuthSessionApplicationService authSessions,
      KnowledgeIngestionRepository repository,
      OriginalFileStorage fileStorage,
      KnowledgeParseProcessor parseProcessor,
      StructuredAuditPort audit,
      Clock clock) {
    this.authSessions = authSessions;
    this.repository = repository;
    this.fileStorage = fileStorage;
    this.parseProcessor = parseProcessor;
    this.audit = audit;
    this.clock = clock;
  }

  public synchronized UploadResult upload(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      KnowledgeDocumentMetadataRequest metadata,
      MultipartFile file) {
    var session = authSessions.current(sessionCookie);
    requireCsrf(session.csrfToken(), csrfToken);
    var identity = session.identity();
    if (!identity.hasRole(Role.KNOWLEDGE_EDITOR)) {
      throw error(HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "当前身份无权执行该操作");
    }
    if (!identity.canAccessProductLine(metadata.productLine().code())) {
      throw error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在");
    }
    validateIdempotencyKey(idempotencyKey);
    if (!SOURCE_TYPES.contains(metadata.sourceType())) {
      throw validation("metadata.sourceType", "UNSUPPORTED", "知识来源类型不受支持");
    }
    var mediaType = normalizeMediaType(file.getContentType());
    var maxBytes = mediaType.equals("application/pdf") ? PDF_MAX_BYTES : TEXT_MAX_BYTES;
    if (file.getSize() > maxBytes) {
      throw new KnowledgeApiException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          "FILE_TOO_LARGE",
          "文件超过允许的原始字节上限",
          List.of(new V2FieldError("file", "MAX_BYTES", "文件超过允许的原始字节上限")),
          false,
          Map.of("mediaType", mediaType, "maxBytes", maxBytes, "mockData", true));
    }
    var bytes = bytes(file);
    var contentHash = sha256(bytes);
    var requestHash =
        sha256(
            String.join(
                    "\n",
                    identity.userCode(),
                    metadata.title(),
                    metadata.productLine().code(),
                    metadata.productLine().displayName(),
                    metadata.sourceType(),
                    mediaType,
                    contentHash)
                .getBytes(StandardCharsets.UTF_8));
    var existing = repository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      if (!existing.get().requestHash().equals(requestHash)) {
        throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求");
      }
      return new UploadResult(existing.get().created(), true);
    }

    var documentId = UUID.randomUUID();
    var versionId = UUID.randomUUID();
    var revisionId = UUID.randomUUID();
    var taskId = UUID.randomUUID();
    var fileId = UUID.randomUUID();
    String storageKey;
    try {
      storageKey = fileStorage.store(fileId, file.getOriginalFilename(), bytes);
    } catch (IOException exception) {
      throw error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "本地文件存储暂时不可用");
    }
    var aggregate =
        new UploadAggregate(
            documentId,
            versionId,
            revisionId,
            taskId,
            fileId,
            metadata.title(),
            new CatalogValue(metadata.productLine().code(), metadata.productLine().displayName()),
            metadata.sourceType(),
            identity.userCode(),
            storageKey,
            contentHash,
            bytes.length,
            mediaType,
            safeOriginalName(file.getOriginalFilename()),
            idempotencyKey,
            requestHash,
            clock.instant());
    try {
      var created = repository.create(aggregate);
      audit.record(
          new StoredAuditEvent(
              new StructuredAuditEvent(
                  UUID.randomUUID(),
                  identity.userCode(),
                  "KNOWLEDGE_VERSION_CREATED",
                  "KnowledgeVersion",
                  created.version().versionId(),
                  "SUCCEEDED",
                  Map.of(
                      "documentId", created.document().documentId().toString(),
                      "parseTaskId", created.parseTask().taskId().toString(),
                      "sourceType", metadata.sourceType(),
                      "productLineCode", metadata.productLine().code()),
                  clock.instant(),
                  true),
              metadata.productLine().code(),
              null));
      parseProcessor.enqueue(taskId);
      return new UploadResult(created, false);
    } catch (RuntimeException exception) {
      fileStorage.deleteIfPresent(storageKey);
      throw exception;
    }
  }

  private void requireCsrf(String expected, String actual) {
    if (actual == null
        || actual.isBlank()
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
      throw new CsrfValidationFailedException();
    }
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw validation("Idempotency-Key", "INVALID", "幂等键长度必须为 8 到 128");
    }
  }

  private String normalizeMediaType(String value) {
    if (value != null) {
      var normalized = value.toLowerCase();
      if (Set.of("text/markdown", "text/plain", "application/pdf").contains(normalized)) {
        return normalized;
      }
    }
    throw new KnowledgeApiException(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_FILE_TYPE",
        "仅支持 Markdown、TXT 和文本型 PDF",
        List.of(new V2FieldError("file", "UNSUPPORTED_MEDIA_TYPE", "文件类型不受支持")),
        false,
        Map.of("mockData", true));
  }

  private byte[] bytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "上传文件暂时不可读取");
    }
  }

  private String safeOriginalName(String value) {
    return value == null || value.isBlank()
        ? "upload"
        : java.nio.file.Path.of(value).getFileName().toString();
  }

  private String sha256(byte[] value) {
    try {
      return "sha256:"
          + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private KnowledgeApiException validation(String field, String code, String message) {
    return new KnowledgeApiException(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "请求字段校验失败",
        List.of(new V2FieldError(field, code, message)),
        false,
        Map.of("mockData", true));
  }

  private KnowledgeApiException error(HttpStatus status, String code, String message) {
    return new KnowledgeApiException(
        status, code, message, List.of(), false, Map.of("mockData", true));
  }

  public record UploadResult(Created created, boolean replayed) {}
}
