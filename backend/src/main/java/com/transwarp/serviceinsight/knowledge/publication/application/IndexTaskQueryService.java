package com.transwarp.serviceinsight.knowledge.publication.application;

import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeApiException;
import com.transwarp.serviceinsight.knowledge.publication.domain.IndexTask;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IndexTaskQueryService {
  private final KnowledgePublicationRepository repository;
  private final AuthSessionApplicationService authSessions;

  public IndexTaskQueryService(
      KnowledgePublicationRepository repository, AuthSessionApplicationService authSessions) {
    this.repository = repository;
    this.authSessions = authSessions;
  }

  public IndexTask get(String sessionCookie, UUID taskId) {
    var identity = authSessions.current(sessionCookie).identity();
    return repository
        .findVisibleTask(taskId, identity.userCode(), identity.productLineCodes())
        .orElseThrow(
            () ->
                new KnowledgeApiException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "资源不存在",
                    List.of(),
                    false,
                    Map.of()));
  }

  public KnowledgePublicationRepository.TaskPage list(
      String sessionCookie,
      int page,
      int size,
      String sortDirection,
      String sortBy,
      String status,
      UUID versionId) {
    var identity = authSessions.current(sessionCookie).identity();
    if (page < 1 || size < 1 || size > 100) {
      throw new KnowledgeApiException(
          HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "分页参数不符合契约",
          List.of(),
          false,
          Map.of("mockData", true));
    }
    if (!("ASC".equals(sortDirection) || "DESC".equals(sortDirection))
        || !("createdAt".equals(sortBy) || "status".equals(sortBy))
        || (status != null
            && !List.of("PENDING", "RUNNING", "SUCCEEDED", "FAILED").contains(status))) {
      throw new KnowledgeApiException(
          HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "任务筛选参数不符合契约",
          List.of(),
          false,
          Map.of("mockData", true));
    }
    return repository.listVisibleTasks(
        identity.productLineCodes(), page, size, sortDirection, sortBy, status, versionId);
  }
}
