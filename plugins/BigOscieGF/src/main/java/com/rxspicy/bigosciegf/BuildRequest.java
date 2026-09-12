package com.rxspicy.bigosciegf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record BuildRequest(String type, String style, String size) {
    private static final Pattern MARKER = Pattern.compile(
            "(?is)\\s*\\[\\[BUILD_SCHEMATIC:\\s*([a-z_]+)\\s*\\|\\s*([a-z_]+)(?:\\s*\\|\\s*([a-z_]+))?\\s*]]");
    private static final Pattern ANY_MARKER = Pattern.compile("(?is)\\s*\\[\\[BUILD_SCHEMATIC:.*?]]");

    static Extraction extract(String source) {
        String text = source == null ? "" : source;
        Matcher matcher = MARKER.matcher(text);
        BuildRequest request = null;
        if (matcher.find()) {
            String size = matcher.group(3) == null ? "small" : normalize(matcher.group(3));
            request = new BuildRequest(normalize(matcher.group(1)), normalize(matcher.group(2)), size);
        }
        String visible = ANY_MARKER.matcher(text).replaceAll("").replaceAll("\\s+([.!?,])", "$1").trim();
        return new Extraction(visible, request);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    record Extraction(String visibleText, BuildRequest request) {}
}
