package com.hbm.handler.jei;

import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModJetpackDependencyItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.machine.ItemRBMKPellet;
import dev.golem.core.viewer.Subtypes;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

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
        reg.components(ModMachineItems.blueprints.get(),
                ModDataComponents.BLUEPRINT_POOL);
        reg.components(ModFluidItems.fluid_tank.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        reg.components(ModFluidItems.fluid_barrel.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        reg.components(ModMachineItems.fluid_identifier.get(),
                ModDataComponents.FLUID);
        reg.components(ModUtilityToolItems.fluid_identifier_multi.get(),
                ModDataComponents.FLUID_IDENTIFIER);
        reg.components(ModFluidItems.fluid_bucket.get(),
                ModDataComponents.FLUID);
        reg.components(ModEntityItems.disperser_canister.get(), ModDataComponents.FLUID);
        reg.components(ModEntityItems.glyphid_gland.get(), ModDataComponents.FLUID);
        reg.components(ModEntityItems.grenade_universal.get(), ModDataComponents.GRENADE);
        // 1.7 NEI matched by metadata alone: any amount, pressure or liquid flag finds the fluid's or material's recipes.
        reg.components(ModFluidItems.fluid_icon.get(), ModDataComponents.FLUID);
        reg.components(ModMachineItems.scraps.get(), ModDataComponents.MATERIAL);
        // The 1.7 metadata families are one item per type; only the items that keep one id
        // with a typed state still need a subtype interpreter.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ItemRBMKPellet) {
                reg.components(item, ModDataComponents.RBMK_PELLET);
            }
        }
        for (var stirling : List.of(ModMachineBlocks.machine_stirling,
                ModMachineBlocks.machine_stirling_steel,
                ModMachineBlocks.machine_stirling_creative)) {
            reg.components(stirling.get().asItem(), ModDataComponents.HAS_COG);
        }
        reg.components(ModMachineBlocks.machine_sawmill.get().asItem(), ModDataComponents.HAS_BLADE);
        reg.components(ModJetpackDependencyItems.ingot_steel_dusted.get(), ModDataComponents.FORGE_COUNT);
        reg.components(ModCustomMachineBlocks.custom_machine_item.get(), ModDataComponents.CUSTOM_MACHINE);
    }
}
