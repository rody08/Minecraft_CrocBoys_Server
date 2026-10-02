package com.rxspicy.bigosciegf;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/** Only the non-air cells will be changed. Never excavate terrain or load chunks. */
final class BuildSite {
    static String problem(Player player, Location at) {
        var world = at.getWorld();
        String where = at.getBlockX() + ", " + at.getBlockY() + ", " + at.getBlockZ();
        if (world == null) return "That world isn't available.";
        if (at.getBlockY() < world.getMinHeight() || at.getBlockY() >= world.getMaxHeight()) return "That spot crosses the world's height limit.";
        if (!world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) return "The chunk at " + where + " isn't loaded. Move closer, then try again.";
        if (!world.getWorldBorder().isInside(at) || !world.getWorldBorder().isInside(at.clone().add(.999, 0, .999))) return "That spot crosses the world border.";
        Material material = world.getBlockAt(at.getBlockX(), at.getBlockY(), at.getBlockZ()).getType();
        if (!replaceable(material)) return material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
                + " is in the way at " + where + ". I won't bury your build in it. Move the preview with /nyx build move.";
        return BuildProtection.canBuild(player, at) ? null : "Protection won't let me build at " + where + ". Pick a spot you can build in.";
    }

    static boolean replaceable(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR
                || material == Material.SHORT_GRASS || material == Material.TALL_GRASS || material == Material.SNOW;
    }

    static String check(Player player, BuildBlueprint blueprint, Location target) {
        if (!player.getWorld().equals(target.getWorld())) return "Come back to the preview's world, or move it with /nyx build move.";
        // Bounds still apply to the whole clipboard, even though empty cells do not obstruct a paste.
        var world = target.getWorld();
        if (target.getBlockY() < world.getMinHeight() || target.getBlockY() + blueprint.height() > world.getMaxHeight())
            return "That spot crosses the world's height limit.";
        if (!world.getWorldBorder().isInside(target)
                || !world.getWorldBorder().isInside(target.clone().add(blueprint.width() - .001, 0, blueprint.depth() - .001)))
            return "That spot crosses the world border.";
        for (var cell : blueprint.blocks()) {
            Location at = target.clone().add(cell.x(), cell.y(), cell.z());
            String issue = problem(player, at);
            if (issue != null) return issue;
            if (occupied(at)) return "Someone's in the way at " + at.getBlockX() + ", " + at.getBlockY() + ", " + at.getBlockZ()
                    + ". Step clear of the preview, then confirm again.";
        }
        return null;
    }

    static boolean occupied(Location at) {
        return !at.getWorld().getNearbyEntities(new BoundingBox(at.getX(), at.getY(), at.getZ(),
                at.getX() + 1, at.getY() + 1, at.getZ() + 1)).isEmpty();
    }

    static Location fit(Player player, BuildBlueprint blueprint, Location requested) {
        // Small, explicit upward adjustment; never search the entire column or generate distant terrain.
        int attempts = Math.max(1, Math.min(9, 65536 / Math.max(1, blueprint.blocks().size())));
        for (int dy = 0; dy < attempts; dy++) {
            Location candidate = requested.clone().add(0, dy, 0);
            String issue = check(player, blueprint, candidate);
            if (issue == null || issue.startsWith("Someone's")) return candidate;
        }
        return requested.clone();
    }
}
