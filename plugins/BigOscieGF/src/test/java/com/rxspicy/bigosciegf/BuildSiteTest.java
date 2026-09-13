package com.rxspicy.bigosciegf;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BuildSiteTest {
    @Test void checksBottomLayerAndPreservesExistingBlocks() {
        World world = world();
        Block block = mock(Block.class);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getType()).thenReturn(Material.CHEST);
        assertFalse(NyxBuildService.available(mock(Player.class), new Location(world, 0, 64, 0)));
        verify(world).getBlockAt(0, 64, 0);
    }

    @Test void neverReadsBlocksInUnloadedChunksOrOutsideWorldBounds() {
        World world = world();
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        assertFalse(NyxBuildService.available(mock(Player.class), new Location(world, 0, 64, 0)));
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        assertFalse(NyxBuildService.available(mock(Player.class), new Location(world, 0, 320, 0)));
        assertFalse(NyxBuildService.available(mock(Player.class), new Location(world, 0, -65, 0)));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    @Test void requiresRegionPermissionAndAllowsTheTopBuildableLayer() {
        World world = world();
        Block block = mock(Block.class);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getType()).thenReturn(Material.AIR);
        Player player = mock(Player.class);
        Location target = new Location(world, 0, 319, 0);
        try (var protection = mockStatic(BuildProtection.class)) {
            protection.when(() -> BuildProtection.canBuild(player, target)).thenReturn(false);
            assertFalse(NyxBuildService.available(player, target));
            protection.when(() -> BuildProtection.canBuild(player, target)).thenReturn(true);
            assertTrue(NyxBuildService.available(player, target));
        }
    }

    @Test void rejectsBlocksStraddlingWorldBorder() {
        World world = world();
        when(world.getWorldBorder().isInside(any())).thenAnswer(call -> ((Location) call.getArgument(0)).getX() <= 0.5);
        assertFalse(NyxBuildService.available(mock(Player.class), new Location(world, 0, 64, 0)));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    private World world() {
        World world = mock(World.class);
        WorldBorder border = mock(WorldBorder.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any())).thenReturn(true);
        return world;
    }
}
