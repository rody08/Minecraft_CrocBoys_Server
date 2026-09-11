package com.rxspicy.bigosciegf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class OwnerMemoryStore {
    private final BigOscieGFPlugin plugin;
    private final Path file;
    private final List<String> facts = new ArrayList<>();

    OwnerMemoryStore(BigOscieGFPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("owner-memory.txt");
        load();
    }

    synchronized void load() {
        facts.clear();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) return;
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String fact = clean(raw);
                if (!fact.isBlank()) facts.add(fact);
            }
            trimToLimit();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load owner-memory.txt: " + e.getMessage());
        }
    }

    synchronized boolean add(String rawFact) {
        String fact = clean(rawFact);
        if (fact.isBlank()) return false;
        String needle = fact.toLowerCase(Locale.ROOT);
        for (String existing : facts) {
            if (existing.toLowerCase(Locale.ROOT).equals(needle)) return false;
        }
        facts.add(fact);
        trimToLimit();
        save();
        return true;
    }

    synchronized boolean remove(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > facts.size()) return false;
        facts.remove(oneBasedIndex - 1);
        save();
        return true;
    }

    synchronized void clear() {
        facts.clear();
        save();
    }

    synchronized List<String> facts() {
        return Collections.unmodifiableList(new ArrayList<>(facts));
    }

    synchronized String promptBlock() {
        if (facts.isEmpty()) return "";
        StringBuilder out = new StringBuilder("Persistent facts about BigOscie:\n");
        for (String fact : facts) out.append("- ").append(fact).append('\n');
        return out.toString();
    }

    private void trimToLimit() {
        int max = Math.max(1, plugin.getConfig().getInt("ai.memory.max-owner-facts", 15));
        while (facts.size() > max) facts.remove(0);
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, facts, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save owner-memory.txt: " + e.getMessage());
        }
    }

    private static String clean(String input) {
        if (input == null) return "";
        String fact = input.replace('\n', ' ').replace('\r', ' ').trim().replaceAll("\\s+", " ");
        if (fact.length() > 140) fact = fact.substring(0, 140).trim();
        return fact;
    }
}
