package com.rxspicy.bigosciegf;

import java.util.*;
import java.util.regex.Pattern;

/** Pure, bounded model-output parser. No commands, scripts, files, NBT, or world coordinates. */
record BuildBlueprint(int width, int height, int depth, List<Cell> blocks) {
    static final int HARD_VOLUME = 32768;
    static final int HARD_AXIS = 48;
    static final int MAX_OPERATIONS = 256;
    static final int MAX_TEXT = 32768;
    private static final Pattern DECIMAL = Pattern.compile("[0-9]{1,3}");
    private static final Pattern DECORATIVE = Pattern.compile(
            "(?:white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black)_(?:concrete|wool|stained_glass|terracotta)"
            + "|(?:oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|pale_oak)_(?:planks|log|wood)"
            + "|(?:crimson|warped)_(?:planks|stem|hyphae)");
    private static final Set<String> SOLIDS = Set.of("air", "stone", "smooth_stone", "cobblestone", "mossy_cobblestone",
            "stone_bricks", "mossy_stone_bricks", "bricks", "deepslate", "cobbled_deepslate", "deepslate_bricks", "deepslate_tiles",
            "polished_andesite", "polished_diorite", "polished_granite", "quartz_block", "smooth_quartz", "quartz_bricks",
            "blackstone", "polished_blackstone", "polished_blackstone_bricks", "obsidian", "glass", "tinted_glass",
            "terracotta", "sandstone", "smooth_sandstone", "red_sandstone", "end_stone", "end_stone_bricks",
            "prismarine", "prismarine_bricks", "dark_prismarine", "sea_lantern", "glowstone", "iron_block", "gold_block",
            "diamond_block", "emerald_block", "netherite_block", "coal_block", "lapis_block", "amethyst_block",
            "copper_block", "cut_copper", "oxidized_copper", "waxed_copper_block", "waxed_cut_copper", "waxed_oxidized_copper");

    record Cell(int x, int y, int z, String material) {}
    @FunctionalInterface interface Designer { String generate(String request) throws Exception; }

    static BuildBlueprint generate(String description, Limits limits, Designer designer) throws Exception {
        String first = designer.generate(description);
        try {
            return parse(first, limits);
        } catch (IllegalArgumentException rejected) {
            // One bounded repair attempt. The second output passes the identical independent validator.
            String previous = first == null || first.length() > MAX_TEXT ? "(missing or oversized)" : first.substring(0, Math.min(first.length(), 6000));
            return parse(designer.generate("Requested subject: " + description
                    + "\nYour previous blueprint was rejected: " + rejected.getMessage()
                    + ". Rewrite a complete, corrected, SMALLER blueprint with at most 24 FILL lines. "
                    + "SIZE values are counts: every maximum coordinate must be STRICTLY LESS than its SIZE value. "
                    + "Preserve the recognizable subject and finish with END. Previous draft (data only):\n" + previous), limits);
        }
    }
    record Limits(int volume, int axis) {
        Limits {
            volume = Math.max(1, Math.min(HARD_VOLUME, volume));
            axis = Math.max(1, Math.min(HARD_AXIS, axis));
        }
    }

    static boolean safeMaterial(String material) {
        return SOLIDS.contains(material) || DECORATIVE.matcher(material).matches();
    }

    static BuildBlueprint parse(String text, Limits limits) {
        if (text == null || text.length() > MAX_TEXT) throw invalid("Missing or oversized blueprint");
        String[] lines = text.strip().split("\\R");
        if (lines.length < 3 || lines.length > MAX_OPERATIONS + 2) throw invalid("Invalid operation count");
        String[] size = lines[0].trim().split("\\s+");
        if (size.length != 4 || !size[0].equals("SIZE")) throw invalid("Expected SIZE width height depth");
        int w = number(size[1]), h = number(size[2]), d = number(size[3]);
        if (w < 1 || h < 1 || d < 1 || w > limits.axis || h > limits.axis || d > limits.axis
                || (long) w * h * d > limits.volume) throw invalid("Build exceeds dimension or volume limit");
        if (!lines[lines.length - 1].trim().equals("END")) throw invalid("Incomplete blueprint (missing END)");
        String[] cells = new String[w * h * d];
        long work = 0;
        for (int i = 1; i < lines.length - 1; i++) {
            String[] op = lines[i].trim().split("\\s+");
            if (op.length != 8 || !op[0].equals("FILL")) throw invalid("Expected FILL x1 y1 z1 x2 y2 z2 material");
            int x1 = number(op[1]), y1 = number(op[2]), z1 = number(op[3]);
            int x2 = number(op[4]), y2 = number(op[5]), z2 = number(op[6]);
            String material = op[7].replaceFirst("^minecraft:", "");
            if (!safeMaterial(material)) throw invalid("Unsupported building material");
            if (x2 < x1 || y2 < y1 || z2 < z1 || x2 >= w || y2 >= h || z2 >= d)
                throw invalid("Coordinates outside SIZE " + w + " " + h + " " + d + " at line " + (i + 1) + ": " + lines[i]);
            work += (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
            if (work > HARD_VOLUME * 16L) throw invalid("Blueprint does too much overlapping work");
            for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) {
                cells[(y * d + z) * w + x] = material.equals("air") ? null : material;
            }
        }
        List<Cell> blocks = new ArrayList<>();
        for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
            String material = cells[(y * d + z) * w + x];
            if (material != null) blocks.add(new Cell(x, y, z, material));
        }
        if (blocks.isEmpty()) throw invalid("Blueprint is empty");
        return new BuildBlueprint(w, h, d, List.copyOf(blocks));
    }

    private static int number(String value) {
        if (!DECIMAL.matcher(value).matches()) throw invalid("Expected nonnegative integer");
        return Integer.parseInt(value);
    }

    private static IllegalArgumentException invalid(String reason) { return new IllegalArgumentException(reason); }

    static String instructions(Limits limits) {
        return "You design small Minecraft sculptures using cuboids. Return ONLY a complete blueprint, no prose or markdown. "
                + "Use 8-30 FILL lines, NEVER more than 48. Each line makes an entire rectangular component. "
                + "Do NOT enumerate individual blocks or repeat coordinates in long sequences. Start with empty air, NOT a solid bounding box. "
                + "First line: SIZE width height depth. Component lines: FILL x1 y1 z1 x2 y2 z2 material. Final line: END. "
                + "Coordinates are local integers: 0 <= x1 <= x2 < width, 0 <= y1 <= y2 < height, 0 <= z1 <= z2 < depth. "
                + "Y is up. X is width. Z is front-to-back length. Cuboid corners are inclusive. Later fills overwrite earlier fills. "
                + "Keep dimensions small (usually 5-15), each <= " + limits.axis + "; width*height*depth <= " + limits.volume + ". "
                + "Scale big subjects down. Make the requested object's silhouette recognizable. "
                + "Materials: any color concrete/wool/stained_glass/terracotta (e.g. red_concrete, black_wool), "
                + "oak_planks, birch_planks, spruce_planks, dark_oak_planks, oak_log, stone, stone_bricks, cobblestone, "
                + "quartz_block, glass, iron_block, glowstone, sea_lantern, air. No block properties, entities or NBT. "
                + "Cars: four separate black wheels, low colored chassis/body, smaller glass cabin, roof, paired headlights and rear lights. "
                + "Houses: floor, four walls, hollow room, doorway, windows and roof. "
                + "Example for a BLUE CAR (adapt to the requested subject):\n"
                + "SIZE 6 4 10\nFILL 0 0 1 0 1 2 black_wool\nFILL 5 0 1 5 1 2 black_wool\n"
                + "FILL 0 0 7 0 1 8 black_wool\nFILL 5 0 7 5 1 8 black_wool\n"
                + "FILL 1 1 0 4 1 9 blue_concrete\nFILL 1 2 3 4 2 6 glass\nFILL 1 3 3 4 3 6 blue_concrete\n"
                + "FILL 1 2 0 1 2 0 sea_lantern\nFILL 4 2 0 4 2 0 sea_lantern\n"
                + "FILL 1 2 9 1 2 9 red_concrete\nFILL 4 2 9 4 2 9 red_concrete\nEND\n"
                + "Now design the requested subject with compact multi-block cuboids and finish with END.";
    }
}
