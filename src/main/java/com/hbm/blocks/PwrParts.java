package com.hbm.blocks;

import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What a block is to the PWR structure scan. {@code weight}: how many of its kind it counts as in the
 * controller's heatex/heatsink/channel/source tallies; ignored by the other kinds.
 */
public final class PwrParts {

    public enum Kind { FUEL, CONTROL, CHANNEL, HEATEX, HEATSINK, SOURCE, REFLECTOR, CASING, PORT }

    public record Part(Kind kind, int weight) {
        public boolean core() {
            return kind != Kind.REFLECTOR && kind != Kind.CASING && kind != Kind.PORT;
        }
    }

    private record Declared(Supplier<? extends Block> block, Part part) {}

    private static final List<Declared> DECLARED = new ArrayList<>();
    private static volatile @Nullable Map<Block, Part> resolved;

    private PwrParts() {}

    public static synchronized void register(Supplier<? extends Block> block, Kind kind, int weight) {
        if (weight < 1) throw new IllegalArgumentException("PWR part weight must be positive: " + weight);
        DECLARED.add(new Declared(block, new Part(kind, weight)));
        resolved = null;
    }

    public static @Nullable Part of(Block block) {
        Map<Block, Part> parts = resolved;
        if (parts == null) parts = resolve();
        return parts.get(block);
    }

    public static boolean is(Block block, Kind kind) {
        Part part = of(block);
        return part != null && part.kind() == kind;
    }

    private static synchronized Map<Block, Part> resolve() {
        if (resolved != null) return resolved;
        Map<Block, Part> parts = new IdentityHashMap<>();
        for (Declared declared : DECLARED) parts.put(declared.block().get(), declared.part());
        resolved = parts;
        return parts;
    }
}
