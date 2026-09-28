package com.hbm.inventory.recipes;

import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code AssemblyMachineRecipes}' custom missile block: {@code ass.launchpad} (the large pad) and the
 * thruster, fuselage and warhead parts, input for input (with {@code ass.mpt10xenon}, which a wave had hand-added
 * to the generated table; gen_machine_recipes.py now skips it as curated).
 *
 * <p>Kept as upstream wrote them: {@code ass.mpt15bf} yields the <i>short</i> balefire thruster, and the
 * "long" fuselage recipes yield the short fuselages. {@code ass.mpw10taint}'s Watz container
 * ({@code Fluids.WATZ.getDict}) is a {@link com.hbm.inventory.FluidContainerStack}. The five nuclear/N2/balefire
 * warheads carry 1.7's {@code setPools528} "controller" blueprint pool, which only applies in 528 mode.
 */
public final class CustomMissileAssemblyRecipes {

    private CustomMissileAssemblyRecipes() {}

    public static void register(GenericRecipes reg) {
        add(reg, "ass.launchpad", 200, ModCustomMissileBlocks.launch_pad_large.get().asItem(),
                STEEL.plateCast().count(6), tag("c", "concretes", 64), ANY_PLASTIC.ingot().count(16),
                item(com.hbm.blocks.ModSteelBlocks.steel_scaffold.get().asItem(), 24), circuit(EnumCircuitType.ADVANCED, 2));

        // thrusters
        thruster(reg, "ass.mpt10kero", 100, ModCustomMissileItems.mp_thruster_10_kerosene, ModItems.seg_10, STEEL.pipe().count(1), W.ingot().count(4), STEEL.plate().count(4));
        thruster(reg, "ass.mpt10solid", 100, ModCustomMissileItems.mp_thruster_10_solid, ModItems.seg_10, DURA.pipe().count(1), W.ingot().count(4), STEEL.plate().count(4));
        thruster(reg, "ass.mpt10xenon", 100, ModCustomMissileItems.mp_thruster_10_xenon, ModItems.seg_10, DURA.plate().count(4), item(com.hbm.foundry.FoundryExpansion.ARC_ELECTRODE.get(), 1));
        thruster(reg, "ass.mpt15kero", 200, ModCustomMissileItems.mp_thruster_15_kerosene, ModItems.seg_15, STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8));
        thruster(reg, "ass.mpt15kerodual", 200, ModCustomMissileItems.mp_thruster_15_kerosene_dual, ModItems.seg_15, STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8));
        thruster(reg, "ass.mpt15kerotriple", 200, ModCustomMissileItems.mp_thruster_15_kerosene_triple, ModItems.seg_15, STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8));
        thruster(reg, "ass.mpt15solid", 200, ModCustomMissileItems.mp_thruster_15_solid, ModItems.seg_15, DURA.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8));
        thruster(reg, "ass.mpt15solid16", 200, ModCustomMissileItems.mp_thruster_15_solid_hexdecuple, ModItems.seg_15, DURA.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8));
        thruster(reg, "ass.mpt15hydro", 200, ModCustomMissileItems.mp_thruster_15_hydrogen, ModItems.seg_15, DURA.pipe().count(3), W.ingot().count(8), DESH.ingot().count(4));
        thruster(reg, "ass.mpt15hydrodual", 200, ModCustomMissileItems.mp_thruster_15_hydrogen_dual, ModItems.seg_15, DURA.pipe().count(3), W.ingot().count(8), DESH.ingot().count(4));
        thruster(reg, "ass.mpt15bfshort", 200, ModCustomMissileItems.mp_thruster_15_balefire_short, ModItems.seg_15, DURA.pipe().count(5), W.plateCast().count(8), BIGMT.plate().count(8));
        thruster(reg, "ass.mpt15bf", 200, ModCustomMissileItems.mp_thruster_15_balefire_short, ModItems.seg_15, DURA.pipe().count(5), W.plateCast().count(16), BIGMT.plate().count(16));
        thruster(reg, "ass.mpt15bflarge", 200, ModCustomMissileItems.mp_thruster_15_balefire_large, ModItems.seg_15, DURA.pipe().count(10), W.plateCast().count(16), BIGMT.plate().count(24));
        thruster(reg, "ass.mpt20kero", 400, ModCustomMissileItems.mp_thruster_20_kerosene, ModItems.seg_20, STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));
        thruster(reg, "ass.mpt20kerodual", 400, ModCustomMissileItems.mp_thruster_20_kerosene_dual, ModItems.seg_20, STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));
        thruster(reg, "ass.mpt20kerotriple", 400, ModCustomMissileItems.mp_thruster_20_kerosene_triple, ModItems.seg_20, STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));
        thruster(reg, "ass.mpt20solid", 400, ModCustomMissileItems.mp_thruster_20_solid, ModItems.seg_20, DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));
        thruster(reg, "ass.mpt20solidmulti", 400, ModCustomMissileItems.mp_thruster_20_solid_multi, ModItems.seg_20, DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));
        thruster(reg, "ass.mpt20solidmultier", 400, ModCustomMissileItems.mp_thruster_20_solid_multier, ModItems.seg_20, DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16));

        // fuselages
        add(reg, "ass.mpf10kero", 100, ModCustomMissileItems.mp_fuselage_10_kerosene.get(), item(ModItems.seg_10.get(), 2), AL.plate().count(12), STEEL.plate().count(3));
        add(reg, "ass.mpf10kerolong", 100, ModCustomMissileItems.mp_fuselage_10_kerosene.get(), item(ModItems.seg_10.get(), 2), AL.plate().count(16), STEEL.plate().count(6));
        add(reg, "ass.mpf10solid", 100, ModCustomMissileItems.mp_fuselage_10_solid.get(), item(ModItems.seg_10.get(), 2), TI.plate().count(12), STEEL.plate().count(3));
        add(reg, "ass.mpf10solidlong", 100, ModCustomMissileItems.mp_fuselage_10_solid.get(), item(ModItems.seg_10.get(), 2), TI.plate().count(16), STEEL.plate().count(6));
        add(reg, "ass.mpf10xenon", 100, ModCustomMissileItems.mp_fuselage_10_xenon.get(), item(ModItems.seg_10.get(), 2), CU.plate().count(12), STEEL.plate().count(3));
        add(reg, "ass.mpf1015kero", 200, ModCustomMissileItems.mp_fuselage_10_15_kerosene.get(), item(ModItems.seg_10.get(), 1), item(ModItems.seg_15.get(), 1), AL.plate().count(24), STEEL.plate().count(8));
        add(reg, "ass.mpf1015solid", 200, ModCustomMissileItems.mp_fuselage_10_15_solid.get(), item(ModItems.seg_10.get(), 1), item(ModItems.seg_15.get(), 1), TI.plate().count(24), STEEL.plate().count(8));
        add(reg, "ass.mpf1015hydro", 200, ModCustomMissileItems.mp_fuselage_10_15_hydrogen.get(), item(ModItems.seg_10.get(), 1), item(ModItems.seg_15.get(), 1), CU.plate().count(24), STEEL.plate().count(8));
        add(reg, "ass.mpf1015bf", 200, ModCustomMissileItems.mp_fuselage_10_15_balefire.get(), item(ModItems.seg_10.get(), 1), item(ModItems.seg_15.get(), 1), BIGMT.plate().count(24), STEEL.plate().count(8));
        add(reg, "ass.mpf15kero", 200, ModCustomMissileItems.mp_fuselage_15_kerosene.get(), item(ModItems.seg_15.get(), 2), AL.plate().count(32), STEEL.plate().count(12));
        add(reg, "ass.mpf15solid", 200, ModCustomMissileItems.mp_fuselage_15_solid.get(), item(ModItems.seg_15.get(), 2), TI.plate().count(32), STEEL.plate().count(12));
        add(reg, "ass.mpf15hydro", 200, ModCustomMissileItems.mp_fuselage_15_hydrogen.get(), item(ModItems.seg_15.get(), 2), CU.plate().count(32), STEEL.plate().count(12));
        add(reg, "ass.mpf1520kero", 400, ModCustomMissileItems.mp_fuselage_15_20_kerosene.get(), item(ModItems.seg_15.get(), 1), item(ModItems.seg_20.get(), 1), AL.plate().count(48), STEEL.plate().count(16));
        add(reg, "ass.mpf1520solid", 400, ModCustomMissileItems.mp_fuselage_15_20_solid.get(), item(ModItems.seg_15.get(), 1), item(ModItems.seg_20.get(), 1), TI.plate().count(48), STEEL.plate().count(16));

        // warheads
        add(reg, "ass.mpw10he", 100, ModCustomMissileItems.mp_warhead_10_he.get(), item(ModItems.seg_10.get(), 1), STEEL.plate().count(6), tag("c", "ingots/any_highexplosive", 3), circuit(EnumCircuitType.BASIC, 1));
        add(reg, "ass.mpw10inc", 100, ModCustomMissileItems.mp_warhead_10_incendiary.get(), item(ModItems.seg_10.get(), 1), STEEL.plate().count(6), tag("c", "ingots/any_highexplosive", 2), P_RED.dust().count(6), circuit(EnumCircuitType.BASIC, 1));
        add(reg, "ass.mpw10bus", 100, ModCustomMissileItems.mp_warhead_10_buster.get(), item(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(6), tag("c", "ingots/any_highexplosive", 6), circuit(EnumCircuitType.BASIC, 2));
        add528(reg, "controller", "ass.mpw10nukesmall", 200, ModCustomMissileItems.mp_warhead_10_nuclear.get(), item(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(16), PU239.billet().count(2), item(ModItems.neutron_reflector.get(), 4), tag("c", "ingots/any_highexplosive", 4), circuit(EnumCircuitType.CONTROLLER, 1));
        add528(reg, "controller", "ass.mpw10nukelarge", 200, ModCustomMissileItems.mp_warhead_10_nuclear_large.get(), item(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(16), PU239.billet().count(6), item(ModItems.neutron_reflector.get(), 8), tag("c", "ingots/any_highexplosive", 12), circuit(EnumCircuitType.CONTROLLER, 1));
        // 1.7 OreDictStack(Fluids.WATZ.getDict(1_000)): any full 1,000 mB Watz container (the mud bucket, a tank)
        add(reg, "ass.mpw10taint", 100, ModCustomMissileItems.mp_warhead_10_taint.get(), item(ModItems.seg_10.get(), 1), STEEL.plate().count(12), item(ModExplosivesBlocks.det_cord.get().asItem(), 2), item(ModItems.powder_magic.get(), 12), new com.hbm.inventory.FluidContainerStack("WATZ", 1_000));
        add(reg, "ass.mpw10cloud", 100, ModCustomMissileItems.mp_warhead_10_cloud.get(), item(ModItems.seg_10.get(), 1), STEEL.plate().count(12), item(ModExplosivesBlocks.det_cord.get().asItem(), 2), item(ModItems.powder_magic.get(), 16));
        add(reg, "ass.mpw15he", 200, ModCustomMissileItems.mp_warhead_15_he.get(), item(ModItems.seg_15.get(), 1), STEEL.plate().count(12), tag("c", "ingots/any_highexplosive", 12), circuit(EnumCircuitType.BASIC, 3));
        add(reg, "ass.mpw15inc", 200, ModCustomMissileItems.mp_warhead_15_incendiary.get(), item(ModItems.seg_15.get(), 1), STEEL.plate().count(12), tag("c", "ingots/any_highexplosive", 8), P_RED.dust().count(16), circuit(EnumCircuitType.BASIC, 3));
        add528(reg, "controller", "ass.mpw15nuke", 400, ModCustomMissileItems.mp_warhead_15_nuclear.get(), item(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), PU239.billet().count(12), item(ModItems.neutron_reflector.get(), 12), item(ModItems.ball_tatb.get(), 24), circuit(EnumCircuitType.CONTROLLER, 3));
        add528(reg, "controller", "ass.mpw15n2", 400, ModCustomMissileItems.mp_warhead_15_n2.get(), item(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), item(ModItems.ball_tatb.get(), 32), circuit(EnumCircuitType.ADVANCED, 8));
        add528(reg, "controller", "ass.mpw15bf", 400, ModCustomMissileItems.mp_warhead_15_balefire.get(), item(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), item(ModItems.neutron_reflector.get(), 16), item(ModItems.powder_magic.get(), 8), item(ModItems.egg_balefire_shard.get(), 4), tag("c", "ingots/any_highexplosive", 16), circuit(EnumCircuitType.CONTROLLER, 2));
    }

    private static void thruster(GenericRecipes reg, String name, int time, RegistryObject<? extends Item> out,
                                 RegistryObject<? extends Item> segment, AStack... rest) {
        AStack[] inputs = new AStack[rest.length + 1];
        inputs[0] = item(segment.get(), 1);
        System.arraycopy(rest, 0, inputs, 1, rest.length);
        add(reg, name, time, out.get(), inputs);
    }

    private static void add(GenericRecipes reg, String name, int time, Item output, AStack... inputs) {
        reg.registerGenerated(new GenericRecipe(name).setup(time, 100).inputItems(inputs).outputItems(new ItemStack(output)));
    }

    /** 1.7 {@code .setPools528(POOL_PREFIX_528 + pool)}: the blueprint pool applies only in 528 mode. */
    private static void add528(GenericRecipes reg, String pool, String name, int time, Item output, AStack... inputs) {
        reg.registerGenerated(new GenericRecipe(name).setup(time, 100).inputItems(inputs).outputItems(new ItemStack(output))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + pool));
    }

    private static AStack item(Item item, int count) {
        return new ComparableStack(item, count);
    }

    private static AStack circuit(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    private static AStack tag(String ns, String path, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of(ns, path)), count);
    }
}
