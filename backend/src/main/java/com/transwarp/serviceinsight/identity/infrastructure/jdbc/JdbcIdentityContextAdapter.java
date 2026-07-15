package com.transwarp.serviceinsight.identity.infrastructure.jdbc;

import com.transwarp.serviceinsight.identity.domain.AuthSession;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.identity.port.IdentityContextPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcIdentityContextAdapter implements IdentityContextPort {
  private final JdbcTemplate jdbcTemplate;

  public JdbcIdentityContextAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<IdentityContext> findEnabledIdentity(String userCode) {
    var identities =
        jdbcTemplate.query(
            """
            SELECT user_code, display_name, mock_data
            FROM local_identity
            WHERE user_code = ? AND enabled = TRUE
            """,
            (resultSet, rowNumber) ->
                new IdentityRow(
                    resultSet.getString("user_code"),
                    resultSet.getString("display_name"),
                    resultSet.getBoolean("mock_data")),
            userCode);
    if (identities.isEmpty()) {
      return Optional.empty();
    }
    var identity = identities.getFirst();
    var roles =
        jdbcTemplate.query(
            "SELECT role_code FROM local_identity_role WHERE user_code = ? ORDER BY role_code",
            (resultSet, rowNumber) -> Role.valueOf(resultSet.getString("role_code")),
            userCode);
    var productLineCodes =
        jdbcTemplate.query(
            """
            SELECT product_line_code
            FROM local_identity_product_line
            WHERE user_code = ?
            ORDER BY product_line_code
            """,
            (resultSet, rowNumber) -> resultSet.getString("product_line_code"),
            userCode);
    return Optional.of(
        new IdentityContext(
            identity.userCode(),
            identity.displayName(),
            roles,
            productLineCodes,
            identity.mockData()));
  }

  @Override
  public Optional<AuthSession> findActiveSession(UUID sessionId, Instant now) {
    var sessions =
        jdbcTemplate.query(
            """
            SELECT session_id, user_code, csrf_token, created_at, expires_at
            FROM auth_session
            WHERE session_id = ? AND invalidated_at IS NULL AND expires_at > ?
            """,
            (resultSet, rowNumber) -> sessionRow(resultSet),
            sessionId,
            Timestamp.from(now));
    if (sessions.isEmpty()) {
      return Optional.empty();
    }
    var session = sessions.getFirst();
    return findEnabledIdentity(session.userCode())
        .map(
            identity ->
                new AuthSession(
                    session.sessionId(),
                    identity,
                    session.csrfToken(),
                    session.createdAt(),
                    session.expiresAt()));
  }

  @Override
  @Transactional
  public void replaceSession(UUID previousSessionId, AuthSession newSession, Instant replacedAt) {
    if (previousSessionId != null) {
      jdbcTemplate.update(
          """
          UPDATE auth_session
          SET invalidated_at = ?
          WHERE session_id = ? AND invalidated_at IS NULL
          """,
          Timestamp.from(replacedAt),
          previousSessionId);
    }
    jdbcTemplate.update(
        """
        INSERT INTO auth_session(
            session_id, user_code, csrf_token, created_at, expires_at, invalidated_at
        ) VALUES (?, ?, ?, ?, ?, NULL)
        """,
        newSession.sessionId(),
        newSession.identity().userCode(),
        newSession.csrfToken(),
        Timestamp.from(newSession.createdAt()),
        Timestamp.from(newSession.expiresAt()));
  }

  @Override
  public boolean invalidateSession(UUID sessionId, String csrfToken, Instant invalidatedAt) {
    return jdbcTemplate.update(
            """
        UPDATE auth_session
        SET invalidated_at = ?
        WHERE session_id = ?
          AND csrf_token = ?
          AND invalidated_at IS NULL
          AND expires_at > ?
        """,
            Timestamp.from(invalidatedAt),
            sessionId,
            csrfToken,
            Timestamp.from(invalidatedAt))
        == 1;
  }

  private SessionRow sessionRow(ResultSet resultSet) throws SQLException {
    return new SessionRow(
        resultSet.getObject("session_id", UUID.class),
        resultSet.getString("user_code"),
        resultSet.getString("csrf_token"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("expires_at").toInstant());
  }

  private record IdentityRow(String userCode, String displayName, boolean mockData) {}

  private record SessionRow(
      UUID sessionId, String userCode, String csrfToken, Instant createdAt, Instant expiresAt) {}
}
