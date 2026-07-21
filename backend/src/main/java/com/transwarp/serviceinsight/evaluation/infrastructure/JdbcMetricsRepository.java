package com.transwarp.serviceinsight.evaluation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.Metrics;
import com.transwarp.serviceinsight.evaluation.port.MetricsRepository;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMetricsRepository implements MetricsRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public JdbcMetricsRepository(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  @Override
  public Metrics calculate(Instant from, Instant to, List<String> scope) {
    var rows =
        jdbc
            .query(
                "SELECT s.session_id, s.host_request_id, r.sequence_number, r.status, r.context_snapshot, a.retrieval_mode, a.retrieval_duration_ms, a.embedding_duration_ms, (SELECT COUNT(*) FROM precheck_evidence e WHERE e.run_id=r.run_id) evidence_count FROM precheck_session_v2 s JOIN precheck_run_v2 r ON r.session_id=s.session_id JOIN precheck_retrieval_audit a ON a.run_id=r.run_id WHERE s.created_at>=? AND s.created_at<?",
                (rs, row) ->
                    new Row(
                        rs.getObject("session_id", UUID.class),
                        rs.getString("host_request_id"),
                        rs.getInt("sequence_number"),
                        rs.getString("status"),
                        productLine(rs.getString("context_snapshot")),
                        rs.getString("retrieval_mode"),
                        rs.getLong("retrieval_duration_ms"),
                        rs.getLong("embedding_duration_ms"),
                        rs.getInt("evidence_count")),
                Timestamp.from(from),
                Timestamp.from(to))
            .stream()
            .filter(row -> !row.hostRequestId().startsWith("evaluation-"))
            .filter(row -> scope.contains(row.productLineCode()))
            .toList();
    var sessions = new LinkedHashMap<UUID, List<Row>>();
    rows.forEach(
        row -> sessions.computeIfAbsent(row.sessionId(), ignored -> new ArrayList<>()).add(row));
    long successful =
        sessions.values().stream()
            .filter(
                values ->
                    values.stream()
                        .max(Comparator.comparingInt(Row::sequence))
                        .map(row -> "COMPLETED".equals(row.status()))
                        .orElse(false))
            .count();
    long supplemented = sessions.values().stream().filter(values -> values.size() > 1).count();
    long degraded = rows.stream().filter(row -> !"HYBRID".equals(row.retrievalMode())).count();
    long evidenceHits = rows.stream().filter(row -> row.evidenceCount() > 0).count();
    long feedback =
        count(
            "SELECT COUNT(*) FROM precheck_feedback_v2 WHERE recorded_at>=? AND recorded_at<? AND product_line_code IN ("
                + placeholders(scope)
                + ")",
            from,
            to,
            scope,
            null);
    long adopted =
        count(
            "SELECT COUNT(*) FROM precheck_feedback_v2 WHERE recorded_at>=? AND recorded_at<? AND adoption_status IN ('ADOPTED','PARTIALLY_ADOPTED') AND product_line_code IN ("
                + placeholders(scope)
                + ")",
            from,
            to,
            scope,
            null);
    long continuations =
        count(
            "SELECT COUNT(*) FROM submission_continuation_v2 WHERE confirmed_at>=? AND confirmed_at<? AND product_line_code IN ("
                + placeholders(scope)
                + ")",
            from,
            to,
            scope,
            null);
    return new Metrics(
        from,
        to,
        sessions.size(),
        ratio(successful, sessions.size()),
        ratio(degraded, rows.size()),
        ratio(rows.size(), sessions.size()),
        ratio(supplemented, sessions.size()),
        ratio(evidenceHits, rows.size()),
        ratio(adopted, feedback),
        ratio(continuations, sessions.size()),
        p95(rows.stream().map(Row::retrievalMs).toList()),
        p95(rows.stream().map(Row::embeddingMs).toList()),
        true);
  }

  private String productLine(String context) {
    try {
      return json.readValue(context, PrecheckContext.class).productLine().code();
    } catch (Exception exception) {
      throw new IllegalStateException("Stored precheck context is invalid", exception);
    }
  }

  private long count(String sql, Instant from, Instant to, List<String> scope, Object ignored) {
    if (scope.isEmpty()) return 0;
    var args = new ArrayList<Object>();
    args.add(Timestamp.from(from));
    args.add(Timestamp.from(to));
    args.addAll(scope);
    return jdbc.queryForObject(sql, Long.class, args.toArray());
  }

  private String placeholders(List<String> values) {
    return String.join(",", Collections.nCopies(values.size(), "?"));
  }

  private double ratio(long value, long total) {
    return total == 0 ? 0 : (double) value / total;
  }

  private long p95(List<Long> values) {
    if (values.isEmpty()) return 0;
    var sorted = values.stream().sorted().toList();
    return sorted.get(Math.max(0, (int) Math.ceil(sorted.size() * .95) - 1));
  }

  private record Row(
      UUID sessionId,
      String hostRequestId,
      int sequence,
      String status,
      String productLineCode,
      String retrievalMode,
      long retrievalMs,
      long embeddingMs,
      int evidenceCount) {}
}
