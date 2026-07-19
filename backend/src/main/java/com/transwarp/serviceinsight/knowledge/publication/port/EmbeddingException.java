package com.transwarp.serviceinsight.knowledge.publication.port;

public class EmbeddingException extends RuntimeException {
  private final String code;
  private final boolean retryable;

  public EmbeddingException(String code, String message, boolean retryable) {
    super(message);
    this.code = code;
    this.retryable = retryable;
  }

  public String code() {
    return code;
  }

  public boolean retryable() {
    return retryable;
  }
}
