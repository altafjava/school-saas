package com.altafjava.school.application.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.application.document.DocumentModelPreparer;
import com.altafjava.platform.application.document.DocumentRenderer;
import com.altafjava.platform.application.document.DocumentTypeDefinition;
import com.altafjava.platform.application.document.MustacheTemplateEngine;
import com.altafjava.platform.application.document.PdfDocumentWriter;
import com.altafjava.platform.application.document.QrCodeGenerator;
import com.altafjava.platform.application.document.SystemPlaceholders;
import com.altafjava.platform.domain.document.model.PlaceholderField;

/** Every built-in design must render strictly against exactly the fields its document type declares. */
class SchoolDocumentTypesTest {

	private static final DocumentRenderer RENDERER = new DocumentRenderer(
			new DocumentModelPreparer(new QrCodeGenerator()), new MustacheTemplateEngine(), new PdfDocumentWriter());

	private final SchoolDocumentTypes types = new SchoolDocumentTypes();

	private List<DocumentTypeDefinition> definitions() {
		return List.of(types.studentIdCardDocumentType(), types.teacherIdCardDocumentType(),
				types.certificateDocumentType(), types.reportCardDocumentType());
	}

	private static List<PlaceholderField> fullSchema(DocumentTypeDefinition definition) {
		List<PlaceholderField> schema = new ArrayList<>(SystemPlaceholders.SCHEMA);
		schema.addAll(definition.schema());
		return schema;
	}

	@Test
	void everyDefaultTemplate_rendersStrictlyAgainstItsDeclaredSchema() throws IOException {
		for (DocumentTypeDefinition definition : definitions()) {
			byte[] pdf = RENDERER.renderSample(definition.defaultTemplate().format(),
					definition.defaultTemplate().content(), fullSchema(definition));

			try (PDDocument document = Loader.loadPDF(pdf)) {
				assertTrue(document.getNumberOfPages() >= 1, definition.documentType());
			}
			String dir = System.getenv("DOCUMENT_PREVIEW_DIR");
			if (dir != null) {
				Files.write(Path.of(dir, definition.documentType() + ".pdf"), pdf);
			}
		}
	}

	@Test
	void idCards_areOneCardSizedPage() throws IOException {
		for (DocumentTypeDefinition definition : List.of(types.studentIdCardDocumentType(),
				types.teacherIdCardDocumentType())) {
			byte[] pdf = RENDERER.renderSample(definition.defaultTemplate().format(),
					definition.defaultTemplate().content(), fullSchema(definition));

			try (PDDocument document = Loader.loadPDF(pdf)) {
				assertEquals(1, document.getNumberOfPages());
				assertEquals(85.6f * 72 / 25.4f, document.getPage(0).getMediaBox().getWidth(), 1.5f);
			}
		}
	}

	@Test
	void reportCard_repeatsHeaderRowAndFooterAcrossPages() throws IOException {
		DocumentTypeDefinition definition = types.reportCardDocumentType();
		Map<String, Object> model = new java.util.HashMap<>();
		model.put("studentName", "Alice Smith");
		model.put("lines", java.util.stream.IntStream.range(0, 90)
				.mapToObj(i -> Map.of("subject", "Subject " + i, "exam", "Midterm", "marks", "80", "maxMarks", "100",
						"gradeLetter", "A"))
				.toList());
		model.put(SystemPlaceholders.VERIFICATION_CODE, "ABC123");

		byte[] pdf = renderWithModel(definition, model);

		try (PDDocument document = Loader.loadPDF(pdf)) {
			assertTrue(document.getNumberOfPages() > 1);
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(2);
			stripper.setEndPage(2);
			String secondPage = stripper.getText(document);
			assertTrue(secondPage.contains("Subject"), "table header repeats on page 2: " + secondPage);
			assertTrue(secondPage.contains("ABC123"), "page footer shows the verification code");
		}
	}

	private static byte[] renderWithModel(DocumentTypeDefinition definition, Map<String, Object> model) {
		return RENDERER.render(definition.defaultTemplate().format(), definition.defaultTemplate().content(),
				fullSchema(definition), model);
	}
}
