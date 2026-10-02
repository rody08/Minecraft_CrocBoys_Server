package com.rxspicy.bigosciegf;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BuildTerrainTest {
    private final World world = mock(World.class);
    private final Player player = mock(Player.class);
    private final Block air = mock(Block.class), ground = mock(Block.class);
    private final BuildBlueprint design = new BuildBlueprint(3, 2, 3, List.of(
            new BuildBlueprint.Cell(0, 0, 0, "stone"), new BuildBlueprint.Cell(2, 0, 2, "stone")));

    private void setup() {
        when(player.getWorld()).thenReturn(world);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        var border = mock(WorldBorder.class);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any())).thenReturn(true);
        when(air.getType()).thenReturn(Material.AIR);
        when(ground.getType()).thenReturn(Material.GRASS_BLOCK);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of());
    }

    @Test void adjustsAboveAHillAndTheResultPassesConfirmationChecks() {
        setup();
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                (int)call.getArgument(1) <= ((int)call.getArgument(0) == 2 ? 65 : 63) ? ground : air);
        try (var protection = mockStatic(BuildProtection.class)) {
            protection.when(() -> BuildProtection.canBuild(any(), any())).thenReturn(true);
            Location original = new Location(world, 0, 64, 0);
            assertTrue(BuildSite.check(player, design, original).contains("grass block"));
            Location fitted = BuildSite.fit(player, design, original);
            assertEquals(66, fitted.getBlockY());
            assertEquals(64, original.getBlockY());
            assertNull(BuildSite.check(player, design, fitted));
            verify(ground, never()).setType(any());
        }
    }

    @Test void emptySchematicCellsDoNotBlockPlacementAndNewObstructionsAreDetected() {
        setup();
        when(world.getBlockAt(1, 64, 1)).thenReturn(ground);
        try (var protection = mockStatic(BuildProtection.class)) {
            protection.when(() -> BuildProtection.canBuild(any(), any())).thenReturn(true);
            Location target = new Location(world, 0, 64, 0);
            assertNull(BuildSite.check(player, design, target));
            when(world.getBlockAt(2, 64, 2)).thenReturn(ground);
            assertTrue(BuildSite.check(player, design, target).contains("2, 64, 2"));
        }
    }

    @Test void neverFitsThroughProtectionOrLoadsMissingChunks() {
        setup();
        try (var protection = mockStatic(BuildProtection.class)) {
            protection.when(() -> BuildProtection.canBuild(any(), any())).thenReturn(false);
            Location original = new Location(world, 0, 64, 0);
            assertEquals(original, BuildSite.fit(player, design, original));
            assertTrue(BuildSite.check(player, design, original).contains("Protection"));
            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
            clearInvocations(world);
            assertEquals(original, BuildSite.fit(player, design, original));
            assertTrue(BuildSite.check(player, design, original).contains("isn't loaded"));
            verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        }
    }

    @Test void asksEntitiesToMoveInsteadOfFloatingTheBuildAboveThem() {
        setup();
        when(world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of(player));
        try (var protection = mockStatic(BuildProtection.class)) {
            protection.when(() -> BuildProtection.canBuild(any(), any())).thenReturn(true);
            Location original = new Location(world, 0, 64, 0);
            assertEquals(original, BuildSite.fit(player, design, original));
            assertTrue(BuildSite.check(player, design, original).contains("Step clear"));
        }
    }
}
