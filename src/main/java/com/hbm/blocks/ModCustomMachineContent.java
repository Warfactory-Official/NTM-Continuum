package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.inventory.ModCustomMachineMenus;
import com.hbm.inventory.recipes.CustomMachineCraftingRecipe;
import com.hbm.items.ModCustomMachineItems;
import com.hbm.tileentity.ModCustomMachineBlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * One bootstrap hook for the custom machine system: the controller and its item per configured machine, the
 * casing, sheet, engine, tank, circuit and port blocks, the flux and heat receivers, the positioning anchor, the
 * structure output wand, the controller's crafting recipe, and {@code hbmConfig/hbmCustomMachines.json}. 1.7 read
 * that file in {@code PostLoad}; it is read here so the client's creative tab and item names see the same list the
 * server does, and the blocks and ingredients it names resolve on first use.
 */
public final class ModCustomMachineContent {

    private ModCustomMachineContent() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);
    public static final RegistryObject<RecipeSerializer<CustomMachineCraftingRecipe>> CUSTOM_MACHINE_RECIPE =
            SERIALIZERS.register("custom_machine", () -> CustomMachineCraftingRecipe.SERIALIZER);

    public static void init() {
        ModCustomMachineBlocks.init();
        ModCustomMachineItems.init();
        ModCustomMachineBlockEntities.init();
        ModCustomMachineMenus.init();
        CustomMachineDeclarations.declare();
        CustomMachineConfigJSON.initialize();
    }
}
