package com.transwarp.serviceinsight.knowledge.domain;

import java.util.UUID;

public record KnowledgeDocument(UUID id, String title, boolean mockData) {
  public KnowledgeDocument {
    if (id == null || title == null || title.isBlank()) {
      throw new IllegalArgumentException("知识文档标识和标题不能为空");
    }
    if (!mockData) {
      throw new IllegalArgumentException("当前阶段只允许模拟数据");
    }
  }
}
