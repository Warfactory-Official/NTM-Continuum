package com.hbm.registration;

import dev.golem.core.dejson.Drops;

/**
 * A block's loot table, stated with {@code .loot(...)} and emitted by Dejson's {@link Drops} under
 * {@code data/hbm/loot_table/blocks/<path>.json}. Each entry is the {@code Drops} shape of the same name.
 */
@FunctionalInterface
public interface Loot {

    void declare(String namespace, String block);

    /** The block drops itself (survives explosion). */
    static Loot self() {
        return Drops::self;
    }

    /** The block drops itself, explosion or not. */
    static Loot selfAlways() {
        return Drops::selfAlways;
    }

    /** An empty loot table. */
    static Loot nothing() {
        return Drops::nothing;
    }

    /** Drops one other item. */
    static Loot only(String drop) {
        return (namespace, block) -> Drops.only(namespace, block, drop);
    }

    static Loot slab() {
        return Drops::slab;
    }

    /** Self-drop that copies the named data components onto the item. */
    static Loot container(String... components) {
        return (namespace, block) -> Drops.container(namespace, block, components);
    }

    static Loot silkTouchOnly() {
        return Drops::silkTouchOnly;
    }

    /**
     * Silk touch gives the block, otherwise {@code min..max} of {@code drop} with no fortune. (The additive
     * {@code uniform_bonus_count} ore shape was 1.12.2's curve; 1.7 ores use {@link #oreMultiplied}.)
     */
    static Loot oreNoFortune(String drop, int min, int max) {
        return (namespace, block) -> Drops.oreNoFortune(namespace, block, drop, min, max);
    }

    /**
     * The 1.7 {@code BlockOre} shape: silk touch gives the block, otherwise {@code min..max} of {@code drop}
     * multiplied by fortune ({@code quantityDropped * (max(0, rand(fortune + 2) - 1) + 1)}, vanilla's
     * {@code ore_drops}). For 1.7 ores without {@code noFortune()}.
     */
    static Loot oreMultiplied(String drop, int min, int max) {
        return (namespace, block) -> Drops.oreMultiplied(namespace, block, drop, min, max);
    }

    /** {@link #oreMultiplied} for a block 1.7 could not silk-harvest ({@code ore_oil}). */
    static Loot dropsMultiplied(String drop, int min, int max) {
        return (namespace, block) -> Drops.dropsMultiplied(namespace, block, drop, min, max);
    }

    /** Silk touch gives the block; else {@code rare} with {@code chance}; else {@code min..max} {@code drop}. */
    static Loot oreOrRare(String drop, int min, int max, String rare, double chance) {
        return (namespace, block) -> Drops.oreOrRare(namespace, block, drop, min, max, rare, chance);
    }

    static Loot selfWhen(String property, String value) {
        return (namespace, block) -> Drops.selfWhen(namespace, block, property, value);
    }

    static Loot chance(String drop, double chance) {
        return (namespace, block) -> Drops.chance(namespace, block, drop, chance);
    }

    /** {@link Drops#stacks}: drop id, count, drop id, count, ... */
    static Loot stacks(Object... dropThenCount) {
        return (namespace, block) -> Drops.stacks(namespace, block, dropThenCount);
    }
}
