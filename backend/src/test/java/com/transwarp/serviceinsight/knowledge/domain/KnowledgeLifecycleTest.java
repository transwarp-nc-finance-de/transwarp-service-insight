package com.transwarp.serviceinsight.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transwarp.serviceinsight.knowledge.application.KnowledgeApplicationService;
import com.transwarp.serviceinsight.knowledge.infrastructure.memory.InMemoryKnowledgeRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeLifecycleTest {
  @Test
  void onlyPublishedApprovedMockVersionIsSearchable() {
    var repository = new InMemoryKnowledgeRepository();
    var service = new KnowledgeApplicationService(repository);
    var draft = version();
    repository.save(draft);

    assertThat(draft.searchable()).isFalse();
    service.transition(draft.id(), KnowledgeStatus.IN_REVIEW);
    service.transition(draft.id(), KnowledgeStatus.APPROVED);
    var published = service.transition(draft.id(), KnowledgeStatus.PUBLISHED);

    assertThat(published.searchable()).isTrue();
    assertThat(repository.findById(draft.id())).contains(published);
  }

  @Test
  void rejectsPublishingWithoutReviewAndApproval() {
    var draft = version();
    assertThatThrownBy(() -> draft.transitionTo(KnowledgeStatus.PUBLISHED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("不允许");
  }

  @Test
  void rejectsNonMockDocumentInCurrentStage() {
    assertThatThrownBy(() -> new KnowledgeDocument(UUID.randomUUID(), "真实资料", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("模拟数据");
  }

  private KnowledgeVersion version() {
    return new KnowledgeVersion(
        UUID.randomUUID(),
        UUID.randomUUID(),
        1,
        KnowledgeStatus.DRAFT,
        "mock-digest-only-no-content",
        "mock-scope",
        Instant.now());
  }
}
