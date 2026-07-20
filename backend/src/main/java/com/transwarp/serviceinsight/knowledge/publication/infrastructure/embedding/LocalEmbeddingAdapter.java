package com.transwarp.serviceinsight.knowledge.publication.infrastructure.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class LocalEmbeddingAdapter implements EmbeddingPort {
  private final RestClient passageClient;
  private final RestClient queryClient;
  private final ObjectMapper objectMapper;

  public LocalEmbeddingAdapter(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${app.embedding.base-url:http://localhost:8090}") String baseUrl,
      @Value("${app.embedding.timeout:PT30S}") Duration timeout,
      @Value("${app.embedding.query-timeout:PT1S}") Duration queryTimeout) {
    this.passageClient = client(builder, baseUrl, timeout);
    this.queryClient = client(builder.clone(), baseUrl, queryTimeout);
    this.objectMapper = objectMapper;
  }

  @Override
  public List<float[]> embedPassages(List<String> texts) {
    return embed(passageClient, "passage", texts);
  }

  @Override
  public List<float[]> embedQueries(List<String> texts) {
    return embed(queryClient, "query", texts);
  }

  private RestClient client(RestClient.Builder builder, String baseUrl, Duration timeout) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);
    return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  private List<float[]> embed(RestClient client, String prefix, List<String> texts) {
    if (texts.isEmpty()) return List.of();
    try {
      var requestBody = serializedRequest(prefix, texts);
      var response =
          client
              .post()
              .uri("/v1/embeddings")
              .contentType(MediaType.APPLICATION_JSON)
              .contentLength(requestBody.length)
              .body(requestBody)
              .retrieve()
              .body(EmbeddingResponse.class);
      if (response == null
          || response.vectors() == null
          || response.vectors().size() != texts.size()) {
        throw invalidResponse();
      }
      var vectors =
          response.vectors().stream()
              .map(
                  values -> {
                    if (values == null || values.size() != 768) throw invalidResponse();
                    var vector = new float[768];
                    for (int index = 0; index < vector.length; index++) {
                      var value = values.get(index);
                      if (value == null || !Float.isFinite(value)) throw invalidResponse();
                      vector[index] = value;
                    }
                    return vector;
                  })
              .toList();
      return vectors;
    } catch (ResourceAccessException exception) {
      throw new EmbeddingException("EMBEDDING_TIMEOUT", "本地向量服务暂时不可用。", true);
    } catch (RestClientResponseException exception) {
      var retryable = exception.getStatusCode().is5xxServerError();
      throw new EmbeddingException(
          retryable ? "EMBEDDING_TEMPORARILY_UNAVAILABLE" : "EMBEDDING_REQUEST_REJECTED",
          retryable ? "本地向量服务暂时不可用。" : "向量请求不符合固定契约。",
          retryable);
    }
  }

  private EmbeddingException invalidResponse() {
    return new EmbeddingException("EMBEDDING_INVALID_RESPONSE", "向量响应不符合 768 维固定契约。", false);
  }

  private byte[] serializedRequest(String prefix, List<String> texts) {
    try {
      return objectMapper.writeValueAsBytes(new EmbeddingRequest(prefix, texts));
    } catch (JsonProcessingException exception) {
      throw new EmbeddingException("EMBEDDING_REQUEST_INVALID", "向量请求无法按固定契约序列化。", false);
    }
  }

  record EmbeddingRequest(String prefix, List<String> texts) {}

  record EmbeddingResponse(List<List<Float>> vectors) {}
}
