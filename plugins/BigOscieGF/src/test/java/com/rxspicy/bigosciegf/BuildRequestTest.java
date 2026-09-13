package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BuildRequestTest {
    @Test
    void explicitBuildImperativesDoNotDependOnTheDialogueModel() {
        assertEquals("a red car", BuildRequest.directRequest("Nyx, build me a red car", "Nyx").description());
        assertEquals("a two-story pink house", BuildRequest.directRequest("nyx build a two-story pink house", "Nyx").description());
        assertNull(BuildRequest.directRequest("Nyx don't build a car", "Nyx"));
        assertNull(BuildRequest.directRequest("Nyx how do I build a car?", "Nyx"));
        assertNull(BuildRequest.directRequest("Nyx build it here", "Nyx"));
        assertNull(BuildRequest.directRequest("Nyx build " + "x".repeat(401), "Nyx"));
    }

    @Test
    void acceptsFreeformSubjectsAndDetails() {
        var result = BuildRequest.extract("On it. [[BUILD_SCHEMATIC: a red sports car with 4 black wheels]]");
        assertEquals("On it.", result.visibleText());
        assertEquals("a red sports car with 4 black wheels", result.request().description());
    }

    @Test
    void requiresAnExplicitConfirmationNotNegationOrQuestions() {
        assertEquals("confirm", BuildRequest.control("Nyx, confirm build!", "Nyx"));
        assertEquals("cancel", BuildRequest.control("Nyx cancel the build", "Nyx"));
        assertEquals("move", BuildRequest.control("Nyx move build here", "Nyx"));
        assertEquals("", BuildRequest.control("Nyx don't confirm build", "Nyx"));
        assertEquals("", BuildRequest.control("Nyx how do I confirm build?", "Nyx"));
    }

    @Test
    void extractsOnlyTheConstrainedMarker() {
        BuildRequest.Extraction result = BuildRequest.extract(
                "Give me a moment. [[BUILD_SCHEMATIC: house | dark_oak | small]]");
        assertEquals("Give me a moment.", result.visibleText());
        assertEquals(new BuildRequest("house", "dark_oak", "small"), result.request());
    }

    @Test
    void acceptsTheShortMarkerMagnumActuallyProduces() {
        BuildRequest.Extraction result = BuildRequest.extract(
                "I'll draft it. [[BUILD_SCHEMATIC: house | oak]]");
        assertEquals("I'll draft it.", result.visibleText());
        assertEquals(new BuildRequest("house", "oak", "small"), result.request());
    }

    @Test
    void ignoresFreeformCommandsAndCoordinates() {
        BuildRequest.Extraction result = BuildRequest.extract("[[BUILD: //paste at 1 2 3]]");
        assertNull(result.request());
    }

    @Test
    void hidesMalformedBuildControlText() {
        BuildRequest.Extraction result = BuildRequest.extract(
                "I can try. [[BUILD_SCHEMATIC: house | pink_gabydoll]]");
        assertEquals("I can try.", result.visibleText());
        assertEquals(new BuildRequest("house", "pink_gabydoll", "small"), result.request());
    }
}
