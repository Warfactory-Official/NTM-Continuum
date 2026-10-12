package com.hbm.handler.jei;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import dev.golem.core.data.ModelData;
import dev.golem.core.viewer.Subtypes;

/**
 * The HBM items whose stacks a recipe viewer must keep apart (JEI subtypes, REI item comparators), declared once for
 * both. Server-safe: REI's common plugin, which also runs on a dedicated server, registers these.
 */
public final class HbmViewerSubtypes {

    private HbmViewerSubtypes() {}

    /**
     * Keep component-backed fluid containers, droplets, scraps, blueprints, and waste variants distinct.
     *
     * <p>The enum-backed families are swept from the registry rather than listed: every one of them
     * selects its variant with the same component, and a family JEI is not told about collapses to a
     * single ingredient, which is how the HIMARS pods and the artillery shells went missing.
     */
    public static void register(Subtypes reg) {
        reg.components(com.hbm.items.ModMachineItems.blueprints.get(),
                ModDataComponents.BLUEPRINT_POOL);
        reg.components(ModFluidItems.fluid_tank.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        reg.components(ModFluidItems.fluid_barrel.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        reg.components(com.hbm.items.ModMachineItems.fluid_identifier.get(),
                ModDataComponents.FLUID);
        reg.components(ModFluidItems.fluid_bucket.get(),
                ModDataComponents.FLUID);
        reg.components(ModFluidItems.fluid_icon.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT, ModDataComponents.FLUID_PRESSURE);
        reg.interpreter(com.hbm.items.ModMachineItems.scraps.get(), stack ->
                ModDataComponents.MATERIAL.get(stack).orElse("") + "|"
                        + ModDataComponents.MAT_AMOUNT.get(stack).map(String::valueOf).orElse("") + "|"
                        + ModelData.flag(stack) + "|" + ModelData.color(stack, -1));
        // The 1.7 metadata families are one item per type (History §19es-itemvariant); only the items that keep one id
        // with a typed state still need a subtype interpreter.
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.hbm.items.machine.ItemRBMKPellet) {
                reg.components(item, ModDataComponents.RBMK_PELLET);
            }
        }
        for (var stirling : java.util.List.of(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling,
                com.hbm.blocks.machine.ModMachineBlocks.machine_stirling_steel,
                com.hbm.blocks.machine.ModMachineBlocks.machine_stirling_creative)) {
            reg.components(stirling.get().asItem(), ModDataComponents.HAS_COG);
        }
        reg.components(com.hbm.blocks.machine.ModMachineBlocks.machine_sawmill.get().asItem(), ModDataComponents.HAS_BLADE);
        reg.components(com.hbm.items.ModJetpackDependencyItems.ingot_steel_dusted.get(), ModDataComponents.FORGE_COUNT);
        reg.components(com.hbm.blocks.ModCustomMachineBlocks.custom_machine_item.get(), ModDataComponents.CUSTOM_MACHINE);
    }
}
