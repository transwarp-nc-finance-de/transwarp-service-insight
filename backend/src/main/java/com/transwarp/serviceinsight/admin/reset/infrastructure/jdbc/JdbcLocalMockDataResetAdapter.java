package com.transwarp.serviceinsight.admin.reset.infrastructure.jdbc;

import com.transwarp.serviceinsight.admin.reset.port.LocalMockDataResetPort;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLocalMockDataResetAdapter implements LocalMockDataResetPort {
  private static final List<String> RESET_TABLES =
      List.of(
          "evaluation_case_result",
          "evaluation_run_idempotency",
          "evaluation_run_idempotency_lock",
          "evaluation_run",
          "submission_continuation_idempotency",
          "submission_continuation_v2",
          "precheck_feedback_idempotency",
          "precheck_feedback_v2",
          "precheck_evidence",
          "precheck_retrieval_candidate",
          "precheck_retrieval_audit",
          "precheck_command_idempotency",
          "precheck_command_lock",
          "precheck_run_v2",
          "precheck_session_v2",
          "precheck_business_key_lock",
          "knowledge_chunk_fts_stage",
          "knowledge_chunk_index",
          "index_task_attempt",
          "knowledge_publication_idempotency",
          "knowledge_publication_idempotency_lock",
          "index_task",
          "knowledge_governance_idempotency",
          "knowledge_governance_idempotency_lock",
          "knowledge_governance_audit",
          "knowledge_review_history",
          "knowledge_revision_parse_warning",
          "knowledge_revision_chunk",
          "knowledge_revision_parsed_block",
          "knowledge_revision_parse_result",
          "knowledge_ingestion_idempotency",
          "knowledge_parse_warning",
          "knowledge_chunk",
          "knowledge_parsed_block",
          "knowledge_parse_result",
          "parse_task",
          "knowledge_draft_revision",
          "knowledge_version_v2",
          "knowledge_original_file",
          "knowledge_document",
          "completeness_policy",
          "auth_session",
          "local_identity_product_line",
          "local_identity_role",
          "local_identity",
          "catalog_product_version",
          "catalog_component",
          "catalog_product",
          "catalog_service_type",
          "catalog_severity",
          "catalog_product_line",
          "seed_version");

  private final JdbcTemplate jdbc;
  private final boolean postgresql;

  public JdbcLocalMockDataResetAdapter(JdbcTemplate jdbc, DataSource dataSource)
      throws SQLException {
    this.jdbc = jdbc;
    try (var connection = dataSource.getConnection()) {
      postgresql =
          connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }
  }

  @Override
  public void resetBusinessData() {
    if (postgresql) {
      jdbc.execute("TRUNCATE TABLE " + String.join(",", RESET_TABLES) + " CASCADE");
    } else {
      jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
      try {
        RESET_TABLES.forEach(table -> jdbc.execute("TRUNCATE TABLE " + table));
      } finally {
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
      }
    }
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("reset/local-mock-data-v1.sql"));
              return null;
            });
  }
}
