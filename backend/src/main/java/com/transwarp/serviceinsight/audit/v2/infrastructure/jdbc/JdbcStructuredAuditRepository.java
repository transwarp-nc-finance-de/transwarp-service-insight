package com.transwarp.serviceinsight.audit.v2.infrastructure.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStructuredAuditRepository implements StructuredAuditPort {
  private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public JdbcStructuredAuditRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Override
  public void record(StoredAuditEvent stored) {
    var event = stored.event();
    jdbc.update(
        "INSERT INTO audit_event_v2(event_id, actor_user_code, action, subject_type, subject_id, outcome, product_line_code, error_code, metadata, occurred_at, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
        event.eventId(),
        event.actorUserCode(),
        event.action(),
        event.subjectType(),
        event.subjectId(),
        event.outcome(),
        stored.productLineCode(),
        stored.errorCode(),
        json(event.metadata()),
        Timestamp.from(event.occurredAt()));
  }

  @Override
  public List<StoredAuditEvent> find(
      List<String> productLineCodes,
      String action,
      UUID subjectId,
      int offset,
      int limit,
      String sortBy,
      String sortDirection) {
    var query = query(productLineCodes, action, subjectId);
    var column = "action".equals(sortBy) ? "action" : "occurred_at";
    var direction = "ASC".equals(sortDirection) ? "ASC" : "DESC";
    query.sql().append(" ORDER BY ").append(column).append(' ').append(direction);
    query.sql().append(", event_id ").append(direction).append(" LIMIT ? OFFSET ?");
    query.args().add(limit);
    query.args().add(offset);
    return jdbc.query(query.sql().toString(), this::event, query.args().toArray());
  }

  @Override
  public long count(List<String> productLineCodes, String action, UUID subjectId) {
    var query = query(productLineCodes, action, subjectId);
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM (" + query.sql() + ") visible_audit",
        Long.class,
        query.args().toArray());
  }

  @Override
  public Optional<StoredAuditEvent> findById(UUID eventId, List<String> productLineCodes) {
    var args = new ArrayList<Object>();
    args.add(eventId);
    args.addAll(productLineCodes);
    return jdbc
        .query(
            "SELECT * FROM audit_event_v2 WHERE event_id = ? AND "
                + visibilityCondition(productLineCodes),
            this::event,
            args.toArray())
        .stream()
        .findFirst();
  }

  private Query query(List<String> productLineCodes, String action, UUID subjectId) {
    var sql =
        new StringBuilder(
            "SELECT * FROM audit_event_v2 WHERE " + visibilityCondition(productLineCodes));
    var args = new ArrayList<Object>(productLineCodes);
    if (action != null && !action.isBlank()) {
      sql.append(" AND action = ?");
      args.add(action);
    }
    if (subjectId != null) {
      sql.append(" AND subject_id = ?");
      args.add(subjectId);
    }
    return new Query(sql, args);
  }

  private String placeholders(List<String> values) {
    return String.join(",", java.util.Collections.nCopies(values.size(), "?"));
  }

  private String visibilityCondition(List<String> productLineCodes) {
    if (productLineCodes.isEmpty()) return "product_line_code IS NULL";
    return "(product_line_code IS NULL OR product_line_code IN ("
        + placeholders(productLineCodes)
        + "))";
  }

  private StoredAuditEvent event(ResultSet resultSet, int rowNumber) throws SQLException {
    try {
      return new StoredAuditEvent(
          new StructuredAuditEvent(
              resultSet.getObject("event_id", UUID.class),
              resultSet.getString("actor_user_code"),
              resultSet.getString("action"),
              resultSet.getString("subject_type"),
              resultSet.getObject("subject_id", UUID.class),
              resultSet.getString("outcome"),
              objectMapper.readValue(resultSet.getString("metadata"), METADATA_TYPE),
              resultSet.getTimestamp("occurred_at").toInstant(),
              resultSet.getBoolean("mock_data")),
          resultSet.getString("product_line_code"),
          resultSet.getString("error_code"));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Stored audit metadata is invalid", exception);
    }
  }

  private String json(Map<String, Object> metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Audit metadata is invalid", exception);
    }
  }

  private record Query(StringBuilder sql, ArrayList<Object> args) {}
}
