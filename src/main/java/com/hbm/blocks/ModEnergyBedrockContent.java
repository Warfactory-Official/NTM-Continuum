package com.hbm.blocks;

import com.hbm.items.ModBedrockOreItems;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;

/**
 * One bootstrap hook for the energy extras and the bedrock ore grade chain: the boxed and coated red copper cable,
 * the charging station and the 25 processing grades of {@code bedrock_ore} beyond the
 * slopper's BASE. The grade recipes are fed into each machine's table by {@code BedrockOreChainRecipes}.
 */
public final class ModEnergyBedrockContent {

    private ModEnergyBedrockContent() {}

    public static void init() {
        ModEnergyBedrockBlocks.init();
        ModBedrockOreItems.init();
        ModEnergyBedrockBlockEntities.init();
        EnergyBedrockDeclarations.declare();
    }
}
