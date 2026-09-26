package com.hbm.hazard;

import com.hbm.blocks.generic.BlockHazard;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.Block;

import static com.hbm.hazard.HazardRegistry.*;
import com.hbm.blocks.ModTerrainBlocks;

/**
 * The 1.7 {@code HazardRegistry} lines for this family, in its two-step order: the block items get their
 * RADIATION hazard, then the {@link BlockHazard}-based blocks read it back ({@code getRawRadsFromBlock})
 * as their in-world emission — exactly what {@code ModHazardBlocks.initHazards} does for its own blocks.
 * Runs from {@code Hazards.init} after the item hazards.
 */
public final class TerrainHazards {

    private TerrainHazards() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        // HazardRegistry 215-217
        item(ModTerrainBlocks.block_waste, RADIATION, wst * block);
        item(ModTerrainBlocks.block_waste_painted, RADIATION, wst * block);
        item(ModTerrainBlocks.block_waste_vitrified, RADIATION, wstv * block);
        // HazardRegistry 222-227: sellafield metas 0..5
        float[] sellafite = {0.5F, 1F, 2.5F, 4F, 5F, 10F};
        for (int i = 0; i < sellafite.length; i++) item(ModTerrainBlocks.sellafield(i), RADIATION, sellafite[i]);
        // HazardRegistry 472, 475
        item(ModTerrainBlocks.block_yellowcake, RADIATION, yc * block * powder_mult);
        item(ModTerrainBlocks.block_fallout, RADIATION, yc * block * powder_mult);

        // the in-world emission of the BlockHazard-based blocks (getRawRadsFromBlock two-step)
        for (RegistryObject<? extends Block> ro : java.util.List.of(ModTerrainBlocks.block_waste,
                ModTerrainBlocks.block_waste_painted, ModTerrainBlocks.block_waste_vitrified)) {
            if (ro.get() instanceof BlockHazard hazard) hazard.addRadiation((float) HazardSystem.getRawRadsFromBlock(ro.get()));
        }
    }

    private static void item(RegistryObject<? extends Block> block, com.hbm.api.hazard.IHazardType type, float level) {
        item(block.get(), type, level);
    }

    private static void item(Block block, com.hbm.api.hazard.IHazardType type, float level) {
        HazardSystem.register(block.asItem(), new HazardData().addEntry(type, level));
    }
}
