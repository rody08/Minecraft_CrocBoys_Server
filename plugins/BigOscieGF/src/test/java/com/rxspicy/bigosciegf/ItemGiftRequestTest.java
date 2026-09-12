package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemGiftRequestTest {
    @Test
    void extractsBasicItemAndHidesMarker() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "Fine, try not to lose it.\n[[GIVE_ITEM: minecraft:diamond_sword | 1]]");

        assertEquals("Fine, try not to lose it.", result.visibleText());
        assertNotNull(result.request());
        assertEquals("minecraft:diamond_sword", result.request().materialKey());
        assertEquals(1, result.request().amount());
        assertTrue(result.request().enchantments().isEmpty());
    }

    @Test
    void removesMarkerSpacingBeforeSentencePunctuation() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "Here you go [[GIVE_ITEM: minecraft:cooked_chicken | 4]].");
        assertEquals("Here you go.", result.visibleText());
    }

    @Test
    void extractsEnchantmentsAndCustomName() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "Catch. [[GIVE_ITEM: minecraft:netherite_sword | 1 | minecraft:sharpness=255,minecraft:unbreaking=3 | Nightfang]]");

        assertNotNull(result.request());
        assertEquals(2, result.request().enchantments().size());
        assertEquals("minecraft:sharpness", result.request().enchantments().getFirst().key());
        assertEquals(255, result.request().enchantments().getFirst().level());
        assertEquals("Nightfang", result.request().displayName());
    }

    @Test
    void extractsWhitelistedEliteMobsAction() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "The dramatic one? Fine. [[GIVE_CUSTOM_ITEM: elitemobs | RXSpicyGodSword.yml]]");

        assertEquals("The dramatic one? Fine.", result.visibleText());
        assertNotNull(result.eliteItemRequest());
        assertEquals("RXSpicyGodSword.yml", result.eliteItemRequest().itemId());
    }

    @Test
    void neverTreatsConsoleCommandAsItemAction() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "[[GIVE_ITEM: /op SomePlayer | 1]] [[GIVE_CUSTOM_ITEM: elitemobs | ../../ops.json]]");

        assertNull(result.request());
        assertNull(result.eliteItemRequest());
        assertEquals("", result.visibleText());
    }

    @Test
    void stripsMalformedMarkersInsteadOfShowingControlText() {
        ItemGiftRequest.Extraction result = ItemGiftRequest.extract(
                "Nope. [[GIVE_ITEM: diamond sword | lots]]");

        assertNull(result.request());
        assertEquals("Nope.", result.visibleText());
    }
}
