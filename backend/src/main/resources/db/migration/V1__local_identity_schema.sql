CREATE TABLE seed_version (
    seed_type VARCHAR(64) PRIMARY KEY,
    version VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_product_line (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_product (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_component (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    product_code VARCHAR(100) NOT NULL REFERENCES catalog_product(code),
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_product_version (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    product_code VARCHAR(100) NOT NULL REFERENCES catalog_product(code),
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_severity (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE catalog_service_type (
    code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE local_identity (
    user_code VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    seed_version VARCHAR(64) NOT NULL REFERENCES seed_version(version),
    enabled BOOLEAN NOT NULL,
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE local_identity_role (
    user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    role_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_code, role_code),
    CONSTRAINT local_identity_role_code_check CHECK (
        role_code IN ('PRECHECK_USER', 'KNOWLEDGE_EDITOR', 'KNOWLEDGE_REVIEWER', 'ADMIN')
    )
);

CREATE TABLE local_identity_product_line (
    user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    PRIMARY KEY (user_code, product_line_code)
);

CREATE TABLE auth_session (
    session_id UUID PRIMARY KEY,
    user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    csrf_token VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidated_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT auth_session_expiry_check CHECK (expires_at > created_at)
);

CREATE INDEX auth_session_active_lookup
    ON auth_session(session_id, expires_at, invalidated_at);
