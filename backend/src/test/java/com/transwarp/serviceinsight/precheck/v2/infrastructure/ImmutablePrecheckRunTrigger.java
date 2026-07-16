package com.transwarp.serviceinsight.precheck.v2.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class ImmutablePrecheckRunTrigger implements Trigger {
  @Override
  public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
    throw new SQLException("precheck run snapshots are immutable");
  }
}
