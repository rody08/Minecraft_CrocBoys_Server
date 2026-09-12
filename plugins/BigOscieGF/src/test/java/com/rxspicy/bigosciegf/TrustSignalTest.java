package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrustSignalTest {
    @Test
    void acceptsOnlyThreeStatesAndHidesMarker() {
        TrustSignal.Extraction result = TrustSignal.extract("Fine. [[TRUST: 100]]");
        assertEquals("Fine.", result.visibleText());
        assertNotNull(result.signal());
        assertEquals(100, result.signal().score());
    }

    @Test
    void stripsInvalidStateWithoutApplyingIt() {
        TrustSignal.Extraction result = TrustSignal.extract("No. [[TRUST: 75]]");
        assertEquals("No.", result.visibleText());
        assertNull(result.signal());
    }
}
