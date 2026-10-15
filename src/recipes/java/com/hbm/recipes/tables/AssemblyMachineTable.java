package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.inventory.recipes.loader.GenericRecipes.POOL_PREFIX_ALT;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.generic.BlockCap;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe.OutputChoice;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModGunItems;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModBombItems;
import com.hbm.items.ModBombs2Items;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ItemPistons;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.weapon.ItemAmmoArty.ArtilleryShellType;
import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import com.hbm.items.ModMachineItems;

/**
 * 1.7 {@code AssemblyMachineRecipes.registerDefaults}. 1.7's two {@code enable528} branches are rows carrying
 * {@code "requires": ["!enable_528"]}.
 */
public final class AssemblyMachineTable extends GenericTableSource<GenericRecipe> {

    private static final String NO_528 = "!enable_528";

    // 1.7 OreDictManager names the port's OreDictManager has no alias for: their c: tags
    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial GRAPHITE = Mats.MAT_GRAPHITE;
    private static final NTMMaterial CS137 = new NTMMaterial(-1, "cs137");
    private static final NTMMaterial EUPH = new NTMMaterial(-1, "euphemium");
    private static final NTMMaterial VOLCANIC = new NTMMaterial(-1, "volcanic");
    private static final NTMMaterial FIBER = new NTMMaterial(-1, "fiberglass");
    private static final NTMMaterial NETHERQUARTZ = new NTMMaterial(-1, "quartz");
    private static final NTMMaterial P_WHITE = new NTMMaterial(-1, "phosphorus");
    private static final NTMMaterial ANY_HIGHEXPLOSIVE = new NTMMaterial(-1, "any_highexplosive");
    private static final NTMMaterial ANY_PLASTICEXPLOSIVE = new NTMMaterial(-1, "any_plasticexplosive");
    private static final NTMMaterial ANY_SMOKELESS = new NTMMaterial(-1, "any_smokeless");
    private static final TagKey<Item> ANY_CONCRETE = tag("c", "concretes");
    private static final TagKey<Item> ANY_TAR = tag("c", "tars");
    private static final TagKey<Item> KEY_PLANKS = tag("minecraft", "planks");
    private static final TagKey<Item> KEY_ANYPANE = tag("c", "glass_panes");
    private static final TagKey<Item> KEY_BLACK = tag("c", "dyes/black");
    private static final TagKey<Item> KEY_BLUE = tag("c", "dyes/blue");
    private static final TagKey<Item> KEY_BROWN = tag("c", "dyes/brown");
    private static final TagKey<Item> KEY_GRAY = tag("c", "dyes/gray");
    private static final TagKey<Item> KEY_GREEN = tag("c", "dyes/green");
    private static final TagKey<Item> KEY_LIGHTGRAY = tag("c", "dyes/light_gray");
    private static final TagKey<Item> KEY_ORANGE = tag("c", "dyes/orange");
    private static final TagKey<Item> KEY_RED = tag("c", "dyes/red");
    private static final TagKey<Item> KEY_WHITE = tag("c", "dyes/white");
    private static final TagKey<Item> KEY_YELLOW = tag("c", "dyes/yellow");

    /** The flag of the 1.7 config branch being registered, null outside one. */
    private String branch;

    public AssemblyMachineTable() {
        super(AssemblyMachineRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // 1.7: boolean no528 = !GeneralConfig.enable528; its two branches' rows carry "requires": ["!enable_528"]

        // plates and ingots
        String autoPlate = "autoswitch.plates";
        this.register(new GenericRecipe("ass.plateiron").setup(60, 100).outputItems(new ItemStack(ModItems.plate_iron.get(), 1)).inputItems(IRON.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plategold").setup(60, 100).outputItems(new ItemStack(ModItems.plate_gold.get(), 1)).inputItems(GOLD.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platetitanium").setup(60, 100).outputItems(new ItemStack(ModItems.plate_titanium.get(), 1)).inputItems(TI.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platealu").setup(60, 100).outputItems(new ItemStack(ModItems.plate_aluminium.get(), 1)).inputItems(AL.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platesteel").setup(60, 100).outputItems(new ItemStack(ModItems.plate_steel.get(), 1)).inputItems(STEEL.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platelead").setup(60, 100).outputItems(new ItemStack(ModItems.plate_lead.get(), 1)).inputItems(PB.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platecopper").setup(60, 100).outputItems(new ItemStack(ModItems.plate_copper.get(), 1)).inputItems(CU.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plateschrab").setup(60, 100).outputItems(new ItemStack(ModItems.plate_schrabidium.get(), 1)).inputItems(SA326.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platecmb").setup(60, 100).outputItems(new ItemStack(ModItems.plate_combine_steel.get(), 1)).inputItems(CMB.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plategunmetal").setup(60, 100).outputItems(new ItemStack(ModItems.plate_gunmetal.get(), 1)).inputItems(GUNMETAL.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plateweaponsteel").setup(60, 100).outputItems(new ItemStack(ModItems.plate_weaponsteel.get(), 1)).inputItems(WEAPONSTEEL.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platesaturnite").setup(60, 100).outputItems(new ItemStack(ModItems.plate_saturnite.get(), 1)).inputItems(BIGMT.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platedura").setup(60, 100).outputItems(new ItemStack(ModItems.plate_dura_steel.get(), 1)).inputItems(DURA.ingot()).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));

        this.register(new GenericRecipe("ass.platemixed").setup(50, 100).outputItems(new ItemStack(ModItems.plate_mixed.get(), 4))
                .inputItems(CU.plate().count(2), new ComparableStack(ModItems.neutron_reflector.get(), 1), BIGMT.plate().count(1)));
        this.register(new GenericRecipe("ass.dalekanium").setup(200, 100).outputItems(new ItemStack(ModItems.plate_dalekanium.get(), 1))
                .inputItems(new ComparableStack(ModBlocks.block_meteor.get().asItem(), 1)));
        this.register(new GenericRecipe("ass.platedesh").setup(200, 100).outputItems(new ItemStack(ModItems.plate_desh.get(), 4))
                .inputItems(DESH.ingot().count(4), ANY_PLASTIC.dust().count(2), DURA.ingot().count(1)));
        this.register(new GenericRecipe("ass.platebismuth").setup(200, 100).outputItems(new ItemStack(ModItems.plate_bismuth.get(), 1))
                .inputItems(new ComparableStack(ModItems.nugget_bismuth.get(), 2), U238.billet().count(2), NB.dust().count(1)));

        // expensive parts
        this.register(new GenericRecipe("ass.exsteelplating").setup(200, 400).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1))
                .inputItems(STEEL.plateCast().count(4), TI.plate().count(4), STEEL.bolt().count(16)));
        this.register(new GenericRecipe("ass.exheavyframe").setup(600, 800).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3), ANY_PLASTIC.ingot().count(8), CU.plateWelded().count(4), DESH.ingot().count(1), DURA.bolt().count(32)));
        this.register(new GenericRecipe("ass.excircuit").setup(400, 4_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 8), RUBBER.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1_000)));
        this.register(new GenericRecipe("ass.exleadplating").setup(400, 4_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), PB.plateCast().count(8), B.ingot().count(2), W.bolt().count(32))
                .inputFluids(new FluidStack(Fluids.LUBRICANT, 1_000)));
        this.register(new GenericRecipe("ass.exferroplating").setup(1_200, 10_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 3), FERRO.plateCast().count(4), ANY_RESISTANTALLOY.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 1_000)));
        this.register(new GenericRecipe("ass.excomputer").setup(1_200, 16_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 4), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 8))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 2_000)));
        this.register(new GenericRecipe("ass.bronzetubes").setup(3_000, 250_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1), ANY_BISMOIDBRONZE.plateCast().count(4), ZR.plateWelded().count(1))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 4_000))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        this.register(new GenericRecipe("ass.explastic").setup(600, 20_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 1))
                .inputItems(ANY_HARDPLASTIC.ingot().count(4), ANY_PLASTIC.ingot().count(16), RUBBER.ingot().count(8))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 1_000)));
        this.register(new GenericRecipe("ass.exgold").setup(600, 10_000).outputItems(new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.GOLD_DUST), 1))
                .inputItems(GOLD.dust().count(64), GOLD.dust().count(64)));

        // cloth
        this.register(new GenericRecipe("ass.hazcloth").setup(50, 100).outputItems(new ItemStack(ModItems.hazmat_cloth.get(), 4))
                .inputItems(PB.dust().count(4), new ComparableStack(Items.STRING, 8)));
        this.register(new GenericRecipe("ass.firecloth").setup(50, 100).outputItems(new ItemStack(ModItems.asbestos_cloth.get(), 4))
                .inputItems(ASBESTOS.ingot().count(1), new ComparableStack(Items.STRING, 8)));
        this.register(new GenericRecipe("ass.filtercoal").setup(50, 100).outputItems(new ItemStack(ModItems.filter_coal.get(), 1))
                .inputItems(COAL.dust().count(4), new ComparableStack(Items.STRING, 2), new ComparableStack(Items.PAPER, 1)));

        // crafting parts
        // 1.7: if(!GeneralConfig.enable528) { // precass otherwise
        branch = NO_528;
        {
            this.register(new GenericRecipe("ass.chip").setup(50, 250).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP)))
                    .inputItems(new ComparableStack(ModItems.plate_polymer.get()), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON), 1), GOLD.wire()));
            this.register(new GenericRecipe("ass.chipBismoid").setup(100, 1_500).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID)))
                    .inputItems(new ComparableStack(ModItems.plate_polymer.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON), 2), ANY_BISMOID.nugget(), GOLD.wire().count(2)));
            this.register(new GenericRecipe("ass.chipQuantum").setup(200, 15_000).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM)))
                    .inputItems(ANY_HARDPLASTIC.ingot().count(2), BSCCO.wireDense(), new ComparableStack(ModItems.pellet_charged.get()), GOLD.wire().count(8)));
            this.register(new GenericRecipe("ass.atomicClock").setup(300, 1_000).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK)))
                    .inputItems(ANY_PLASTIC.ingot().count(2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 3), SR.dust().count(1)));

            // alternates
            this.register(new GenericRecipe("ass.analogAlt").setupNamed(200, 250).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG)))
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 4), ANY_PLASTIC.ingot().count(4), W.wire().count(8), NB.ingot()).setPools(POOL_PREFIX_ALT + ".circuit"));
            this.register(new GenericRecipe("ass.factorioChip").setupNamed(300, 20_000).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC)))
                    .inputItems(RUBBER.ingot().count(2), IRON.plate().count(4), CU.wire().count(8)).setPools(POOL_PREFIX_ALT + ".circuit"));
            this.register(new GenericRecipe("ass.atomicClockAlt").setupNamed(300, 20_000).outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK), 4))
                    .inputItems(ANY_PLASTIC.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 3), CS137.dust().count(1)).setPools(POOL_PREFIX_ALT + ".circuit"));
        }
        branch = null;

        // machine parts
        this.register(new GenericRecipe("ass.centrifugetower").setup(100, 100).outputItems(new ItemStack(ModItems.centrifuge_element.get(), 1))
                .inputItems(DURA.plate().count(4), TI.plate().count(4), new ComparableStack(ModItems.motor.get(), 1)));
        this.register(new GenericRecipe("ass.reactorcore").setup(100, 100).outputItems(new ItemStack(ModItems.reactor_core.get(), 1))
                .inputItems(PB.plateCast().count(4), BE.ingot().count(8), DURA.plate().count(8), ASBESTOS.ingot().count(4)));
        this.register(new GenericRecipe("ass.thermoelement").setup(60, 100).outputItems(new ItemStack(ModItems.thermo_element.get(), 1))
                .inputItems(STEEL.plate().count(1), MINGRADE.wire().count(2), NETHERQUARTZ.dust().count(2)));
        this.register(new GenericRecipe("ass.thermoelementsilicon").setup(60, 100).outputItems(new ItemStack(ModItems.thermo_element.get(), 1))
                .inputItems(STEEL.plate().count(1), GOLD.wire().count(2), SI.billet().count(1)));
        this.register(new GenericRecipe("ass.rtgunit").setup(100, 100).outputItems(new ItemStack(ModItems.rtg_unit.get(), 1))
                .inputItems(PB.plateCast().count(2), CU.plate().count(4), new ComparableStack(ModItems.thermo_element.get(), 2)));
        this.register(new GenericRecipe("ass.magnetron").setup(40, 100).outputItems(new ItemStack(ModItems.magnetron.get(), 1))
                .inputItems(CU.plate().count(3), W.wire().count(4)));
        this.register(new GenericRecipe("ass.titaniumdrill").setup(100, 100).outputItems(new ItemStack(ModItems.drill_titanium.get(), 1))
                .inputItems(DURA.plateCast().count(1), TI.plate().count(8)));
        this.register(new GenericRecipe("ass.entanglementkit").setup(200, 100).outputItems(new ItemStack(ModItems.entanglement_kit.get(), 1))
                .inputItems(DURA.plateCast().count(4), CU.plate().count(24), GOLD.wireDense().count(16))
                .inputFluids(new FluidStack(Fluids.XENON, 8_000)));
        this.register(new GenericRecipe("ass.protoreactor").setup(200, 100).outputItems(new ItemStack(ModItems.dysfunctional_reactor.get(), 1))
                .inputItems(STEEL.shell().count(4), PB.plateCast().count(4), new ComparableStack(ModItems.rod_quad_empty.get(), 10), new TagStack(KEY_BROWN).count(3)));

        // pile rods
        String autoPileRod = "autoswitch.pilerod";
        this.register(new GenericRecipe("ass.pilepabe").setup(40, 200).outputItems(new ItemStack(ModPileBlocks.pile_rod.get(EnumPileRod.RA226BE), 1))
                .inputItems(new ComparableStack(ModItems.billet_ra226be.get(), 3)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilepobe").setup(40, 200).outputItems(new ItemStack(ModPileBlocks.pile_rod.get(EnumPileRod.PO210BE), 1))
                .inputItems(new ComparableStack(ModItems.billet_po210be.get(), 3)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilezr").setup(40, 200).outputItems(new ItemStack(ModPileBlocks.pile_rod.get(EnumPileRod.ZR), 3))
                .inputItems(ZR.billet().count(1)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilenu").setup(40, 200).outputItems(new ItemStack(ModPileBlocks.pile_rod.get(EnumPileRod.NU), 1))
                .inputItems(U.billet().count(3)).setGroup(autoPileRod, this));

        // powders
        String autoCyclotron = "autoswitch.cyclotron";
        this.register(new GenericRecipe("ass.partlith").setup(40, 100).outputItems(new ItemStack(ModItems.part_lithium.get(), 8))
                .inputItems(LI.dust().count(1)).setGroup(autoCyclotron, this));
        this.register(new GenericRecipe("ass.partberyl").setup(40, 100).outputItems(new ItemStack(ModItems.part_beryllium.get(), 8))
                .inputItems(BE.dust().count(1)).setGroup(autoCyclotron, this));
        this.register(new GenericRecipe("ass.partcoal").setup(40, 100).outputItems(new ItemStack(ModItems.part_carbon.get(), 8))
                .inputItems(COAL.dust().count(1)).setGroup(autoCyclotron, this));
        this.register(new GenericRecipe("ass.partcop").setup(40, 100).outputItems(new ItemStack(ModItems.part_copper.get(), 8))
                .inputItems(CU.dust().count(1)).setGroup(autoCyclotron, this));
        this.register(new GenericRecipe("ass.partplut").setup(40, 100).outputItems(new ItemStack(ModItems.part_plutonium.get(), 8))
                .inputItems(PU.dust().count(1)).setGroup(autoCyclotron, this));

        // bunker blocks
        this.register(new GenericRecipe("ass.cmbtile").setup(100, 100).outputItems(new ItemStack(ModBlocks.cmb_brick.get().asItem(), 8))
                .inputItems(new TagStack(ANY_CONCRETE).count(4), CMB.plate().count(4)));
        this.register(new GenericRecipe("ass.cmbbrick").setup(100, 100).outputItems(new ItemStack(ModHazardBlocks.cmb_brick_reinforced.get().asItem(), 8))
                .inputItems(MAGTUNG.ingot().count(8), new ComparableStack(ModHazardBlocks.ducrete.get().asItem(), 4), new ComparableStack(ModBlocks.cmb_brick.get().asItem(), 8)));
        this.register(new GenericRecipe("ass.sealframe").setup(100, 100).outputItems(new ItemStack(ModBlocks.seal_frame.get().asItem(), 1))
                .inputItems(DURA.ingot().count(1), STEEL.plateCast().count(1), MINGRADE.wireDense().count(1)));
        this.register(new GenericRecipe("ass.sealcontroller").setup(100, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.seal_controller.get().asItem(), 1))
                .inputItems(DURA.ingot().count(1), STEEL.plateCast().count(1), ANY_PLASTIC.ingot().count(4), MINGRADE.wireDense().count(4)));

        // blocks
        this.register(new GenericRecipe("ass.yellowbarrel").setup(400, 400).outputItems(new ItemStack(ModFluidNetBlocks.yellow_barrel.get().asItem(), 1))
            .inputItems(new ComparableStack(ModItems.tank_steel.get(), 1), PB.plate().count(2), new ComparableStack(ModItems.nuclear_waste.get(), 10)));
        this.register(new GenericRecipe("ass.vitrifiedbarrel").setup(400, 400).outputItems(new ItemStack(ModFluidNetBlocks.vitrified_barrel.get().asItem(), 1))
            .inputItems(new ComparableStack(ModItems.tank_steel.get(), 1), PB.plate().count(2), new ComparableStack(ModItems.nuclear_waste_vitrified.get(), 10)));

        // nuclear door mod
        this.register(new GenericRecipe("ass.vaultdoor").setup(600, 100).outputItems(new ItemStack(ModDoorBlocks.VAULT_DOOR.get().asItem(), 1))
                .inputItems(STEEL.ingot().count(32), DURA.ingot().count(32), PB.plateCast().count(8), ANY_RUBBER.ingot().count(12), DURA.bolt().count(32), new ComparableStack(ModItems.motor.get(), 3)));
        this.register(new GenericRecipe("ass.blastdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.BLAST_DOOR.get().asItem(), 1))
                .inputItems(STEEL.ingot().count(12), PB.plate().count(6), ANY_RUBBER.ingot().count(2), DURA.bolt().count(8), new ComparableStack(ModItems.motor.get(), 1)));
        this.register(new GenericRecipe("ass.firedoor").setup(300, 100).outputItems(new ItemStack(ModDoorBlocks.FIRE_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), DURA.bolt().count(8), new ComparableStack(ModItems.motor.get(), 2)));
        this.register(new GenericRecipe("ass.seal").setup(1_200, 100).outputItems(new ItemStack(ModDoorBlocks.TRANSITION_SEAL.get().asItem(), 1))
                .inputItems(new ComparableStack(ModHazardBlocks.cmb_brick_reinforced.get().asItem(), 16), STEEL.plate().count(64), ANY_RUBBER.ingot().count(36), STEEL.block().count(32), new ComparableStack(ModItems.motor_desh.get(), 16), new TagStack(KEY_YELLOW).count(4)));
        this.register(new GenericRecipe("ass.slidingdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SLIDING_BLAST_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), W.ingot().count(8), new ComparableStack(ModGlassBlocks.reinforced_glass.get().asItem(), 4), ANY_RUBBER.ingot().count(4), DURA.bolt().count(16), new ComparableStack(ModItems.motor.get(), 2)));
        this.register(new GenericRecipe("ass.vehicledoor").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.LARGE_VEHICLE_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(16), new ComparableStack(ModItems.plate_polymer.get(), 4), new ComparableStack(ModItems.motor.get(), 4), DURA.bolt().count(16), new TagStack(KEY_GREEN).count(4)));
        this.register(new GenericRecipe("ass.waterdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.WATER_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), DURA.bolt().count(4), new TagStack(KEY_RED).count(1)));
        this.register(new GenericRecipe("ass.qedoor").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.QE_CONTAINMENT.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(6), new ComparableStack(ModItems.plate_polymer.get(), 8), new ComparableStack(ModItems.motor.get(), 2), DURA.bolt().count(32), new TagStack(KEY_BLACK).count(4)));
        this.register(new GenericRecipe("ass.queslidingdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.QE_SLIDING_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plate().count(4), new ComparableStack(ModItems.plate_polymer.get(), 4), new ComparableStack(ModItems.motor.get(), 2), DURA.bolt().count(4), new TagStack(KEY_WHITE).count(4), new ComparableStack(Blocks.GLASS.asItem(), 4)));
        this.register(new GenericRecipe("ass.roundairlock").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.ROUND_AIRLOCK_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(12), new ComparableStack(ModItems.plate_polymer.get(), 16), new ComparableStack(ModItems.motor.get(), 4), DURA.bolt().count(16), new TagStack(KEY_GREEN).count(4)));
        this.register(new GenericRecipe("ass.secureaccess").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.SECURE_ACCESS_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(12), new ComparableStack(ModItems.plate_polymer.get(), 8), new ComparableStack(ModItems.motor.get(), 4), DURA.bolt().count(32), new TagStack(KEY_RED).count(8)));
        this.register(new GenericRecipe("ass.slidingseal").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SLIDING_SEAL_DOOR.get().asItem(), 1))
                .inputItems(STEEL.plate().count(12), new ComparableStack(ModItems.plate_polymer.get(), 4), new ComparableStack(ModItems.motor.get(), 2), DURA.bolt().count(4), new TagStack(KEY_WHITE).count(2)));
        this.register(new GenericRecipe("ass.silohatch").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SILO_HATCH.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(4), new ComparableStack(ModItems.plate_polymer.get(), 4), new ComparableStack(ModItems.motor.get(), 2), STEEL.bolt().count(16), new TagStack(KEY_GREEN).count(4)));
        this.register(new GenericRecipe("ass.silohatchlarge").setup(300, 100).outputItems(new ItemStack(ModDoorBlocks.SILO_HATCH_LARGE.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(6), new ComparableStack(ModItems.plate_polymer.get(), 8), new ComparableStack(ModItems.motor.get(), 2), STEEL.bolt().count(16), new TagStack(KEY_GREEN).count(8)));
        this.register(new GenericRecipe("ass.cargodoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.CARGO_DOOR.get().asItem(), 1))
            .inputItems(new ComparableStack(ModDecoBlocks.steel_beam.get().asItem(), 32), STEEL.plate().count(4), DURA.bolt().count(4), new ComparableStack(ModItems.motor.get(), 2), new TagStack(KEY_GRAY).count(1)));
        // decoration
        this.register(new GenericRecipe("ass.capnuka").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.NUKA).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_nuka.get(), 64), new ComparableStack(ModItems.cap_nuka.get(), 64)));
        this.register(new GenericRecipe("ass.capquantum").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.QUANTUM).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_quantum.get(), 64), new ComparableStack(ModItems.cap_quantum.get(), 64)));
        this.register(new GenericRecipe("ass.capsparkle").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.SPARKLE).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_sparkle.get(), 64), new ComparableStack(ModItems.cap_sparkle.get(), 64)));
        this.register(new GenericRecipe("ass.caprad").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.RAD).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_rad.get(), 64), new ComparableStack(ModItems.cap_rad.get(), 64)));
        this.register(new GenericRecipe("ass.capfritz").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.FRITZ).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_fritz.get(), 64), new ComparableStack(ModItems.cap_fritz.get(), 64)));
        this.register(new GenericRecipe("ass.capkorl").setup(10, 100).outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(BlockCap.EnumCapBlock.KORL).get().asItem()))
                .inputItems(new ComparableStack(ModItems.cap_korl.get(), 64), new ComparableStack(ModItems.cap_korl.get(), 64)));

        // machines
        this.register(new GenericRecipe("ass.shredder").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.machine_shredder.get().asItem(), 1))
                .inputItems(STEEL.plate().count(8), CU.plate().count(4), new ComparableStack(ModItems.motor.get(), 2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1), CU.plate().count(4), new ComparableStack(ModItems.motor.get(), 2)));
        this.register(new GenericRecipe("ass.assembler").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_assembly_machine.get().asItem(), 1))
                .inputItems(STEEL.ingot().count(4), CU.plate().count(4), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1)));
        this.register(new GenericRecipe("ass.chemplant").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_chemical_plant.get().asItem(), 1))
                .inputItems(STEEL.ingot().count(8), CU.pipe().count(2), new ComparableStack(ModItems.plate_polymer.get(), 16), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModItems.coil_tungsten.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3), CU.pipe().count(2), new ComparableStack(ModItems.plate_polymer.get(), 16), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModItems.coil_tungsten.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 3)));
        this.register(new GenericRecipe("ass.chemplantAlt").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_chemical_plant.get().asItem(), 1))
                .inputItems(DURA.plate().count(8), CU.pipe().count(4), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 5), CU.pipe().count(4), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 2)));
        this.register(new GenericRecipe("ass.purex").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.machine_purex.get().asItem(), 1))
                .inputItems(STEEL.shell().count(4), RUBBER.pipe().count(8), PB.plateCast().count(4), new ComparableStack(ModItems.motor_desh.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 2), STEEL.shell().count(4), RUBBER.pipe().count(12), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2)));
        this.register(new GenericRecipe("ass.precass").setup(1_200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_precass.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(8), ZR.ingot().count(8), new ComparableStack(ModItems.motor.get(), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4), ZR.ingot().count(8), new ComparableStack(ModItems.motor.get(), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 8)));
        this.register(new GenericRecipe("ass.centrifuge").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_centrifuge.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.centrifuge_element.get(), 1), ANY_PLASTIC.ingot().count(4), STEEL.plate().count(8), CU.plate().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1))
                .inputItemsEx(new ComparableStack(ModItems.centrifuge_element.get(), 1), ANY_PLASTIC.ingot().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3), CU.plateCast().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1)));
        this.register(new GenericRecipe("ass.gascent").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_gascent.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.centrifuge_element.get(), 4), ANY_PLASTIC.ingot().count(8), DESH.ingot().count(2), STEEL.plate().count(8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1))
                .inputItemsEx(new ComparableStack(ModItems.centrifuge_element.get(), 4), STEEL.plateWelded().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "gascent"));
        this.register(new GenericRecipe("ass.arcfurnace").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_arc_furnace.get(), 1))
                .inputItems(new TagStack(ANY_CONCRETE).count(12), ANY_PLASTIC.ingot().count(8), new ComparableStack(ModItems.ingot_firebrick.get(), 16),STEEL.plateCast().count(8), new ComparableStack(ModBlocks.machine_transformer.get().asItem(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1))
                .inputItemsEx(new TagStack(ANY_CONCRETE).count(12), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), new ComparableStack(ModItems.ingot_firebrick.get(), 16), new ComparableStack(ModBlocks.machine_transformer.get().asItem(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 3)));
        this.register(new GenericRecipe("ass.acidizer").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_crystallizer.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(2), TI.shell().count(3), DESH.ingot().count(4), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), TI.shell().count(3), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1)));
        this.register(new GenericRecipe("ass.electrolyzer").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_electrolyser.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(8), CU.plate().count(16), TI.shell().count(3), RUBBER.ingot().count(8), new ComparableStack(ModItems.ingot_firebrick.get(), 16), new ComparableStack(ModItems.coil_copper.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), TI.shell().count(3), RUBBER.ingot().count(8), new ComparableStack(ModItems.ingot_firebrick.get(), 16), new ComparableStack(ModItems.coil_copper.get(), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4)));
        this.register(new GenericRecipe("ass.rtg").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_rtg_grey.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.rtg_unit.get(), 3), STEEL.plate().count(4), MINGRADE.wire().count(16), ANY_PLASTIC.ingot().count(4)));
        this.register(new GenericRecipe("ass.derrick").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_well.get().asItem(), 1))
                .inputItems(STEEL.plate().count(8), CU.plateCast().count(2), STEEL.pipe().count(4), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModItems.drill_titanium.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), STEEL.pipe().count(4), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModItems.drill_titanium.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "steel"));
        this.register(new GenericRecipe("ass.pumpjack").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_pumpjack.get().asItem(), 1))
                .inputItems(DURA.plate().count(8), STEEL.plateWelded().count(8), STEEL.pipe().count(12), new ComparableStack(ModItems.motor_desh.get()), new ComparableStack(ModItems.drill_titanium.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), STEEL.pipe().count(12), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModItems.drill_titanium.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "steel"));
        this.register(new GenericRecipe("ass.fracker").setup(600, 100).outputItems(new ItemStack(ModMachineBlocks.machine_fracking_tower.get().asItem(), 1))
                .inputItems(STEEL.shell().count(24), STEEL.pipe().count(12), new ComparableStack(ModBlocks.concrete_smooth.get().asItem(), 64), new ComparableStack(ModItems.drill_titanium.get()), new ComparableStack(ModItems.motor_desh.get(), 2), new ComparableStack(ModItems.plate_desh.get(), 24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 2), STEEL.pipe().count(12), new ComparableStack(ModBlocks.concrete_smooth.get().asItem(), 64), new ComparableStack(ModItems.drill_titanium.get()), new ComparableStack(ModItems.motor_desh.get(), 5), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.flarestack").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.machine_flare.get().asItem(), 1))
                .inputItems(STEEL.plate().count(12), CU.plate().count(4), STEEL.shell().count(4), new ComparableStack(ModItems.thermo_element.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 8), CU.plate().count(4), new ComparableStack(ModItems.thermo_element.get(), 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.refinery").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_refinery.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(3), CU.plate().count(8), STEEL.shell().count(4), STEEL.pipe().count(12), new ComparableStack(ModItems.plate_polymer.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4), STEEL.pipe().count(12), new ComparableStack(ModItems.plate_polymer.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 5)));
        this.register(new GenericRecipe("ass.crackingtower").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_catalytic_cracker.get().asItem(), 1))
                .inputItems(new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16), STEEL.shell().count(6), DESH.ingot().count(12), NB.ingot().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 8), ANY_PLASTIC.ingot().count(16), NB.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.radiolysis").setup(200, 100).outputItems(new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get().asItem(), 1))
                .inputItems(STEEL.shell().count(4), ANY_RESISTANTALLOY.ingot().count(4), PB.plate().count(12), CU.plateCast().count(4), RUBBER.ingot().count(8), new ComparableStack(ModItems.thermo_element.get(), 8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), CU.plateCast().count(4), RUBBER.ingot().count(16), new ComparableStack(ModItems.thermo_element.get(), 8)));
        this.register(new GenericRecipe("ass.coker").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_coker.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(8), STEEL.shell().count(4), CU.plate().count(8), RUBBER.ingot().count(4), NB.ingot().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 8), RUBBER.ingot().count(16), NB.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "rubber"));
        this.register(new GenericRecipe("ass.vaccumrefinery").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_vacuum_distill.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(16), CU.plate().count(16), ANY_RESISTANTALLOY.ingot().count(4), new ComparableStack(ModItems.sphere_steel.get(), 1), STEEL.pipe().count(12), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 2), DURA.pipe().count(16), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1)));
        this.register(new GenericRecipe("ass.reformer").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_catalytic_reformer.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(12), CU.plate().count(8), NB.ingot().count(8), ANY_RESISTANTALLOY.ingot().count(4), STEEL.shell().count(3), STEEL.pipe().count(8), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 2), NB.ingot().count(8), ANY_RESISTANTALLOY.ingot().count(4), DURA.pipe().count(16), new ComparableStack(ModItems.motor.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.hydrotreater").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_hydrotreater.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(8), CU.plateCast().count(4), NB.ingot().count(8), ANY_RESISTANTALLOY.ingot().count(4), STEEL.shell().count(2), STEEL.pipe().count(8), new ComparableStack(ModItems.motor_desh.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 2), NB.ingot().count(8), ANY_RESISTANTALLOY.ingot().count(4), DURA.pipe().count(16), new ComparableStack(ModItems.motor_desh.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.pyrooven").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.machine_pyrooven.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 4), CU.pipe().count(12), new ComparableStack(ModItems.motor_desh.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 6), ANY_HARDPLASTIC.ingot().count(32), new ComparableStack(ModItems.ingot_cft.get(), 4), new ComparableStack(ModItems.motor_bismuth.get(), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.liquefactor").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_liquefactor.get().asItem(), 1))
                .inputItems(STEEL.shell().count(4), CU.plate().count(12), new TagStack(ANY_TAR).count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 12), new ComparableStack(ModItems.coil_tungsten.get(), 8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), new TagStack(ANY_TAR).count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 16)));
        this.register(new GenericRecipe("ass.solidifier").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_solidifier.get().asItem(), 1))
                .inputItems(STEEL.shell().count(4), AL.plate().count(12), ANY_PLASTIC.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 12), new ComparableStack(ModItems.coil_copper.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 16)));
        this.register(new GenericRecipe("ass.compressor").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_compressor.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(8), CU.plate().count(4), STEEL.shell().count(2), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3), STEEL.shell().count(4), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 3)));
        this.register(new GenericRecipe("ass.compactcompressor").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_compressor_compact.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(8), TI.shell().count(4), CU.pipe().count(4), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4), CU.pipe().count(16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1)));
        this.register(new GenericRecipe("ass.epress").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.machine_epress.get().asItem(), 1))
                .inputItems(STEEL.plate().count(8), ANY_RUBBER.ingot().count(4), new ComparableStack(ModIngredientItems.part(EnumPartType.PISTON_HYDRAULIC), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), new ComparableStack(ModIngredientItems.part(EnumPartType.PISTON_HYDRAULIC), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4)));
        this.register(new GenericRecipe("ass.fel").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_fel.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1), GOLD.wireDense().count(64), STEEL.plateCast().count(12), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModIngredientItems.part(EnumPartType.GLASS_POLARIZED), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 3), GOLD.wireDense().count(64), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModIngredientItems.part(EnumPartType.GLASS_POLARIZED), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        this.register(new GenericRecipe("ass.silex").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_silex.get().asItem(), 1))
                .inputItems(new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16), STEEL.plateCast().count(8), DESH.ingot().count(4), RUBBER.ingot().count(8), STEEL.pipe().count(8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 2), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16), RUBBER.ingot().count(16)));
        this.register(new GenericRecipe("ass.excavator").setup(200, 100).outputItems(new ItemStack(ModProduction2Blocks.MACHINE_EXCAVATOR.get().asItem(), 1))
                .inputItems(new ComparableStack(Blocks.STONE_BRICKS.asItem(), 8), STEEL.ingot().count(8), IRON.ingot().count(8), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1)));
        this.register(new GenericRecipe("ass.drillsteel").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_steel.get(), 1))
                .inputItems(STEEL.ingot().count(12), W.ingot().count(4)));
        this.register(new GenericRecipe("ass.drillsteeldiamond").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_steel_diamond.get(), 1))
                .inputItems(new ComparableStack(ModToolItems.drillbit_steel.get(), 1), DIAMOND.dust().count(16)));
        this.register(new GenericRecipe("ass.drilldura").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_hss.get(), 1))
                .inputItems(DURA.ingot().count(12), ANY_PLASTIC.ingot().count(12), TI.ingot().count(8)));
        this.register(new GenericRecipe("ass.drillduradiamond").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_hss_diamond.get(), 1))
                .inputItems(new ComparableStack(ModToolItems.drillbit_hss.get(), 1), DIAMOND.dust().count(24)));
        this.register(new GenericRecipe("ass.drilldesh").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_desh.get(), 1))
                .inputItems(DESH.ingot().count(16), RUBBER.ingot().count(12), NB.ingot().count(4)));
        this.register(new GenericRecipe("ass.drilldeshdiamond").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_desh_diamond.get(), 1))
                .inputItems(new ComparableStack(ModToolItems.drillbit_desh.get(), 1), DIAMOND.dust().count(32)));
        this.register(new GenericRecipe("ass.drilltc").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_tcalloy.get(), 1))
                .inputItems(ANY_RESISTANTALLOY.ingot().count(20), DESH.ingot().count(12), RUBBER.ingot().count(8)));
        this.register(new GenericRecipe("ass.drilltcdiamond").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_tcalloy_diamond.get(), 1))
                .inputItems(new ComparableStack(ModToolItems.drillbit_tcalloy.get(), 1), DIAMOND.dust().count(48)));
        this.register(new GenericRecipe("ass.drillferro").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_ferro.get(), 1))
                .inputItems(FERRO.ingot().count(24), ANY_RESISTANTALLOY.ingot().count(12), BI.ingot().count(4)));
        this.register(new GenericRecipe("ass.drillferrodiamond").setup(100, 100).outputItems(new ItemStack(ModToolItems.drillbit_ferro_diamond.get(), 1))
                .inputItems(new ComparableStack(ModToolItems.drillbit_ferro.get(), 1), DIAMOND.dust().count(56)));
        this.register(new GenericRecipe("ass.slopper").setup(200, 100).outputItems(new ItemStack(ModProduction2Blocks.MACHINE_ORE_SLOPPER.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(6), TI.plate().count(8), CU.pipe().count(3), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1)));
        this.register(new GenericRecipe("ass.mininglaser").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_mining_laser.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), TI.shell().count(4), DURA.plate().count(4), new ComparableStack(ModItems.crystal_redstone.get(), 3), new ComparableStack(Items.DIAMOND, 3), ANY_PLASTIC.ingot().count(8), new ComparableStack(ModItems.motor.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), DURA.plate().count(4), new ComparableStack(ModItems.crystal_redstone.get(), 12), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModItems.motor_desh.get(), 3)));
        this.register(new GenericRecipe("ass.teleporter").setup(100, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.machine_teleporter.get().asItem(), 1))
                .inputItems(TI.plate().count(12), DURA.plate().count(12), GOLD.wire().count(32), new ComparableStack(ModItems.entanglement_kit.get(), 1), new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1)));
        this.register(new GenericRecipe("ass.radar").setup(300, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.machine_radar.get().asItem(), 1))
                .inputItems(STEEL.plate().count(12), ANY_RUBBER.ingot().count(12), new ComparableStack(ModItems.magnetron.get(), 5), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 8), new ComparableStack(ModItems.crt_display.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2), new ComparableStack(ModItems.crt_display.get(), 4)));
        this.register(new GenericRecipe("ass.radarlarge").setup(400, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.machine_radar_large.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(6), ANY_RESISTANTALLOY.ingot().count(4), ANY_RUBBER.ingot().count(24), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor_desh.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4), new ComparableStack(ModItems.crt_display.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), ANY_RESISTANTALLOY.ingot().count(8), ANY_RUBBER.ingot().count(24), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModItems.motor_desh.get(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 6), new ComparableStack(ModItems.crt_display.get(), 4)));
        this.register(new GenericRecipe("ass.forcefield").setup(600, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.machine_forcefield.get().asItem(), 1))
                .inputItems(DURA.plate().count(8), new ComparableStack(ModItems.plate_desh.get(), 4), new ComparableStack(ModItems.coil_gold_torus.get(), 6), new ComparableStack(ModItems.coil_magnetized_tungsten.get(), 12), new ComparableStack(ModItems.motor.get(), 1), new ComparableStack(ModRadarUtilityItems.upgrade_radius.get(), 1), new ComparableStack(ModRadarUtilityItems.upgrade_health.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4), new ComparableStack(ModBlocks.machine_transformer.get().asItem(), 1)));
        this.register(new GenericRecipe("ass.strandcaster").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_strand_caster.get(), 1))
                .inputItems(new ComparableStack(ModItems.ingot_firebrick.get(), 16), STEEL.plateCast().count(6), CU.plateWelded().count(2), STEEL.shell().count(2), new TagStack(ANY_CONCRETE).count(8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), new ComparableStack(ModItems.ingot_firebrick.get(), 16), STEEL.shell().count(4), new TagStack(ANY_CONCRETE).count(8)));
        this.register(new GenericRecipe("ass.assemfac").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_assembly_factory.get(), 1))
                .inputItems(DURA.ingot().count(16), ANY_RESISTANTALLOY.ingot().count(8), RUBBER.ingot().count(16), B.ingot().count(8), STEEL.shell().count(4), new ComparableStack(ModItems.motor.get(), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), ANY_RESISTANTALLOY.ingot().count(16), RUBBER.ingot().count(16), B.ingot().count(8), STEEL.shell().count(4), new ComparableStack(ModItems.motor.get(), 24), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4)));
        this.register(new GenericRecipe("ass.chemfac").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_chemical_factory.get(), 1))
                .inputItems(DURA.ingot().count(16), ANY_RESISTANTALLOY.ingot().count(8), RUBBER.ingot().count(16), STEEL.shell().count(12), CU.pipe().count(8), new ComparableStack(ModItems.motor_desh.get(), 4), new ComparableStack(ModItems.coil_tungsten.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), ANY_RESISTANTALLOY.ingot().count(16), RUBBER.ingot().count(16), STEEL.shell().count(12), CU.pipe().count(16), new ComparableStack(ModItems.motor_desh.get(), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4)));

        // generators
        this.register(new GenericRecipe("ass.dieselgen").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_diesel.get().asItem(), 1))
                .inputItems(STEEL.shell().count(1), CU.plateCast().count(2), new ComparableStack(ModItems.coil_copper.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1), CU.pipe().count(2), new ComparableStack(ModItems.coil_copper.get(), 4)));
        this.register(new GenericRecipe("ass.combustiongen").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.machine_combustion_engine.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), CU.ingot().count(12), GOLD.wireDense().count(8), new ComparableStack(ModItems.canister_empty.get(), 4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), GOLD.wireDense().count(16), new ComparableStack(ModItems.canister_empty.get(), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1)));
        this.register(new GenericRecipe("ass.pistonsetsteel").setup(200, 100).outputItems(new ItemStack(ModOilItems.piston(ItemPistons.Variant.STEEL), 1))
                .inputItems(STEEL.plate().count(16), CU.plate().count(4), W.ingot().count(8), W.bolt().count(16)));
        this.register(new GenericRecipe("ass.pistonsetdura").setup(200, 100).outputItems(new ItemStack(ModOilItems.piston(ItemPistons.Variant.DURA), 1))
                .inputItems(DURA.ingot().count(24), TI.plate().count(8), W.ingot().count(8), DURA.bolt().count(16)));
        this.register(new GenericRecipe("ass.pistonsetdesh").setup(200, 100).outputItems(new ItemStack(ModOilItems.piston(ItemPistons.Variant.DESH), 1))
                .inputItems(DESH.ingot().count(24), ANY_PLASTIC.ingot().count(12), CU.plate().count(24), W.ingot().count(16), DURA.pipe().count(4)));
        this.register(new GenericRecipe("ass.pistonsetstar").setup(200, 100).outputItems(new ItemStack(ModOilItems.piston(ItemPistons.Variant.STARMETAL), 1))
                .inputItems(STAR.ingot().count(24), RUBBER.ingot().count(16), BIGMT.plate().count(24), NB.ingot().count(16), DURA.pipe().count(4)));
        this.register(new GenericRecipe("ass.turbofan").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.machine_turbofan.get().asItem(), 1))
                .inputItems(TI.shell().count(8), DURA.pipe().count(4), ANY_PLASTIC.ingot().count(12), new ComparableStack(ModItems.turbine_tungsten.get(), 1), GOLD.wireDense().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 3), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModItems.turbine_tungsten.get(), 3), GOLD.wireDense().count(16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        this.register(new GenericRecipe("ass.gasturbine").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_turbine_gas.get().asItem(), 1))
                .inputItems(STEEL.shell().count(10), GOLD.wireDense().count(12), DURA.pipe().count(4),STEEL.pipe().count(4), new ComparableStack(ModItems.turbine_tungsten.get(), 1), RUBBER.ingot().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), GOLD.wireDense().count(16), DURA.pipe().count(16), new ComparableStack(ModItems.turbine_tungsten.get(), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        this.register(new GenericRecipe("ass.hephaestus").setup(200, 100).outputItems(new ItemStack(ModPowerGenBlocks.MACHINE_HEPHAESTUS.get().asItem(), 1))
                .inputItems(STEEL.pipe().count(12), STEEL.ingot().count(24), CU.plate().count(24), NB.ingot().count(4), RUBBER.ingot().count(12), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 8), NB.ingot().count(16), RUBBER.ingot().count(16), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16)));
        this.register(new GenericRecipe("ass.iturbine").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_industrial_turbine.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), RUBBER.ingot().count(4), new ComparableStack(ModItems.turbine_titanium.get(), 2), GOLD.wireDense().count(4), DURA.pipe().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), new ComparableStack(ModItems.turbine_titanium.get(), 3), GOLD.wireDense().count(16), DURA.pipe().count(16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        this.register(new GenericRecipe("ass.leviturbine").setup(600, 100).outputItems(new ItemStack(ModPowerGenBlocks.MACHINE_CHUNGUS.get().asItem(), 1))
                .inputItems(STEEL.shell().count(6), STEEL.plateWelded().count(16), TI.plate().count(12), ANY_RESISTANTALLOY.ingot().count(16), new ComparableStack(ModItems.turbine_tungsten.get(), 5), new ComparableStack(ModItems.turbine_titanium.get(), 3), new ComparableStack(ModItems.flywheel_beryllium.get(), 1), GOLD.wireDense().count(48), DURA.pipe().count(16), STEEL.pipe().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 8), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 4), new ComparableStack(ModItems.turbine_tungsten.get(), 5), new ComparableStack(ModItems.turbine_titanium.get(), 12), new ComparableStack(ModItems.flywheel_beryllium.get(), 1), GOLD.wireDense().count(64)));
        this.register(new GenericRecipe("ass.radgen").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.machine_radgen.get().asItem(), 1)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "radgen")
                .inputItems(STEEL.ingot().count(8), STEEL.plate().count(32), new ComparableStack(ModItems.coil_magnetized_tungsten.get(), 6), MAGTUNG.wire().count(24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16), new ComparableStack(ModItems.reactor_core.get(), 3), STAR.ingot().count(1), new TagStack(KEY_RED).count(1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4), new ComparableStack(ModItems.coil_magnetized_tungsten.get(), 16), new ComparableStack(ModItems.reactor_core.get(), 3), STAR.ingot().count(1), new TagStack(KEY_RED).count(1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));

        // condensers
        this.register(new GenericRecipe("ass.hpcondenser").setup(600, 100).outputItems(new ItemStack(ModMachineBlocks.machine_condenser_powered.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(8), ANY_RESISTANTALLOY.plateWelded().count(4), CU.plate().count(16), new ComparableStack(ModItems.motor_desh.get(), 3), STEEL.pipe().count(24), getDict(Fluids.LUBRICANT, 1_000).count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 4), new ComparableStack(ModItems.motor_desh.get(), 5), STEEL.pipe().count(24), getDict(Fluids.LUBRICANT, 1_000).count(16)));

        // batteries
        this.register(new GenericRecipe("ass.capacitorgold").setup(100, 100).outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_GOLD).get(), 1))
                .inputItems(STEEL.plate().count(8),
                        GOLD.wireDense().count(16)));
        this.register(new GenericRecipe("ass.capacitorniobium").setup(100, 1_000).outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_NIOBIUM).get(), 1))
                .inputItems(ANY_PLASTIC.ingot().count(12),
                        NB.wireDense().count(24)));
        this.register(new GenericRecipe("ass.capacitortantalum").setup(100, 10_000).outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_TANTALUM).get(), 1))
                .inputItems(ANY_HARDPLASTIC.ingot().count(16),
                        TA.ingot().count(24)));
        this.register(new GenericRecipe("ass.capacitorbismuth").setup(100, 25_000).outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_BISMUTH).get(), 1))
                .inputItems(ANY_HARDPLASTIC.ingot().count(24),
                        BI.ingot().count(24),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM), 1)));
        this.register(new GenericRecipe("ass.capacitorspark").setup(100, 100_000).outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_SPARK).get(), 1))
                .inputItems(CMB.plateCast().count(12),
                        new ComparableStack(ModItems.powder_spark_mix.get(), 32),
                        new ComparableStack(ModItems.pellet_charged.get(), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM), 16))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 8_000))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 8_000)));

        // fluid tanks
        this.register(new GenericRecipe("ass.tank").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_fluidtank.get().asItem(), 1))
                .inputItems(STEEL.plate().count(8), STEEL.shell().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4), new TagStack(ANY_TAR).count(16)));
        this.register(new GenericRecipe("ass.bigasstank").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_bigasstank.get().asItem(), 1))
                .inputItems(STEEL.plate().count(16), ANY_RESISTANTALLOY.plateWelded().count(4), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 6), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16), new TagStack(ANY_TAR).count(16)));
        this.register(new GenericRecipe("ass.orbus").setup(300, 100).outputItems(new ItemStack(ModRadarUtilityBlocks.machine_orbus.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(8), BIGMT.plateCast().count(4), BSCCO.wireDense().count(8), new ComparableStack(ModBatterySCItems.get(EnumBatterySC.PO210), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 8), BIGMT.plateCast().count(16), BSCCO.wireDense().count(16), new ComparableStack(ModBatterySCItems.get(EnumBatterySC.PO210), 1)));

        // accelerators
        this.register(new GenericRecipe("ass.cyclotron").setup(600, 100).outputItems(new ItemStack(ModMachineBlocks.machine_cyclotron.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1), ND.wireDense().count(32), STEEL.plateCast().count(16), ANY_PLASTIC.ingot().count(24), RUBBER.ingot().count(24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 8), ND.wireDense().count(32), AL.plateWelded().count(16), RUBBER.ingot().count(32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 8)));
        this.register(new GenericRecipe("ass.beamline").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.pa_beamline.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(8), CU.plate().count(16), GOLD.wireDense().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4), CU.plate().count(16), GOLD.wireDense().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.rfc").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.pa_rfc.get().asItem(), 1))
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), STEEL.plateCast().count(16), CU.plate().count(64), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModItems.magnetron.get(), 16))
                .inputItemsEx(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), CU.plate().count(64), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModItems.magnetron.get(), 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.quadrupole").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.pa_quadrupole.get().asItem(), 1))
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 1), STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .inputItemsEx(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.dipole").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.pa_dipole.get().asItem(), 1))
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 2), STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4))
                .inputItemsEx(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), ANY_HARDPLASTIC.ingot().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.source").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.pa_source.get().asItem(), 1))
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 1))
                .inputItemsEx(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModItems.magnetron.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.detector").setup(400, 100).outputItems(new ItemStack(ModMachineBlocks.pa_detector.get().asItem(), 1))
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), STEEL.plateCast().count(24), GOLD.wireDense().count(16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4))
                .inputItemsEx(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 32), GOLD.wireDense().count(64), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.pagold").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil_gold.get(), 1)).inputItems(GOLD.wireDense().count(64), GOLD.wireDense().count(64)));
        this.register(new GenericRecipe("ass.panbti").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil_niobium.get(), 1)).inputItems(NB.wireDense().count(64), TI.wireDense().count(64)));
        this.register(new GenericRecipe("ass.pabscco").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil_bscco.get(), 1)).inputItems(BSCCO.wireDense().count(64), ANY_PLASTIC.ingot().count(64)));
        this.register(new GenericRecipe("ass.pachlorophyte").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil_chlorophyte.get(), 1)).inputItems(CU.wireDense().count(64), CU.wireDense().count(64), new ComparableStack(ModItems.powder_chlorophyte.get(), 16)));
        this.register(new GenericRecipe("ass.exposurechamber").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_exposure_chamber.get().asItem(), 1))
                .inputItems(AL.plateCast().count(12), ANY_RESISTANTALLOY.ingot().count(4), ANY_HARDPLASTIC.ingot().count(12), GOLD.wireDense().count(32), new ComparableStack(ModItems.motor_desh.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_TANTALUM).get(), 1), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 8), ANY_HARDPLASTIC.ingot().count(24), GOLD.wireDense().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));

        // pile
        this.register(new GenericRecipe("ass.pileblock").setup(20, 250).outputItems(new ItemStack(ModPileBlocks.pile_brick.get().asItem(), 1))
                .inputItems(new TagStack(KEY_PLANKS).count(1), GRAPHITE.ingot().count(4), STEEL.bolt().count(2))
                .inputItemsEx(new TagStack(KEY_PLANKS).count(1), GRAPHITE.ingot().count(8), STEEL.plate().count(1)));

        // reactors
        this.register(new GenericRecipe("ass.cirnox").setup(600, 100).outputItems(new ItemStack(ModMachineBlocks.machine_zirnox.get().asItem(), 1))
                .inputItems(STEEL.shell().count(4), STEEL.pipe().count(8), B.ingot().count(8), GRAPHITE.ingot().count(16), RUBBER.ingot().count(16), new TagStack(ANY_CONCRETE).count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), GRAPHITE.ingot().count(16), RUBBER.ingot().count(16), new TagStack(ANY_CONCRETE).count(16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2)));
        this.register(new GenericRecipe("ass.rbmk").setup(100, 100).outputItems(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBlocks.concrete_asbestos.get().asItem(), 4), STEEL.plateCast().count(2), CU.plate().count(4), RUBBER.ingot().count(2))
                .inputItemsEx(new ComparableStack(ModBlocks.concrete_asbestos.get().asItem(), 4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), CU.plate().count(16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "ferrouranium"));
        this.register(new GenericRecipe("ass.rbmkautoloader").setup(100, 100).outputItems(new ItemStack(ModRBMKBlocks.rbmk_autoloader.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(4), PB.plateCast().count(4), B.ingot().count(4), new ComparableStack(ModItems.motor.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1), new ComparableStack(ModItems.motor_desh.get(), 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "ferrouranium"));

        // PWR
        this.register(new GenericRecipe("ass.pwrfuel").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_FUEL.get().asItem(), 4))
                .inputItems(PB.plateCast().count(4), ZR.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), ZR.plateWelded().count(2)));
        this.register(new GenericRecipe("ass.pwrcontrol").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CONTROL.get().asItem(), 4))
                .inputItems(STEEL.plateCast().count(2), B.ingot().count(4), new ComparableStack(ModItems.motor.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1), B.ingot().count(4), new ComparableStack(ModItems.motor.get(), 1)));
        this.register(new GenericRecipe("ass.pwrchannel").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CHANNEL.get().asItem(), 4))
                .inputItems(STEEL.pipe().count(4), CU.plate().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), STEEL.pipe().count(4)));
        this.register(new GenericRecipe("ass.pwrheatex").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_HEATEX.get().asItem(), 4))
                .inputItems(CU.plateCast().count(4), new ComparableStack(ModItems.motor.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), CU.plateCast().count(4))
                .inputFluidsEx(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        this.register(new GenericRecipe("ass.pwrheatsink").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_HEATSINK.get().asItem(), 4))
                .inputItems(BIGMT.plateCast().count(4), CU.plateCast().count(4))
                .inputItemsEx(BIGMT.plateCast().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1)));
        this.register(new GenericRecipe("ass.pwrreflector").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_REFLECTOR.get().asItem(), 4))
                .inputItems(STEEL.plateCast().count(2), new ComparableStack(ModItems.neutron_reflector.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1), new ComparableStack(ModItems.neutron_reflector.get(), 16)));
        this.register(new GenericRecipe("ass.pwrreflectoralt").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_REFLECTOR.get().asItem(), 4))
                .inputItems(STEEL.plateCast().count(2), WEAPONSTEEL.plate().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 1), WEAPONSTEEL.plate().count(32)));
        this.register(new GenericRecipe("ass.pwrcasing").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CASING.get().asItem(), 4))
                .inputItems(PB.plate().count(4), new TagStack(ANY_CONCRETE).count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), new TagStack(ANY_CONCRETE).count(4)));
        this.register(new GenericRecipe("ass.pwrcontroller").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CONTROLLER.get().asItem(), 1))
                .inputItems(PB.plateCast().count(4), RUBBER.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        this.register(new GenericRecipe("ass.pwrport").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_PORT.get().asItem(), 4))
                .inputItems(PB.plate().count(4), new TagStack(ANY_CONCRETE).count(4), STEEL.pipe().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), new TagStack(ANY_CONCRETE).count(4), STEEL.pipe().count(4)));
        this.register(new GenericRecipe("ass.pwrneutronsource").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_NEUTRON_SOURCE.get().asItem(), 1))
                .inputItems(ZR.plateWelded().count(1), new ComparableStack(ModItems.billet_ra226be.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 1), ZR.plateWelded().count(1), new ComparableStack(ModItems.billet_ra226be.get(), 3)));

        // fusion reactor
        this.register(new GenericRecipe("ass.fusioncore").setup(600, 100).outputItems(new ItemStack(ModMachineBlocks.struct_torus_core.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(8), ANY_HARDPLASTIC.ingot().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 8), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionbscco").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_component.get().asItem(), 2))
                .inputItems(BSCCO.wireDense().count(1), CU.pipe().count(1), ANY_RESISTANTALLOY.ingot().count(1), ANY_PLASTIC.ingot().count(4))
                .inputItemsEx(BSCCO.wireDense().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionblanket").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_component_blanket.get().asItem(), 4))
                .inputItems(W.plateWelded().count(1), STEEL.plateWelded().count(2), BE.ingot().count(4))
                .inputItemsEx(W.plateWelded().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 1), BE.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionpipes").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_component_motor.get().asItem(), 4))
                .inputItems(ANY_HARDPLASTIC.ingot().count(4), CU.pipe().count(2), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 1), CU.pipe().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));

        this.register(new GenericRecipe("ass.fusionklystron").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_klystron.get().asItem(), 1))
                .inputItems(W.plateWelded().count(4), ANY_RESISTANTALLOY.plateCast().count(16), CU.plate().count(32), ANY_HARDPLASTIC.ingot().count(16), BSCCO.wireDense().count(8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 2))
                .inputItemsEx(BSCCO.wireDense().count(32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 8), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 1)));
        this.register(new GenericRecipe("ass.fusioncollector").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_collector.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateCast().count(4), STEEL.plate().count(16), GRAPHITE.ingot().count(16), ANY_HARDPLASTIC.ingot().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 2), GRAPHITE.ingot().count(64), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 4)));
        this.register(new GenericRecipe("ass.fusionbreeder").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_breeder.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateCast().count(4), STEEL.pipe().count(4), B.ingot().count(16), ANY_HARDPLASTIC.ingot().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 4), B.ingot().count(64), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 4)));
        this.register(new GenericRecipe("ass.fusionboiler").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_boiler.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateCast().count(16), CU.shell().count(16), STEEL.pipe().count(8), ANY_HARDPLASTIC.ingot().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 8), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 8), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 16)));
        this.register(new GenericRecipe("ass.fusionmhdt").setup(1_200, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_mhdt.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(16), CU.plateWelded().count(64), ANY_BISMOIDBRONZE.plateCast().count(16), SBD.wireDense().count(64), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 16), ANY_BISMOIDBRONZE.plateCast().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register(new GenericRecipe("ass.fusioncoupler").setup(300, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_coupler.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(4), CU.plate().count(32), BSCCO.wireDense().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 4), BSCCO.wireDense().count(16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 2)));
        this.register(new GenericRecipe("ass.fusionplasmaforge").setup(1_200, 100).outputItems(new ItemStack(ModMachineBlocks.fusion_plasma_forge.get().asItem(), 1))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(8), BSCCO.wireDense().count(32), ANY_BISMOIDBRONZE.plateCast().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 16), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 16), BSCCO.wireDense().count(32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));

        // watz
        this.register(new GenericRecipe("ass.watzrod").setup(200, 100).outputItems(new ItemStack(ModBlocks.watz_element.get().asItem(), 3))
                .inputItems(STEEL.plateCast().count(2), ZR.ingot().count(2), BIGMT.ingot().count(2), ANY_HARDPLASTIC.ingot().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 3), BIGMT.ingot().count(6), ANY_HARDPLASTIC.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.watzcooler").setup(200, 100).outputItems(new ItemStack(ModBlocks.watz_cooler.get().asItem(), 3))
                .inputItems(STEEL.plateCast().count(2), CU.plateCast().count(4), RUBBER.ingot().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 3), RUBBER.ingot().count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.watzcasing").setup(100, 100).outputItems(new ItemStack(ModMachineBlocks.watz_casing.get().asItem(), 3))
                .inputItems(ANY_RESISTANTALLOY.plateWelded(), B.ingot().count(3), STEEL.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 6), ANY_RESISTANTALLOY.plateWelded())
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));

        // upgrades
        // 1.7: if(no528) {
        branch = NO_528;
        {
            this.register(new GenericRecipe("ass.overdrive1").setup(200, 100).outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_1.get(), 1))
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1), new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1), BIGMT.ingot().count(16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 16)));
            this.register(new GenericRecipe("ass.overdrive2").setup(600, 100).outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_2.get(), 1))
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get(), 1), new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1), new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1), BIGMT.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 16)));
            this.register(new GenericRecipe("ass.overdrive3").setup(1_200, 100).outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_3.get(), 1))
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_2.get(), 1), new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1), new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1), ANY_BISMOIDBRONZE.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16)));
        }
        branch = null;

        // rancid shit mob spawners
        this.register(new GenericRecipe("ass.chopper").setup(1_200, 100).outputItems(new ItemStack(ModEntityItems.spawn_chopper.get(), 1))
                .inputItems(CMB.plateCast().count(24), STEEL.plate().count(32), MAGTUNG.wire().count(48), new ComparableStack(ModItems.motor_desh.get(), 5), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1)));
        this.register(new GenericRecipe("ass.ballsotron").setup(1_200, 100).outputItems(new ItemStack(ModEntityItems.spawn_worm.get(), 1))
                .inputItems(TI.plateWelded().count(32), RUBBER.ingot().count(64), new ComparableStack(ModItems.motor.get(), 64), GOLD.wireDense().count(64), U238.block().count(10), new ComparableStack(ModItems.mech_key.get(), 1)));

        // weapon parts
        this.register(new GenericRecipe("ass.clusterpellets").setup(50, 100).outputItems(new ItemStack(ModItems.pellet_cluster.get(), 1))
                .inputItems(STEEL.plate().count(4), ANY_HIGHEXPLOSIVE.ingot().count(1)));
        this.register(new GenericRecipe("ass.buckshot").setup(50, 100).outputItems(new ItemStack(ModItems.pellet_buckshot.get(), 1))
                .inputItems(PB.nugget().count(6)));

        // bombs
        this.register(new GenericRecipe("ass.minenaval").setup(300, 100).outputItems(new ItemStack(ModExplosivesBlocks.mine_naval.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), STEEL.pipe().count(3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), ANY_PLASTICEXPLOSIVE.ingot().count(24)));
        this.register(new GenericRecipe("ass.gadget").setup(300, 100).outputItems(new ItemStack(ModBombBlocks.nuke_gadget.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), new ComparableStack(ModItems.fins_flat.get(), 2), new ComparableStack(ModItems.pedestal_steel.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3), new TagStack(KEY_GRAY).count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.littleboy").setup(300, 100).outputItems(new ItemStack(ModBombBlocks.nuke_boy.get().asItem(), 1))
                .inputItems(STEEL.shell().count(2), new ComparableStack(ModItems.fins_small_steel.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 2), new TagStack(KEY_BLUE).count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.fatman").setup(300, 100).outputItems(new ItemStack(ModBombBlocks.nuke_man.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), STEEL.shell().count(2), new ComparableStack(ModItems.fins_big_steel.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3), new TagStack(KEY_YELLOW).count(6))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.ivymike").setup(600, 100).outputItems(new ItemStack(ModBombBlocks.nuke_mike.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), AL.shell().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8), new TagStack(KEY_LIGHTGRAY).count(16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.tsarbomba").setup(1_200, 100).outputItems(new ItemStack(ModBombBlocks.nuke_tsar.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), TI.shell().count(6), STEEL.shell().count(2), new ComparableStack(ModItems.fins_tri_steel.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 16), new TagStack(KEY_BLACK).count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.ninadidnothingwrong").setup(300, 100).outputItems(new ItemStack(ModBombs2Blocks.nuke_prototype.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.dysfunctional_reactor.get(), 1), STEEL.shell().count(2), new ComparableStack(ModItems.ingot_euphemium.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.fleija").setup(300, 100).outputItems(new ItemStack(ModBombs2Blocks.nuke_fleija.get().asItem(), 1))
                .inputItems(AL.shell().count(1), new ComparableStack(ModItems.fins_quad_titanium.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1), new TagStack(KEY_WHITE).count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.solinium").setup(300, 100).outputItems(new ItemStack(ModBombs2Blocks.nuke_solinium.get().asItem(), 1))
                .inputItems(STEEL.shell().count(2), new ComparableStack(ModItems.fins_quad_titanium.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1), new TagStack(KEY_GRAY).count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.n2mine").setup(200, 100).outputItems(new ItemStack(ModBombBlocks.nuke_n2.get().asItem(), 1))
                .inputItems(STEEL.shell().count(6), MAGTUNG.wire().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 2), new TagStack(KEY_GREEN).count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.balefirebomb").setup(400, 100).outputItems(new ItemStack(ModBombs2Blocks.nuke_fstbmb.get().asItem(), 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel.get(), 1), TI.shell().count(6), new ComparableStack(ModItems.fins_big_steel.get(), 1), new ComparableStack(ModItems.powder_magic.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 4), new TagStack(KEY_GRAY).count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.customnuke").setup(300, 100).outputItems(new ItemStack(ModBombs2Blocks.nuke_custom.get().asItem(), 1))
                .inputItems(STEEL.shell().count(2), new ComparableStack(ModItems.fins_small_steel.get(), 1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8), new TagStack(KEY_GRAY).count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.levibomb").setup(200, 100).outputItems(new ItemStack(ModBombs2Blocks.float_bomb.get().asItem(), 1))
                .inputItems(TI.plate().count(12), SA326.nugget().count(3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4), GOLD.wireDense().count(8)));
        this.register(new GenericRecipe("ass.endobomb").setup(200, 100).outputItems(new ItemStack(ModBombs2Blocks.therm_endo.get().asItem(), 1))
                .inputItems(TI.plate().count(12), new ComparableStack(ModItems.powder_ice.get(), 32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), new ComparableStack(ModItems.coil_gold.get(), 4)));
        this.register(new GenericRecipe("ass.exobomb").setup(200, 100).outputItems(new ItemStack(ModBombs2Blocks.therm_exo.get().asItem(), 1))
                .inputItems(TI.plate().count(12), P_RED.dust().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), new ComparableStack(ModItems.coil_gold.get(), 4)));

        // bomb parts
        this.register(new GenericRecipe("ass.explosivelenses1").setup(400, 100).outputItems(new ItemStack(ModBombItems.early_explosive_lenses.get(), 1))
                .inputItems(AL.plate().count(8), new ComparableStack(ModExplosivesBlocks.det_cord.get().asItem(), 8), BIGMT.plate().count(2), ANY_HIGHEXPLOSIVE.ingot().count(20), ANY_PLASTIC.ingot().count(4)));
        this.register(new GenericRecipe("ass.explosivelenses2").setup(400, 100).outputItems(new ItemStack(ModBombItems.explosive_lenses.get(), 1))
                .inputItems(AL.plate().count(8), ANY_PLASTICEXPLOSIVE.ingot().count(4), new ComparableStack(ModItems.neutron_reflector.get(), 2), new ComparableStack(ModItems.ball_tatb.get(), 16), RUBBER.ingot().count(2)));
        this.register(new GenericRecipe("ass.wiring").setup(200, 100).outputItems(new ItemStack(ModBombItems.gadget_wireing.get(), 1))
                .inputItems(GOLD.wire().count(24)));
        this.register(new GenericRecipe("ass.core1").setup(1_200, 100).outputItems(new ItemStack(ModBombItems.gadget_core.get(), 1))
                .inputItems(PU239.nugget().count(7), U238.nugget().count(3)));
        this.register(new GenericRecipe("ass.boyshield").setup(200, 100).outputItems(new ItemStack(ModBombItems.boy_shielding.get(), 1))
                .inputItems(new ComparableStack(ModItems.neutron_reflector.get(), 12), STEEL.plate().count(4)));
        this.register(new GenericRecipe("ass.boytarget").setup(200, 100).outputItems(new ItemStack(ModBombItems.boy_target.get(), 1))
                .inputItems(U235.nugget().count(18)));
        this.register(new GenericRecipe("ass.boybullet").setup(200, 100).outputItems(new ItemStack(ModBombItems.boy_bullet.get(), 1))
                .inputItems(U235.nugget().count(9)));
        this.register(new GenericRecipe("ass.boypropellant").setup(200, 100).outputItems(new ItemStack(ModBombItems.boy_propellant.get(), 1))
                .inputItems(new ComparableStack(ModItems.cordite.get(), 8), IRON.plate().count(8), AL.plate().count(4), MINGRADE.wire().count(4)));
        this.register(new GenericRecipe("ass.boyigniter").setup(200, 100).outputItems(new ItemStack(ModBombItems.boy_igniter.get(), 1))
                .inputItems(AL.shell().count(3), DURA.plateCast().count(1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), MINGRADE.wire().count(16)));
        this.register(new GenericRecipe("ass.manigniter").setup(200, 100).outputItems(new ItemStack(ModBombItems.man_igniter.get(), 1))
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 3), GOLD.wire().count(24)));
        this.register(new GenericRecipe("ass.mancore").setup(1_200, 100).outputItems(new ItemStack(ModBombItems.man_core.get(), 1))
                .inputItems(PU239.nugget().count(8), BE.nugget().count(2)));
        this.register(new GenericRecipe("ass.mikecore").setup(1_200, 100).outputItems(new ItemStack(ModBombItems.mike_core.get(), 1))
                .inputItems(U238.nugget().count(24), PB.plate().count(6)));
        this.register(new GenericRecipe("ass.mikedeut").setup(600, 100).outputItems(new ItemStack(ModBombItems.mike_deut.get(), 1))
                .inputItems(WEAPONSTEEL.plate().count(16), TI.plate().count(16))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10_000)));
        this.register(new GenericRecipe("ass.mikecooler").setup(300, 100).outputItems(new ItemStack(ModBombItems.mike_cooling_unit.get(), 1))
                .inputItems(DURA.plate().count(8), new ComparableStack(ModItems.coil_copper.get(), 5), new ComparableStack(ModItems.coil_tungsten.get(), 5), new ComparableStack(ModItems.motor.get(), 2)));
        this.register(new GenericRecipe("ass.fleijaigniter").setup(200, 100).outputItems(new ItemStack(ModBombs2Items.fleija_igniter.get(), 1))
                .inputItems(TI.plate().count(6), SA326.wire().count(2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1)));
        this.register(new GenericRecipe("ass.fleijacore").setup(600, 100).outputItems(new ItemStack(ModBombs2Items.fleija_core.get(), 1))
                .inputItems(U235.nugget().count(8), NP237.nugget().count(2), BE.nugget().count(4), new ComparableStack(ModItems.coil_copper.get(), 2)));
        this.register(new GenericRecipe("ass.fleijacharge").setup(300, 100).outputItems(new ItemStack(ModBombs2Items.fleija_propellant.get(), 1))
                .inputItems(ANY_HIGHEXPLOSIVE.ingot().count(3), SA326.plate().count(8)));
        this.register(new GenericRecipe("ass.soliniumigniter").setup(200, 100).outputItems(new ItemStack(ModBombs2Items.solinium_igniter.get(), 1))
                .inputItems(TI.plate().count(4), MINGRADE.wire().count(8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), new ComparableStack(ModItems.coil_gold.get(), 1)));
        this.register(new GenericRecipe("ass.soliniumcore").setup(600, 100).outputItems(new ItemStack(ModBombs2Items.solinium_core.get(), 1))
                .inputItems(SA327.nugget().count(9), EUPH.nugget().count(1)));
        this.register(new GenericRecipe("ass.soliniumcharge").setup(300, 100).outputItems(new ItemStack(ModBombs2Items.solinium_propellant.get(), 1))
                .inputItems(ANY_HIGHEXPLOSIVE.ingot().count(3), new ComparableStack(ModItems.neutron_reflector.get(), 2), new ComparableStack(ModItems.plate_polymer.get(), 6), W.wire().count(6), new ComparableStack(ModFuelItems.biomass_compressed.get(), 4)));

        // turrets
        this.register(new GenericRecipe("ass.turretchekhov").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_chekhov.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), STEEL.pipe().count(3), mechanism(GUNMETAL).count(3), new ComparableStack(ModStorageBlocks.crate_iron.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretfriendly").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_friendly.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1), STEEL.pipe().count(3), mechanism(GUNMETAL).count(1), new ComparableStack(ModStorageBlocks.crate_iron.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretjeremy").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_jeremy.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(16), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), new ComparableStack(ModItems.motor_desh.get(), 1), STEEL.shell().count(3), mechanism(WEAPONSTEEL).count(3), new ComparableStack(ModStorageBlocks.crate_steel.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turrettauon").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_tauon.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_NIOBIUM).get(), 1), STEEL.ingot().count(16), ANY_PLASTIC.ingot().count(4), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), new ComparableStack(ModItems.motor_desh.get(), 1), CU.ingot().count(32), mechanism(BIGMT).count(3), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretrichard").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_richard.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(16), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), ANY_PLASTIC.ingot().count(2), STEEL.shell().count(8), mechanism(WEAPONSTEEL).count(3), new ComparableStack(ModStorageBlocks.crate_steel.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turrethoward").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_howard.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(24), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModItems.motor_desh.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 3), STEEL.pipe().count(10), mechanism(WEAPONSTEEL).count(3), new ComparableStack(ModStorageBlocks.crate_steel.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.maxwell").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_maxwell.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_NIOBIUM).get(), 1), STEEL.ingot().count(24), new ComparableStack(ModItems.motor.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 2), STEEL.pipe().count(4), mechanism(BIGMT).count(3), new ComparableStack(ModItems.magnetron.get(), 16), ANY_RESISTANTALLOY.ingot().count(8), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.fritz").setup(200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_fritz.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1), STEEL.ingot().count(16), new ComparableStack(ModItems.motor.get(), 3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1), STEEL.pipe().count(8), mechanism(GUNMETAL).count(3), new ComparableStack(ModFluidNetBlocks.barrel_steel.get().asItem()))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.arty").setup(1_200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_arty.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1), STEEL.ingot().count(64), STEEL.ingot().count(64), new ComparableStack(ModItems.motor_desh.get(), 5), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 3), STEEL.pipe().count(12), mechanism(WEAPONSTEEL).count(16), new ComparableStack(ModRadarUtilityBlocks.machine_radar.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "arty"));
        this.register(new GenericRecipe("ass.himars").setup(1_200, 100).outputItems(new ItemStack(ModTurretBlocks.turret_himars.get().asItem(), 1))
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1), STEEL.ingot().count(64), STEEL.ingot().count(64), ANY_PLASTIC.ingot().count(64), new ComparableStack(ModItems.motor_desh.get(), 5), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 8), mechanism(BIGMT).count(8), new ComparableStack(ModRadarUtilityBlocks.machine_radar.get().asItem(), 1), new ComparableStack(ModItems.crt_display.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "arty"));
        this.register(new GenericRecipe("ass.himarssmall").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL), 1))
                .inputItems(STEEL.plate().count(24), ANY_PLASTIC.ingot().count(12), new ComparableStack(ModFuelItems.rocket_fuel.get(), 48), ANY_HIGHEXPLOSIVE.ingot().count(48), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarssmallhe").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL_HE), 1))
                .inputItems(STEEL.plate().count(24), ANY_PLASTIC.ingot().count(24), new ComparableStack(ModFuelItems.rocket_fuel.get(), 48), ANY_PLASTICEXPLOSIVE.ingot().count(18), ANY_HIGHEXPLOSIVE.ingot().count(48), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarssmallwp").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL_WP), 1))
                .inputItems(STEEL.plate().count(24), ANY_PLASTIC.ingot().count(24), new ComparableStack(ModFuelItems.rocket_fuel.get(), 48), P_WHITE.ingot().count(18), ANY_HIGHEXPLOSIVE.ingot().count(48), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarssmalltb").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL_TB), 1))
                .inputItems(STEEL.plate().count(24), ANY_PLASTIC.ingot().count(24), new ComparableStack(ModFuelItems.rocket_fuel.get(), 48), new ComparableStack(ModItems.ball_tatb.get(), 32), getDict(Fluids.KEROSENE_REFORM, 1_000).count(12), getDict(Fluids.PEROXIDE, 1_000).count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarssmallnuke").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL_MINI_NUKE), 1))
                .inputItems(STEEL.plate().count(24), ANY_PLASTIC.ingot().count(24), new ComparableStack(ModFuelItems.rocket_fuel.get(), 48), new ComparableStack(ModItems.ball_tatb.get(), 6), PU239.nugget().count(12), new ComparableStack(ModItems.neutron_reflector.get(), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarssmalllava").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.SMALL_LAVA), 1))
                .inputItems(STEEL.plate().count(24), ANY_HARDPLASTIC.ingot().count(12), new ComparableStack(ModFuelItems.rocket_fuel.get(), 32), new ComparableStack(ModItems.ball_tatb.get(), 4), VOLCANIC.gem().count(1), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 6)));
        this.register(new GenericRecipe("ass.himarslarge").setup(200, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.LARGE), 1))
                .inputItems(STEEL.plate().count(24), ANY_HARDPLASTIC.ingot().count(12), new ComparableStack(ModFuelItems.rocket_fuel.get(), 36), new ComparableStack(ModItems.ball_tatb.get(), 16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1)));
        this.register(new GenericRecipe("ass.himarslargetb").setup(200, 100).outputItems(new ItemStack(ModAmmoItems.ammo_himars.get(HIMARSRocketType.LARGE_TB), 1))
                .inputItems(STEEL.plate().count(24), ANY_HARDPLASTIC.ingot().count(12), new ComparableStack(ModFuelItems.rocket_fuel.get(), 36), new ComparableStack(ModItems.ball_tatb.get(), 24), getDict(Fluids.KEROSENE_REFORM, 1_000).count(16), getDict(Fluids.PEROXIDE, 1_000).count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1)));

        // missile parts
        this.register(new GenericRecipe("ass.missileassembly").setup(200, 100).outputItems(new ItemStack(ModItems.missile_assembly.get(), 1))
                .inputItems(AL.shell().count(2), TI.shell().count(4), ANY_PLASTIC.ingot().count(8), new ComparableStack(ModFuelItems.rocket_fuel.get(), 8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1)));
        this.register(new GenericRecipe("ass.warheadhe1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_generic_small.get(), 1))
                .inputItems(TI.plate().count(4), new ComparableStack(ModItems.ball_dynamite.get(), 2), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 1)));
        this.register(new GenericRecipe("ass.warheadhe2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_generic_medium.get(), 1))
                .inputItems(TI.plate().count(8), ANY_HIGHEXPLOSIVE.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1)));
        this.register(new GenericRecipe("ass.warheadhe3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_generic_large.get(), 1))
                .inputItems(TI.plate().count(16), ANY_HIGHEXPLOSIVE.ingot().count(8), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1)));
        this.register(new GenericRecipe("ass.warheadinc1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_small.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small.get(), 1), P_RED.dust().count(2)));
        this.register(new GenericRecipe("ass.warheadinc2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_medium.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium.get(), 1), P_RED.dust().count(4)));
        this.register(new GenericRecipe("ass.warheadinc3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_large.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large.get(), 1), P_RED.dust().count(8)));
        this.register(new GenericRecipe("ass.warheadcl1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_cluster_small.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small.get(), 1), new ComparableStack(ModItems.pellet_cluster.get(), 2)));
        this.register(new GenericRecipe("ass.warheadcl2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_cluster_medium.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium.get(), 1), new ComparableStack(ModItems.pellet_cluster.get(), 4)));
        this.register(new GenericRecipe("ass.warheadcl3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_cluster_large.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large.get(), 1), new ComparableStack(ModItems.pellet_cluster.get(), 8)));
        this.register(new GenericRecipe("ass.warheadbb1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_buster_small.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small.get(), 1), ANY_HIGHEXPLOSIVE.ingot().count(2)));
        this.register(new GenericRecipe("ass.warheadbb2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_buster_medium.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium.get(), 1), ANY_HIGHEXPLOSIVE.ingot().count(4)));
        this.register(new GenericRecipe("ass.warheadbb3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_buster_large.get(), 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large.get(), 1), ANY_HIGHEXPLOSIVE.ingot().count(8)));
        this.register(new GenericRecipe("ass.warheadnuke").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_nuclear.get(), 1))
                .inputItems(TI.plateCast().count(12), PB.plateCast().count(6), U235.billet().count(6), new ComparableStack(ModItems.cordite.get(), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.warheadthermonuke").setup(600, 100).outputItems(new ItemStack(ModItems.warhead_mirv.get(), 1))
                .inputItems(TI.plateCast().count(12), PB.plateCast().count(6), PU239.billet().count(8), new ComparableStack(ModItems.ball_tatb.get(), 12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 2))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 4_000))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.warheadvolcano").setup(600, 100).outputItems(new ItemStack(ModItems.warhead_volcano.get(), 1))
                .inputItems(TI.plateCast().count(12), STEEL.plateCast().count(6), new ComparableStack(ModExplosivesBlocks.det_nuke.get().asItem(), 3), U238.block().count(24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 5)));
        this.register(new GenericRecipe("ass.thrusternerva").setup(600, 100).outputItems(new ItemStack(ModItems.thruster_nuclear.get(), 1))
                .inputItems(DURA.ingot().count(32), B.ingot().count(8), PB.plate().count(16), STEEL.pipe().count(4)));
        this.register(new GenericRecipe("ass.stealthmissile").setup(1_200, 100).outputItems(new ItemStack(ModMissileItems.missile_stealth.get(), 1))
                .inputItems(TI.plate().count(20), AL.plate().count(20), new TagStack(KEY_BLACK).count(16), ANY_HARDPLASTIC.ingot().count(16), ANY_HIGHEXPLOSIVE.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4), STEEL.bolt().count(32)));
        this.register(new GenericRecipe("ass.shuttlemissile").setup(200, 100).outputItems(new ItemStack(ModMissileItems.missile_shuttle.get(), 1))
                .inputItems(new ComparableStack(ModMissileItems.missile_generic.get(), 1), new ComparableStack(ModMissileItems.missile_strong.get(), 1), new TagStack(KEY_ORANGE).count(5), new ComparableStack(canisterFull(Fluids.GASOLINE_LEADED)).count(24), FIBER.ingot().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3), ANY_PLASTICEXPLOSIVE.ingot().count(8), new TagStack(KEY_ANYPANE).count(6), STEEL.plate().count(4)));
        this.register(new GenericRecipe("ass.launchpad").setup(200, 100).outputItems(new ItemStack(ModCustomMissileBlocks.launch_pad_large.get().asItem(), 1))
                .inputItems(STEEL.plateCast().count(6), new TagStack(ANY_CONCRETE).count(64), ANY_PLASTIC.ingot().count(16), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 2)));
        this.register(new GenericRecipe("ass.launchpadsilo").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.launch_pad.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(8), new TagStack(ANY_CONCRETE).count(8), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4)));

        // custom missile thrusters
        this.register(new GenericRecipe("ass.mpt10kero").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), STEEL.pipe().count(1), W.ingot().count(4), STEEL.plate().count(4)));
        this.register(new GenericRecipe("ass.mpt10solid").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), DURA.pipe().count(1), W.ingot().count(4), STEEL.plate().count(4)));
        this.register(new GenericRecipe("ass.mpt10xenon").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_xenon.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), DURA.plate().count(4), new ComparableStack(ModMachineItems.arc_electrode.get(), 1)));
        this.register(new GenericRecipe("ass.mpt15kero").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15kerodual").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene_dual.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15kerotriple").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene_triple.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), STEEL.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15solid").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15solid16").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_solid_hexdecuple.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(3), W.ingot().count(8), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15hydro").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_hydrogen.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(3), W.ingot().count(8), DESH.ingot().count(4)));
        this.register(new GenericRecipe("ass.mpt15hydrodual").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_hydrogen_dual.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(3), W.ingot().count(8), DESH.ingot().count(4)));
        this.register(new GenericRecipe("ass.mpt15bfshort").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire_short.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(5), W.plateCast().count(8), BIGMT.plate().count(8)));
        this.register(new GenericRecipe("ass.mpt15bf").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire_short.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(5), W.plateCast().count(16), BIGMT.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt15bflarge").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire_large.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), DURA.pipe().count(10), W.plateCast().count(16), BIGMT.plate().count(24)));
        this.register(new GenericRecipe("ass.mpt20kero").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt20kerodual").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene_dual.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt20kerotriple").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene_triple.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), STEEL.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt20solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt20solidmulti").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid_multi.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpt20solidmultier").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid_multier.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_20.get(), 1), DURA.pipe().count(6), W.ingot().count(16), STEEL.plate().count(16)));

        // custom missile fuselages
        this.register(new GenericRecipe("ass.mpf10kero").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 2), AL.plate().count(12), STEEL.plate().count(3)));
        this.register(new GenericRecipe("ass.mpf10kerolong").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 2), AL.plate().count(16), STEEL.plate().count(6)));
        this.register(new GenericRecipe("ass.mpf10solid").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 2), TI.plate().count(12), STEEL.plate().count(3)));
        this.register(new GenericRecipe("ass.mpf10solidlong").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 2), TI.plate().count(16), STEEL.plate().count(6)));
        this.register(new GenericRecipe("ass.mpf10xenon").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_xenon.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 2), CU.plate().count(12), STEEL.plate().count(3)));
        this.register(new GenericRecipe("ass.mpf1015kero").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), new ComparableStack(ModItems.seg_15.get(), 1), AL.plate().count(24), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpf1015solid").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), new ComparableStack(ModItems.seg_15.get(), 1), TI.plate().count(24), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpf1015hydro").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_hydrogen.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), new ComparableStack(ModItems.seg_15.get(), 1), CU.plate().count(24), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpf1015bf").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_balefire.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), new ComparableStack(ModItems.seg_15.get(), 1), BIGMT.plate().count(24), STEEL.plate().count(8)));
        this.register(new GenericRecipe("ass.mpf15kero").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 2), AL.plate().count(32), STEEL.plate().count(12)));
        this.register(new GenericRecipe("ass.mpf15solid").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 2), TI.plate().count(32), STEEL.plate().count(12)));
        this.register(new GenericRecipe("ass.mpf15hydro").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_hydrogen.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 2), CU.plate().count(32), STEEL.plate().count(12)));
        this.register(new GenericRecipe("ass.mpf1520kero").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_20_kerosene.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), new ComparableStack(ModItems.seg_20.get(), 1), AL.plate().count(48), STEEL.plate().count(16)));
        this.register(new GenericRecipe("ass.mpf1520solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_20_solid.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), new ComparableStack(ModItems.seg_20.get(), 1), TI.plate().count(48), STEEL.plate().count(16)));

        // custom missile warheads
        this.register(new GenericRecipe("ass.mpw10he").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_he.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), STEEL.plate().count(6), ANY_HIGHEXPLOSIVE.ingot().count(3), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1)));
        this.register(new GenericRecipe("ass.mpw10inc").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_incendiary.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), STEEL.plate().count(6), ANY_HIGHEXPLOSIVE.ingot().count(2), P_RED.dust().count(6), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1)));
        this.register(new GenericRecipe("ass.mpw10bus").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_buster.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(6), ANY_HIGHEXPLOSIVE.ingot().count(6), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 2)));
        this.register(new GenericRecipe("ass.mpw10nukesmall").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_nuclear.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(16), PU239.billet().count(2), new ComparableStack(ModItems.neutron_reflector.get(), 4), ANY_HIGHEXPLOSIVE.ingot().count(4), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw10nukelarge").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_nuclear_large.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), WEAPONSTEEL.plate().count(16), PU239.billet().count(6), new ComparableStack(ModItems.neutron_reflector.get(), 8), ANY_HIGHEXPLOSIVE.ingot().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw10taint").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_taint.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), STEEL.plate().count(12), new ComparableStack(ModExplosivesBlocks.det_cord.get().asItem(), 2), new ComparableStack(ModItems.powder_magic.get(), 12), getDict(Fluids.WATZ, 1_000).count(1)));
        this.register(new GenericRecipe("ass.mpw10cloud").setup(100, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_10_cloud.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_10.get(), 1), STEEL.plate().count(12), new ComparableStack(ModExplosivesBlocks.det_cord.get().asItem(), 2), new ComparableStack(ModItems.powder_magic.get(), 16)));
        this.register(new GenericRecipe("ass.mpw15he").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_15_he.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), STEEL.plate().count(12), ANY_HIGHEXPLOSIVE.ingot().count(12), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3)));
        this.register(new GenericRecipe("ass.mpw15inc").setup(200, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_15_incendiary.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), STEEL.plate().count(12), ANY_HIGHEXPLOSIVE.ingot().count(8), P_RED.dust().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3)));
        this.register(new GenericRecipe("ass.mpw15nuke").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_15_nuclear.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), PU239.billet().count(12), new ComparableStack(ModItems.neutron_reflector.get(), 12), new ComparableStack(ModItems.ball_tatb.get(), 24), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15n2").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_15_n2.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), new ComparableStack(ModItems.ball_tatb.get(), 32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15bf").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_warhead_15_balefire.get(), 1))
                .inputItems(new ComparableStack(ModItems.seg_15.get(), 1), WEAPONSTEEL.plate().count(32), new ComparableStack(ModItems.neutron_reflector.get(), 16), new ComparableStack(ModItems.powder_magic.get(), 8), new ComparableStack(ModItems.egg_balefire_shard.get(), 4), ANY_HIGHEXPLOSIVE.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));

        // ammo
        this.register(new GenericRecipe("ass.50bmgsm").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.bmg50_sm.get(), 6))
                .inputItems(new ComparableStack(ModCasingItems.casing(EnumCasingType.LARGE_STEEL), 1), ANY_SMOKELESS.dust().count(6), STAR.ingot().count(3))
                .setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "silverstorm"));
        this.register(new GenericRecipe("ass.50bmgbypass").setup(100, 100).outputItems(new ItemStack(ModAmmoItems.bmg50_black.get(), 12))
                .inputItems(new ComparableStack(ModCasingItems.casing(EnumCasingType.LARGE_STEEL), 2), ANY_SMOKELESS.dust().count(24), new ComparableStack(ModFinalConsumableItems.FLAT.get("item_secret_selenium_steel").get(), 1), new ComparableStack(ModArmorModItems.black_diamond.get()))
                .setPools(GenericRecipes.POOL_PREFIX_SECRET + "psalm"));
        this.register(new GenericRecipe("chem.shellchlorine").setup(100, 1_000).outputItems(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.CHLORINE), 1))
                .inputItems(new ComparableStack(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.NORMAL))), ANY_PLASTIC.ingot().count(1))
                .inputFluids(new FluidStack(Fluids.CHLORINE, 4_000)));
        this.register(new GenericRecipe("ass.shellphosgene").setup(100, 1_000).outputItems(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.PHOSGENE), 1))
                .inputItems(new ComparableStack(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.NORMAL))), ANY_PLASTIC.ingot().count(1))
                .inputFluids(new FluidStack(Fluids.PHOSGENE, 4_000)));
        this.register(new GenericRecipe("ass.shellmustard").setup(100, 1_000).outputItems(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.MUSTARD), 1))
                .inputItems(new ComparableStack(new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.NORMAL))), ANY_PLASTIC.ingot().count(1))
                .inputFluids(new FluidStack(Fluids.MUSTARDGAS, 4_000)));

        // space
        this.register(new GenericRecipe("ass.soyuzcore").setup(1_200, 25_000).outputItems(new ItemStack(ModSoyuzBlocks.struct_soyuz_core.get().asItem(), 1))
                .inputItems(STEEL.plateWelded().count(16),
                        new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()),
                        new ComparableStack(ModUpgradeItems.upgrade_power_3.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 4),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "soyuz"));
        this.register(new GenericRecipe("ass.soyuz").setup(6_000, 25_000).outputItems(new ItemStack(ModSoyuzBlocks.missile_soyuz.get(SoyuzSkinType.NORMAL), 1))
                .inputItems(TI.shell().count(32),
                        RUBBER.ingot().count(64),
                        new ComparableStack(ModFuelItems.rocket_fuel.get(), 64),
                        new ComparableStack(ModItems.thruster_small.get(), 12),
                        new ComparableStack(ModItems.thruster_medium.get(), 12),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 4),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 32)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "soyuz"));
        this.register(new GenericRecipe("ass.lander").setup(2_400, 25_000).outputItems(new ItemStack(ModItems.missile_soyuz_lander.get(), 1))
                .inputItems(AL.shell().count(4),
                        RUBBER.ingot().count(16),
                        new ComparableStack(ModFuelItems.rocket_fuel.get(), 16),
                        new ComparableStack(ModItems.thruster_small.get(), 3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 3),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 12)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "soyuz"));

        this.register(new GenericRecipe("ass.spysat").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.SPY), 1))
                .inputItems(AL.shell().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.GLASS_POLARIZED), 8),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3)));
        this.register(new GenericRecipe("ass.scansat").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.SCANNER), 1))
                .inputItems(AL.shell().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        GOLD.wireDense().count(32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3)));
        this.register(new GenericRecipe("ass.radarsat").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.RADAR), 1))
                .inputItems(GOLD.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModItems.magnetron.get(), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 3)));
        this.register(new GenericRecipe("ass.astrominer").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.MINER_ASTRO), 1))
                .inputItems(BIGMT.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 8),
                        new ComparableStack(ModItems.thruster_medium.get(), 1),
                        new ComparableStack(ModItems.motor_bismuth.get(), 4),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 2)));
        this.register(new GenericRecipe("ass.lunarminer").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.MINER_LUNAR), 1))
                .inputItems(BIGMT.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 8),
                        new ComparableStack(ModItems.thruster_medium.get(), 1),
                        new ComparableStack(ModItems.motor_bismuth.get(), 4),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 2)));
        this.register(new GenericRecipe("ass.orbitallaser").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.PRECISION_LASER), 1))
                .inputItems(WEAPONSTEEL.shell().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_TANTALUM).get(), 1),
                        new ComparableStack(ModItems.crystal_redstone.get(), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 3)));
        this.register(new GenericRecipe("ass.deathray").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.DEATH_RAY), 1))
                .inputItems(WEAPONSTEEL.shell().count(32),
                        PURG.billet().count(32),
                        new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_BISMUTH).get(), 1),
                        EMERALD.gem().count(32),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 5)));
        this.register(new GenericRecipe("ass.xenrelay").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.XENIUM_RESONATOR), 1))
                .inputItems(GOLD.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModUpgradeItems.crystal_xen.get(), 1),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 24),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1)));
        this.register(new GenericRecipe("ass.detectorsat").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.DETECTOR), 1))
                .inputItems(GOLD.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 64),
                        BSCCO.wireDense().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 24),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1)));
        this.register(new GenericRecipe("ass.rayscansat").setup(1_200, 25_000).outputItems(new ItemStack(ModSatelliteItems.satellite.get(EnumSatType.RAY_SCAN), 1))
                .inputItems(BIGMT.shell().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        SBD.wireDense().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 3)));

        this.register(new GenericRecipe("ass.satlink").setup(100, 1_000).outputItems(new ItemStack(ModMachineBlocks.machine_satlink.get().asItem()))
                .inputItems(new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        AL.plate().count(16),
                        new ComparableStack(ModItems.magnetron.get(), 3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1)));

        this.register(new GenericRecipe("ass.nitra").setupNamed(200, 500)
                .inputItems(new ComparableStack(ModItems.nitra.get()))
                .setIcon(new ItemStack(ModItems.nitra.get())) // 1.7 setIconToFirstIngredient()
                .outputChoices(
                        new OutputChoice(new ItemStack(ModCasingItems.casing(EnumCasingType.SHOTSHELL), 1), 20),
                        new OutputChoice(new ItemStack(ModCasingItems.casing(EnumCasingType.BUCKSHOT), 1), 10),
                        new OutputChoice(new ItemStack(ModCasingItems.casing(EnumCasingType.SMALL), 2), 20),
                        new OutputChoice(new ItemStack(ModCasingItems.casing(EnumCasingType.LARGE), 1), 10),
                        new OutputChoice(new ItemStack(Items.GUNPOWDER), 30),
                        new OutputChoice(new ItemStack(ModItems.cordite.get()), 20),
                        new OutputChoice(new ItemStack(ModItems.ballistite.get()), 20),
                        new OutputChoice(new ItemStack(ModEntityItems.grenade_shell.get(EnumGrenadeShell.FRAG), 1), 5),
                        new OutputChoice(new ItemStack(ModEntityItems.grenade_shell.get(EnumGrenadeShell.STICK), 1), 5),
                        new OutputChoice(new ItemStack(ModEntityItems.grenade_fuze.get(EnumGrenadeFuze.S3), 1), 10)
                ));

        this.register(new GenericRecipe("ass.emptypackage").setup(40, 100).outputItems(new ItemStack(ModItems.fluid_pack_empty.get(), 1))
                .inputItems(TI.plate().count(4), ANY_PLASTIC.ingot().count(2)));

        // 1.7 then registers ass.package<fluid> / ass.unpackage<fluid> for every fluid with a container
        // (Fluids.getInNiceOrder()); in the port they stay a rule, AssemblyMachineRecipes.arrange, not rows
        // port: 1.7's ass.digimemer (if enableMekanismChanges && Mekanism is loaded: a Mekanism digital miner from
        // "alloyUltimate" and HBM parts) is not a row: the port has no Mekanism integration and no enableMekanismChanges
        // key; Mekanism exists only on the NeoForge/Forge nodes, and the row would need a mod-loaded load condition so its
        // mekanism:digital_miner output decodes, plus Mekanism's ultimate-alloy tag as the ingredient

        // port: 1.12.2 AssemblerRecipes' gluon gun (256 ticks), a gun 1.7 does not have. Its tier 6 targeting circuits,
        // advanced ring coils and copper wire item are 1.7's bismoid circuit, golden ring coil and copper wire.
        this.register(new GenericRecipe("ass.gunegon").setup(256, 100).outputItems(new ItemStack(ModGunItems.gun_egon.get(), 1))
                .inputItems(new ComparableStack(ModItems.mechanism_special.get(), 4), STEEL.plate().count(16), PB.plate().count(24),
                        new ComparableStack(ModItems.coil_gold_torus.get(), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4),
                        new ComparableStack(ModItems.plate_polymer.get(), 8),
                        new ComparableStack(ModEnergyNetBlocks.machine_lithium_battery.get().asItem(), 2),
                        new ComparableStack(ModMachineBlocks.machine_waste_drum.get().asItem(), 1), CU.wire().count(8)));
    }

    /** A row registered inside a 1.7 config branch carries the branch. */
    @Override
    public void register(String id, GenericRecipe recipe) {
        if (branch != null) recipe.requires(branch);
        super.register(id, recipe);
    }

    /** 1.7 {@code new ItemStack(ModItems.canister_full, 1, fluid.getID())}: a full canister of the fluid. */
    private static ItemStack canisterFull(FluidType fluid) {
        ItemFluidContainerNT canister = ModFluidNetItems.canister_full.get();
        return canister.make(fluid, canister.capacity);
    }

    /** 1.7 {@code fluid.getDict(amount)}: any full container of that much of the fluid. */
    private static AStack getDict(FluidType fluid, int amount) {
        return new FluidContainerStack(fluid.getName(), amount);
    }

    /** 1.7 {@code mat.mechanism()}: the material's gun mechanism tag. */
    private static AStack mechanism(NTMMaterial mat) {
        return new TagStack(mat.tag(MaterialShapes.MECHANISM));
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }
}
