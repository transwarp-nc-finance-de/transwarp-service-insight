package com.transwarp.serviceinsight.precheck.retrieval.domain;

import java.util.UUID;

public record RetrievalCandidate(
    UUID documentId,
    UUID versionId,
    UUID chunkId,
    String productLineCode,
    String title,
    String text,
    String contentHash) {}
