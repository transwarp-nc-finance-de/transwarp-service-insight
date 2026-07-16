package com.transwarp.serviceinsight.knowledge.governance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class KnowledgeGovernancePolicyTest {
  @Test
  void onlyDraftCanBeSubmittedAndOnlyInReviewCanBeReturnedOrApproved() {
    assertThat(KnowledgeGovernancePolicy.submit("DRAFT")).isEqualTo("IN_REVIEW");
    assertThat(KnowledgeGovernancePolicy.returnToDraft("IN_REVIEW")).isEqualTo("DRAFT");
    assertThat(KnowledgeGovernancePolicy.approve("IN_REVIEW")).isEqualTo("APPROVED");

    assertThatThrownBy(() -> KnowledgeGovernancePolicy.submit("IN_REVIEW"))
        .isInstanceOf(IllegalGovernanceTransition.class);
    assertThatThrownBy(() -> KnowledgeGovernancePolicy.returnToDraft("DRAFT"))
        .isInstanceOf(IllegalGovernanceTransition.class);
    assertThatThrownBy(() -> KnowledgeGovernancePolicy.approve("APPROVED"))
        .isInstanceOf(IllegalGovernanceTransition.class);
  }

  @Test
  void approvalRequiresFreshParseAcknowledgedWarningsAndDifferentReviewer() {
    KnowledgeGovernancePolicy.validateApproval(
        "mock-editor",
        "mock-reviewer",
        "sha256:current",
        "sha256:current",
        Set.of("CONTENT_LOSS_SUSPECTED", "READING_ORDER_UNCERTAIN"),
        Set.of("CONTENT_LOSS_SUSPECTED", "READING_ORDER_UNCERTAIN"));

    assertThatThrownBy(
            () ->
                KnowledgeGovernancePolicy.validateApproval(
                    "same-user",
                    "same-user",
                    "sha256:current",
                    "sha256:current",
                    Set.of(),
                    Set.of()))
        .isInstanceOf(DutySeparationViolation.class);
    assertThatThrownBy(
            () ->
                KnowledgeGovernancePolicy.validateApproval(
                    "mock-editor",
                    "mock-reviewer",
                    "sha256:old",
                    "sha256:current",
                    Set.of(),
                    Set.of()))
        .isInstanceOf(IllegalGovernanceTransition.class);
    assertThatThrownBy(
            () ->
                KnowledgeGovernancePolicy.validateApproval(
                    "mock-editor",
                    "mock-reviewer",
                    "sha256:current",
                    "sha256:current",
                    Set.of("CONTENT_LOSS_SUSPECTED"),
                    Set.of()))
        .isInstanceOf(IllegalGovernanceTransition.class);
  }
}
