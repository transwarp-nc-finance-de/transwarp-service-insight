package com.transwarp.serviceinsight.knowledge.ingestion.domain;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Chunk;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseWarning;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedBlock;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentParser {
  static final String PARSER_VERSION = "text-structure-v1";
  static final String CHUNKING_RULE_VERSION = "structure-400-overlap-50-v1";

  public ParsedDocument parse(byte[] bytes, String mediaType, UUID parseContextId) {
    if (mediaType.equals("application/pdf")) {
      return parsePdf(bytes, parseContextId);
    }
    if (!mediaType.equals("text/markdown") && !mediaType.equals("text/plain")) {
      throw new ParseFailure("UNSUPPORTED_FILE_TYPE", "The file type cannot be parsed.", false);
    }
    return parseText(
        new String(bytes, StandardCharsets.UTF_8), PARSER_VERSION, List.of(), parseContextId);
  }

  private ParsedDocument parsePdf(byte[] bytes, UUID parseContextId) {
    try (var document = Loader.loadPDF(bytes)) {
      var warnings = new ArrayList<ParseWarning>();
      var imageCount = 0;
      for (var page : document.getPages()) {
        var resources = page.getResources();
        if (resources == null) continue;
        for (var name : resources.getXObjectNames()) {
          if (resources.getXObject(name) instanceof PDImageXObject) imageCount++;
        }
      }
      if (imageCount > 0) {
        warnings.add(
            new ParseWarning(
                "IMAGE_CONTENT_IGNORED", "Image content was ignored.", "document", imageCount));
        warnings.add(
            new ParseWarning(
                "CONTENT_LOSS_SUSPECTED",
                "Ignored image content may contain text or diagrams.",
                "document",
                imageCount));
      }
      var text = new PDFTextStripper().getText(document);
      if (text == null || text.isBlank()) {
        throw new ParseFailure(
            "PDF_TEXT_LAYER_REQUIRED", "The PDF has no extractable text layer.", false);
      }
      var tableLikeLineCount =
          Math.toIntExact(
              text.lines()
                  .filter(line -> line.contains("\t") || line.matches(".*\\S {2,}\\S.*"))
                  .count());
      if (tableLikeLineCount > 0) {
        warnings.add(
            new ParseWarning(
                "TABLE_STRUCTURE_FLATTENED",
                "Table-like content was flattened to safe text.",
                "document",
                tableLikeLineCount));
        warnings.add(
            new ParseWarning(
                "READING_ORDER_UNCERTAIN",
                "The reading order of table-like content may be uncertain.",
                "document",
                tableLikeLineCount));
      }
      return parseText(text, "pdf-text-v1", warnings, parseContextId);
    } catch (ParseFailure failure) {
      throw failure;
    } catch (IOException failure) {
      throw new ParseFailure("INVALID_PDF", "The PDF cannot be parsed.", false);
    }
  }

  private ParsedDocument parseText(
      String rawText, String parserVersion, List<ParseWarning> warnings, UUID parseContextId) {
    var text = rawText.replace("\r\n", "\n").trim();
    if (text.isBlank()) {
      throw new ParseFailure("EMPTY_DOCUMENT", "The document contains no parseable text.", false);
    }

    var blocks = new ArrayList<ParsedBlock>();
    var chunks = new ArrayList<Chunk>();
    var sections = text.split("\\n\\s*\\n");
    var path = "document";
    for (var section : sections) {
      var normalized = section.trim();
      if (normalized.isBlank()) continue;
      if (normalized.startsWith("#")) {
        path = normalized.lines().findFirst().orElse("# document").replaceFirst("^#+\\s*", "");
      }
      blocks.add(
          new ParsedBlock(
              UUID.randomUUID(), blocks.size() + 1, path, normalized, sha256(normalized)));
      addChunks(chunks, normalized, path);
    }
    var hashMaterial =
        new StringBuilder(parserVersion)
            .append('|')
            .append(CHUNKING_RULE_VERSION)
            .append('|')
            .append(parseContextId);
    blocks.forEach(
        block ->
            hashMaterial
                .append('|')
                .append(block.sequence())
                .append('|')
                .append(block.structurePath())
                .append('|')
                .append(block.contentHash()));
    warnings.forEach(
        warning ->
            hashMaterial
                .append('|')
                .append(warning.code())
                .append('|')
                .append(warning.structurePath())
                .append('|')
                .append(warning.occurrenceCount()));
    var combinedHash = sha256(hashMaterial.toString());
    return new ParsedDocument(
        parserVersion,
        combinedHash,
        CHUNKING_RULE_VERSION,
        List.copyOf(blocks),
        List.copyOf(chunks),
        List.copyOf(warnings));
  }

  private void addChunks(List<Chunk> chunks, String text, String path) {
    var tokens = tokenSpans(text);
    var startToken = 0;
    while (startToken < tokens.size()) {
      var endToken = Math.min(startToken + 400, tokens.size());
      var value =
          text.substring(tokens.get(startToken).start(), tokens.get(endToken - 1).end()).trim();
      chunks.add(
          new Chunk(
              UUID.randomUUID(),
              chunks.size() + 1,
              path,
              value,
              endToken - startToken,
              sha256(value),
              CHUNKING_RULE_VERSION));
      if (endToken == tokens.size()) break;
      startToken = endToken - 50;
    }
  }

  private List<TokenSpan> tokenSpans(String text) {
    var tokens = new ArrayList<TokenSpan>();
    var offset = 0;
    while (offset < text.length()) {
      var codePoint = text.codePointAt(offset);
      var nextOffset = offset + Character.charCount(codePoint);
      if (Character.isWhitespace(codePoint)) {
        offset = nextOffset;
        continue;
      }
      if (isAsciiWord(codePoint)) {
        var start = offset;
        offset = nextOffset;
        while (offset < text.length()) {
          var nextCodePoint = text.codePointAt(offset);
          if (!isAsciiWord(nextCodePoint)) break;
          offset += Character.charCount(nextCodePoint);
        }
        tokens.add(new TokenSpan(start, offset));
        continue;
      }
      tokens.add(new TokenSpan(offset, nextOffset));
      offset = nextOffset;
    }
    return tokens;
  }

  private boolean isAsciiWord(int codePoint) {
    return codePoint < 128 && (Character.isLetterOrDigit(codePoint) || codePoint == '_');
  }

  private String sha256(String value) {
    try {
      return "sha256:"
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  public static final class ParseFailure extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public ParseFailure(String code, String message, boolean retryable) {
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

  private record TokenSpan(int start, int end) {}
}
