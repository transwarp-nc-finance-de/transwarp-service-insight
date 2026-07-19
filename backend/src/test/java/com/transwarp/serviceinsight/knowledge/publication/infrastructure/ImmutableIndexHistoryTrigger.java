package com.transwarp.serviceinsight.knowledge.publication.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class ImmutableIndexHistoryTrigger implements Trigger {
  @Override
  public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
    throw new SQLException("dual index history is immutable");
  }
}
