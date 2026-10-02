package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NyxPersonalityTest {
    @Test void keepsHerSpokenPersonalityWithoutActedStageDirections() {
        assertEquals("Oh, just peachy. Nobody has blown up the house yet.", NyxPersonality.dialogue(
                "*I look up from my book, arching an eyebrow.* Oh, just peachy. *I smirk, closing the book.* Nobody has blown up the house yet."));
        assertEquals("I *really* like it. [[BUILD_SCHEMATIC: a car]]", NyxPersonality.dialogue("I *really* like it. [[BUILD_SCHEMATIC: a car]]"));
    }
    @Test void restoresOnlyTheRegressedGenericDefault() {
        assertEquals(NyxPersonality.DEFAULT, NyxPersonality.restore(NyxPersonality.GENERIC.replace(". ", ".\n")));
        assertEquals("My custom Nyx persona.", NyxPersonality.restore("My custom Nyx persona."));
        assertEquals(NyxPersonality.DEFAULT, NyxPersonality.restore(null));
        assertTrue(OpenAiClient.defaultPersonality().contains("goth girlfriend"));
    }
}
