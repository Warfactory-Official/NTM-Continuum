package com.hbm.blocks;

import com.hbm.items.ModBedrockOreItems;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import com.hbm.tileentity.network.ConverterEnergySource;
import dev.golem.core.world.EnergyExposure;
import net.minecraft.world.level.block.Block;

/**
 * One bootstrap hook for the energy extras and the bedrock ore grade chain: the boxed and coated red copper cable,
 * the charging station, the HE/RF converters, and the 25 processing grades of {@code bedrock_ore} beyond the
 * slopper's BASE. The grade recipes are fed into each machine's table by {@code BedrockOreChainRecipes}.
 *
 * <p>The converters' RF side is published through Core's {@link EnergyExposure} (the platform energy capability on
 * NeoForge and Forge; Fabric has no standard energy API, so there the RF side is inert).
 */
public final class ModEnergyBedrockContent {

    private ModEnergyBedrockContent() {}

    public static void init() {
        ModEnergyBedrockBlocks.init();
        ModBedrockOreItems.init();
        ModEnergyBedrockBlockEntities.init();
        EnergyExposure.expose(ConverterEnergySource.INSTANCE, () -> new Block[]{
                ModEnergyBedrockBlocks.machine_converter_he_rf.get(), ModEnergyBedrockBlocks.machine_converter_rf_he.get()});
        EnergyBedrockDeclarations.declare();
    }
}
