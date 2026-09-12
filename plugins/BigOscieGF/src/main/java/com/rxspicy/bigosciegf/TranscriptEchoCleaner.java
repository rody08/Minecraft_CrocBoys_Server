package com.rxspicy.bigosciegf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TranscriptEchoCleaner {
    private TranscriptEchoCleaner() {
    }

    static String clean(String text, String playerName, String newestMessage, String characterName) {
        String clean = normalize(text);
        if (clean.isEmpty()) return clean;

        String withoutCharacterLabel = stripSpeakerLabel(clean, characterName);
        if (!withoutCharacterLabel.equals(clean)) return finish(withoutCharacterLabel);
        if (!startsWithSpeaker(clean, playerName)) return finish(clean);

        String afterPlayer = clean.substring(speakerPrefixEnd(clean, playerName)).trim();
        String normalizedMessage = normalize(newestMessage);
        if (!normalizedMessage.isEmpty() && startsWithIgnoreCase(afterPlayer, normalizedMessage)) {
            afterPlayer = afterPlayer.substring(normalizedMessage.length()).trim();
        }

        String actualReply = stripSpeakerLabel(afterPlayer, characterName);
        if (!actualReply.equals(afterPlayer)) return finish(actualReply);

        Matcher marker = speakerPattern(characterName, false).matcher(afterPlayer);
        if (marker.find() && marker.start() <= 280) return finish(afterPlayer.substring(marker.end()).trim());
        return finish(clean);
    }

    private static String stripSpeakerLabel(String text, String speaker) {
        String safe = speaker == null ? "" : speaker.trim();
        Matcher bracketed = Pattern.compile("(?i)^\\[\\s*" + Pattern.quote(safe) + "\\s*]\\s*").matcher(text);
        if (bracketed.find()) return text.substring(bracketed.end()).trim();
        Matcher matcher = speakerPattern(speaker, true).matcher(text);
        return matcher.find() ? text.substring(matcher.end()).trim() : text;
    }

    private static boolean startsWithSpeaker(String text, String speaker) {
        return speakerPrefixEnd(text, speaker) >= 0;
    }

    private static int speakerPrefixEnd(String text, String speaker) {
        Matcher matcher = speakerPattern(speaker, true).matcher(text);
        return matcher.find() ? matcher.end() : -1;
    }

    private static Pattern speakerPattern(String speaker, boolean anchored) {
        String safe = speaker == null ? "" : speaker.trim();
        return Pattern.compile((anchored ? "(?i)^" : "(?i)(?:^|\\s)") + Pattern.quote(safe) + "\\s*:\\s*");
    }

    private static boolean startsWithIgnoreCase(String text, String prefix) {
        return text.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String finish(String text) {
        String clean = normalize(text);
        if (clean.length() >= 2 && ((clean.startsWith("\"") && clean.endsWith("\"")) ||
                (clean.startsWith("“") && clean.endsWith("”")))) {
            return clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }
}
