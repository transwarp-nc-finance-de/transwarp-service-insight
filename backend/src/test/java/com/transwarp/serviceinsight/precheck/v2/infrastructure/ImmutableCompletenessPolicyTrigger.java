package com.transwarp.serviceinsight.precheck.v2.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class ImmutableCompletenessPolicyTrigger implements Trigger {
  @Override
  public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
    throw new SQLException("completeness policy versions are immutable");
  }
}
