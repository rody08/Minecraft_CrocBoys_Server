package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BuildRequestTest {
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
