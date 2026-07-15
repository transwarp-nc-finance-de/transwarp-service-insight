package com.transwarp.serviceinsight.knowledge.governance.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class ImmutableGovernanceTrigger implements Trigger {
  @Override
  public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
    throw new SQLException("knowledge governance records are immutable");
  }
}
