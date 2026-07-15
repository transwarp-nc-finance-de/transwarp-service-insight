package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ParseTaskQueryService {
  private final AuthSessionApplicationService authSessions;
  private final KnowledgeIngestionRepository repository;

  public ParseTaskQueryService(
      AuthSessionApplicationService authSessions, KnowledgeIngestionRepository repository) {
    this.authSessions = authSessions;
    this.repository = repository;
  }

  public ParseTask get(String sessionCookie, UUID taskId) {
    var identity = authSessions.current(sessionCookie).identity();
    return repository
        .findVisibleTask(taskId, identity)
        .orElseThrow(
            () ->
                new KnowledgeApiException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "The resource does not exist.",
                    List.of(),
                    false,
                    Map.of("mockData", true)));
  }
}
