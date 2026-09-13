package com.rxspicy.bigosciegf;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Isolate optional WorldGuard linkage; installed but unavailable protection fails closed. */
final class BuildProtection {
    static boolean canBuild(Player player, Location location) {
        var installed = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (installed == null) return true;
        if (!installed.isEnabled()) return false;
        try { return Query.canBuild(player, location); }
        catch (RuntimeException | LinkageError unavailable) { return false; }
    }

    private static final class Query {
        static boolean canBuild(Player player, Location location) {
            var actor = WorldGuardPlugin.inst().wrapPlayer(player);
            var guard = WorldGuard.getInstance();
            if (guard.getPlatform().getSessionManager().hasBypass(actor, BukkitAdapter.adapt(location.getWorld()))) return true;
            return guard.getPlatform().getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(location), actor);
        }
    }
}
