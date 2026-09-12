package com.rxspicy.bigosciegf;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;

import java.util.Locale;

final class SkinService {
    private final NyxPlugin plugin;

    SkinService(NyxPlugin plugin) {
        this.plugin = plugin;
    }

    void migrateLegacyConfig() {
        if (plugin.getConfig().contains("npc.skin.mode")) return;
        String legacy = plugin.getConfig().getString("npc.skin-username", "");
        if (legacy != null && !legacy.isBlank()) {
            plugin.getConfig().set("npc.skin.mode", "username");
            plugin.getConfig().set("npc.skin.username", legacy.trim());
        } else {
            plugin.getConfig().set("npc.skin.mode", "none");
        }
        plugin.saveConfig();
    }

    void applyConfigured(NPC npc) {
        if (npc == null) return;
        migrateLegacyConfig();
        String mode = plugin.getConfig().getString("npc.skin.mode", "none");
        mode = mode == null ? "none" : mode.trim().toLowerCase(Locale.ROOT);
        SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);

        switch (mode) {
            case "username" -> {
                String username = plugin.getConfig().getString("npc.skin.username", "");
                if (username != null && !username.isBlank()) {
                    trait.setShouldUpdateSkins(true);
                    trait.setSkinName(username.trim(), true);
                }
            }
            case "texture", "mineskin" -> {
                String value = plugin.getConfig().getString("npc.skin.texture-value", "");
                String signature = plugin.getConfig().getString("npc.skin.texture-signature", "");
                String cacheKey = plugin.getConfig().getString("npc.skin.cache-key", "bigosciegf-custom");
                if (value != null && !value.isBlank() && signature != null && !signature.isBlank()) {
                    trait.setShouldUpdateSkins(false);
                    trait.setSkinPersistent(cacheKey == null || cacheKey.isBlank() ? "bigosciegf-custom" : cacheKey,
                            signature.trim(), value.trim());
                }
            }
            case "none" -> { /* leave current/default skin alone */ }
            default -> plugin.getLogger().warning("Unknown npc.skin.mode: " + mode);
        }
    }

    void setUsername(String username, NPC npc) {
        plugin.getConfig().set("npc.skin.mode", "username");
        plugin.getConfig().set("npc.skin.username", username);
        plugin.getConfig().set("npc.skin-username", username); // backwards compatibility
        plugin.saveConfig();
        if (npc != null) applyConfigured(npc);
    }

    void setTexture(String value, String signature, String cacheKey, String mode, NPC npc) {
        plugin.getConfig().set("npc.skin.mode", mode);
        plugin.getConfig().set("npc.skin.texture-value", value);
        plugin.getConfig().set("npc.skin.texture-signature", signature);
        plugin.getConfig().set("npc.skin.cache-key", cacheKey);
        plugin.saveConfig();
        if (npc != null) applyConfigured(npc);
    }

    void setMineSkin(MineSkinClient.SkinData data, NPC npc) {
        plugin.getConfig().set("npc.skin.mineskin-id", data.uuid());
        setTexture(data.value(), data.signature(), "mineskin-" + data.uuid(), "mineskin", npc);
    }

    void clear(NPC npc) {
        plugin.getConfig().set("npc.skin.mode", "none");
        plugin.getConfig().set("npc.skin.username", "");
        plugin.getConfig().set("npc.skin-username", "");
        plugin.getConfig().set("npc.skin.texture-value", "");
        plugin.getConfig().set("npc.skin.texture-signature", "");
        plugin.getConfig().set("npc.skin.mineskin-id", "");
        plugin.saveConfig();
        if (npc != null) {
            SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
            trait.clearTexture();
            trait.setShouldUpdateSkins(true);
        }
    }

    String status() {
        String mode = plugin.getConfig().getString("npc.skin.mode", "none");
        if (mode == null) mode = "none";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "username" -> "username:" + plugin.getConfig().getString("npc.skin.username", "");
            case "mineskin" -> "mineskin:" + plugin.getConfig().getString("npc.skin.mineskin-id", "cached");
            case "texture" -> "direct signed texture";
            default -> "none/default";
        };
    }
}
