package com.transwarp.serviceinsight.precheck.retrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridRetrievalPolicyTest {
  private static final UUID VERSION_A = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID VERSION_B = UUID.fromString("00000000-0000-4000-8000-000000000002");
  private static final UUID CHUNK_A = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID CHUNK_B = UUID.fromString("10000000-0000-4000-8000-000000000002");

  @Test
  void fusesTopTwentyCandidatesWithKSixtyAndStableUuidTieBreak() {
    var first = candidate(VERSION_B, CHUNK_B);
    var second = candidate(VERSION_A, CHUNK_A);

    var result = new HybridRetrievalPolicy().fuse(List.of(first, second), List.of(second, first));

    assertThat(result.ruleVersion()).isEqualTo("rrf-k60-top20-v1");
    assertThat(result.selected())
        .extracting(item -> item.candidate().chunkId())
        .containsExactly(CHUNK_A, CHUNK_B);
    assertThat(result.selected())
        .allSatisfy(item -> assertThat(item.rrfScore()).isEqualTo(1.0 / 61 + 1.0 / 62));
    assertThat(result.selected())
        .extracting(HybridRetrievalPolicy.FusedCandidate::selectedRank)
        .containsExactly(1, 2);
  }

  @Test
  void ignoresCandidatesAfterBranchTopTwentyAndReturnsOnlyTopFive() {
    var candidates =
        java.util.stream.IntStream.rangeClosed(1, 25)
            .mapToObj(
                index ->
                    candidate(
                        UUID.fromString(String.format("00000000-0000-4000-8000-%012d", index)),
                        UUID.fromString(String.format("10000000-0000-4000-8000-%012d", index))))
            .toList();

    var result = new HybridRetrievalPolicy().fuse(candidates, List.of());

    assertThat(result.selected()).hasSize(5);
    assertThat(result.ftsCandidates()).hasSize(20);
    assertThat(result.selected())
        .extracting(item -> item.candidate().chunkId())
        .containsExactlyElementsOf(
            candidates.stream().limit(5).map(RetrievalCandidate::chunkId).toList());
  }

  private RetrievalCandidate candidate(UUID versionId, UUID chunkId) {
    return new RetrievalCandidate(
        UUID.randomUUID(), versionId, chunkId, "TDH", "模拟数据：知识", "模拟数据：内容", "sha256:fixture");
  }
}
