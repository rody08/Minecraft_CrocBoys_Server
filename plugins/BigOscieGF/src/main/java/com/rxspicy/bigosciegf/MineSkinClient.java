package com.rxspicy.bigosciegf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MineSkinClient {
    record SkinData(String uuid, String value, String signature) {}

    private static final Pattern UUID = Pattern.compile("(?i)([0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12})");
    private static final Pattern VALUE = Pattern.compile("\\\"value\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final Pattern SIGNATURE = Pattern.compile("\\\"signature\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final Pattern RESPONSE_UUID = Pattern.compile("\\\"uuid\\\"\\s*:\\s*\\\"([0-9a-fA-F-]{32,36})\\\"");
    private static final Pattern ERROR = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");

    private final NyxPlugin plugin;
    private final HttpClient http;

    MineSkinClient(NyxPlugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    CompletableFuture<SkinData> fetch(String input) {
        String uuid = extractUuid(input);
        if (uuid == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("MineSkin value must contain a skin UUID."));
        }
        String endpoint = plugin.getConfig().getString("npc.skin.mineskin-endpoint", "https://api.mineskin.org/v2/skins/");
        if (endpoint == null || endpoint.isBlank()) endpoint = "https://api.mineskin.org/v2/skins/";
        if (!endpoint.endsWith("/")) endpoint += "/";

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint + uuid))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "Nyx/0.5.0")
                .GET();
        String apiKey = plugin.getConfig().getString("npc.skin.mineskin-api-key", "");
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("MINESKIN_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey.trim());

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Matcher err = ERROR.matcher(response.body());
                        String detail = err.find() ? unescape(err.group(1)) : "HTTP " + response.statusCode();
                        throw new IllegalStateException("MineSkin: " + detail);
                    }
                    String body = response.body();
                    Matcher valueMatch = VALUE.matcher(body);
                    Matcher sigMatch = SIGNATURE.matcher(body);
                    if (!valueMatch.find() || !sigMatch.find()) {
                        throw new IllegalStateException("MineSkin response did not contain texture value/signature.");
                    }
                    Matcher uuidMatch = RESPONSE_UUID.matcher(body);
                    String returnedUuid = uuidMatch.find() ? uuidMatch.group(1) : uuid;
                    return new SkinData(returnedUuid, unescape(valueMatch.group(1)), unescape(sigMatch.group(1)));
                });
    }

    static String extractUuid(String input) {
        if (input == null) return null;
        Matcher m = UUID.matcher(input.trim());
        if (!m.find()) return null;
        String raw = m.group(1).replace("-", "").toLowerCase(Locale.ROOT);
        if (raw.length() != 32) return null;
        return raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16) + "-" +
                raw.substring(16, 20) + "-" + raw.substring(20);
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    default -> out.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else out.append(c);
        }
        if (escape) out.append('\\');
        return out.toString();
    }
}
