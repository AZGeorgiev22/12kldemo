package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EventReportPdfService {
    private static final DateTimeFormatter ENGLISH_DATE =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public byte[] create(EventReportService.EventReport report) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PDFont bold = font;
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 790;
                y = line(stream, font, 18, 50, y, "Financial Event Report");
                y = line(stream, font, 11, 50, y - 14, "Report date: "
                        + report.selectedDate().format(ENGLISH_DATE) + " | Period: "
                        + report.months() + (report.months() == 1 ? " month" : " months"));
                y = line(stream, font, 12, 50, y - 22, "Total events: " + report.eventCount()
                        + " | Total revenue: " + report.totalAmount() + " BGN");
                y -= 18;
                for (EventReportService.MonthReport month : report.monthsReport()) {
                    y = line(stream, bold, 12, 50, y, month.month().format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) + ": "
                            + month.eventCount() + " events | Revenue: " + month.amount() + " BGN");
                    y = line(stream, font, 10, 68, y - 4, "Event names: "
                            + (month.eventNames().isEmpty() ? "none" : String.join(", ", month.eventNames())));
                    y -= 10;
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("The PDF report could not be created", exception);
        }
    }

    private float line(PDPageContentStream stream, PDFont font, int size, float x, float y, String text)
            throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
        return y - size;
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        String[] candidates = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"
        };
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) return PDType0Font.load(document, path.toFile());
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
