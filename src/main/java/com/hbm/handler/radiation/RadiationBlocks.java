package com.hbm.handler.radiation;

import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sealing and emission assigned to blocks from outside their class. Block entries beat tag entries; a
 * seal entry beats {@link IRadResistantBlock}. Emission is per block, into its pocket, at
 * {@link com.hbm.blocks.generic.BlockHazard}'s mean rate at {@code randomTickSpeed 3}, capped at its
 * {@code rads}; it adds to whatever the block emits itself.
 */
public final class RadiationBlocks {

    private RadiationBlocks() {}

    private static final Map<Block, Boolean> SEALED = new ConcurrentHashMap<>();
    private static final Map<TagKey<Block>, Boolean> SEALED_TAGS = new ConcurrentHashMap<>();
    private static final Map<Block, Double> EMITTERS = new ConcurrentHashMap<>();
    private static final Map<TagKey<Block>, Double> EMITTER_TAGS = new ConcurrentHashMap<>();

    /** Per second, per rad: BlockHazard's {@code 0.1·rads} per random tick, {@code 3·20/4096} ticks/s. */
    static final double EMISSION_RATE = 0.1D * 3D * 20D / 4096D;

    private static volatile boolean sealing;
    private static volatile boolean emitting;
    private static volatile int epoch;

    public static @Nullable Boolean seal(Block block, @Nullable Boolean sealed) {
        return changed(sealed == null ? SEALED.remove(block) : SEALED.put(block, sealed));
    }

    public static @Nullable Boolean seal(TagKey<Block> tag, @Nullable Boolean sealed) {
        return changed(sealed == null ? SEALED_TAGS.remove(tag) : SEALED_TAGS.put(tag, sealed));
    }

    public static @Nullable Double emit(Block block, @Nullable Double rads) {
        return changed(rads == null ? EMITTERS.remove(block) : EMITTERS.put(block, rads));
    }

    public static @Nullable Double emit(TagKey<Block> tag, @Nullable Double rads) {
        return changed(rads == null ? EMITTER_TAGS.remove(tag) : EMITTER_TAGS.put(tag, rads));
    }

    private static <T> T changed(T previous) {
        sealing = !SEALED.isEmpty() || !SEALED_TAGS.isEmpty();
        emitting = !EMITTERS.isEmpty() || !EMITTER_TAGS.isEmpty();
        epoch++;
        return previous;
    }

    static int epoch() {
        return epoch;
    }

    static boolean active() {
        return sealing || emitting;
    }

    static boolean emitting() {
        return emitting;
    }

    public static boolean isRadResistant(BlockGetter level, BlockPos pos, BlockState state) {
        Boolean sealed = sealOverride(state);
        if (sealed != null) return sealed;
        return state.getBlock() instanceof IRadResistantBlock r && r.isRadResistant(level, pos);
    }

    static @Nullable Boolean sealOverride(BlockState state) {
        if (!sealing) return null;
        Boolean sealed = SEALED.get(state.getBlock());
        if (sealed != null) return sealed;
        for (Map.Entry<TagKey<Block>, Boolean> entry : SEALED_TAGS.entrySet()) {
            if (state.is(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    public static double emission(BlockState state) {
        if (!emitting) return 0D;
        Double rads = EMITTERS.get(state.getBlock());
        if (rads != null) return rads;
        for (Map.Entry<TagKey<Block>, Double> entry : EMITTER_TAGS.entrySet()) {
            if (state.is(entry.getKey())) return entry.getValue();
        }
        return 0D;
    }

    static boolean emits(BlockState state) {
        return emission(state) > 0D;
    }

    static boolean scripted(BlockState state) {
        return sealOverride(state) != null || emits(state);
    }
}
