package com.hbm.client.render;

import java.util.List;
import java.util.Set;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Cross-loader registration of HBM's block tint sources (same Sink pattern as
 * {@link HbmEntityRenderers}). 26.1 replaced the old per-quad {@code BlockColor} callback with
 * {@link BlockTintSource} (a state-keyed provider registered on {@code BlockColors}); NeoForge hooks
 * it via {@code RegisterColorHandlersEvent.BlockTintSources}, Fabric by registering directly on
 * {@code Minecraft.getBlockColors()} once the client has started.
 *
 * <p>Currently only the sellafite family: a grayscale tint darkening with the {@code SHADE} state —
 * {@code brightness = 1 − shade/15}, the old {@code BlockSellafieldSlaked} {@code IBlockColor}
 * (divisor stays 15 although only shades 0..9 are used, so shade 9 is dark grey, not black).
 * Client-only.
 */
public final class HbmBlockColors {

    private HbmBlockColors() {}

    /** Loader-provided registration callback. */
    public interface Sink {
        void register(List<BlockTintSource> sources, Block... blocks);
    }

    /** Shade-keyed grayscale: declares {@code SHADE} as its relevant property so the tint cache keys on it. */
    private static final BlockTintSource SELLAFIELD_TINT = new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            int shade = state.getValue(BlockSellafieldSlaked.SHADE);
            return 0xFF000000 | java.awt.Color.HSBtoRGB(0F, 0F, 1F - shade / 15F);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of(BlockSellafieldSlaked.SHADE);
        }
    };

    public static void registerAll(Sink sink) {
        sink.register(List.of(SELLAFIELD_TINT),
                ModFalloutBlocks.sellafield_slaked.get(),
                ModFalloutBlocks.sellafield_bedrock.get(),
                ModFalloutBlocks.ore_sellafield_diamond.get(),
                ModFalloutBlocks.ore_sellafield_emerald.get(),
                ModFalloutBlocks.ore_sellafield_radgem.get(),
                ModFalloutBlocks.ore_sellafield_schrabidium.get(),
                ModFalloutBlocks.ore_sellafield_uranium_scorched.get());
    }
}
