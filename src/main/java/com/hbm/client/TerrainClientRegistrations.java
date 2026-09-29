package com.hbm.client;

import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.TerrainDeclarations;
import dev.golem.core.client.BlockTint;
import dev.golem.core.client.Tints;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Client hooks for the terrain family: the grass tint on tobacco and hemp (1.7 {@code colorMultiplier}
 * averaged the biome foliage colour over a 3x3; the state-keyed 26.1 tint cannot read the biome, so the
 * old item colour {@code ColorizerGrass.getGrassColor(0.5, 1.0)} is used in-world too).
 */
public final class TerrainClientRegistrations {

    private TerrainClientRegistrations() {}

    private static final BlockTint GRASS = new BlockTint() {
        @Override public int color(BlockState state) { return 0xFF000000 | TerrainDeclarations.GRASS_TINT; }
        @Override public Set<Property<?>> relevantProperties() { return Set.of(); }
    };

    /** 1.7 RenderReeds: {@code colorMultiplier} (white) times 0.75, on every segment (tint index 0 of the top model). */
    private static final BlockTint REEDS = new BlockTint() {
        @Override public int color(BlockState state) { return com.hbm.client.render.RenderReeds.COLOR; }
        @Override public Set<Property<?>> relevantProperties() { return Set.of(); }
    };

    @SuppressWarnings("unchecked")
    public static void registerBlockColors() {
        Tints.registerBlockTint(REEDS, ModTerrainBlocks.plant_reeds);
        List<Supplier<? extends Block>> tinted = new ArrayList<>();
        for (BlockNTMFlower.Type t : BlockNTMFlower.Type.values()) {
            if (t.tinted()) tinted.add(() -> ModTerrainBlocks.plant_flower(t));
        }
        for (BlockTallPlant.Type t : BlockTallPlant.Type.values()) {
            if (t.tinted()) tinted.add(() -> ModTerrainBlocks.plant_tall(t));
        }
        Tints.registerBlockTint(GRASS, tinted.toArray(new Supplier[0]));
    }
}
