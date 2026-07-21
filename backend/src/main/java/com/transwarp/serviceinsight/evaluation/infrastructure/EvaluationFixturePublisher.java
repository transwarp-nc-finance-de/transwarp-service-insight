package com.transwarp.serviceinsight.evaluation.infrastructure;

import com.transwarp.serviceinsight.evaluation.domain.EvidenceFixtureManifest;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EvaluationFixturePublisher
    implements com.transwarp.serviceinsight.evaluation.port.EvaluationFixturePort {
  private final JdbcTemplate jdbc;
  private final EmbeddingPort embedding;
  private final boolean postgresql;

  public EvaluationFixturePublisher(
      JdbcTemplate jdbc, EmbeddingPort embedding, DataSource dataSource)
      throws java.sql.SQLException {
    this.jdbc = jdbc;
    this.embedding = embedding;
    try (var connection = dataSource.getConnection()) {
      this.postgresql =
          connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }
  }

  public void publish(EvidenceFixtureManifest manifest) {
    var pending =
        manifest.evidenceFixtures().stream()
            .filter(item -> !exists(UUID.fromString(item.chunkId())))
            .toList();
    if (pending.isEmpty()) return;
    var texts =
        pending.stream().map(item -> item.document().title() + ". " + item.excerpt()).toList();
    List<float[]> vectors;
    try {
      vectors = embedding.embedPassages(texts);
    } catch (EmbeddingException exception) {
      vectors = texts.stream().map(ignored -> new float[768]).toList();
    }
    var sequences = new HashMap<String, Integer>();
    for (int index = 0; index < pending.size(); index++) {
      var item = pending.get(index);
      publish(
          item,
          texts.get(index),
          vectors.get(index),
          sequences.merge(item.versionId(), 1, Integer::sum));
    }
  }

  private void publish(
      EvidenceFixtureManifest.EvidenceFixture item, String text, float[] vector, int sequence) {
    var documentId = UUID.fromString(item.document().documentId());
    var versionId = UUID.fromString(item.versionId());
    var chunkId = UUID.fromString(item.chunkId());
    var fileId = stable("evaluation-file-" + versionId);
    var revisionId = stable("evaluation-revision-" + versionId);
    var taskId = stable("evaluation-parse-" + versionId);
    jdbc.update(
        "INSERT INTO knowledge_document(document_id,title,product_line_code,product_line_display_name,source_type,created_by,created_at,mock_data) VALUES (?,?,?,?, 'MOCK','mock-knowledge-editor',CURRENT_TIMESTAMP,TRUE) ON CONFLICT DO NOTHING",
        documentId,
        item.document().title(),
        item.productLineCode(),
        item.productLineCode() + "（模拟数据）");
    jdbc.update(
        "INSERT INTO knowledge_original_file(file_id,storage_key,content_hash,size_bytes,media_type,original_name,created_at) VALUES (?,?,?,1,'text/plain','evaluation-fixture.txt',CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
        fileId,
        "evaluation/mock-eval-v1/" + fileId,
        item.contentHash());
    jdbc.update(
        "INSERT INTO knowledge_version_v2(version_id,document_id,revision_number,status,created_by,submitted_by,approved_by,original_file_id,created_at,updated_at,mock_data) VALUES (?,?,1,'PUBLISHED','mock-knowledge-editor','mock-knowledge-editor','mock-knowledge-reviewer',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,TRUE) ON CONFLICT DO NOTHING",
        versionId,
        documentId,
        fileId);
    jdbc.update(
        "INSERT INTO knowledge_draft_revision(revision_id,version_id,revision_number,title,product_line_code,created_at,cleaned_text_hash,product_line_display_name,created_by,mock_data) VALUES (?,?,1,?,?,CURRENT_TIMESTAMP,?,?, 'mock-knowledge-editor',TRUE) ON CONFLICT DO NOTHING",
        revisionId,
        versionId,
        item.document().title(),
        item.productLineCode(),
        item.contentHash(),
        item.productLineCode() + "（模拟数据）");
    jdbc.update(
        "UPDATE knowledge_version_v2 SET current_draft_revision_id=? WHERE version_id=? AND current_draft_revision_id IS DISTINCT FROM ?",
        revisionId,
        versionId,
        revisionId);
    jdbc.update(
        "UPDATE knowledge_document SET current_published_version_id=? WHERE document_id=? AND current_published_version_id IS DISTINCT FROM ?",
        versionId,
        documentId,
        versionId);
    jdbc.update(
        "INSERT INTO parse_task(task_id,resource_id,draft_revision_id,status,attempt,max_attempts,created_at,started_at,completed_at) VALUES (?,?,?,'SUCCEEDED',1,3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
        taskId,
        versionId,
        revisionId);
    jdbc.update(
        "INSERT INTO knowledge_revision_chunk(chunk_id,task_id,revision_id,sequence,structure_path,text_content,token_count,content_hash,chunking_rule_version) VALUES (?,?,?,?,'# evaluation fixture',?,12,?,'structure-400-overlap-50-v1') ON CONFLICT DO NOTHING",
        chunkId,
        taskId,
        revisionId,
        sequence,
        text,
        item.contentHash());
    if (postgresql) {
      jdbc.update(
          "INSERT INTO knowledge_chunk_index(chunk_id,version_id,revision_id,product_line_code,text_content,embedding,embedding_model,embedding_revision,content_hash,indexed_at) VALUES (?,?,?,?,?,CAST(? AS vector),'intfloat/multilingual-e5-base','d13f1b27baf31030b7fd040960d60d909913633f',?,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
          chunkId,
          versionId,
          revisionId,
          item.productLineCode(),
          text,
          vector(vector),
          item.contentHash());
    } else {
      jdbc.update(
          "INSERT INTO knowledge_chunk_index(chunk_id,version_id,revision_id,product_line_code,text_content,fts_document,embedding,embedding_dimensions,embedding_model,embedding_revision,content_hash,indexed_at) VALUES (?,?,?,?,?,?,?,768,'intfloat/multilingual-e5-base','d13f1b27baf31030b7fd040960d60d909913633f',?,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
          chunkId,
          versionId,
          revisionId,
          item.productLineCode(),
          text,
          text.toLowerCase(Locale.ROOT),
          vector(vector),
          item.contentHash());
    }
  }

  private boolean exists(UUID chunkId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT COUNT(*) > 0 FROM knowledge_chunk_index WHERE chunk_id=?",
            Boolean.class,
            chunkId));
  }

  private UUID stable(String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private String vector(float[] values) {
    var result = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) result.append(',');
      result.append(Float.toString(values[i]));
    }
    return result.append(']').toString();
  }
}
