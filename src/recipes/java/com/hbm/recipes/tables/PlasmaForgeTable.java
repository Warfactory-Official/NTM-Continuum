package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code PlasmaForgeRecipes.registerDefaults}. */
public final class PlasmaForgeTable extends GenericTableSource<PlasmaForgeRecipe> {

    // 1.7 OreDictManager names the port's OreDictManager has no alias for: their c: tags
    private static final NTMMaterial EUPH = new NTMMaterial(-1, "euphemium");
    private static final NTMMaterial AT = new NTMMaterial(-1, "astatine");
    private static final NTMMaterial VOLCANIC = new NTMMaterial(-1, "volcanic");

    public PlasmaForgeTable() {
        super(PlasmaForgeRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        String autoPlate = "autoswitch.weldPlates";

        // Plates
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.plateeuphemium").setInputEnergy(1_000_000).setup(600, 10_000_000).outputItems(new ItemStack(ModItems.plate_euphemium.get(), 4))
                .inputItems(EUPH.ingot().count(4), AT.dust().count(3), BI.dust().count(1), VOLCANIC.gem().count(1), OSMIRIDIUM.ingot()));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.platednt").setInputEnergy(1_000_000).setup(600, 10_000_000).outputItems(new ItemStack(ModItems.plate_dineutronium.get(), 4))
                .inputItems(DNT.ingot().count(4), new ComparableStack(ModItems.powder_spark_mix.get(), 2), DESH.ingot().count(1)));

        // Components
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.hde").setInputEnergy(10_000_000).setup(600, 25_000_000L)
                .outputItems(new ItemStack(ModIngredientItems.part(EnumPartType.HDE)))
                .inputItems(ANY_BISMOIDBRONZE.plateCast().count(2), CMB.plateWelded().count(1), new ComparableStack(ModItems.ingot_cft.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));

        // Expensive mode
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.exsteelplating").setInputEnergy(1_000_000).setup(40, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING, 1))
                .inputItems(STEEL.plateCast().count(4), TI.plate().count(4), STEEL.bolt().count(16)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.exheavyframe").setInputEnergy(1_000_000).setup(120, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3), ANY_PLASTIC.ingot().count(8), CU.plateWelded().count(4), DESH.ingot().count(1), DURA.bolt().count(32)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.excircuit").setInputEnergy(1_000_000).setup(80, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 8), RUBBER.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.exleadplating").setInputEnergy(1_000_000).setup(80, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), PB.plateCast().count(8), B.ingot().count(2), W.bolt().count(32))
                .inputFluids(new FluidStack(Fluids.LUBRICANT, 1_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.exferroplating").setInputEnergy(1_000_000).setup(240, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 3), FERRO.plateCast().count(4), ANY_RESISTANTALLOY.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 1_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.excomputer").setInputEnergy(1_000_000).setup(240, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 4), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 8))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 2_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.bronzetubes").setInputEnergy(10_000_000).setup(600, 1_000_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1), ANY_BISMOIDBRONZE.plateCast().count(4), ZR.plateWelded().count(1))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 4_000))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.explastic").setInputEnergy(1_000_000).setup(120, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC, 1))
                .inputItems(ANY_HARDPLASTIC.ingot().count(4), ANY_PLASTIC.ingot().count(16), RUBBER.ingot().count(8))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 1_000)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.exgold").setInputEnergy(1_000_000).setup(120, 100_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.GOLD_DUST, 1))
                .inputItems(GOLD.dust().count(64), GOLD.dust().count(64)));

        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.cmbtubes").setInputEnergy(12_500_000).setup(6_000, 10_000_000L)
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES, 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 3), CMB.plateCast().count(4), new ComparableStack(ModItems.powder_chlorophyte.get(), 12))
                .inputFluids(new FluidStack(Fluids.REFORMGAS, 4_000)));

        // Welded Plates
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldiron").setInputEnergy(500_000).setup(50, 100L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_IRON), 1))
                .inputItems(IRON.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldsteel").setInputEnergy(500_000).setup(50, 500L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_STEEL), 1))
                .inputItems(STEEL.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldcopper").setInputEnergy(500_000).setup(50, 1_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_COPPER), 1))
                .inputItems(CU.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldtitanium").setInputEnergy(500_000).setup(300, 50_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_TITANIUM), 1))
                .inputItems(TI.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldzirconium").setInputEnergy(500_000).setup(300, 10_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_ZIRCONIUM), 1))
                .inputItems(ZR.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldaluminium").setInputEnergy(500_000).setup(150, 10_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_ALUMINIUM), 1))
                .inputItems(AL.plateCast().count(2)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldtcalloy").setInputEnergy(500_000).setup(600, 1_000_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_TCALLOY), 1))
                .inputItems(TCALLOY.plateCast().count(2))
                .inputFluids(new FluidStack(Fluids.OXYGEN, 1_000)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldcdalloy").setInputEnergy(500_000).setup(600, 1_000_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_CDALLOY), 1))
                .inputItems(CDALLOY.plateCast().count(2))
                .inputFluids(new FluidStack(Fluids.OXYGEN, 1_000)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldtungsten").setInputEnergy(500_000).setup(600, 250_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_TUNGSTEN), 1))
                .inputItems(W.plateCast().count(2))
                .inputFluids(new FluidStack(Fluids.OXYGEN, 1_000)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldcmb").setInputEnergy(500_000).setup(600, 10_000_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_CMB), 1))
                .inputItems(CMB.plateCast().count(2))
                .inputFluids(new FluidStack(Fluids.REFORMGAS, 1_000)).setGroup(autoPlate, this));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldosmiridium").setInputEnergy(500_000).setup(3_000, 50_000_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_OSMIRIDIUM), 1))
                .inputItems(OSMIRIDIUM.plateCast().count(2))
                .inputFluids(new FluidStack(Fluids.REFORMGAS, 16_000)).setGroup(autoPlate, this));

        // Fusion
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.fusionvessel").setInputEnergy(3_000_000).setup(1_200, 2_000_000).outputItems(new ItemStack(ModMachineBlocks.fusion_torus.get().asItem()))
                .inputItems(new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_motor.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_motor.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_motor.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_blanket.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_blanket.get().asItem(), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));

        // ICF
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfcell").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_cell.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 2), ANY_BISMOIDBRONZE.plateCast().count(4), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 1), new ComparableStack(ModItems.ingot_cft.get(), 8), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfemitter").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_emitter.get().asItem(), 1))
                .inputItems(W.plateWelded().count(4), MAGTUNG.wireDense().count(16))
                .inputItemsEx(W.plateWelded().count(8), MAGTUNG.wireDense().count(16))
                .inputFluids(new FluidStack(Fluids.XENON, 16_000))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfcapacitor").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_capacitor.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(1), ND.wireDense().count(16), SBD.wireDense().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 3), ND.wireDense().count(16), SBD.wireDense().count(2))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfturbo").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_turbo.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(2), DNT.wireDense().count(4), SBD.wireDense().count(4))
                .inputItemsEx(ANY_RESISTANTALLOY.plateWelded().count(8), DNT.wireDense().count(8), SBD.wireDense().count(4))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfcasing").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_casing.get().asItem(), 1))
                .inputItems(ANY_BISMOIDBRONZE.plateCast().count(4), BIGMT.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 4), BIGMT.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfport").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_laser_component_port.get().asItem(), 1))
                .inputItems(ANY_BISMOIDBRONZE.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16), ND.wireDense().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 4), ANY_HARDPLASTIC.ingot().count(16), ND.wireDense().count(16))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfcontroller").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_controller.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 16), ANY_BISMOIDBRONZE.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 4), new ComparableStack(ModItems.ingot_cft.get(), 16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 4))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfscaffold").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_component.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(4), TI.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 8))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfvessel").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_component_vessel.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 1), CMB.plateCast().count(1), W.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 1), new ComparableStack(ModItems.ingot_cft.get(), 1), W.plateWelded().count(2))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfstructural").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.icf_component_structure.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(2), CU.plateWelded().count(2), ANY_BISMOIDBRONZE.plateCast().count(1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 1), STEEL.plateWelded().count(8))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfcore").setInputEnergy(1_000_000).setup(3_000, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.struct_icf_core.get().asItem(), 1))
                .inputItems(CMB.plateWelded().count(16), ANY_RESISTANTALLOY.plateWelded().count(16), ANY_BISMOIDBRONZE.plateCast().count(16), SBD.wireDense().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 12), CMB.plateWelded().count(16), SBD.wireDense().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 16))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.icfpress").setInputEnergy(1_000_000).setup(800, 10_000_000).outputItems(new ItemStack(ModIcfBlocks.machine_icf_press.get().asItem(), 1))
                .inputItems(GOLD.plateCast().count(8), new ComparableStack(ModItems.motor.get(), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .setPools528(PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"));

        // weapons
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.schrabhammer").setInputEnergy(25_000_000).setup(6_000, 10_000_000).outputItems(new ItemStack(ModMeleeKitItems.schrabidium_hammer.get(), 1))
                .inputItems(
                        SA326.block().count(35),
                        new ComparableStack(ModItems.billet_yharonite.get(), 64),
                        new ComparableStack(ModItems.billet_yharonite.get(), 64),
                        new ComparableStack(ModItems.coin_ufo.get(), 1),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64),
                        new ComparableStack(ModItems.fragment_meteorite.get(), 64)));

        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("ass.fensusan").setInputEnergy(50_000_000).setup(6_000, 50_000_000).outputItems(new ItemStack(ModBatteryBlocks.machine_battery_redd.get().asItem(), 1))
                .inputItems(new ComparableStack(ModFinalConsumableItems.ingot_electronium.get(), 64),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_QUANTUM).get(), 1),
                        OSMIRIDIUM.plateWelded().count(64),
                        OSMIRIDIUM.plateWelded().count(64),
                        ANY_BISMOIDBRONZE.plateCast().count(64),
                        CMB.plateCast().count(32),
                        MAGTUNG.wireDense().count(32),
                        new ComparableStack(ModItems.plate_dineutronium.get(), 64),
                        new ComparableStack(ModItems.powder_magic.get(), 64),
                        new ComparableStack(new ItemStack(ModNuclearMiscItems.INGOT_U238M2.get())),
                        new ComparableStack(ModItems.ingot_cft.get(), 64),
                        new ComparableStack(ModItems.ingot_cft.get(), 64))
                .inputItemsEx(new ComparableStack(ModFinalConsumableItems.ingot_electronium.get(), 64),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_QUANTUM).get(), 1),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 64),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 64),
                        OSMIRIDIUM.plateWelded().count(64),
                        OSMIRIDIUM.plateWelded().count(64),
                        OSMIRIDIUM.plateWelded().count(64),
                        ANY_BISMOIDBRONZE.plateCast().count(64),
                        CMB.plateCast().count(64),
                        new ComparableStack(new ItemStack(ModNuclearMiscItems.INGOT_U238M2.get())),
                        new ComparableStack(ModItems.ingot_cft.get(), 64),
                        new ComparableStack(ModItems.ingot_cft.get(), 64)));

        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.gerald").setInputEnergy(25_000_000).setup(12_000, 50_000_000).outputItems(new ItemStack(ModSatelliteItems.sat_gerald.get(), 1))
                .inputItems(SBD.plateCast().count(64),
                        SBD.plateCast().count(64),
                        BSCCO.wireDense().count(64),
                        BSCCO.wireDense().count(64),
                        new ComparableStack(ModExplosivesBlocks.det_nuke.get().asItem(), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CRYSTAL), 12),
                        new ComparableStack(ModIntermediateItems.drive(EnumDriveType.DISK_ORBITDATA_PROCESSED), 16))
                .inputItemsEx(SBD.plateCast().count(64),
                        BSCCO.wireDense().count(64),
                        new ComparableStack(ModExplosivesBlocks.det_nuke.get().asItem(), 64),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 64),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CMB_TUBES), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.HDE), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CRYSTAL), 64),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 64),
                        new ComparableStack(ModIntermediateItems.drive(EnumDriveType.DISK_ORBITDATA_PROCESSED), 64)));

        // really, really boring fucking temp recipes
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dfccore").setInputEnergy(50_000_000).setup(12_000, 100_000_000).outputItems(new ItemStack(ModDFCBlocks.dfc_core.get().asItem())).inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 12_000))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), DNT.wireDense().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 12), new ComparableStack(ModSingularityItems.singularity_spark.get()), new ComparableStack(ModItems.powder_chlorophyte.get(), 64)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dfcemitter").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(ModDFCBlocks.dfc_emitter.get().asItem())).inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), STAR.wireDense().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CRYSTAL), 4)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dfcreceiver").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(ModDFCBlocks.dfc_receiver.get().asItem())).inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), STAR.plateCast().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dfcinjector").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(ModDFCBlocks.dfc_injector.get().asItem())).inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), BIGMT.plateCast().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 4)));
        this.register((PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dfcstabilizer").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(ModDFCBlocks.dfc_stabilizer.get().asItem())).inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), SBD.wireDense().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8)));
    }
}
