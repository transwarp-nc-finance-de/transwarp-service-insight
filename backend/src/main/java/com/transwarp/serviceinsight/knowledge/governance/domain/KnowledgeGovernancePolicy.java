package com.transwarp.serviceinsight.knowledge.governance.domain;

import java.util.Objects;
import java.util.Set;

public final class KnowledgeGovernancePolicy {
  private KnowledgeGovernancePolicy() {}

  public static String submit(String currentStatus) {
    requireStatus(currentStatus, "DRAFT");
    return "IN_REVIEW";
  }

  public static String returnToDraft(String currentStatus) {
    requireStatus(currentStatus, "IN_REVIEW");
    return "DRAFT";
  }

  public static String approve(String currentStatus) {
    requireStatus(currentStatus, "IN_REVIEW");
    return "APPROVED";
  }

  public static void validateCurrentParseHash(String suppliedHash, String currentHash) {
    if (!Objects.equals(suppliedHash, currentHash)) {
      throw new IllegalGovernanceTransition("解析结果已变化，请基于当前预览重试");
    }
  }

  public static void validateApproval(
      String submittedBy,
      String reviewer,
      String suppliedHash,
      String currentHash,
      Set<String> currentWarningCodes,
      Set<String> acknowledgedWarningCodes) {
    if (Objects.equals(submittedBy, reviewer)) {
      throw new DutySeparationViolation();
    }
    validateCurrentParseHash(suppliedHash, currentHash);
    if (!acknowledgedWarningCodes.containsAll(currentWarningCodes)) {
      throw new IllegalGovernanceTransition("必须确认当前解析预览中的全部警告码");
    }
  }

  private static void requireStatus(String actual, String expected) {
    if (!expected.equals(actual)) {
      throw new IllegalGovernanceTransition("当前状态不允许该命令");
    }
  }
}
