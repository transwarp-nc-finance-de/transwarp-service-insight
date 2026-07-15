package com.transwarp.serviceinsight.identity.domain;

import java.util.List;
import java.util.Objects;

public record IdentityContext(
    String userCode,
    String displayName,
    List<Role> roles,
    List<String> productLineCodes,
    boolean mockData) {

  public IdentityContext {
    Objects.requireNonNull(userCode);
    Objects.requireNonNull(displayName);
    roles = List.copyOf(roles);
    productLineCodes = List.copyOf(productLineCodes);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public boolean canAccessProductLine(String productLineCode) {
    return productLineCodes.contains(productLineCode);
  }
}
