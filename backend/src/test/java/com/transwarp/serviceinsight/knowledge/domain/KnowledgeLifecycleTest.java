package com.transwarp.serviceinsight.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transwarp.serviceinsight.knowledge.application.KnowledgeApplicationService;
import com.transwarp.serviceinsight.knowledge.infrastructure.memory.InMemoryKnowledgeRepository;
import com.transwarp.serviceinsight.knowledge.infrastructure.mock.MockDataAccessPolicy;
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
  void classifiesAuthorizationWithoutHardCodingEnvironmentPolicy() {
    var document = new KnowledgeDocument(UUID.randomUUID(), "公开资料元数据", false);
    assertThat(document.dataClassification())
        .isEqualTo(KnowledgeDocument.DataClassification.INTERNAL);
    assertThat(document.authorizationStatus())
        .isEqualTo(KnowledgeDocument.AuthorizationStatus.PENDING);
    assertThat(new MockDataAccessPolicy().allows(document)).isFalse();
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
