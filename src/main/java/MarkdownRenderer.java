import java.util.regex.Pattern;

public class MarkdownRenderer {

    public static String renderHtml(String markdown, boolean isDarkTheme) {
        if (markdown == null)
            return "";

        // Basic Escaping
        String html = markdown.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // --- MARKDOWN PARSING ---

        // 1. Headers (### Header)
        // Must be done before bold/italic to avoid conflicts
        html = Pattern.compile("^### (.*)$", Pattern.MULTILINE).matcher(html).replaceAll("<h3>$1</h3>");
        html = Pattern.compile("^## (.*)$", Pattern.MULTILINE).matcher(html).replaceAll("<h2>$1</h2>");
        html = Pattern.compile("^# (.*)$", Pattern.MULTILINE).matcher(html).replaceAll("<h1>$1</h1>");

        // 2. Bold (**text**)
        html = Pattern.compile("\\*\\*(.*?)\\*\\*").matcher(html).replaceAll("<b>$1</b>");

        // 3. Italic (*text*) - simplified, assumes no nesting issues
        html = Pattern.compile("\\*(.*?)\\*").matcher(html).replaceAll("<i>$1</i>");

        // 4. Unordered Lists (matches "- Item" at start of line)
        // This is a simple implementation. For true nested lists, a parser is needed.
        // We wrap the whole content later, so here we just turn items into <li>
        // Ideally we'd wrap consecutive <li>s in <ul>, but for simple display:
        html = Pattern.compile("^- (.*)$", Pattern.MULTILINE).matcher(html).replaceAll("<li>$1</li>");

        // 5. Line Breaks (Double newline -> paragraph, Single newline -> br)
        html = html.replace("\n\n", "<p>")
                .replace("\n", "<br>");

        // --- STYLING ---
        String bgColor = isDarkTheme ? "#1f2937" : "#ffffff";
        String textColor = isDarkTheme ? "#f9fafb" : "#111827";
        String accentColor = isDarkTheme ? "#60a5fa" : "#2563eb";

        String style = "body { " +
                "   font-family: 'Segoe UI', sans-serif; " +
                "   background-color: " + bgColor + "; " +
                "   color: " + textColor + "; " +
                "   font-size: 14px; " +
                "   line-height: 1.6; " +
                "   padding: 10px; " +
                "   margin: 0; " +
                "} " +
                "h1, h2, h3 { color: " + accentColor + "; margin-top: 15px; margin-bottom: 5px; } " +
                "li { margin-bottom: 5px; } " +
                "b { color: " + accentColor + "; }";

        return "<html><head><meta charset='UTF-8'><style>" + style + "</style></head><body>" + html + "</body></html>";
    }
}
