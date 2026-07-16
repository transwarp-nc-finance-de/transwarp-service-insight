package com.transwarp.serviceinsight.precheck.v2.api;

import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.AdditionalInformationItem;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.AttachmentMetadata;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CatalogValue;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.IssueTypeValue;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class PersistentPrecheckRequests {
  private PersistentPrecheckRequests() {}

  public record CatalogValueRequest(
      @NotBlank @Size(max = 100) String code, @NotBlank @Size(max = 200) String displayName) {
    CatalogValue toDomain() {
      return new CatalogValue(code, displayName);
    }
  }

  public record IssueTypeValueRequest(
      @NotBlank @Size(max = 100) String code, @NotBlank @Size(max = 200) String displayName) {
    IssueTypeValue toDomain() {
      return new IssueTypeValue(code, displayName);
    }
  }

  public record AdditionalInformationItemRequest(
      @NotBlank @Size(max = 100) String fieldCode,
      @NotBlank @Size(max = 200) String displayName,
      @NotNull @Size(max = 10000) String value) {
    AdditionalInformationItem toDomain() {
      return new AdditionalInformationItem(fieldCode, displayName, value);
    }
  }

  public record AttachmentMetadataRequest(
      @NotBlank @Size(max = 200) String attachmentId,
      @NotBlank @Size(max = 255) String fileName,
      @NotBlank @Size(max = 100) String mediaType,
      @Min(0) long byteSize) {
    AttachmentMetadata toDomain() {
      return new AttachmentMetadata(attachmentId, fileName, mediaType, byteSize);
    }
  }

  public record PrecheckContextRequest(
      @NotBlank String sourceSystem,
      @NotBlank @Size(max = 100) String hostRequestId,
      @NotBlank @Size(max = 32) String formSchemaVersion,
      @Valid @NotNull IssueTypeValueRequest issueType,
      @Valid @NotNull CatalogValueRequest productLine,
      @Valid CatalogValueRequest product,
      @Valid CatalogValueRequest component,
      @Size(max = 100) String version,
      @Valid CatalogValueRequest severity,
      @Valid CatalogValueRequest serviceType,
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 10000) String descriptionPlainText,
      @Valid @Size(max = 100) List<AdditionalInformationItemRequest> additionalInformation,
      @Size(max = 2000) String impactScope,
      @Valid @Size(max = 20) List<AttachmentMetadataRequest> attachments) {
    PrecheckContext toDomain() {
      return new PrecheckContext(
          sourceSystem,
          hostRequestId,
          formSchemaVersion,
          issueType.toDomain(),
          productLine.toDomain(),
          product == null ? null : product.toDomain(),
          component == null ? null : component.toDomain(),
          version,
          severity == null ? null : severity.toDomain(),
          serviceType == null ? null : serviceType.toDomain(),
          title,
          descriptionPlainText,
          additionalInformation == null
              ? List.of()
              : additionalInformation.stream()
                  .map(AdditionalInformationItemRequest::toDomain)
                  .toList(),
          impactScope,
          attachments == null
              ? List.of()
              : attachments.stream().map(AttachmentMetadataRequest::toDomain).toList());
    }
  }

  public record CreateSessionRequest(@Valid @NotNull PrecheckContextRequest context) {
    PrecheckContext toDomain() {
      return context.toDomain();
    }
  }

  public record ConfirmationReason(
      @AssertTrue boolean confirmed, @NotBlank @Size(max = 1000) String reason) {}
}
