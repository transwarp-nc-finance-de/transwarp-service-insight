package com.transwarp.serviceinsight.admin.reset.application;

import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminReset;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminResetPage;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.CreationResult;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.PageMetadata;
import com.transwarp.serviceinsight.admin.reset.port.AdminResetRepository;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminResetService {
  public static final String CONFIRMATION_PHRASE = "RESET LOCAL MOCK DATA";

  private final AdminResetRepository repository;
  private final AuthSessionApplicationService authSessions;
  private final Clock clock;
  private final String configuredEnvironment;

  public AdminResetService(
      AdminResetRepository repository,
      AuthSessionApplicationService authSessions,
      Clock clock,
      @Value("${app.environment-code:LOCAL}") String configuredEnvironment) {
    this.repository = repository;
    this.authSessions = authSessions;
    this.clock = clock;
    this.configuredEnvironment = configuredEnvironment;
  }

  @Transactional
  public CreationResult create(
      String environmentCode,
      String confirmationPhrase,
      String reason,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    validateRequest(environmentCode, confirmationPhrase, reason);
    if (!"LOCAL".equals(configuredEnvironment)) {
      throw new PrecheckV2Exception(
          "ADMIN_RESET_NOT_AVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "受控重置仅在明确标识的本地环境可用",
          Map.of("mockData", true));
    }
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requireAdmin(auth.identity().hasRole(Role.ADMIN));
    var normalizedReason = normalize(reason);
    var requestHash = hash(environmentCode + ":" + confirmationPhrase + ":" + normalizedReason);
    repository.lockIdempotency(idempotencyKey);
    var replay = repository.findIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) {
        throw new PrecheckV2Exception(
            "IDEMPOTENCY_KEY_CONFLICT",
            HttpStatus.CONFLICT,
            "幂等键已用于不同请求",
            Map.of("mockData", true));
      }
      return new CreationResult(replay.get().reset(), true);
    }
    var now = clock.instant();
    var taskId = UUID.randomUUID();
    var reset =
        new AdminReset(
            taskId,
            taskId,
            "PENDING",
            0,
            3,
            null,
            null,
            now,
            null,
            null,
            true,
            "LOCAL",
            auth.identity().userCode(),
            UUID.randomUUID());
    repository.create(reset, normalizedReason, idempotencyKey, requestHash, now);
    return new CreationResult(reset, false);
  }

  @Transactional(readOnly = true)
  public AdminReset get(UUID taskId, String sessionCookie) {
    requireAdmin(authSessions.current(sessionCookie).identity().hasRole(Role.ADMIN));
    return repository
        .findById(taskId)
        .orElseThrow(
            () ->
                new PrecheckV2Exception(
                    "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of("mockData", true)));
  }

  @Transactional(readOnly = true)
  public AdminResetPage list(
      int page, int size, String sortBy, String direction, String status, String sessionCookie) {
    requireAdmin(authSessions.current(sessionCookie).identity().hasRole(Role.ADMIN));
    if (page < 0 || size < 1 || size > 100) throw validation("page", "分页参数不合法");
    if (!List.of("createdAt", "status").contains(sortBy)) throw validation("sortBy", "排序字段不合法");
    if (!List.of("asc", "desc").contains(direction.toLowerCase()))
      throw validation("sortDirection", "排序方向不合法");
    if (status != null && !List.of("PENDING", "RUNNING", "SUCCEEDED", "FAILED").contains(status))
      throw validation("status", "任务状态不合法");
    Comparator<AdminReset> comparator =
        "status".equals(sortBy)
            ? Comparator.comparing(AdminReset::status)
            : Comparator.comparing(AdminReset::createdAt);
    if ("desc".equalsIgnoreCase(direction)) comparator = comparator.reversed();
    var values =
        repository.findAll().stream()
            .filter(item -> status == null || status.equals(item.status()))
            .sorted(comparator.thenComparing(AdminReset::taskId))
            .toList();
    int start = Math.min(page * size, values.size());
    var items = values.subList(start, Math.min(start + size, values.size()));
    return new AdminResetPage(
        items,
        new PageMetadata(
            page,
            size,
            values.size(),
            values.isEmpty() ? 0 : (int) Math.ceil((double) values.size() / size),
            sortBy,
            direction.toLowerCase()));
  }

  private void validateRequest(String environment, String phrase, String reason) {
    if (!"LOCAL".equals(environment)) throw validation("environmentCode", "环境代码必须为 LOCAL");
    if (!CONFIRMATION_PHRASE.equals(phrase)) throw validation("confirmationPhrase", "确认短语不正确");
    if (reason == null || reason.isBlank() || reason.length() > 1000)
      throw validation("reason", "原因长度必须为 1 到 1000");
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128)
      throw validation("Idempotency-Key", "幂等键长度必须为 8 到 128");
  }

  private void requireAdmin(boolean allowed) {
    if (!allowed)
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权执行本地重置", Map.of("mockData", true));
  }

  private PrecheckV2Exception validation(String field, String message) {
    return new PrecheckV2Exception(
        "VALIDATION_ERROR",
        HttpStatus.BAD_REQUEST,
        message,
        Map.of("field", field, "mockData", true));
  }

  private String normalize(String value) {
    return Normalizer.normalize(
        value.replace("\r\n", "\n").replace('\r', '\n').trim(), Normalizer.Form.NFC);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
