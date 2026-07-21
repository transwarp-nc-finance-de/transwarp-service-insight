package com.transwarp.serviceinsight.precheck.retrieval.infrastructure.jdbc;

import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceSnapshot;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.StoredEvidence;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalAuditPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRetrievalAuditRepository implements RetrievalAuditPort {
  private final JdbcTemplate jdbc;

  public JdbcRetrievalAuditRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(UUID runId, String ownerUserCode, RetrievalOutcome outcome, Instant createdAt) {
    jdbc.update(
        "INSERT INTO precheck_retrieval_audit(run_id, owner_user_code, rule_version, retrieval_mode, fts_status_code, embedding_status_code, retrieval_duration_ms, embedding_duration_ms, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        runId,
        ownerUserCode,
        outcome.fusion().ruleVersion(),
        outcome.mode(),
        outcome.fts().code(),
        outcome.embedding().code(),
        outcome.retrievalDurationMs(),
        outcome.embeddingDurationMs(),
        Timestamp.from(createdAt));
    for (var candidate : outcome.fusion().ftsCandidates()) {
      saveCandidate(runId, "FTS", candidate.candidate().chunkId(), candidate.rank(), outcome);
    }
    for (var candidate : outcome.fusion().vectorCandidates()) {
      saveCandidate(runId, "VECTOR", candidate.candidate().chunkId(), candidate.rank(), outcome);
    }
    for (var evidence : outcome.evidence()) saveEvidence(runId, evidence, createdAt);
  }

  @Override
  public Optional<StoredEvidence> findEvidence(UUID evidenceId) {
    return jdbc
        .query(
            "SELECT e.*, a.owner_user_code FROM precheck_evidence e JOIN precheck_retrieval_audit a ON a.run_id=e.run_id WHERE e.evidence_id = ?",
            (resultSet, rowNumber) ->
                new StoredEvidence(
                    new EvidenceSnapshot(
                        resultSet.getObject("evidence_id", UUID.class),
                        resultSet.getObject("document_id", UUID.class),
                        resultSet.getObject("version_id", UUID.class),
                        resultSet.getObject("chunk_id", UUID.class),
                        resultSet.getString("product_line_code"),
                        resultSet.getString("title"),
                        resultSet.getString("excerpt"),
                        resultSet.getString("content_hash"),
                        resultSet.getString("retrieval_mode"),
                        nullableInteger(resultSet, "fts_rank"),
                        nullableInteger(resultSet, "vector_rank"),
                        resultSet.getDouble("rrf_score"),
                        resultSet.getInt("selected_rank")),
                    resultSet.getObject("run_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getString("owner_user_code")),
            evidenceId)
        .stream()
        .findFirst();
  }

  private void saveCandidate(
      UUID runId, String branch, UUID chunkId, int branchRank, RetrievalOutcome outcome) {
    var selected =
        outcome.fusion().selected().stream()
            .filter(value -> value.candidate().chunkId().equals(chunkId))
            .findFirst();
    jdbc.update(
        "INSERT INTO precheck_retrieval_candidate(run_id, branch, chunk_id, branch_rank, rrf_score, selected_rank) VALUES (?, ?, ?, ?, ?, ?)",
        runId,
        branch,
        chunkId,
        branchRank,
        selected.map(value -> value.rrfScore()).orElse(null),
        selected.map(value -> value.selectedRank()).orElse(null));
  }

  private void saveEvidence(UUID runId, EvidenceSnapshot evidence, Instant createdAt) {
    jdbc.update(
        "INSERT INTO precheck_evidence(evidence_id, run_id, document_id, version_id, chunk_id, product_line_code, title, excerpt, content_hash, retrieval_mode, fts_rank, vector_rank, rrf_score, selected_rank, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        evidence.evidenceId(),
        runId,
        evidence.documentId(),
        evidence.versionId(),
        evidence.chunkId(),
        evidence.productLineCode(),
        evidence.title(),
        evidence.excerpt(),
        evidence.contentHash(),
        evidence.retrievalMode(),
        evidence.ftsRank(),
        evidence.vectorRank(),
        evidence.rrfScore(),
        evidence.selectedRank(),
        Timestamp.from(createdAt));
  }

  private Integer nullableInteger(java.sql.ResultSet resultSet, String column)
      throws java.sql.SQLException {
    var value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
  }
}
