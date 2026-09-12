package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrustPolicyTest {
    private final TrustPolicy policy = new TrustPolicy(50, 50, 100, 100);

    @Test
    void unlocksCapabilitiesInOrder() {
        assertEquals("food only", policy.tier(0));
        assertFalse(policy.canEnchant(49));
        assertTrue(policy.canEnchant(50));
        assertFalse(policy.canUseUnsafeItems(49));
        assertTrue(policy.canUseUnsafeItems(50));
        assertFalse(policy.canUseCustomItems(99));
        assertTrue(policy.canUseCustomItems(100));
        assertEquals("everything", policy.tier(100));
    }

    @Test
    void clampsScores() {
        assertEquals(0, policy.clamp(-10));
        assertEquals(100, policy.clamp(999));
    }

    @Test
    void rejectsMisorderedThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new TrustPolicy(50, 10, 75, 100));
    }
}
