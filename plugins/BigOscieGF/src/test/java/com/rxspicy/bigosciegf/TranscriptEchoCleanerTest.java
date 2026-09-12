package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptEchoCleanerTest {
    @Test
    void removesTheExactScreenshotStyleTranscriptReplay() {
        assertEquals("What kind of house did you have in mind?", TranscriptEchoCleaner.clean(
                "RXSpicy: hey nyx, build us a house Nyx: What kind of house did you have in mind?",
                "RXSpicy", "hey nyx, build us a house", "Nyx"));
    }

    @Test
    void handlesLineBreaksAndACharacterLabel() {
        assertEquals("I've drafted a small oak house.", TranscriptEchoCleaner.clean(
                "RXSpicy: just a small humble house nyx\nNyx: I've drafted a small oak house.",
                "RXSpicy", "just a small humble house nyx", "Nyx"));
    }

    @Test
    void removesOnlyAStandaloneCharacterLabel() {
        assertEquals("Fine, but you're decorating it.", TranscriptEchoCleaner.clean(
                "Nyx: Fine, but you're decorating it.", "RXSpicy", "build", "Nyx"));
    }

    @Test
    void removesTheBracketedLabelMagnumSometimesUses() {
        assertEquals("Hello! How can I help?", TranscriptEchoCleaner.clean(
                "[NYX] Hello! How can I help?", "RXSpicy", "hi nyx", "Nyx"));
        assertEquals("Hello! How can I help?", TranscriptEchoCleaner.clean(
                "[NYX] \"Hello! How can I help?\"", "RXSpicy", "hi nyx", "Nyx"));
    }

    @Test
    void leavesNormalDialogueUntouched() {
        assertEquals("RXSpicy, you're decorating this one.", TranscriptEchoCleaner.clean(
                "RXSpicy, you're decorating this one.", "RXSpicy", "build", "Nyx"));
    }

    @Test
    void doesNotMistakeAColonInsideThePlayersMessageForTheFinalLabel() {
        assertEquals("Absolutely.", TranscriptEchoCleaner.clean(
                "RXSpicy: Nyx: build us a house Nyx: Absolutely.",
                "RXSpicy", "Nyx: build us a house", "Nyx"));
    }
}
