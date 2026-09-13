package com.rxspicy.bigosciegf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record BuildRequest(String type, String style, String size) {
    private static final Pattern MARKER = Pattern.compile(
            "(?is)\\s*\\[\\[BUILD_SCHEMATIC:\\s*([a-z_]+)\\s*\\|\\s*([a-z_]+)(?:\\s*\\|\\s*([a-z_]+))?\\s*]]");
    private static final Pattern ANY_MARKER = Pattern.compile("(?is)\\s*\\[\\[BUILD_SCHEMATIC:.*?]]");
    private static final Pattern DESIGN = Pattern.compile("(?is)\\s*\\[\\[BUILD_SCHEMATIC:\\s*([^\\[\\]\\r\\n|]{1,400})\\s*]]");

    String description() {
        return style.isBlank() ? type : size + " " + style.replace('_', ' ') + " " + type.replace('_', ' ');
    }

    static String control(String source, String name) {
        if (source == null) return "";
        String text = source.trim().toLowerCase(Locale.ROOT);
        text = text.replaceFirst("^(?:" + Pattern.quote(name.toLowerCase(Locale.ROOT)) + "|nyx)[,!: ]+", "");
        text = text.replaceFirst("[.!]+$", "").trim();
        return switch (text) {
            case "confirm build", "confirm the build" -> "confirm";
            case "cancel build", "cancel the build" -> "cancel";
            case "move build here", "move the build here" -> "move";
            default -> "";
        };
    }

    static String prompt() {
        return " When directly asked to build, you can draft ANY subject as a Minecraft block sculpture: cars, houses, statues, ships, etc. " +
                "A request to build or design a structure means CREATE A SCHEMATIC, never give building materials instead. " +
                "Append [[BUILD_SCHEMATIC: short description of the requested build]] with the subject, colors and details in at most 400 characters. " +
                "Example: player says 'Nyx build me a car'; reply 'I'll sketch a car for you. [[BUILD_SCHEMATIC: a red sports car with black wheels and glass windows]]'. " +
                "Preserve requested details and follow-up context. Large subjects will be scaled to the server's size limits. " +
                "A separate designer generates the blocks. Do not output block data yourself or claim the build is finished. " +
                "The player must confirm the preview before placement. Builds are static, not drivable vehicles or working machines.";
    }

    static BuildRequest directRequest(String source, String name) {
        if (source == null || source.length() > 480) return null;
        String text = source.strip().replaceFirst("(?i)^(?:" + Pattern.quote(name) + "|nyx)[,!: ]+", "");
        Matcher direct = Pattern.compile("(?is)^(?:please\\s+)?build\\s+(?:me\\s+)?(.{1,400})$").matcher(text);
        if (!direct.matches()) return null;
        String description = direct.group(1).strip();
        if (description.matches("(?i)(?:confirm|cancel|status|move)")) return null;
        if (description.matches("(?is)^(?:it|that|this|one|another one|the same)(?:\\s.*|[.!?]*)$")) return null;
        return new BuildRequest(description, "", "");
    }

    static Extraction extract(String source) {
        String text = source == null ? "" : source;
        Matcher matcher = MARKER.matcher(text);
        BuildRequest request = null;
        Matcher design = DESIGN.matcher(text);
        if (design.find()) {
            request = new BuildRequest(design.group(1).trim(), "", "");
        } else if (matcher.find()) {
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
