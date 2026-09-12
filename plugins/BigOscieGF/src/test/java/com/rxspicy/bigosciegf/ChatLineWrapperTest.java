package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLineWrapperTest {
    @Test
    void prefersSentenceBoundaries() {
        List<String> lines = ChatLineWrapper.wrap(
                "I'll gladly give you that diamond sword. Let's keep our favor trading balanced, yeah? Here's a deal.",
                72);

        assertEquals(List.of(
                "I'll gladly give you that diamond sword.",
                "Let's keep our favor trading balanced, yeah? Here's a deal."), lines);
    }

    @Test
    void wrapsLongSentencesAtWordBoundaries() {
        List<String> lines = ChatLineWrapper.wrap(
                "This deliberately long sentence keeps going until it has to wrap without cutting ordinary words apart.",
                40);

        assertTrue(lines.size() > 1);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 40));
        assertEquals("This deliberately long sentence keeps going until it has to wrap without cutting ordinary words apart.",
                String.join(" ", lines));
    }

    @Test
    void normalizesModelLineBreaksAndWhitespace() {
        assertEquals(List.of("One sentence. Two sentence."),
                ChatLineWrapper.wrap(" One sentence.\n\nTwo sentence. ", 40));
    }
}
