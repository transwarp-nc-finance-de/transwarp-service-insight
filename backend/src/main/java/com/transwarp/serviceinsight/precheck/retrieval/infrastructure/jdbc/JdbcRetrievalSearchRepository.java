package com.transwarp.serviceinsight.precheck.retrieval.infrastructure.jdbc;

import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalCandidate;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalSearchPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcRetrievalSearchRepository implements RetrievalSearchPort {
  private static final String VISIBLE_FROM =
      " FROM knowledge_chunk_index i JOIN knowledge_version_v2 v ON v.version_id=i.version_id JOIN knowledge_document d ON d.document_id=v.document_id WHERE v.status='PUBLISHED' AND d.current_published_version_id=v.version_id AND i.product_line_code IN (:productLines) ";

  private final NamedParameterJdbcTemplate jdbc;
  private final boolean postgresql;

  public JdbcRetrievalSearchRepository(NamedParameterJdbcTemplate jdbc, DataSource dataSource)
      throws SQLException {
    this.jdbc = jdbc;
    try (var connection = dataSource.getConnection()) {
      this.postgresql =
          connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public List<RetrievalCandidate> searchFts(String query, List<String> productLineCodes) {
    var parameters =
        Map.<String, Object>of("ftsQuery", websearchQuery(query), "productLines", productLineCodes);
    if (postgresql) {
      return jdbc.query(
          "SELECT d.document_id, v.version_id, i.chunk_id, i.product_line_code, d.title, i.text_content, i.content_hash"
              + VISIBLE_FROM
              + "AND i.fts_document @@ websearch_to_tsquery('simple', :ftsQuery) ORDER BY ts_rank_cd(i.fts_document, websearch_to_tsquery('simple', :ftsQuery)) DESC, v.version_id, i.chunk_id LIMIT 20",
          parameters,
          this::candidate);
    }
    return jdbc
        .query(
            "SELECT d.document_id, v.version_id, i.chunk_id, i.product_line_code, d.title, i.text_content, i.content_hash"
                + VISIBLE_FROM,
            parameters,
            this::candidate)
        .stream()
        .map(candidate -> new Scored(candidate, lexicalScore(candidate.text(), query)))
        .filter(value -> value.score() > 0)
        .sorted(
            Comparator.comparingInt(Scored::score)
                .reversed()
                .thenComparing(value -> value.candidate().versionId())
                .thenComparing(value -> value.candidate().chunkId()))
        .limit(20)
        .map(Scored::candidate)
        .toList();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public List<RetrievalCandidate> searchVector(
      float[] queryEmbedding, List<String> productLineCodes) {
    var vector = vector(queryEmbedding);
    var parameters = Map.<String, Object>of("embedding", vector, "productLines", productLineCodes);
    if (postgresql) {
      return jdbc.query(
          "SELECT d.document_id, v.version_id, i.chunk_id, i.product_line_code, d.title, i.text_content, i.content_hash"
              + VISIBLE_FROM
              + "ORDER BY i.embedding <=> CAST(:embedding AS vector), v.version_id, i.chunk_id LIMIT 20",
          parameters,
          this::candidate);
    }
    return jdbc
        .query(
            "SELECT d.document_id, v.version_id, i.chunk_id, i.product_line_code, d.title, i.text_content, i.content_hash, i.embedding"
                + VISIBLE_FROM,
            Map.of("productLines", productLineCodes),
            (resultSet, rowNumber) ->
                new VectorScored(
                    candidate(resultSet, rowNumber),
                    cosineDistance(queryEmbedding, parseVector(resultSet.getString("embedding")))))
        .stream()
        .sorted(
            Comparator.comparingDouble(VectorScored::distance)
                .thenComparing(value -> value.candidate().versionId())
                .thenComparing(value -> value.candidate().chunkId()))
        .limit(20)
        .map(VectorScored::candidate)
        .toList();
  }

  private RetrievalCandidate candidate(ResultSet resultSet, int rowNumber) throws SQLException {
    return new RetrievalCandidate(
        resultSet.getObject("document_id", java.util.UUID.class),
        resultSet.getObject("version_id", java.util.UUID.class),
        resultSet.getObject("chunk_id", java.util.UUID.class),
        resultSet.getString("product_line_code"),
        resultSet.getString("title"),
        resultSet.getString("text_content"),
        resultSet.getString("content_hash"));
  }

  private int lexicalScore(String text, String query) {
    var normalized = text.toLowerCase(java.util.Locale.ROOT);
    var score = 0;
    for (var token : query.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}_-]+")) {
      if (token.length() > 1 && normalized.contains(token)) score++;
    }
    return score;
  }

  private String websearchQuery(String query) {
    return java.util.Arrays.stream(query.split("[^\\p{L}\\p{N}_-]+"))
        .filter(token -> token.length() > 1)
        .distinct()
        .map(token -> "\"" + token.replace("\"", "") + "\"")
        .collect(java.util.stream.Collectors.joining(" OR "));
  }

  private String vector(float[] values) {
    var result = new StringBuilder("[");
    for (int index = 0; index < values.length; index++) {
      if (index > 0) result.append(',');
      result.append(Float.toString(values[index]));
    }
    return result.append(']').toString();
  }

  private float[] parseVector(String value) {
    var body = value.substring(1, value.length() - 1);
    if (body.isBlank()) return new float[0];
    var parts = body.split(",");
    var result = new float[parts.length];
    for (int index = 0; index < parts.length; index++)
      result[index] = Float.parseFloat(parts[index]);
    return result;
  }

  private double cosineDistance(float[] left, float[] right) {
    if (left.length != right.length) return Double.POSITIVE_INFINITY;
    double dot = 0;
    double leftNorm = 0;
    double rightNorm = 0;
    for (int index = 0; index < left.length; index++) {
      dot += left[index] * right[index];
      leftNorm += left[index] * left[index];
      rightNorm += right[index] * right[index];
    }
    if (leftNorm == 0 || rightNorm == 0) return 1;
    return 1 - dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
  }

  private record Scored(RetrievalCandidate candidate, int score) {}

  private record VectorScored(RetrievalCandidate candidate, double distance) {}
}
