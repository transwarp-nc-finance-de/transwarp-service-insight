package com.transwarp.serviceinsight.knowledge.domain;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeVersion(
    UUID id,
    UUID documentId,
    int version,
    KnowledgeStatus status,
    String contentDigest,
    String permissionScope,
    Instant updatedAt) {
  public KnowledgeVersion {
    if (id == null || documentId == null || status == null || updatedAt == null) {
      throw new IllegalArgumentException("知识版本必填字段不能为空");
    }
    if (version < 1) throw new IllegalArgumentException("version 必须大于 0");
    if (contentDigest == null || contentDigest.isBlank()) {
      throw new IllegalArgumentException("模拟内容摘要不能为空");
    }
    if (permissionScope == null || permissionScope.isBlank()) {
      throw new IllegalArgumentException("权限范围不能为空");
    }
  }

  public KnowledgeVersion transitionTo(KnowledgeStatus next) {
    var allowed =
        switch (status) {
          case DRAFT -> next == KnowledgeStatus.IN_REVIEW;
          case IN_REVIEW -> next == KnowledgeStatus.APPROVED || next == KnowledgeStatus.DRAFT;
          case APPROVED -> next == KnowledgeStatus.PUBLISHED;
          case PUBLISHED -> next == KnowledgeStatus.DEPRECATED;
          case DEPRECATED, FAILED -> false;
        };
    if (!allowed) {
      throw new IllegalStateException("不允许从 " + status + " 转换到 " + next);
    }
    return new KnowledgeVersion(
        id, documentId, version, next, contentDigest, permissionScope, Instant.now());
  }

  public boolean searchable() {
    return status == KnowledgeStatus.PUBLISHED;
  }
}
