package com.transwarp.serviceinsight.precheck.retrieval.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HybridRetrievalPolicy {
  public static final String RULE_VERSION = "rrf-k60-top20-v1";
  private static final int BRANCH_LIMIT = 20;
  private static final int RESULT_LIMIT = 5;
  private static final int RRF_K = 60;

  public FusionResult fuse(
      List<RetrievalCandidate> ftsCandidates, List<RetrievalCandidate> vectorCandidates) {
    var fts = ranked(ftsCandidates);
    var vector = ranked(vectorCandidates);
    var merged = new LinkedHashMap<CandidateKey, MutableScore>();
    contribute(merged, fts, true);
    contribute(merged, vector, false);
    var selected =
        merged.values().stream()
            .sorted(
                Comparator.comparingDouble(MutableScore::score)
                    .reversed()
                    .thenComparing(value -> value.candidate().versionId())
                    .thenComparing(value -> value.candidate().chunkId()))
            .limit(RESULT_LIMIT)
            .toList();
    var fused = new ArrayList<FusedCandidate>();
    for (int index = 0; index < selected.size(); index++) {
      var value = selected.get(index);
      fused.add(
          new FusedCandidate(
              value.candidate(), value.ftsRank(), value.vectorRank(), value.score(), index + 1));
    }
    return new FusionResult(RULE_VERSION, fts, vector, List.copyOf(fused));
  }

  private List<BranchCandidate> ranked(List<RetrievalCandidate> candidates) {
    var limited = candidates.stream().limit(BRANCH_LIMIT).toList();
    var result = new ArrayList<BranchCandidate>();
    for (int index = 0; index < limited.size(); index++) {
      result.add(new BranchCandidate(limited.get(index), index + 1));
    }
    return List.copyOf(result);
  }

  private void contribute(
      Map<CandidateKey, MutableScore> merged, List<BranchCandidate> candidates, boolean fts) {
    for (var branch : candidates) {
      var candidate = branch.candidate();
      var value =
          merged.computeIfAbsent(
              new CandidateKey(candidate.versionId(), candidate.chunkId()),
              ignored -> new MutableScore(candidate));
      value.add(branch.rank(), fts);
    }
  }

  public record BranchCandidate(RetrievalCandidate candidate, int rank) {}

  public record FusedCandidate(
      RetrievalCandidate candidate,
      Integer ftsRank,
      Integer vectorRank,
      double rrfScore,
      int selectedRank) {}

  public record FusionResult(
      String ruleVersion,
      List<BranchCandidate> ftsCandidates,
      List<BranchCandidate> vectorCandidates,
      List<FusedCandidate> selected) {}

  private record CandidateKey(java.util.UUID versionId, java.util.UUID chunkId) {}

  private static final class MutableScore {
    private final RetrievalCandidate candidate;
    private Integer ftsRank;
    private Integer vectorRank;
    private double score;

    private MutableScore(RetrievalCandidate candidate) {
      this.candidate = candidate;
    }

    private void add(int rank, boolean fts) {
      score += 1.0 / (RRF_K + rank);
      if (fts) ftsRank = rank;
      else vectorRank = rank;
    }

    private RetrievalCandidate candidate() {
      return candidate;
    }

    private Integer ftsRank() {
      return ftsRank;
    }

    private Integer vectorRank() {
      return vectorRank;
    }

    private double score() {
      return score;
    }
  }
}
