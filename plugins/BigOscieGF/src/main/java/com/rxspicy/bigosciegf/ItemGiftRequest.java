package com.rxspicy.bigosciegf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses Nyx's constrained gift actions without accepting arbitrary commands. */
record ItemGiftRequest(String materialKey, int amount, List<EnchantmentRequest> enchantments, String displayName) {
    private static final Pattern VANILLA_MARKER = Pattern.compile("(?is)\\[\\[GIVE_ITEM:\\s*(.*?)\\s*]]");
    private static final Pattern ELITE_MARKER = Pattern.compile(
            "(?is)\\[\\[GIVE_CUSTOM_ITEM:\\s*elitemobs\\s*\\|\\s*([a-z0-9_.-]{1,80})\\s*]]");
    private static final Pattern ANY_MARKER = Pattern.compile(
            "(?is)\\s*\\[\\[(?:GIVE_ITEM|GIVE_CUSTOM_ITEM):.*?]]");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.:-]+");
    private static final Pattern ENCHANTMENT = Pattern.compile("([a-z0-9_.:-]+)=(\\d{1,4})");

    ItemGiftRequest {
        enchantments = List.copyOf(enchantments == null ? List.of() : enchantments);
        displayName = displayName == null ? "" : displayName;
    }

    static Extraction extract(String response) {
        String source = response == null ? "" : response;
        ItemGiftRequest request = parseVanilla(source);
        EliteItemGiftRequest eliteItem = parseElite(source);
        String visible = ANY_MARKER.matcher(source).replaceAll("").replaceAll("\\s+([.!?,])", "$1").trim();
        return new Extraction(visible, request, eliteItem);
    }

    private static ItemGiftRequest parseVanilla(String source) {
        Matcher marker = VANILLA_MARKER.matcher(source);
        if (!marker.find()) return null;

        String[] parts = marker.group(1).split("\\s*\\|\\s*", -1);
        if (parts.length < 2 || parts.length > 4) return null;
        String material = parts[0].trim().toLowerCase(Locale.ROOT);
        if (!KEY.matcher(material).matches()) return null;

        int amount;
        try {
            amount = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException ignored) {
            return null;
        }

        List<EnchantmentRequest> enchantments = new ArrayList<>();
        if (parts.length >= 3 && !parts[2].isBlank() && !parts[2].equalsIgnoreCase("none")) {
            String[] requested = parts[2].split(",");
            if (requested.length > 8) return null;
            for (String raw : requested) {
                Matcher enchantment = ENCHANTMENT.matcher(raw.trim().toLowerCase(Locale.ROOT));
                if (!enchantment.matches()) return null;
                try {
                    enchantments.add(new EnchantmentRequest(
                            enchantment.group(1), Integer.parseInt(enchantment.group(2))));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        String displayName = parts.length == 4 ? sanitizeName(parts[3]) : "";
        if (displayName == null) return null;
        return new ItemGiftRequest(material, amount, enchantments, displayName);
    }

    private static EliteItemGiftRequest parseElite(String source) {
        Matcher marker = ELITE_MARKER.matcher(source);
        if (!marker.find()) return null;
        return new EliteItemGiftRequest(marker.group(1));
    }

    private static String sanitizeName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.equalsIgnoreCase("none")) return "";
        if (name.length() > 48 || name.contains("[") || name.contains("]")) return null;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) return null;
        }
        return name;
    }

    record Extraction(String visibleText, ItemGiftRequest request, EliteItemGiftRequest eliteItemRequest) {}
}

record EnchantmentRequest(String key, int level) {}

record EliteItemGiftRequest(String itemId) {}
