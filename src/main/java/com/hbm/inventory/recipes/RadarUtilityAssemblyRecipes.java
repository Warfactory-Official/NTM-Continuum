package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;

import static com.hbm.inventory.OreDictManager.*;

/**
 * The 1.7 {@code AssemblyMachineRecipes} and {@code SolderingRecipes} entries of the radar and utility machines,
 * kept with the feature. {@code ass.orbus} carries 1.7's expensive-mode list through {@code inputItemsEx}; its
 * Po-210 {@code battery_sc} is {@code battery_sc_po210} ({@link ModBatterySCItems}).
 */
public final class RadarUtilityAssemblyRecipes {

    private RadarUtilityAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        add(recipes, new GenericRecipe("ass.sealcontroller").setup(100, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.seal_controller.get(), 1))
                .inputItems(DURA.ingot(), STEEL.plateCast(), ANY_PLASTIC.ingot().count(4), MINGRADE.wireDense().count(4)));
        add(recipes, new GenericRecipe("ass.teleporter").setup(100, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.machine_teleporter.get(), 1))
                .inputItems(TI.plate().count(12), DURA.plate().count(12), GOLD.wire().count(32),
                        new ComparableStack(ModItems.entanglement_kit.get(), 1),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1)));
        add(recipes, new GenericRecipe("ass.radar").setup(300, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.machine_radar.get(), 1))
                .inputItems(STEEL.plate().count(12), ANY_RUBBER.ingot().count(12), new ComparableStack(ModItems.magnetron.get(), 5),
                        new ComparableStack(ModItems.motor.get(), 1),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 8),
                        new ComparableStack(ModItems.crt_display.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2), new ComparableStack(ModItems.crt_display.get(), 4)));
        add(recipes, new GenericRecipe("ass.radarlarge").setup(400, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.machine_radar_large.get(), 1))
                .inputItems(STEEL.plateWelded().count(6), ANY_RESISTANTALLOY.ingot().count(4), ANY_RUBBER.ingot().count(24),
                        new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor_desh.get(), 1),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4),
                        new ComparableStack(ModItems.crt_display.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), ANY_RESISTANTALLOY.ingot().count(8), ANY_RUBBER.ingot().count(24), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 6), new ComparableStack(ModItems.crt_display.get(), 4)));
        add(recipes, new GenericRecipe("ass.forcefield").setup(600, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.machine_forcefield.get(), 1))
                .inputItems(DURA.plate().count(8), new ComparableStack(ModItems.plate_desh.get(), 4),
                        new ComparableStack(ModItems.coil_gold_torus.get(), 6),
                        new ComparableStack(ModItems.coil_magnetized_tungsten.get(), 12),
                        new ComparableStack(ModItems.motor.get(), 1),
                        new ComparableStack(ModRadarUtilityItems.upgrade_radius.get(), 1),
                        new ComparableStack(ModRadarUtilityItems.upgrade_health.get(), 1),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4),
                        new ComparableStack(ModBlocks.machine_transformer.get().asItem(), 1)));
        // 1.7 AssemblyMachineRecipes:467-469
        add(recipes, new GenericRecipe("ass.orbus").setup(300, 100)
                .outputItems(new ItemStack(ModRadarUtilityBlocks.machine_orbus.get(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(8), BIGMT.plateCast().count(4),
                        BSCCO.wireDense().count(8), new ComparableStack(ModBatterySCItems.get(EnumBatterySC.PO210), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 8), BIGMT.plateCast().count(16),
                        BSCCO.wireDense().count(16), new ComparableStack(ModBatterySCItems.get(EnumBatterySC.PO210), 1)));
    }

    private static void add(GenericRecipes recipes, GenericRecipe recipe) {
        // curated recipes win; a later generator run that emits the same name is skipped here
        if (recipes.getRecipe(recipe.name) == null) recipes.registerGenerated(recipe);
    }

    /** 1.7 {@code SolderingRecipes}: the two forcefield upgrades, template + four dusts over chips and capacitors. */
    public static void registerSoldering() {
        solder(new ItemStack(ModRadarUtilityItems.upgrade_radius.get()),
                new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "dusts/glowstone")), 4));
        solder(new ItemStack(ModRadarUtilityItems.upgrade_health.get()), LI.dust().count(4));
    }

    private static void solder(ItemStack output, AStack dust) {
        AStack[] top = {new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 4),
                new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 4)};
        AStack[] board = {new ComparableStack(ModItems.upgrade_template.get()), dust};
        AStack[] sold = {};
        SolderingRecipes.recipes.add(new SolderingRecipes.SolderingRecipe(output, 200, 1_000, null, top, board, sold));
        Collections.addAll(SolderingRecipes.toppings, top);
        Collections.addAll(SolderingRecipes.pcb, board);
    }
}
