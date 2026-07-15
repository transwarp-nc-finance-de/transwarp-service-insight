package com.transwarp.serviceinsight.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser.ParseFailure;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentParserTest {
  private final KnowledgeDocumentParser parser = new KnowledgeDocumentParser();

  @Test
  void keepsContentHashesStableButChangesParseHashForANewParseContext() {
    var input = "# Section\n\nMock data first paragraph.\n\nMock data second paragraph.";

    var first =
        parser.parse(input.getBytes(StandardCharsets.UTF_8), "text/markdown", UUID.randomUUID());
    var second =
        parser.parse(input.getBytes(StandardCharsets.UTF_8), "text/markdown", UUID.randomUUID());

    assertThat(first.blocks()).extracting("sequence").containsExactly(1, 2, 3);
    assertThat(first.blocks())
        .extracting("contentHash")
        .containsExactlyElementsOf(
            second.blocks().stream().map(value -> value.contentHash()).toList());
    assertThat(first.chunks()).extracting("sequence").containsExactly(1, 2, 3);
    assertThat(first.parseResultHash()).isNotEqualTo(second.parseResultHash());
  }

  @Test
  void parsesTextLayerPdfAndRejectsPdfWithoutText() throws Exception {
    var textPdf = pdfWithText("Mock data PDF text");
    var blankPdf = blankPdf();

    assertThat(parser.parse(textPdf, "application/pdf", UUID.randomUUID()).blocks()).isNotEmpty();
    assertThatThrownBy(() -> parser.parse(blankPdf, "application/pdf", UUID.randomUUID()))
        .isInstanceOf(ParseFailure.class)
        .extracting("code")
        .isEqualTo("PDF_TEXT_LAYER_REQUIRED");
  }

  @Test
  void chunksByDeterministicTokensWithFiftyTokenOverlap() {
    var input =
        IntStream.range(0, 451).mapToObj(index -> "token" + index).collect(Collectors.joining(" "));

    var parsed =
        parser.parse(input.getBytes(StandardCharsets.UTF_8), "text/plain", UUID.randomUUID());

    assertThat(parsed.chunks()).hasSize(2);
    assertThat(parsed.chunks()).extracting("tokenCount").containsExactly(400, 101);
    assertThat(parsed.chunks().getFirst().text()).startsWith("token0 ").endsWith(" token399");
    assertThat(parsed.chunks().get(1).text()).startsWith("token350 ").endsWith(" token450");
  }

  @Test
  void aggregatesSafePdfContentLossAndReadingOrderWarnings() throws Exception {
    var parsed =
        parser.parse(pdfWithTextAndImage("Mock  data table"), "application/pdf", UUID.randomUUID());

    assertThat(parsed.warnings())
        .extracting("code")
        .containsExactly(
            "IMAGE_CONTENT_IGNORED",
            "CONTENT_LOSS_SUSPECTED",
            "TABLE_STRUCTURE_FLATTENED",
            "READING_ORDER_UNCERTAIN");
    assertThat(parsed.warnings())
        .allSatisfy(warning -> assertThat(warning.occurrenceCount()).isOne());
  }

  private byte[] pdfWithText(String text) throws Exception {
    try (var document = new PDDocument();
        var output = new ByteArrayOutputStream()) {
      var page = new PDPage();
      document.addPage(page);
      try (var content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(72, 720);
        content.showText(text);
        content.endText();
      }
      document.save(output);
      return output.toByteArray();
    }
  }

  private byte[] blankPdf() throws Exception {
    try (var document = new PDDocument();
        var output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.save(output);
      return output.toByteArray();
    }
  }

  private byte[] pdfWithTextAndImage(String text) throws Exception {
    try (var document = new PDDocument();
        var output = new ByteArrayOutputStream()) {
      var page = new PDPage();
      document.addPage(page);
      try (var content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(72, 720);
        content.showText(text);
        content.endText();
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        content.drawImage(LosslessFactory.createFromImage(document, image), 72, 680, 1, 1);
      }
      document.save(output);
      return output.toByteArray();
    }
  }
}
