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

    static Loot ore(String drop) {
        return (namespace, block) -> Drops.ore(namespace, block, drop);
    }

    static Loot ore(String drop, int min, int max) {
        return (namespace, block) -> Drops.ore(namespace, block, drop, min, max);
    }

    static Loot oreNoFortune(String drop, int min, int max) {
        return (namespace, block) -> Drops.oreNoFortune(namespace, block, drop, min, max);
    }

    static Loot oreDrops(String drop) {
        return (namespace, block) -> Drops.oreDrops(namespace, block, drop);
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
