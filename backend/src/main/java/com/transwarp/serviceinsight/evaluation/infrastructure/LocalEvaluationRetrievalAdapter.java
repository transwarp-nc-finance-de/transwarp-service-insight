package com.transwarp.serviceinsight.evaluation.infrastructure;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationCase.Degradation;
import com.transwarp.serviceinsight.evaluation.port.EvaluationRetrievalPort;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import com.transwarp.serviceinsight.precheck.retrieval.application.AuthorizedRetrievalService;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalCandidate;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalSearchPort;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import java.util.List;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Component;

@Component
public class LocalEvaluationRetrievalAdapter implements EvaluationRetrievalPort {
  private final AuthorizedRetrievalService retrieval;
  private final RetrievalSearchPort search;
  private final EmbeddingPort embedding;

  public LocalEvaluationRetrievalAdapter(
      AuthorizedRetrievalService retrieval, RetrievalSearchPort search, EmbeddingPort embedding) {
    this.retrieval = retrieval;
    this.search = search;
    this.embedding = embedding;
  }

  @Override
  public RetrievalOutcome retrieve(
      IdentityContext identity, PrecheckContext context, Degradation degradationScenario) {
    return switch (degradationScenario) {
      case NONE -> retrieval.retrieve(identity, context);
      case FTS_ONLY ->
          retrieval.retrieveUsing(
              identity, context, search, new UnavailableEmbeddingPort(embedding));
      case UNAVAILABLE ->
          retrieval.retrieveUsing(identity, context, new UnavailableFtsPort(search), embedding);
    };
  }

  private record UnavailableEmbeddingPort(EmbeddingPort delegate) implements EmbeddingPort {
    @Override
    public List<float[]> embedPassages(List<String> texts) {
      return delegate.embedPassages(texts);
    }

    @Override
    public List<float[]> embedQueries(List<String> texts) {
      throw new EmbeddingException("EMBEDDING_UNAVAILABLE", "模拟数据：评估场景注入本地向量检索不可用", true);
    }
  }

  private record UnavailableFtsPort(RetrievalSearchPort delegate) implements RetrievalSearchPort {
    @Override
    public List<RetrievalCandidate> searchFts(String query, List<String> productLineCodes) {
      throw new TransientDataAccessResourceException("模拟数据：评估场景注入全文检索不可用");
    }

    @Override
    public List<RetrievalCandidate> searchVector(
        float[] queryVector, List<String> productLineCodes) {
      return delegate.searchVector(queryVector, productLineCodes);
    }
  }
}
