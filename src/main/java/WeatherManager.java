import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages weather data retrieval using IP geolocation and Open-Meteo API.
 * Designed to be robust, supporting IPv6 via ip2location.io.
 */
public class WeatherManager {
    private static final String IP2LOC_BASE_URL = "https://api.ip2location.io/?";
    private static final String WEATHER_API_BASE = "https://api.open-meteo.com/v1/forecast";

    private static class LocationData {
        String city;
        double lat;
        double lon;

        LocationData(String city, double lat, double lon) {
            this.city = city;
            this.lat = lat;
            this.lon = lon;
        }
    }

    /**
     * @param translateToEnglish if false, translates common terms to Malay.
     * @return Formatted string "City: Weather (Updated: Time)"
     */
    public static String getCurrentWeather(boolean translateToEnglish) {
        try {
            System.out.println("[WeatherManager] Detecting location strategy: IP2Location...");
            LocationData loc = fetchLocationData();

            if (loc == null || (loc.city != null && loc.city.startsWith("Error:"))
                    || "Location Unavailable".equals(loc.city) || "Location Not Found".equals(loc.city)) {
                if (loc != null)
                    return loc.city;
                return "Location Unavailable";
            }

            System.out.println("[WeatherManager] Location found: " + loc.city);
            String weatherData = fetchWeatherData(loc);

            if (weatherData == null || weatherData.isEmpty()) {
                return loc.city + ": Weather Data Unavailable";
            }
            return parseAndFormatWeather(loc.city, weatherData, translateToEnglish);

        } catch (Exception e) {
            e.printStackTrace();
            return "Weather Error";
        }
    }

    private static LocationData fetchLocationData() {
        try {
            String key = EnvLoader.get("IP2LOCATION_KEY");
            if (key == null || key.isEmpty()) {
                System.err.println("[WeatherManager] Missing IP2LOCATION_KEY in .env");
                return new LocationData("Error: No API Key", 0, 0);
            }

            // Fetches location data using IP2Location API
            String url = IP2LOC_BASE_URL + "key=" + key + "&format=json";
            String json = API.get(url);

            if (json == null)
                return new LocationData("Error: Connection Failed", 0, 0);

            // JSON extraction
            String city = extractJsonValue(json, "city_name", 0);
            String latStr = extractJsonValue(json, "latitude", 0);
            String lonStr = extractJsonValue(json, "longitude", 0);

            if (latStr != null && lonStr != null) {
                try {
                    double lat = Double.parseDouble(latStr);
                    double lon = Double.parseDouble(lonStr);
                    if (city == null || city.equals("null") || city.equals("-"))
                        return new LocationData("Location Not Found", 0, 0); // Explicit error

                    return new LocationData(city, lat, lon);
                } catch (NumberFormatException e) {
                    System.err.println("[WeatherManager] Error parsing coordinates: " + e.getMessage());
                    return new LocationData("Error: Data Parse", 0, 0);
                }
            }
        } catch (Exception e) {
            System.err.println("[WeatherManager] Location fetch error: " + e.getMessage());
            return new LocationData("Error: Exception", 0, 0);
        }
        return new LocationData("Location Unavailable", 0, 0);
    }

    private static String fetchWeatherData(LocationData loc) {
        // timezone=auto allows the server to determine local time for the coordinates
        String url = WEATHER_API_BASE + "?latitude=" + loc.lat + "&longitude=" + loc.lon
                + "&current_weather=true&timezone=auto";
        return API.get(url);
    }

    private static String parseAndFormatWeather(String city, String json, boolean english) {
        // Locate current_weather block
        int blockIndex = json.indexOf("\"current_weather\"");
        if (blockIndex == -1)
            blockIndex = 0;

        // Extract raw values
        String codeStr = extractJsonValue(json, "weathercode", blockIndex);
        String timeStr = extractJsonValue(json, "time", blockIndex);

        // Decode Code
        int code = -1;
        if (codeStr != null) {
            try {
                code = Integer.parseInt(codeStr.trim());
            } catch (Exception e) {
            }
        }
        String desc = decodeWMOCode(code);

        // Format Time
        String formattedTime = formatTime(timeStr);

        // Translate if needed
        if (!english) {
            desc = translateToMalay(desc);
        }

        return String.format("%s: %s (Updated: %s)", city, desc, formattedTime);
    }

    private static String extractJsonValue(String json, String key, int fromIndex) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search, fromIndex);
        if (start == -1)
            return null;

        start += search.length();

        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start)))
            start++;

        if (start >= json.length())
            return null;

        char firstChar = json.charAt(start);
        if (firstChar == '"') {
            // String value
            start++;
            int end = json.indexOf("\"", start);
            return (end == -1) ? null : json.substring(start, end);
        } else {
            // Number/Boolean value
            int end = start;
            while (end < json.length()
                    && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }

    private static String formatTime(String isoTime) {
        if (isoTime == null)
            return "Just now";
        try {
            // API returns ISO like "2023-10-10T14:30"
            LocalDateTime dt = LocalDateTime.parse(isoTime);
            return dt.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return isoTime.replace("T", " ");
        }
    }

    private static String decodeWMOCode(int code) {
        switch (code) {
            case 0:
                return "Clear Sky";
            case 1:
            case 2:
            case 3:
                return "Partly Cloudy";
            case 45:
            case 48:
                return "Foggy";
            case 51:
            case 53:
            case 55:
                return "Drizzle";
            case 61:
            case 63:
            case 65:
                return "Rain";
            case 80:
            case 81:
            case 82:
                return "Showers";
            case 95:
                return "Thunderstorms";
            case 96:
            case 99:
                return "Severe Thunderstorms";
            default:
                return "Cloudy";
        }
    }

    private static String translateToMalay(String eng) {
        if (eng.contains("Thunder"))
            return "Ribut Petir";
        if (eng.contains("Rain"))
            return "Hujan";
        if (eng.contains("Drizzle"))
            return "Gerimis";
        if (eng.contains("Cloud"))
            return "Mendung";
        if (eng.contains("Fog"))
            return "Kababus";
        if (eng.contains("Clear"))
            return "Cerah";
        if (eng.contains("Shower"))
            return "Hujan Lebat";
        return eng;
    }
}