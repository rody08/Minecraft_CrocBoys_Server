package com.rxspicy.bigosciegf;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class NyxBuildService {
    private static final int WIDTH = 9;
    private static final int HEIGHT = 7;
    private static final int DEPTH = 11;

    private final NyxPlugin plugin;
    private final Map<UUID, BuildPlan> pending = new ConcurrentHashMap<>();

    NyxBuildService(NyxPlugin plugin) {
        this.plugin = plugin;
    }

    boolean hasPending(Player player) {
        BuildPlan plan = pending.get(player.getUniqueId());
        if (plan == null) return false;
        if (plan.expiresAt() >= System.currentTimeMillis()) return true;
        pending.remove(player.getUniqueId());
        return false;
    }

    String prepare(Player player, BuildRequest request, int trustScore) {
        if (!plugin.getConfig().getBoolean("ai.building.enabled", true)) return "My building tools are disabled right now.";
        if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null) return "I need WorldEdit before I can build safely.";
        String permission = plugin.getConfig().getString("ai.building.permission", "nyx.build");
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) return "You don't have permission to ask me to build here.";
        int requiredTrust = plugin.getConfig().getInt("ai.building.trust-required", 50);
        if (trustScore < requiredTrust) return "Building together takes full trust first.";
        if (!request.type().equals("house") || !request.size().equals("small") || !isStyle(request.style())) {
            return "I can currently draft a small oak, spruce, or dark oak house.";
        }
        int volume = WIDTH * HEIGHT * DEPTH;
        if (volume > plugin.getConfig().getInt("ai.building.max-blocks", 1200)) return "That plan exceeds my configured build limit.";

        Location target = targetFor(player);
        if (!isOpen(target)) return "I need a clear 9 by 11 space ahead of you before I can place that house.";
        try {
            Clipboard clipboard = createHouse(request.style());
            Path schematic = save(player.getUniqueId(), clipboard);
            long seconds = Math.max(15, plugin.getConfig().getLong("ai.building.confirmation-seconds", 120));
            pending.put(player.getUniqueId(), new BuildPlan(schematic, target, request.style(), System.currentTimeMillis() + seconds * 1000L));
            preview(target);
            return "I've drafted a small " + displayStyle(request.style()) + " house ahead of you. Say “Nyx, confirm build” within " + seconds + " seconds to paste it.";
        } catch (Exception e) {
            plugin.getLogger().warning("Could not prepare Nyx build: " + e.getMessage());
            return "My schematic table just misbehaved. I couldn't prepare that house.";
        }
    }

    String confirm(Player player) {
        BuildPlan plan = pending.remove(player.getUniqueId());
        if (plan == null || plan.expiresAt() < System.currentTimeMillis()) return "That build preview expired. Ask me to draft it again.";
        if (!isOpen(plan.target())) return "Something moved into the build area, so I cancelled the paste.";
        try {
            ClipboardFormat format = ClipboardFormats.findByPath(plan.schematic());
            if (format == null) return "I couldn't recognize my saved schematic.";
            Clipboard clipboard;
            try (InputStream input = Files.newInputStream(plan.schematic());
                 ClipboardReader reader = format.getReader(input)) {
                clipboard = reader.read();
            }
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(plan.target().getWorld()))) {
                Operation paste = new ClipboardHolder(clipboard).createPaste(editSession)
                        .to(BlockVector3.at(plan.target().getBlockX(), plan.target().getBlockY(), plan.target().getBlockZ()))
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .copyBiomes(false)
                        .build();
                Operations.complete(paste);
                WorldEdit.getInstance().getSessionManager().get(actor).remember(editSession);
            }
            return "House is built. If you hate it, your WorldEdit undo history has the paste.";
        } catch (Exception e) {
            plugin.getLogger().warning("Could not paste Nyx build: " + e.getMessage());
            return "The paste failed, so I left the world alone.";
        }
    }

    String cancel(Player player) {
        return pending.remove(player.getUniqueId()) == null ? "You don't have a pending build." : "Build cancelled.";
    }

    private Clipboard createHouse(String style) throws Exception {
        CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(WIDTH - 1, HEIGHT - 1, DEPTH - 1));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BlockVector3.ZERO);
        BlockState planks = block(style + "_planks");
        BlockState logs = block(style + "_log");
        BlockState roof = block(style.equals("dark_oak") ? "deepslate_tiles" : "cobblestone");
        BlockState glass = block("glass_pane");
        BlockState bricks = block("stone_bricks");

        for (int x = 0; x < WIDTH; x++) for (int z = 0; z < DEPTH; z++) {
            clipboard.setBlock(BlockVector3.at(x, 0, z), bricks);
            clipboard.setBlock(BlockVector3.at(x, 1, z), planks);
        }
        for (int y = 2; y <= 4; y++) for (int x = 0; x < WIDTH; x++) for (int z = 0; z < DEPTH; z++) {
            if (x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1) clipboard.setBlock(BlockVector3.at(x, y, z), planks);
        }
        for (int x : new int[]{0, WIDTH - 1}) for (int z : new int[]{0, DEPTH - 1}) {
            for (int y = 2; y <= 5; y++) clipboard.setBlock(BlockVector3.at(x, y, z), logs);
        }
        for (int x : new int[]{2, 6}) {
            clipboard.setBlock(BlockVector3.at(x, 3, 0), glass);
            clipboard.setBlock(BlockVector3.at(x, 3, DEPTH - 1), glass);
        }
        for (int z : new int[]{3, 7}) {
            clipboard.setBlock(BlockVector3.at(0, 3, z), glass);
            clipboard.setBlock(BlockVector3.at(WIDTH - 1, 3, z), glass);
        }
        clipboard.setBlock(BlockVector3.at(4, 2, 0), block("air"));
        clipboard.setBlock(BlockVector3.at(4, 3, 0), block("air"));
        for (int x = 0; x < WIDTH; x++) for (int z = 0; z < DEPTH; z++) clipboard.setBlock(BlockVector3.at(x, 5, z), roof);
        for (int z = 1; z < DEPTH - 1; z++) clipboard.setBlock(BlockVector3.at(WIDTH / 2, 6, z), roof);
        return clipboard;
    }

    private Path save(UUID playerId, Clipboard clipboard) throws IOException {
        Path directory = plugin.getDataFolder().toPath().resolve("schematics").resolve("generated");
        Files.createDirectories(directory);
        Path file = directory.resolve("house-" + playerId + "-" + System.currentTimeMillis() + ".schem");
        try (OutputStream output = Files.newOutputStream(file);
             ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(output)) {
            writer.write(clipboard);
        }
        return file;
    }

    private Location targetFor(Player player) {
        Location eye = player.getLocation();
        org.bukkit.util.Vector direction = eye.getDirection().setY(0);
        if (direction.lengthSquared() < 0.01) direction.setZ(1);
        direction.normalize().multiply(9);
        int centerX = eye.getBlockX() + direction.getBlockX();
        int centerZ = eye.getBlockZ() + direction.getBlockZ();
        World world = player.getWorld();
        int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
        return new Location(world, centerX - WIDTH / 2, y, centerZ - DEPTH / 2);
    }

    private boolean isOpen(Location origin) {
        World world = origin.getWorld();
        if (world == null || origin.getBlockY() < world.getMinHeight() || origin.getBlockY() + HEIGHT >= world.getMaxHeight()) return false;
        for (int x = 0; x < WIDTH; x++) for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) {
            Material material = world.getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z).getType();
            if (!material.isAir() && material != Material.SHORT_GRASS && material != Material.TALL_GRASS && material != Material.SNOW) return false;
        }
        return true;
    }

    private void preview(Location origin) {
        World world = origin.getWorld();
        if (world == null) return;
        for (int x : new int[]{0, WIDTH - 1}) for (int y : new int[]{0, HEIGHT - 1}) for (int z : new int[]{0, DEPTH - 1}) {
            world.spawnParticle(Particle.END_ROD, origin.clone().add(x + 0.5, y + 0.5, z + 0.5), 4, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static BlockState block(String id) {
        BlockType type = BlockTypes.get("minecraft:" + id);
        if (type == null) throw new IllegalArgumentException("Unknown block: " + id);
        return type.getDefaultState();
    }

    private static boolean isStyle(String style) {
        return style.equals("oak") || style.equals("spruce") || style.equals("dark_oak");
    }

    private static String displayStyle(String style) {
        return style.replace('_', ' ');
    }

    private record BuildPlan(Path schematic, Location target, String style, long expiresAt) {}
}
