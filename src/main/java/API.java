import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

public class API {

    // CRITICAL FIX: Updated to use the new Hugging Face router endpoint for the
    // multilingual model
    public static final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/tabularisai/multilingual-sentiment-analysis";
    // Using Gemini 2.5 Flash as requested
    public static final String SUMMARY_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    /**
     * GENERIC GET REQUEST
     */
    public static String get(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            // CRITICAL FIX: Increased timeout to 15 seconds (15000ms) to allow models to
            // load
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GENERIC POST REQUEST (For Mood/AI)
     */
    public static String post(String urlString, String jsonInputString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Load Token
            // If communicating with Google APIs (Gemini), we likely use the key param in
            // URL,
            // so we should NOT attach the Bearer token intended for Hugging Face.
            if (!urlString.contains("googleapis.com")) {
                String token = EnvLoader.get("BEARER_TOKEN");
                if (token != null)
                    conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            // CRITICAL FIX: Increased timeout to 15 seconds (15000ms)
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            return readResponse(conn);
        } catch (Exception e) {
            System.err.println("API POST Error: " + e.getMessage());
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        BufferedReader br;
        if (status >= 200 && status < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            // Read error stream for debugging API errors
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }
}