package com.hbm.inventory.recipes.anvil;

import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGearItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.special.ItemAutogen;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.hbm.inventory.OreDictManager.U;
import static com.hbm.inventory.OreDictManager.ZR;

/**
 * 1.7 {@code AnvilRecipes.registerConstructionRecycling} (AnvilRecipes.java:629-998): the 1:many dismantling
 * recipes, in the 1.7 order. The FM radio and the filing cabinet of the same block are registered by
 * {@link com.hbm.inventory.recipes.DecoCrateMachineRecipes#anvil}. The RBMK fuel columns and the research reactor
 * are only dismantled while {@code enable528} is off, as in 1.7 (528 mode makes them from precision parts).
 *
 * <p>1.7 metadata variants that the port registers as blocks of their own are named here by their port ids:
 * {@code platemetal:0} = {@code platemetal_base}, {@code deco_toaster:0/1/2} = iron/steel/wooden toaster,
 * {@code deco_computer:0} = {@code deco_computer_ibm_300pl}, {@code rbmk_rod[_mod]} = {@code rbmk_fuel_rod[_moderated]},
 * {@code reactor_research} = {@code machine_reactor_small}; 1.7's wildcard {@code deco_crt} accepts every CRT, and
 * {@code gear_large:1} is {@code gear_large_steel}, and {@code machine_stirling[_steel]} metadata 1 (gear removed) is
 * {@code hbm:has_cog} false (History §19es-itemvariant).
 * Ids are resolved when the (lazy) anvil list is built and a missing one fails loudly.
 */
public final class AnvilRecyclingRecipes {

    private AnvilRecyclingRecipes() {}

    public static void register(List<AnvilConstructionRecipe> r) {

        r.add(new AnvilConstructionRecipe(new ComparableStack(ModIntermediateItems.chunk(EnumChunkType.RARE)), new AnvilOutput[] {
                out("fragment_boron", 1), out("fragment_boron", 1, 0.5F), out("fragment_lanthanium", 1, 0.1F),
                out("fragment_cobalt", 1), out("fragment_cobalt", 1, 0.5F), out("fragment_cerium", 1, 0.1F),
                out("fragment_neodymium", 1, 0.5F), out("fragment_niobium", 1, 0.5F)}).setTier(2));

        r.add(one(in("deco_titanium", 4), "ingot_titanium", 1, 1));
        r.add(one(in("deco_red_copper", 4), "ingot_red_copper", 1, 1));
        r.add(one(in("deco_tungsten", 4), "ingot_tungsten", 1, 1));
        r.add(one(in("deco_aluminium", 4), "ingot_aluminium", 1, 1));
        r.add(one(in("deco_steel", 4), "ingot_steel", 1, 1));
        r.add(one(in("deco_rusty_steel", 8), "ingot_steel", 1, 1));
        r.add(one(in("deco_lead", 4), "ingot_lead", 1, 1));
        r.add(one(in("deco_beryllium", 4), "ingot_beryllium", 1, 1));
        r.add(one(in("deco_asbestos", 4), "ingot_asbestos", 1, 1));
        r.add(one(in("platemetal_base", 4), "plate_iron", 1, 1));

        r.add(rec(in("heater_firebox", 1), 2, out("plate_steel", 8), out("ingot_copper", 6)));
        r.add(rec(in("heater_oven", 1), 2, out("ingot_firebrick", 16), out("ingot_copper", 8)));
        // 1.7 metadata 1 of machine_stirling[_steel] is the engine whose gear was removed (MachineStirling.getDrops);
        // the port carries that as hbm:has_cog false. gear_large metadata 1 is the item gear_large_steel.
        r.add(rec(stirling("machine_stirling", true), 2, out("plate_steel", 6), out("ingot_copper", 8), out("coil_copper", 4), gear(0)));
        r.add(rec(stirling("machine_stirling", false), 2, out("plate_steel", 6), out("ingot_copper", 8), out("coil_copper", 4)));
        r.add(rec(stirling("machine_stirling_steel", true), 2, out("plate_steel", 16), out("ingot_beryllium", 6), out("ingot_copper", 4), out("coil_gold", 8), gear(1)));
        r.add(rec(in("gear_large_steel", 1), 2, out("plate_steel", 8), out("ingot_titanium", 1)));
        r.add(rec(in("gear_large", 1), 2, out("plate_iron", 8), out("ingot_copper", 1)));
        r.add(rec(stirling("machine_stirling_steel", false), 2, out("plate_steel", 16), out("ingot_beryllium", 6), out("ingot_copper", 4), out("coil_gold", 8)));
        r.add(rec(in("machine_bat9000", 1), 3, autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_TCALLOY, 4, 1F), out("plate_steel", 16)));

        r.add(rec(in("deco_computer_ibm_300pl", 1), 2, out("crt_display", 1), out("scrap", 3),
                autogen(MaterialShapes.WIRE, Mats.MAT_COPPER, 4, 1F), circuit(EnumCircuitType.PCB, 2, 1F),
                circuit(EnumCircuitType.VACUUM_TUBE, 1, 0.5F), circuit(EnumCircuitType.CAPACITOR, 1, 0.75F),
                circuit(EnumCircuitType.CAPACITOR, 1, 0.5F), circuit(EnumCircuitType.ANALOG, 1, 0.1F)));
        r.add(rec(new AnyItemStack(item("deco_crt_clean"), item("deco_crt_broken"), item("deco_crt_blinking"), item("deco_crt_bsod")), 2,
                out("crt_display", 1), out("scrap", 2), autogen(MaterialShapes.WIRE, Mats.MAT_COPPER, 2, 1F),
                autogen(MaterialShapes.WIRE, Mats.MAT_GOLD, 2, 0.25F), circuit(EnumCircuitType.VACUUM_TUBE, 1, 0.25F)));
        r.add(rec(in("deco_toaster_iron", 1), 2, out("plate_iron", 3), out("scrap", 1), out("coil_tungsten", 1),
                new AnvilOutput(new ItemStack(Items.BREAD), 0.5F), out("fusion_core", 1, 0.01F)));
        r.add(rec(in("deco_toaster_steel", 1), 2, out("plate_steel", 3), out("scrap", 1), out("coil_tungsten", 2),
                new AnvilOutput(new ItemStack(Items.BREAD), 0.5F), out("battery_sc_ra226", 1, 0.1F), out("fusion_core", 1, 0.05F)));
        r.add(rec(in("deco_toaster_wood", 1), 2, out("powder_sawdust", 4), out("scrap", 1), out("coil_tungsten", 4),
                new AnvilOutput(new ItemStack(Items.BREAD), 0.5F), out("fusion_core", 1, 0.5F), out("fusion_core", 1, 0.5F),
                out("gem_alexandrite", 1, 0.25F), out("flame_pony", 1, 0.01F)));
        // radiorec: DecoCrateMachineRecipes.anvil
        r.add(rec(in("tape_recorder", 1), 2, out("ingot_steel", 1), out("ingot_tungsten", 1, 0.25F)));
        r.add(rec(in("pole_top", 1), 2, out("ingot_tungsten", 3), out("ingot_red_copper", 1), out("ingot_beryllium", 2), out("ingot_beryllium", 1, 0.5F)));
        r.add(rec(in("pole_satellite_receiver", 1), 2, out("ingot_steel", 3), out("ingot_steel", 2, 0.5F),
                circuit(EnumCircuitType.VACUUM_TUBE, 1, 0.5F), autogen(MaterialShapes.WIRE, Mats.MAT_RED_COPPER, 1, 1F)));
        // filing_cabinet: DecoCrateMachineRecipes.anvil

        r.add(rec(in("billet_ra226be", 3), 2, pileRod(EnumPileRod.RA226BE)));
        r.add(rec(in("billet_po210be", 3), 2, pileRod(EnumPileRod.PO210BE)));
        r.add(rec(ZR.billet().count(3), 2, pileRod(EnumPileRod.ZR)));
        r.add(rec(U.billet().count(3), 2, pileRod(EnumPileRod.NU)));

        // RBMK
        r.add(rec(in("rbmk_moderator", 1), 4, out("rbmk_blank", 1), out("block_graphite", 4)));
        r.add(rec(in("rbmk_absorber", 1), 4, out("rbmk_blank", 1), out("ingot_boron", 8)));
        r.add(rec(in("rbmk_reflector", 1), 4, out("rbmk_blank", 1), out("neutron_reflector", 8)));
        r.add(rec(in("rbmk_control", 1), 4, out("rbmk_absorber", 1), out("ingot_graphite", 2), out("motor", 2)));
        r.add(rec(in("rbmk_control_mod", 1), 4, out("rbmk_control", 1), out("block_graphite", 4), out("nugget_bismuth", 4)));
        r.add(rec(in("rbmk_control_auto", 1), 4, out("rbmk_control", 1), circuit(EnumCircuitType.ADVANCED, 1, 1F), out("crt_display", 1)));
        r.add(rec(in("rbmk_fuel_rod_reasim", 1), 4, out("rbmk_blank", 1), out("ingot_zirconium", 4), autogen(MaterialShapes.SHELL, Mats.MAT_STEEL, 2, 1F)));
        r.add(rec(in("rbmk_fuel_rod_reasim_moderated", 1), 4, out("rbmk_fuel_rod_reasim", 1), out("block_graphite", 4), out("ingot_tcalloy", 4)));
        r.add(rec(in("rbmk_outgasser", 1), 4, out("rbmk_blank", 1), out("steel_grate", 6), out("tank_steel", 1), new AnvilOutput(new ItemStack(Items.HOPPER))));
        r.add(rec(in("rbmk_storage", 1), 4, out("rbmk_blank", 1), out("crate_steel", 2)));

        if (!GeneralConfig.enable528) {
            r.add(rec(in("rbmk_fuel_rod", 1), 4, out("rbmk_blank", 1), autogen(MaterialShapes.SHELL, Mats.MAT_STEEL, 2, 1F)));
            r.add(rec(in("rbmk_fuel_rod_moderated", 1), 4, out("rbmk_fuel_rod", 1), out("block_graphite", 4), out("nugget_bismuth", 4)));
            r.add(rec(in("rbmk_boiler", 1), 4, out("rbmk_blank", 1), autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 6, 1F), autogen(MaterialShapes.SHELL, Mats.MAT_COPPER, 2, 1F)));
            r.add(rec(in("rbmk_cooler", 1), 4, out("rbmk_blank", 1), out("steel_grate", 4), out("plate_polymer", 4)));
            r.add(rec(in("machine_reactor_small", 1), 4, out("ingot_steel", 8), out("ingot_tcalloy", 4), out("motor_desh", 2),
                    out("ingot_boron", 5), out("plate_lead", 8), out("crt_display", 3),
                    circuit(EnumCircuitType.BASIC, 1, 1F), circuit(EnumCircuitType.BASIC, 1, 0.5F)));
        }

        r.add(rec(in("machine_turbine", 1), 3, out("turbine_titanium", 1), out("coil_copper", 2), out("ingot_steel", 4)));
        r.add(rec(in("yellow_barrel", 1), 3, out("tank_steel", 1), out("plate_lead", 2), out("nuclear_waste", 10)));
        r.add(rec(in("vitrified_barrel", 1), 3, out("tank_steel", 1), out("plate_lead", 2), out("nuclear_waste_vitrified", 10)));
        r.add(rec(in("egg_glyphid", 1), 1, out("glyphid_meat", 2), out("glyphid_meat", 1, 0.5F),
                new AnvilOutput(new ItemStack(Items.BONE), 0.75F), new AnvilOutput(new ItemStack(Items.EXPERIENCE_BOTTLE), 0.5F)));
        r.add(rec(in("fusion_heater", 1), 1, autogen(MaterialShapes.PIPE, Mats.MAT_STEEL, 4, 1F),
                autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 2, 1F), circuit(EnumCircuitType.ANALOG, 1, 0.5F)));
        r.add(rec(in("fusion_hatch", 1), 1, autogen(MaterialShapes.PIPE, Mats.MAT_STEEL, 4, 1F),
                autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 4, 1F), circuit(EnumCircuitType.ANALOG, 1, 0.75F)));
    }

    // ---- helpers ----

    private static AnvilConstructionRecipe rec(AStack input, int tier, AnvilOutput... outputs) {
        return new AnvilConstructionRecipe(input, outputs).setTier(tier);
    }

    private static AnvilConstructionRecipe one(AStack input, String output, int count, int tier) {
        return rec(input, tier, out(output, count));
    }

    private static AStack in(String id, int count) {
        return new ComparableStack(item(id), count);
    }

    private static AnvilOutput out(String id, int count) {
        return new AnvilOutput(new ItemStack(item(id), count));
    }

    private static AnvilOutput out(String id, int count, float chance) {
        return new AnvilOutput(new ItemStack(item(id), count), chance);
    }

    private static AnvilOutput circuit(EnumCircuitType type, int count, float chance) {
        return new AnvilOutput(ModIntermediateItems.circuit(type, count), chance);
    }

    private static AnvilOutput autogen(MaterialShapes shape, NTMMaterial mat, int count, float chance) {
        ItemAutogen item = ModAutogenItems.get(shape, mat);
        if (item == null) throw new IllegalStateException("anvil recycling: no autogen " + shape + " of " + mat.name);
        return new AnvilOutput(new ItemStack(item, count), chance);
    }

    private static AnvilOutput gear(int variant) {
        return new AnvilOutput(ModGearItems.gear_large.get().of(variant));
    }

    private static AStack stirling(String id, boolean hasCog) {
        return new CogStack(item(id), hasCog);
    }

    private static AnvilOutput pileRod(EnumPileRod type) {
        return new AnvilOutput(ModPileBlocks.pile_rod.stack(type));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id))
                .orElseThrow(() -> new IllegalStateException("anvil recycling names unregistered hbm:" + id));
    }

    /** 1.7 {@code new ComparableStack(machine_stirling, 1, meta)}: meta 1 is the engine without its gear. */
    private static final class CogStack extends AStack {
        private final Item item;
        private final boolean hasCog;

        CogStack(Item item, boolean hasCog) {
            this.item = item;
            this.hasCog = hasCog;
            this.stacksize = 1;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item
                    && com.hbm.blocks.machine.MachineStirling.hasCog(stack) == hasCog
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = new ItemStack(item, stacksize);
            if (!hasCog) ModDataComponents.HAS_COG.set(stack, false);
            return List.of(stack);
        }
    }

    /** 1.7 {@code new ComparableStack(block, 1, OreDictionary.WILDCARD_VALUE)}: any of the flattened variants. */
    private static final class AnyItemStack extends AStack {
        private final List<Item> items;

        AnyItemStack(Item... items) {
            this.items = List.of(items);
            this.stacksize = 1;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && items.contains(stack.getItem()) && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return items.stream().map(item -> new ItemStack(item, stacksize)).toList();
        }
    }
}
