package com.transwarp.serviceinsight.precheck.v2.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PrecheckContextNormalizer {
  private final ObjectMapper objectMapper;

  public PrecheckContextNormalizer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String contextHash(PrecheckContext context) {
    try {
      var normalized = new TreeMap<String, Object>();
      normalized.put("sourceSystem", text(context.sourceSystem()));
      normalized.put("hostRequestId", text(context.hostRequestId()));
      normalized.put("formSchemaVersion", text(context.formSchemaVersion()));
      normalized.put("issueType", text(context.issueType().code()));
      normalized.put("productLine", text(context.productLine().code()));
      normalized.put("product", context.product() == null ? null : text(context.product().code()));
      normalized.put(
          "component", context.component() == null ? null : text(context.component().code()));
      normalized.put("version", text(context.version()));
      normalized.put(
          "severity", context.severity() == null ? null : text(context.severity().code()));
      normalized.put(
          "serviceType", context.serviceType() == null ? null : text(context.serviceType().code()));
      normalized.put("title", text(context.title()));
      normalized.put("descriptionPlainText", text(context.descriptionPlainText()));
      normalized.put("impactScope", text(context.impactScope()));
      normalized.put(
          "additionalInformation",
          context.additionalInformation().stream()
              .map(item -> List.of(text(item.fieldCode()), text(item.value())))
              .sorted(Comparator.comparing(Object::toString))
              .toList());
      normalized.put(
          "attachments",
          context.attachments().stream()
              .map(
                  item ->
                      List.of(
                          text(item.attachmentId()),
                          text(item.fileName()),
                          text(item.mediaType()),
                          Long.toString(item.byteSize())))
              .sorted(Comparator.comparing(Object::toString))
              .toList());
      return sha256(objectMapper.writeValueAsBytes(normalized));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot normalize precheck context", exception);
    }
  }

  public String runCommandHash(UUID sessionId, PrecheckContext context) {
    return sha256((sessionId + ":" + contextHash(context)).getBytes(StandardCharsets.UTF_8));
  }

  public String terminationCommandHash(UUID sessionId, String reason) {
    return sha256((sessionId + ":" + text(reason)).getBytes(StandardCharsets.UTF_8));
  }

  public String feedbackCommandHash(
      UUID sessionId, UUID runId, String adoptionStatus, String helpfulness, String reason) {
    return sha256(
        (sessionId
                + ":"
                + runId
                + ":"
                + adoptionStatus
                + ":"
                + text(helpfulness)
                + ":"
                + text(reason))
            .getBytes(StandardCharsets.UTF_8));
  }

  public String continuationCommandHash(UUID sessionId, boolean confirmed, String reason) {
    return sha256(
        (sessionId + ":" + confirmed + ":" + text(reason)).getBytes(StandardCharsets.UTF_8));
  }

  private String text(String value) {
    if (value == null) return "";
    return Normalizer.normalize(
        value.replace("\r\n", "\n").replace('\r', '\n').trim(), Normalizer.Form.NFC);
  }

  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
