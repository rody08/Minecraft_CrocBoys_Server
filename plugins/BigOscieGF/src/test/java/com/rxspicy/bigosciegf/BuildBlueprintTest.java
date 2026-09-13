package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildBlueprintTest {
    private final BuildBlueprint.Limits limits = new BuildBlueprint.Limits(4096, 32);

    @Test void retriesInvalidDesignOnceAndValidatesTheRepair() throws Exception {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var design = BuildBlueprint.generate("car", limits, prompt -> attempts.incrementAndGet() == 1
                ? "SIZE 1 1 1\nFILL 0 0 0 1 0 0 stone\nEND" : "SIZE 2 1 1\nFILL 0 0 0 1 0 0 stone\nEND");
        assertEquals(2, attempts.get());
        assertEquals(2, design.blocks().size());
        attempts.set(0);
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.generate("car", limits, prompt -> {
            attempts.incrementAndGet();
            return "SIZE 1 1 1\nFILL 0 0 0 0 0 0 command_block\nEND";
        }));
        assertEquals(2, attempts.get());
    }

    @Test void fillsOverlapAndCarveWithoutPlacingAir() {
        var design = BuildBlueprint.parse("SIZE 3 3 3\nFILL 0 0 0 2 2 2 stone\nFILL 1 1 1 1 1 1 air\nFILL 1 2 1 1 2 1 glass\nEND", limits);
        assertEquals(26, design.blocks().size());
        assertFalse(design.blocks().stream().anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1));
        assertTrue(design.blocks().contains(new BuildBlueprint.Cell(1, 2, 1, "glass")));
    }

    @Test void rejectsOversizedBoundsEvenWhenAlmostEmpty() {
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 32 32 32\nFILL 0 0 0 0 0 0 stone\nEND", limits));
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 33 1 1\nFILL 0 0 0 0 0 0 stone\nEND", limits));
    }

    @Test void configurationCannotDisableHardLimits() {
        assertEquals(new BuildBlueprint.Limits(32768, 48), new BuildBlueprint.Limits(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(new BuildBlueprint.Limits(1, 1), new BuildBlueprint.Limits(-1, -1));
    }

    @Test void rejectsTruncationProseAndExecutableInput() {
        for (String text : new String[]{"SIZE 1 1 1\nFILL 0 0 0 0 0 0 stone", "```\nSIZE 1 1 1\nEND\n```",
                "SIZE 1 1 1\nRUN //paste\nEND", "SIZE 1 1 1\nFILL 0 0 0 0 0 0 stone\nEND\nhello"}) {
            assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse(text, limits));
        }
    }

    @Test void rejectsPhysicsBlocksEntitiesNbtPropertiesAndInvalidCoordinates() {
        for (String material : new String[]{"command_block", "tnt", "sand", "water", "lava", "chest", "hopper", "spawner",
                "redstone_block", "oak_log[axis=x]", "stone{foo:1}", "../stone", "mod:stone"}) {
            assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 1 1 1\nFILL 0 0 0 0 0 0 " + material + "\nEND", limits));
        }
        for (String coords : new String[]{"-1 0 0 0 0 0", "0 0 0 1 0 0", "1 0 0 0 0 0", "1e9 0 0 0 0 0", "9999999999 0 0 0 0 0"}) {
            assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 1 1 1\nFILL " + coords + " stone\nEND", limits));
        }
    }

    @Test void boundsTextOperationsAndRepeatedWork() {
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("x".repeat(32769), limits));
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 1 1 1\n" + "FILL 0 0 0 0 0 0 stone\n".repeat(257) + "END", limits));
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 32 32 32\n" + "FILL 0 0 0 31 31 31 stone\n".repeat(17) + "END", new BuildBlueprint.Limits(32768, 48)));
        assertThrows(IllegalArgumentException.class, () -> BuildBlueprint.parse("SIZE 1 1 1\nFILL 0 0 0 0 0 0 air\nEND", limits));
    }

    @Test void acceptsCrimsonWarpedAndQuartzWithoutInventingLogNames() {
        for (String material : new String[]{"crimson_stem", "warped_hyphae", "quartz_block", "minecraft:red_concrete"})
            assertEquals(1, BuildBlueprint.parse("SIZE 1 1 1\nFILL 0 0 0 0 0 0 " + material + "\nEND", limits).blocks().size());
    }
}
