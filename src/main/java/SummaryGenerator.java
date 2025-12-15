import java.util.List;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.InputStream;
import java.io.IOException;

public class SummaryGenerator {

    public static String generate(List<SmartJournal.JournalEntry> entries, boolean isEnglish) {
        if (entries == null || entries.isEmpty()) {
            return isEnglish ? "No data available for analysis." : "Tiada data untuk analisis.";
        }

        // Limit to last 7 entries to avoid context limit issues
        int limit = Math.min(entries.size(), 7);
        List<SmartJournal.JournalEntry> recentEntries = entries.subList(0, limit);

        StringBuilder prompt = new StringBuilder();

        // Detailed User Prompt Structure
        prompt.append(
                "Goal: Analyze the provided journal entries from the last week and generate a clear, empathetic, and insight-driven summary for the user. Avoid technical jargon like \"polarity\" or \"distribution.\" Focus on feelings, moments, and patterns.\n\n");

        prompt.append("Input Data: Journal entries from the provided date range.\n\n");

        prompt.append("Output Structure & Requirements:\n");
        prompt.append(
                "Generate the summary using the following four-part, simplified structure. Use **Markdown** formatting for bolding titles.\n\n");

        prompt.append("1. Your Weekly Vibe Check\n");
        prompt.append(
                "Overall Feeling: Summarize the dominant emotional tone of the week in a short sentence or phrase.\n");
        prompt.append(
                "Vibe Trend: Briefly mention how this week compared to typical days (based on the provided entries).\n\n");

        prompt.append("2. The Week's Key Moments\n");
        prompt.append(
                "Best Day/Highlight: Identify the happiest or most positive day and entry. State what happened or what you achieved that made it so good.\n");
        prompt.append(
                "Toughest Day/Challenge: Identify the most challenging or negative day/entry. Briefly describe the main source of the difficulty.\n\n");

        prompt.append("3. What Drove Your Feelings?\n");
        prompt.append(
                "Top 3 Positive Fuel: What were the major themes that consistently made you feel good this week? (e.g., Time with Family, Creative Projects).\n");
        prompt.append("Top 3 Challenge Areas: What were the major themes that repeatedly brought down your mood?\n\n");

        prompt.append("4. Your Reflection for Next Week\n");
        prompt.append(
                "The Big Takeaway: State one clear, simple observation that connects a positive action to a positive feeling this week.\n");
        prompt.append(
                "Suggested Focus: Based on the 'Challenge Areas,' offer one gentle, actionable suggestion for the user to try in the coming week.\n\n");

        prompt.append("Translate to " + (isEnglish ? "English" : "Bahasa Malaysia")
                + ". Address the user directly as 'You'.\n\n");
        prompt.append("Entries:\n");

        for (SmartJournal.JournalEntry entry : recentEntries) {
            prompt.append("- Date: ").append(entry.getDate())
                    .append(", Mood: ").append(entry.getAiMood())
                    .append(", Weather: ").append(entry.getWeather()) // Added Weather
                    .append(", Content: ").append(entry.getContent().replace("\n", " "))
                    .append("\n");
        }

        prompt.append("\nSummary:");

        // Gemini JSON Payload: { "contents": [{ "parts": [{"text": "..."}] }] }
        String jsonInput = "{"
                + "\"contents\": [{"
                + "\"parts\": [{\"text\": \"" + escapeJson(prompt.toString()) + "\"}]"
                + "}]"
                + "}";

        String apiKey = EnvLoader.get("GEMINI_API_KEY");
        if (apiKey == null) {
            return "Error: GEMINI_API_KEY not found in .env";
        }

        String fullUrl = API.SUMMARY_API_URL + "?key=" + apiKey;
        System.out.println("DEBUG: AI Summary Prompt len=" + prompt.length());

        String response = API.post(fullUrl, jsonInput);

        if (response == null || response.isEmpty()) {
            return isEnglish ? "Could not generate summary at this time."
                    : "Tidak dapat menjana ringkasan pada masa ini.";
        }

        return cleanResponse(response);
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String cleanResponse(String jsonResponse) {
        // Gemini Response: { "candidates": [ { "content": { "parts": [ { "text": "THE
        // SUMMARY" } ] } } ] }
        // Simple extraction logic without JSON lib
        try {
            // Find "text":
            int idx = jsonResponse.indexOf("\"text\":");
            if (idx == -1)
                return jsonResponse; // or error

            // Find start quote of value
            int startQuote = jsonResponse.indexOf("\"", idx + 7);
            if (startQuote == -1)
                return jsonResponse;

            // Find end quote (handle minimal escaping if possible, though clean extract is
            // harder without lib)
            int endQuote = -1;
            for (int i = startQuote + 1; i < jsonResponse.length(); i++) {
                if (jsonResponse.charAt(i) == '"' && jsonResponse.charAt(i - 1) != '\\') {
                    endQuote = i;
                    break;
                }
            }

            if (endQuote != -1) {
                String content = jsonResponse.substring(startQuote + 1, endQuote);
                // Unescape basic JSON chars
                content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                // Unescape Unicode characters form
                return unescapeUnicode(content);
            }

            return jsonResponse;
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private static String unescapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u') {
                // Potential Unicode escape
                try {
                    String hex = input.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                } catch (Exception e) {
                    // Not a valid unicode escape, treat as literal
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public static boolean exportToPDF(String summary, String filePath) {
        try (PDDocument document = new PDDocument()) {
            // Load fonts (Regular & Bold)
            PDType0Font fontRegular;
            PDType0Font fontBold;
            try (InputStream regularStream = SummaryGenerator.class.getResourceAsStream("/fonts/arial.ttf");
                    InputStream boldStream = SummaryGenerator.class.getResourceAsStream("/fonts/arialbd.ttf")) {

                if (regularStream == null)
                    throw new IOException("Regular font not found in /fonts/arial.ttf");
                if (boldStream == null)
                    throw new IOException("Bold font not found in /fonts/arialbd.ttf");

                fontRegular = PDType0Font.load(document, regularStream);
                fontBold = PDType0Font.load(document, boldStream);
            }

            // PDF Context Helper to manage state
            PDFContext ctx = new PDFContext(document, fontRegular, fontBold);
            ctx.addPage(); // Start first page

            String[] lines = summary.split("\n");

            for (String line : lines) {
                // Formatting Logic:
                // 1. Headers (Lines starting with '1.', '2.', 'Goal:', etc. or wrapped in **)
                // 2. Bold Keys (e.g. "**Best Day:** ...")

                boolean isHeader = line.matches(
                        "^(1\\.|2\\.|3\\.|4\\.|Goal:|Overall Feeling:|Vibe Trend:|Best Day/Highlight:|Toughest Day/Challenge:|Top 3 Positive Fuel:|Top 3 Challenge Areas:|The Big Takeaway:|Suggested Focus:).*")
                        || line.startsWith("#");

                // Simple markdown cleanup for headers
                String cleanLine = line.replace("**", "").replace("#", "").trim();

                if (cleanLine.isEmpty()) {
                    ctx.yPosition -= ctx.leading; // Empty line spacing
                    continue;
                }

                if (isHeader) {
                    ctx.yPosition -= ctx.leading * 0.5f; // Extra space before header
                    ctx.checkPage();
                    ctx.printMultiLine(cleanLine, fontBold, 14); // Larger/Bold for headers
                    ctx.yPosition -= ctx.leading * 0.5f; // Extra space after header
                } else {
                    ctx.printMarkdownLine(line, 12);
                }
            }

            ctx.closeStream();
            document.save(filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper Class for maintaining PDF State
    private static class PDFContext {
        PDDocument document;
        PDPageContentStream contentStream;
        PDType0Font fontRegular;
        PDType0Font fontBold;

        // Configuration
        final float margin = 50;
        final float width = PDRectangle.A4.getWidth() - 2 * margin;
        final float startY = PDRectangle.A4.getHeight() - margin;
        final float bottomY = margin;
        final float leading = 16f;

        float yPosition;

        PDFContext(PDDocument doc, PDType0Font reg, PDType0Font bld) {
            this.document = doc;
            this.fontRegular = reg;
            this.fontBold = bld;
            this.yPosition = startY;
        }

        void addPage() throws IOException {
            closeStream();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = startY;
        }

        void closeStream() throws IOException {
            if (contentStream != null)
                contentStream.close();
        }

        void checkPage() throws IOException {
            if (yPosition < bottomY) {
                addPage();
            }
        }

        // Prints a line that might wrap. Assumes one font style for the whole block.
        void printMultiLine(String text, PDType0Font font, float fontSize) throws IOException {
            List<String> wrappedLines = breakText(text, font, fontSize, width);
            for (String line : wrappedLines) {
                checkPage();
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= leading;
            }
        }

        // Handles simple inline bold markdown like "**Bold** Normal"
        void printMarkdownLine(String text, float fontSize) throws IOException {
            String[] segments = text.split("\\*\\*");

            List<Token> tokens = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                boolean isBold = (i % 2 != 0); // ODD segments are inside **...**

                String seg = segments[i];
                String[] words = seg.split(" ");
                for (int j = 0; j < words.length; j++) {
                    String w = words[j];
                    tokens.add(new Token(w, isBold));
                    if (j < words.length - 1 || seg.endsWith(" ")) {
                        tokens.add(new Token(" ", isBold));
                    }
                }
                // Check if we need to add a trailing space if the segment ended with one but
                // split consumed it
                // OR if it's not the last segment and usually expects a space separator if
                // adjacent
                // But markdown **Bold**Normal vs **Bold** Normal.
                // We'll trust the input spacing mostly.
            }

            List<Token> lineBuffer = new ArrayList<>();
            float currentLineWidth = 0;

            for (Token t : tokens) {
                float wordWidth = 0;
                try {
                    wordWidth = (t.isBold ? fontBold : fontRegular).getStringWidth(t.text) / 1000 * fontSize;
                } catch (IllegalArgumentException e) {
                    // Fallback for unsupported chars?
                    wordWidth = (t.isBold ? fontBold : fontRegular).getStringWidth("?") / 1000 * fontSize;
                }

                if (currentLineWidth + wordWidth > width) {
                    printTokens(lineBuffer, fontSize);
                    yPosition -= leading;
                    checkPage();
                    lineBuffer.clear();
                    currentLineWidth = 0;
                    if (t.text.equals(" "))
                        continue; // Skip leading space on new line
                }

                lineBuffer.add(t);
                currentLineWidth += wordWidth;
            }
            if (!lineBuffer.isEmpty()) {
                printTokens(lineBuffer, fontSize);
                yPosition -= leading;
            }
        }

        void printTokens(List<Token> lineTokens, float fontSize) throws IOException {
            checkPage();
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);

            for (Token t : lineTokens) {
                contentStream.setFont(t.isBold ? fontBold : fontRegular, fontSize);
                try {
                    contentStream.showText(t.text);
                } catch (IllegalArgumentException e) {
                    // Handle unsupported characters gracefully-ish
                    contentStream.showText("?");
                }
            }
            contentStream.endText();
        }

        List<String> breakText(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            float lineWidth = 0;

            for (String word : words) {
                float w = 0;
                try {
                    w = font.getStringWidth(word + " ") / 1000 * fontSize;
                } catch (Exception e) {
                    w = 10;
                } // Fallback width

                if (lineWidth + w > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                    lineWidth = 0;
                }
                line.append(word).append(" ");
                lineWidth += w;
            }
            lines.add(line.toString());
            return lines;
        }
    }

    private static class Token {
        String text;
        boolean isBold;

        Token(String t, boolean b) {
            text = t;
            isBold = b;
        }
    }
}