package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelOutputGuardTest {
    @Test
    void blocksThePromptLeakShownInTheScreenshot() {
        assertEquals("", ModelOutputGuard.clean(
                "You are participating in one shared Minecraft group conversation. The newest player speaking is RXSpicy.",
                "RXSpicy", "hi nyx", "Nyx"));
    }

    @Test
    void blocksCurrentPromptWordingToo() {
        assertEquals("", ModelOutputGuard.clean(
                "Reply as Nyx in a shared Minecraft chat. Use plain speech with little to no roleplay.",
                "RXSpicy", "hello", "Nyx"));
    }

    @Test
    void keepsLegitimateDialogueAndActionMarkers() {
        assertEquals("I'll draft it. [[BUILD_SCHEMATIC: house | oak | small]]", ModelOutputGuard.clean(
                "I'll draft it. [[BUILD_SCHEMATIC: house | oak | small]]",
                "RXSpicy", "build a house", "Nyx"));
    }
}
