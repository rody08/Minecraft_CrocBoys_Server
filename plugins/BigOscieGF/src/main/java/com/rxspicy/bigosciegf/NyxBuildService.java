package com.rxspicy.bigosciegf;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** World access stays on the server thread; inference and file writes use one worker. */
final class NyxBuildService {
    private final NyxPlugin plugin;
    private final Map<UUID, BuildPlan> pending = new ConcurrentHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private UUID generating;
    private volatile boolean generationCancelled;
    private long nextGeneration;
    private Paste active;

    NyxBuildService(NyxPlugin plugin) { this.plugin = plugin; }

    boolean hasPending(Player player) {
        BuildPlan plan = pending.get(player.getUniqueId());
        return plan != null && plan.expiresAt >= System.currentTimeMillis();
    }

    String status(Player player) {
        if (player.getUniqueId().equals(generating)) return "I'm still designing your schematic.";
        if (active != null && active.player.getUniqueId().equals(player.getUniqueId())) return "Your build is being placed.";
        BuildPlan plan = pending.get(player.getUniqueId());
        return hasPending(player) ? previewMessage(plan) : "You don't have a pending build.";
    }

    private BuildBlueprint.Limits limits() {
        return new BuildBlueprint.Limits(plugin.getConfig().getInt("ai.building.max-blocks", 1200),
                plugin.getConfig().getInt("ai.building.max-dimension", 32));
    }

    private String denial(Player player) {
        if (closed || !plugin.getConfig().getBoolean("ai.building.enabled", true)) return "My building tools are disabled.";
        String permission = plugin.getConfig().getString("ai.building.permission", "nyx.build");
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) return "You don't have building permission.";
        if (plugin.currentTrust(player) < plugin.getConfig().getInt("ai.building.trust-required", 50)) return "I need more trust before we build together.";
        return null;
    }

    String prepare(Player player, BuildRequest request, int ignoredSnapshotTrust) {
        String denied = denial(player);
        if (denied != null) return denied;
        if (!plugin.getConfig().getBoolean("ai.enabled", false)) return "Enable my AI before asking me to design a build.";
        if (generating != null || active != null) return "I'm working on another build. Try again when it's finished.";
        if (System.currentTimeMillis() < nextGeneration) return "Give my building tools a few seconds before another design.";
        pending.values().removeIf(plan -> plan.expiresAt < System.currentTimeMillis());
        if (pending.size() >= 16 && !pending.containsKey(player.getUniqueId())) return "My preview table is full. Try again shortly.";
        String description = request.description();
        if (description.isBlank() || description.length() > 400) return "Keep the build description between 1 and 400 characters.";
        OpenAiClient designer = new OpenAiClient(plugin);
        if (!designer.isConfigured()) return "My AI connection isn't configured.";
        UUID id = player.getUniqueId();
        generating = id;
        generationCancelled = false;
        nextGeneration = System.currentTimeMillis() + 10000;
        pending.remove(id);
        Location anchor = player.getLocation().clone();
        BuildBlueprint.Limits budget = limits();
        Path directory = plugin.getDataFolder().toPath().resolve("schematics/generated");
        worker.submit(() -> {
            try {
                BuildBlueprint blueprint = BuildBlueprint.generate(description, budget, prompt -> {
                    if (generationCancelled || closed) throw new java.util.concurrent.CancellationException();
                    return designer.generateBlueprint(prompt, budget);
                });
                sync(() -> compile(id, anchor, blueprint, directory));
            } catch (Exception e) { sync(() -> failGeneration(id, e)); }
        });
        return "I'm designing your build. I'll save a schematic and show its bounds before asking you to confirm.";
    }

    private void compile(UUID id, Location anchor, BuildBlueprint blueprint, Path directory) {
        if (generationCancelled || closed) { generating = null; return; }
        try {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector3.ZERO,
                    BlockVector3.at(blueprint.width() - 1, blueprint.height() - 1, blueprint.depth() - 1)));
            clipboard.setOrigin(BlockVector3.ZERO);
            Map<String, BlockState> palette = new HashMap<>();
            for (BuildBlueprint.Cell cell : blueprint.blocks()) {
                BlockState state = palette.computeIfAbsent(cell.material(), key -> {
                    var type = BlockTypes.get("minecraft:" + key);
                    if (type == null) throw new IllegalArgumentException("Material unavailable on this server");
                    return type.getDefaultState();
                });
                clipboard.setBlock(BlockVector3.at(cell.x(), cell.y(), cell.z()), state);
            }
            worker.submit(() -> {
                try {
                    Files.createDirectories(directory);
                    // Retain 100 designs; never touch imported schematics or legacy house files.
                    try (var files = Files.list(directory)) {
                        var old = files.filter(p -> p.getFileName().toString().matches("nyx-[a-f0-9-]{36}\\.schem"))
                                .sorted(Comparator.comparingLong(p -> p.toFile().lastModified())).toList();
                        for (int i = 0; i <= old.size() - 100; i++) Files.deleteIfExists(old.get(i));
                    }
                    Path file = directory.resolve("nyx-" + UUID.randomUUID() + ".schem");
                    try (var output = Files.newOutputStream(file);
                         var writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(output)) { writer.write(clipboard); }
                    sync(() -> ready(id, anchor, blueprint, clipboard, file));
                } catch (Exception e) { sync(() -> failGeneration(id, e)); }
            });
        } catch (Exception e) { failGeneration(id, e); }
    }

    private void ready(UUID id, Location anchor, BuildBlueprint blueprint, BlockArrayClipboard clipboard, Path file) {
        generating = null;
        if (generationCancelled || closed) return;
        Player player = Bukkit.getPlayer(id);
        if (player == null) return;
        String denied = denial(player);
        if (denied != null) { tell(player, denied); return; }
        var direction = anchor.getDirection().setY(0);
        if (direction.lengthSquared() < 0.01) direction.setZ(1);
        direction.normalize().multiply(Math.max(blueprint.width(), blueprint.depth()) / 2.0 + 4);
        Location target = new Location(anchor.getWorld(), anchor.getBlockX() + direction.getBlockX() - blueprint.width() / 2,
                anchor.getBlockY(), anchor.getBlockZ() + direction.getBlockZ() - blueprint.depth() / 2);
        long seconds = Math.max(15, Math.min(600, plugin.getConfig().getLong("ai.building.confirmation-seconds", 120)));
        BuildPlan plan = new BuildPlan(blueprint, clipboard, file, target, System.currentTimeMillis() + seconds * 1000);
        pending.put(id, plan);
        preview(player, plan);
        String problem = checkArea(player, plan);
        tell(player, previewMessage(plan) + (problem == null ? "" : " " + problem + " Use /nyx build move in clear space."));
    }

    private void failGeneration(UUID id, Exception error) {
        generating = null;
        plugin.getLogger().warning("Nyx schematic generation failed: " + error.getClass().getSimpleName());
        Player player = Bukkit.getPlayer(id);
        if (!generationCancelled && player != null) tell(player, "I couldn't finish a valid design within my limits. Try a smaller or simpler description.");
    }

    String move(Player player) {
        String denied = denial(player);
        if (denied != null) return denied;
        BuildPlan old = pending.get(player.getUniqueId());
        if (!hasPending(player)) return "Ask me to draft a build first.";
        BuildPlan plan = new BuildPlan(old.blueprint, old.clipboard, old.file, player.getLocation().getBlock().getLocation(), old.expiresAt);
        pending.put(player.getUniqueId(), plan);
        preview(player, plan);
        String problem = checkArea(player, plan);
        return previewMessage(plan) + (problem == null ? " Step outside the bounds before confirming." : " " + problem);
    }

    String confirm(Player player) {
        String denied = denial(player);
        if (denied != null) return denied;
        if (active != null || generating != null) return "Wait for the current build job to finish.";
        BuildPlan plan = pending.get(player.getUniqueId());
        if (!hasPending(player)) return "That preview expired. Ask me to draft it again.";
        String problem = checkArea(player, plan);
        if (problem != null) return problem;
        try {
            var actor = BukkitAdapter.adapt(player);
            EditSession edit = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(plan.target.getWorld())).actor(actor).maxBlocks(limits().volume()).build();
            edit.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.defaults()
                    .with(com.sk89q.worldedit.util.SideEffect.NEIGHBORS, com.sk89q.worldedit.util.SideEffect.State.OFF));
            edit.disableBuffering();
            pending.remove(player.getUniqueId());
            active = new Paste(player, plan, edit);
            active.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickPaste, 1, 1);
            return "Placing " + plan.blueprint.blocks().size() + " blocks in small batches. I'll tell you when it's finished.";
        } catch (Exception e) { return "I couldn't start the WorldEdit paste."; }
    }

    private void tickPaste() {
        Paste paste = active;
        if (paste == null) return;
        try {
            String denied = denial(paste.player);
            if (!paste.player.isOnline() || !paste.player.getWorld().equals(paste.plan.target.getWorld()) || denied != null)
                throw new IllegalStateException("Builder unavailable");
            int count = Math.max(1, Math.min(200, plugin.getConfig().getInt("ai.building.blocks-per-tick", 100)));
            long deadline = System.nanoTime() + 2_000_000L;
            for (int i = 0; i < count && paste.index < paste.plan.blueprint.blocks().size(); i++) {
                BuildBlueprint.Cell cell = paste.plan.blueprint.blocks().get(paste.index);
                Location at = paste.plan.target.clone().add(cell.x(), cell.y(), cell.z());
                if (!available(paste.player, at) || occupiedByEntity(at)) throw new IllegalStateException("Build area changed");
                paste.edit.setBlock(BlockVector3.at(at.getBlockX(), at.getBlockY(), at.getBlockZ()),
                        paste.plan.clipboard.getBlock(BlockVector3.at(cell.x(), cell.y(), cell.z())));
                paste.index++;
                if (System.nanoTime() >= deadline) break;
            }
            if (paste.index == paste.plan.blueprint.blocks().size()) finishPaste("Build finished. Use WorldEdit //undo to undo it.");
        } catch (Exception e) { finishPaste("The build stopped. Some blocks may have been placed; use WorldEdit //undo to remove them."); }
    }

    private void finishPaste(String message) {
        Paste paste = active;
        active = null;
        if (paste == null) return;
        if (paste.task != null) paste.task.cancel();
        try { paste.edit.close(); }
        finally {
            WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(paste.player)).remember(paste.edit);
            tell(paste.player, message);
        }
    }

    String cancel(Player player) {
        if (player.getUniqueId().equals(generating)) { generationCancelled = true; return "Design cancelled."; }
        if (active != null && active.player.getUniqueId().equals(player.getUniqueId())) {
            finishPaste("Placement cancelled. Use WorldEdit //undo to remove the blocks already placed.");
            return "Build cancelled.";
        }
        return pending.remove(player.getUniqueId()) == null ? "You don't have a pending build." : "Build cancelled.";
    }

    private String checkArea(Player player, BuildPlan plan) {
        BuildBlueprint b = plan.blueprint;
        BuildBlueprint.Limits limit = limits();
        if ((long) b.width() * b.height() * b.depth() > limit.volume()
                || Math.max(b.width(), Math.max(b.height(), b.depth())) > limit.axis()) return "That design exceeds the current build limits.";
        if (!player.getWorld().equals(plan.target.getWorld())) return "Return to the preview's world or use /nyx build move.";
        for (int x = 0; x < b.width(); x++) for (int z = 0; z < b.depth(); z++) for (int y = 0; y < b.height(); y++) {
            if (!available(player, plan.target.clone().add(x, y, z))) return "The whole build area must be clear, loaded, inside the world border, and permitted.";
        }
        var box = new org.bukkit.util.BoundingBox(plan.target.getX(), plan.target.getY(), plan.target.getZ(),
                plan.target.getX() + b.width(), plan.target.getY() + b.height(), plan.target.getZ() + b.depth());
        if (!player.getWorld().getNearbyEntities(box).isEmpty()) return "Move players and entities outside the preview before confirming.";
        return null;
    }

    static boolean available(Player player, Location at) {
        World world = at.getWorld();
        if (world == null || at.getBlockY() < world.getMinHeight() || at.getBlockY() >= world.getMaxHeight()
                || !world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)
                || !world.getWorldBorder().isInside(at) || !world.getWorldBorder().isInside(at.clone().add(0.999, 0, 0.999))) return false;
        Material type = world.getBlockAt(at.getBlockX(), at.getBlockY(), at.getBlockZ()).getType();
        if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR
                && type != Material.SHORT_GRASS && type != Material.TALL_GRASS && type != Material.SNOW) return false;
        return BuildProtection.canBuild(player, at);
    }

    private boolean occupiedByEntity(Location at) {
        return !at.getWorld().getNearbyEntities(new org.bukkit.util.BoundingBox(at.getX(), at.getY(), at.getZ(),
                at.getX() + 1, at.getY() + 1, at.getZ() + 1)).isEmpty();
    }

    private void preview(Player player, BuildPlan plan) {
        for (int x : new int[]{0, plan.blueprint.width()}) for (int y : new int[]{0, plan.blueprint.height()}) for (int z : new int[]{0, plan.blueprint.depth()})
            player.spawnParticle(Particle.END_ROD, plan.target.clone().add(x, y, z), 5, 0.1, 0.1, 0.1, 0);
    }

    private String previewMessage(BuildPlan plan) {
        return "Schematic ready: " + plan.blueprint.width() + "x" + plan.blueprint.height() + "x" + plan.blueprint.depth()
                + " (" + plan.blueprint.blocks().size() + " blocks) at " + plan.target.getWorld().getName() + " "
                + plan.target.getBlockX() + " " + plan.target.getBlockY() + " " + plan.target.getBlockZ()
                + ". Say 'Nyx, confirm build' within " + Math.max(0, (plan.expiresAt - System.currentTimeMillis()) / 1000)
                + "s. /nyx build move relocates it to your feet.";
    }

    private void sync(Runnable action) {
        if (!closed) Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) action.run(); });
    }

    private void tell(Player player, String text) { player.sendMessage("§dNyx: §f" + text); }

    void close() {
        closed = true;
        worker.shutdownNow();
        pending.clear();
        finishPaste("Building stopped. Any partial paste is in your WorldEdit undo history for this session.");
    }

    private record BuildPlan(BuildBlueprint blueprint, BlockArrayClipboard clipboard, Path file, Location target, long expiresAt) {}
    private static final class Paste {
        final Player player;
        final BuildPlan plan;
        final EditSession edit;
        BukkitTask task;
        int index;
        Paste(Player player, BuildPlan plan, EditSession edit) { this.player = player; this.plan = plan; this.edit = edit; }
    }
}
