package com.transwarp.serviceinsight.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser.ParseFailure;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentParserTest {
  private final KnowledgeDocumentParser parser = new KnowledgeDocumentParser();

  @Test
  void producesStableStructureOrderAndHashesForText() {
    var input = "# Section\n\nMock data first paragraph.\n\nMock data second paragraph.";

    var first = parser.parse(input.getBytes(StandardCharsets.UTF_8), "text/markdown");
    var second = parser.parse(input.getBytes(StandardCharsets.UTF_8), "text/markdown");

    assertThat(first.blocks()).extracting("sequence").containsExactly(1, 2, 3);
    assertThat(first.blocks())
        .extracting("contentHash")
        .containsExactlyElementsOf(
            second.blocks().stream().map(value -> value.contentHash()).toList());
    assertThat(first.chunks()).extracting("sequence").containsExactly(1, 2, 3);
    assertThat(first.parseResultHash()).isEqualTo(second.parseResultHash());
  }

  @Test
  void parsesTextLayerPdfAndRejectsPdfWithoutText() throws Exception {
    var textPdf = pdfWithText("Mock data PDF text");
    var blankPdf = blankPdf();

    assertThat(parser.parse(textPdf, "application/pdf").blocks()).isNotEmpty();
    assertThatThrownBy(() -> parser.parse(blankPdf, "application/pdf"))
        .isInstanceOf(ParseFailure.class)
        .extracting("code")
        .isEqualTo("PDF_TEXT_LAYER_REQUIRED");
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
}
