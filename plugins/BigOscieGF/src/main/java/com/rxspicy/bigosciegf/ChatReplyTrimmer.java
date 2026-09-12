package com.rxspicy.bigosciegf;

final class ChatReplyTrimmer {
    private ChatReplyTrimmer() {
    }

    static String fit(String text, int maxCharacters) {
        String clean = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxCharacters) return clean;

        int sentenceEnd = lastSentenceEnd(clean, maxCharacters);
        if (sentenceEnd >= 0) return clean.substring(0, sentenceEnd + 1).trim();

        int wordEnd = clean.lastIndexOf(' ', Math.max(0, maxCharacters - 1));
        if (wordEnd < 1) wordEnd = Math.max(1, maxCharacters - 1);
        return clean.substring(0, wordEnd).trim() + "…";
    }

    private static int lastSentenceEnd(String text, int before) {
        for (int i = Math.min(before - 1, text.length() - 1); i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') return i;
        }
        return -1;
    }
}
