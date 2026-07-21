INSERT INTO seed_version(seed_type, version, display_name, mock_data) VALUES
    ('IDENTITY', 'local-identity-v1', '本地身份种子 local-identity-v1（模拟数据）', TRUE),
    ('CATALOG', 'local-catalog-v1', '本地目录种子 local-catalog-v1（模拟数据）', TRUE),
    ('RESET', 'local-mock-data-v1', '受控重置种子 local-mock-data-v1（模拟数据）', TRUE),
    ('EVALUATION', 'mock-eval-v1', '固定评估集 mock-eval-v1（模拟数据）', TRUE);

INSERT INTO catalog_product_line(code, display_name, seed_version, mock_data) VALUES
    ('TDH', 'TDH（模拟数据）', 'local-catalog-v1', TRUE),
    ('STREAMING', '流处理产品线（模拟数据）', 'local-catalog-v1', TRUE);
INSERT INTO catalog_product(code, display_name, product_line_code, seed_version, mock_data) VALUES
    ('INCEPTOR', 'Inceptor（模拟数据）', 'TDH', 'local-catalog-v1', TRUE),
    ('KAFKA', 'Kafka（模拟数据）', 'STREAMING', 'local-catalog-v1', TRUE);
INSERT INTO catalog_component(code, display_name, product_code, seed_version, mock_data) VALUES
    ('SQL_ENGINE', 'SQL 引擎（模拟数据）', 'INCEPTOR', 'local-catalog-v1', TRUE),
    ('BROKER', 'Broker（模拟数据）', 'KAFKA', 'local-catalog-v1', TRUE);
INSERT INTO catalog_product_version(code, display_name, product_code, seed_version, mock_data) VALUES
    ('9.1.0-mock', '9.1.0-mock（模拟数据）', 'INCEPTOR', 'local-catalog-v1', TRUE),
    ('3.7.0-mock', '3.7.0-mock（模拟数据）', 'KAFKA', 'local-catalog-v1', TRUE);
INSERT INTO catalog_severity(code, display_name, seed_version, mock_data) VALUES
    ('S1', 'S1（模拟数据）', 'local-catalog-v1', TRUE), ('S2', 'S2（模拟数据）', 'local-catalog-v1', TRUE),
    ('S3', 'S3（模拟数据）', 'local-catalog-v1', TRUE), ('S4', 'S4（模拟数据）', 'local-catalog-v1', TRUE);
INSERT INTO catalog_service_type(code, display_name, seed_version, mock_data) VALUES
    ('CONSULTATION', '咨询（模拟数据）', 'local-catalog-v1', TRUE),
    ('TROUBLESHOOTING', '故障排查（模拟数据）', 'local-catalog-v1', TRUE),
    ('INSTALLATION_GUIDANCE', '安装配置指导（模拟数据）', 'local-catalog-v1', TRUE);

INSERT INTO local_identity(user_code, display_name, seed_version, enabled, mock_data) VALUES
    ('mock-precheck-tdh', 'TDH 预诊用户（模拟数据）', 'local-identity-v1', TRUE, TRUE),
    ('mock-knowledge-editor', '知识编辑人员（模拟数据）', 'local-identity-v1', TRUE, TRUE),
    ('mock-knowledge-reviewer', '知识审核人员（模拟数据）', 'local-identity-v1', TRUE, TRUE),
    ('mock-admin', '本地管理员（模拟数据）', 'local-identity-v1', TRUE, TRUE);
INSERT INTO local_identity_role(user_code, role_code) VALUES
    ('mock-precheck-tdh', 'PRECHECK_USER'), ('mock-knowledge-editor', 'KNOWLEDGE_EDITOR'),
    ('mock-knowledge-reviewer', 'KNOWLEDGE_REVIEWER'), ('mock-admin', 'ADMIN');
INSERT INTO local_identity_product_line(user_code, product_line_code) VALUES
    ('mock-precheck-tdh', 'TDH'), ('mock-knowledge-editor', 'TDH'),
    ('mock-knowledge-editor', 'STREAMING'), ('mock-knowledge-reviewer', 'TDH'),
    ('mock-knowledge-reviewer', 'STREAMING'), ('mock-admin', 'TDH'), ('mock-admin', 'STREAMING');

INSERT INTO completeness_policy(policy_version, issue_type_code, issue_type_display_name, general_field_codes, issue_specific_field_codes, created_at, mock_data) VALUES
    ('mock-completeness-v1', 'FUNCTIONAL_FAILURE', '功能故障（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'ERROR_MESSAGE,REPRODUCTION_STEPS,RECENT_CHANGES', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'PERFORMANCE_DEGRADATION', '性能下降（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'TIME_WINDOW,BASELINE_METRIC,CURRENT_METRIC,WORKLOAD_IDENTIFIER', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'INSTALLATION_CONFIGURATION', '安装配置（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'OPERATION_GOAL,EXECUTED_STEPS,ENVIRONMENT_SUMMARY,ERROR_MESSAGE', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'DATA_CORRECTNESS', '数据正确性（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'EXPECTED_RESULT,ACTUAL_RESULT,AFFECTED_DATA_SCOPE,QUERY_OR_JOB_ID', CURRENT_TIMESTAMP, TRUE);
