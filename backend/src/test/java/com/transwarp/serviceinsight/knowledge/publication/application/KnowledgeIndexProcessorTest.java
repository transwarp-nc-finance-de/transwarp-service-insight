package com.transwarp.serviceinsight.knowledge.publication.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transwarp.serviceinsight.knowledge.publication.domain.IndexAttemptPolicy;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository.IndexChunk;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository.IndexInput;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

class KnowledgeIndexProcessorTest {
  @Test
  void vectorBranchStillRunsWhenFtsBranchFails() {
    var repository = mock(KnowledgePublicationRepository.class);
    var embedding = mock(EmbeddingPort.class);
    var taskId = UUID.randomUUID();
    var input =
        new IndexInput(
            taskId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "TDH",
            "mock-knowledge-reviewer",
            3,
            3,
            List.of(new IndexChunk(UUID.randomUUID(), "模拟数据：FTS 故障", "sha256:test")));
    when(repository.claimPendingTask(eq(taskId), any())).thenReturn(Optional.of(input));
    when(embedding.embedPassages(List.of("模拟数据：FTS 故障"))).thenReturn(List.of(new float[768]));
    doThrow(new TransientDataAccessResourceException("模拟数据：FTS 暂时不可用"))
        .when(repository)
        .markFtsPrepared(input);
    var processor =
        new KnowledgeIndexProcessor(
            repository,
            embedding,
            new IndexAttemptPolicy(),
            Clock.fixed(Instant.parse("2026-07-19T15:00:00Z"), ZoneOffset.UTC),
            Runnable::run);

    processor.enqueue(taskId);

    verify(embedding).embedPassages(List.of("模拟数据：FTS 故障"));
    verify(repository)
        .failTask(
            eq(input),
            eq("FTS"),
            eq("FTS_TEMPORARILY_UNAVAILABLE"),
            any(),
            eq(false),
            eq(Instant.parse("2026-07-19T15:00:00Z")));
  }
}
