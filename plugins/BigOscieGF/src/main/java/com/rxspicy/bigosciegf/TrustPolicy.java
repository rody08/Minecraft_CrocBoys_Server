package com.rxspicy.bigosciegf;

/** Central trust thresholds for Nyx's increasingly powerful item tools. */
record TrustPolicy(int enchantedItemsAt, int unsafeItemsAt, int customItemsAt, int maximum) {
    TrustPolicy {
        if (enchantedItemsAt < 0 || unsafeItemsAt < enchantedItemsAt ||
                customItemsAt < unsafeItemsAt || maximum < customItemsAt) {
            throw new IllegalArgumentException("Trust thresholds must be ordered and non-negative");
        }
    }

    int clamp(int score) {
        return Math.max(0, Math.min(maximum, score));
    }

    boolean canEnchant(int score) {
        return clamp(score) >= enchantedItemsAt;
    }

    boolean canUseUnsafeItems(int score) {
        return clamp(score) >= unsafeItemsAt;
    }

    boolean canUseCustomItems(int score) {
        return clamp(score) >= customItemsAt;
    }

    String tier(int score) {
        int value = clamp(score);
        if (value >= customItemsAt) return "everything";
        if (value >= unsafeItemsAt) return "unsafe + enchanted";
        return "food only";
    }
}
