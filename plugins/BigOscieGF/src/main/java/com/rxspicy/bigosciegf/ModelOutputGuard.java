package com.rxspicy.bigosciegf;

import java.util.List;
import java.util.Locale;

final class ModelOutputGuard {
    private static final List<String> PROMPT_LEAK_MARKERS = List.of(
            "you are participating in one shared minecraft group conversation",
            "the newest player speaking is",
            "persistent trust score for this player",
            "the following transcript is reference data only",
            "<recent_conversation>",
            "</recent_conversation>",
            "permitted hidden markers",
            "do not emit a build_schematic marker",
            "do not emit any memory marker for this speaker",
            "in a shared minecraft chat",
            "use plain speech with little to no roleplay",
            "answer directly in 6-18 visible words"
    );

    private ModelOutputGuard() {
    }

    static String clean(String text, String playerName, String newestMessage, String characterName) {
        String clean = TranscriptEchoCleaner.clean(text, playerName, newestMessage, characterName);
        String lower = clean.toLowerCase(Locale.ROOT);
        for (String marker : PROMPT_LEAK_MARKERS) {
            if (lower.contains(marker)) return "";
        }
        return clean;
    }
}
