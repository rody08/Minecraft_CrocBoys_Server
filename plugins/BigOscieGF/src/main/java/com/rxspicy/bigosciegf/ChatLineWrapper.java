package com.rxspicy.bigosciegf;

import java.util.ArrayList;
import java.util.List;

final class ChatLineWrapper {
    private ChatLineWrapper() {
    }

    static List<String> wrap(String text, int maxCharacters) {
        String clean = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return List.of();

        int limit = Math.max(20, maxCharacters);
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : clean.split("(?<=[.!?])\\s+")) {
            if (sentence.length() <= limit && appendIfFits(current, sentence, limit)) continue;

            flush(lines, current);
            if (sentence.length() <= limit) {
                current.append(sentence);
                continue;
            }

            for (String word : sentence.split("\\s+")) {
                if (word.length() > limit) {
                    flush(lines, current);
                    for (int start = 0; start < word.length(); start += limit) {
                        int end = Math.min(word.length(), start + limit);
                        String part = word.substring(start, end);
                        if (end == word.length()) current.append(part);
                        else lines.add(part);
                    }
                } else if (!appendIfFits(current, word, limit)) {
                    flush(lines, current);
                    current.append(word);
                }
            }
        }

        flush(lines, current);
        return List.copyOf(lines);
    }

    private static boolean appendIfFits(StringBuilder current, String value, int limit) {
        int separator = current.isEmpty() ? 0 : 1;
        if (current.length() + separator + value.length() > limit) return false;
        if (separator == 1) current.append(' ');
        current.append(value);
        return true;
    }

    private static void flush(List<String> lines, StringBuilder current) {
        if (current.isEmpty()) return;
        lines.add(current.toString());
        current.setLength(0);
    }
}
