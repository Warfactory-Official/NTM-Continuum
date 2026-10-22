package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.ConstructionRow;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OutputRow;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OverlayType;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.inventory.recipes.loader.TableCodecs.CogStack;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModBombItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModPowerGenItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.machine.ModStampItems;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;





/**
 * 1.7 {@code AnvilRecipes.registerConstruction} and the methods it calls, in 1.7's order. The rows are the runtime
 * {@link AnvilConstructionRecipe}s; a row states its overlay only where it differs from the shape's default
 * ({@link AnvilRecipes#defaultOverlay}).
 */
public final class AnvilConstructionTable extends RecipeSource<ConstructionRow> {

    private static final TagKey<Item> KEY_PLANKS = ItemTags.PLANKS;
    private static final TagKey<Item> KEY_LOG = ItemTags.LOGS;
    private static final TagKey<Item> KEY_COBBLESTONE = tag("c", "cobblestones");
    private static final TagKey<Item> KEY_STONE = tag("c", "stones");
    /** 1.7 {@code KEY_BRICK}, {@code "ingotBrick"}: the brick item. */
    private static final TagKey<Item> KEY_BRICK = tag("c", "ingots/brick");
    private static final TagKey<Item> ANY_CONCRETE = tag("c", "concretes");

    /** The 1.7 config branch the rows sit in (a row's {@code requires}). */
    private List<String> requires = List.of();
    private final Set<String> ids = new HashSet<>();
    private int position;

    public AnvilConstructionTable() {
        super(AnvilRecipes.CONSTRUCTION.table);
    }

    @Override
    protected void registerDefaults() {
        registerConstruction();
    }

    private void registerConstruction() {

        add(new AnvilConstructionRecipe(IRON.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_iron.get()))).setTier(3));
        add(new AnvilConstructionRecipe(GOLD.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_gold.get()))).setTier(3));
        add(new AnvilConstructionRecipe(TI.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_titanium.get()))).setTier(3));
        add(new AnvilConstructionRecipe(AL.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_aluminium.get()))).setTier(3));
        add(new AnvilConstructionRecipe(STEEL.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_steel.get()))).setTier(3));
        add(new AnvilConstructionRecipe(PB.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_lead.get()))).setTier(3));
        add(new AnvilConstructionRecipe(CU.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_copper.get()))).setTier(3));
        add(new AnvilConstructionRecipe(GUNMETAL.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_gunmetal.get()))).setTier(3));
        add(new AnvilConstructionRecipe(WEAPONSTEEL.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_weaponsteel.get()))).setTier(3));
        add(new AnvilConstructionRecipe(BIGMT.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_saturnite.get()))).setTier(3));
        add(new AnvilConstructionRecipe(DURA.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_dura_steel.get()))).setTier(3));
        add(new AnvilConstructionRecipe(SA326.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_schrabidium.get()))).setTier(3));
        add(new AnvilConstructionRecipe(CMB.ingot(), new AnvilOutput(new ItemStack(ModItems.plate_combine_steel.get()))).setTier(3));

        for(NTMMaterial mat : Mats.orderedList) {
            if(mat.autogen.contains(MaterialShapes.WIRE) && doesOreNameExist(MaterialShapes.INGOT, mat)) {
                add(new AnvilConstructionRecipe(new TagStack(mat.tag(MaterialShapes.INGOT)), new AnvilOutput(autogen(MaterialShapes.WIRE, mat, 8))).setTier(4));
            }
        }

        add(new AnvilConstructionRecipe(Mats.MAT_COAL.dust(), new AnvilOutput(new ItemStack(Items.COAL))).setTier(3));
        add(new AnvilConstructionRecipe(dust("quartz"), new AnvilOutput(new ItemStack(Items.QUARTZ))).setTier(3));
        add(new AnvilConstructionRecipe(dust("lapis"), new AnvilOutput(new ItemStack(Items.LAPIS_LAZULI))).setTier(3)); // 1.7 LAPIS.dust(), dye:4
        add(new AnvilConstructionRecipe(DIAMOND.dust(), new AnvilOutput(new ItemStack(Items.DIAMOND))).setTier(3));
        add(new AnvilConstructionRecipe(EMERALD.dust(), new AnvilOutput(new ItemStack(Items.EMERALD))).setTier(3));

        registerConstructionRecipes();
        registerConstructionStamps();
        registerConstructionAmmo();
        registerConstructionSirens();
        registerConstructionUpgrades();
        registerConstructionRecycling();
    }

    private void registerConstructionRecipes() {

        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(Items.STONE_BRICKS, 16), new ComparableStack(ModItems.ingot_firebrick.get(), 16), IRON.ingot().count(8), CU.ingot().count(8)},
                new AnvilOutput(new ItemStack(ModProduction2Blocks.MACHINE_ANNIHILATOR.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(IRON.plate(), new AnvilOutput(new ItemStack(item("platemetal_base"), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(AL.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_aluminium.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(BE.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_beryllium.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(PB.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_lead.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(MINGRADE.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_red_copper.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(STEEL.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_steel.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(TI.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_titanium.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(W.ingot(), new AnvilOutput(new ItemStack(ModBlocks.deco_tungsten.get().asItem(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));

        add(new AnvilConstructionRecipe(
                new AStack[] {DNT.ingot().count(4), new ComparableStack(ModDecoContent.depth_brick.get().asItem())},
                new AnvilOutput(new ItemStack(ModDecoContent.depth_dnt.get().asItem()))).setTier(1916169));

        for(NTMMaterial mat : Mats.orderedList) if(mat.autogen.contains(MaterialShapes.SHELL)) add(new AnvilConstructionRecipe(
                new TagStack(mat.tag(MaterialShapes.PLATE), 4),
                new AnvilOutput(autogen(MaterialShapes.SHELL, mat, 1))).setTier(1));
        for(NTMMaterial mat : Mats.orderedList) if(mat.autogen.contains(MaterialShapes.PIPE)) {
            MaterialShapes key = doesOreNameExist(MaterialShapes.PLATE, mat) ? MaterialShapes.PLATE : MaterialShapes.INGOT;
            add(new AnvilConstructionRecipe(
                    new TagStack(mat.tag(key), 3),
                    new AnvilOutput(autogen(MaterialShapes.PIPE, mat, 1))).setTier(1));
        }

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.coil_copper.get(), 2),
                new AnvilOutput(new ItemStack(ModItems.coil_copper_torus.get()))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.coil_gold.get(), 2),
                new AnvilOutput(new ItemStack(ModItems.coil_gold_torus.get()))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));

        add(new AnvilConstructionRecipe(
                new AStack[] {IRON.plate().count(2), new ComparableStack(ModItems.coil_copper.get()), new ComparableStack(ModItems.coil_copper_torus.get())},
                new AnvilOutput(new ItemStack(ModItems.motor.get(), 2))).setTier(1));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModItems.motor.get()), ANY_PLASTIC.ingot().count(2), DESH.ingot().count(2), GOLD.wireDense()},
                new AnvilOutput(new ItemStack(ModItems.motor_desh.get(), 1))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE_BRICKS, 4),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 32),
                        CU.plate().count(8)
                },
                new AnvilOutput(new ItemStack(ModMachineBlocks.machine_blast_furnace.get().asItem()))).setTier(1));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(KEY_STONE, 16),
                        STEEL.plate().count(4),
                        CU.pipe().count(1),
                        new ComparableStack(ModItems.motor.get(), 1)
                },
                new AnvilOutput(new ItemStack(ModMachineBlocks.machine_rockmill.get().asItem()))).setTier(2));

        // boolean exp = GeneralConfig.enableExpensiveMode: a row for each branch, both at this one position
        for (boolean exp : new boolean[] {false, true}) {
            requires = List.of(exp ? "expensive_mode" : "!expensive_mode");
            add(exp ? "machine_assembly_machine_expensive" : null, new AnvilConstructionRecipe(
                    new AStack[] {
                            STEEL.ingot().count(8),
                            CU.plate().count(4),
                            new ComparableStack(ModItems.motor.get(), 2),
                            exp ? cir(EnumCircuitType.ANALOG, 2) : cir(EnumCircuitType.VACUUM_TUBE, 4)
                    }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_assembly_machine.get().asItem()))).setTier(2));
            if (!exp) position--;
        }
        requires = List.of();

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(KEY_COBBLESTONE, 8),
                        new TagStack(KEY_PLANKS, 16),
                        CU.plate().count(8),
                        PB.pipe().count(2)
                }, new AnvilOutput(new ItemStack(item("machine_pump_steam")))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE_BRICKS, 8),
                        STEEL.plate().count(16),
                        PB.pipe().count(4),
                        new ComparableStack(ModItems.motor.get(), 2),
                        cir(EnumCircuitType.VACUUM_TUBE, 4)
                }, new AnvilOutput(new ItemStack(item("machine_pump_electric")))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.FURNACE),
                        STEEL.plate().count(8),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.heater_firebox.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        STEEL.plate().count(4),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.heater_oven.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE, 8),
                        STEEL.plate().count(2),
                        IRON.ingot().count(4)
                }, new AnvilOutput(new ItemStack(item("machine_ashpit")))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.tank_steel.get(), 4),
                        STEEL.pipe().count(3),
                        TI.ingot().count(12),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.heater_oilburner.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        ANY_PLASTIC.ingot().count(4),
                        CU.ingot().count(8),
                        STEEL.plate().count(8),
                        new ComparableStack(ModItems.coil_tungsten.get(), 8),
                        cir(EnumCircuitType.BASIC, 1)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.heater_electric.get().asItem()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        RUBBER.ingot().count(4),
                        CU.ingot().count(16),
                        STEEL.plate().count(16),
                        STEEL.pipe().count(3),
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.heater_heatex.get().asItem()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE_BRICKS, 16),
                        IRON.ingot().count(4),
                        STEEL.plate().count(16),
                        CU.ingot().count(8),
                        new ComparableStack(ModSteelBlocks.steel_grate.get().asItem(), 16)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.furnace_steel.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE_BRICKS, 8),
                        new TagStack(KEY_LOG, 16),
                        CU.plateCast().count(2),
                        new TagStack(KEY_BRICK, 16)
                }, new AnvilOutput(new ItemStack(item("machine_furnace_combination")))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.STONE_BRICKS, 8),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        IRON.ingot().count(4),
                        CU.plate().count(8),
                }, new AnvilOutput(new ItemStack(ModProductionBlocks.MACHINE_ROTARY_FURNACE.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(KEY_PLANKS, 16),
                        STEEL.plate().count(6),
                        CU.ingot().count(8),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        new ComparableStack(ModGearItems.gear(0))
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_stirling.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(16),
                        BE.ingot().count(6),
                        CU.ingot().count(4),
                        new ComparableStack(ModItems.coil_gold.get(), 8),
                        new ComparableStack(ModGearItems.gear(1))
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_stirling_steel.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModBlocks.reinforced_stone.get().asItem(), 16),
                        STEEL.plate().count(12),
                        STEEL.shell().count(2),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        new ComparableStack(ModGearItems.gear(0))
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_steam_engine.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(KEY_PLANKS, 16),
                        STEEL.plate().count(6),
                        CU.ingot().count(8),
                        IRON.ingot().count(4),
                        new ComparableStack(ModItems.sawblade.get())
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_sawmill.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.ingot_firebrick.get(), 20),
                        CU.ingot().count(8),
                        STEEL.plate().count(8)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_crucible.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.ingot().count(4),
                        CU.plate().count(16),
                        new ComparableStack(ModItems.plate_polymer.get(), 8)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_boiler.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plateCast().count(2),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        W.bolt().count(4),
                        cir(EnumCircuitType.VACUUM_TUBE, 2)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_soldering_station.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plateCast().count(4),
                        W.ingot().count(8),
                        new ComparableStack(ModBlocks.machine_transformer.get().asItem(), 1),
                        new ComparableStack(item("arc_electrode"), 2)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_arc_welder.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plateCast().count(8),
                        CU.ingot().count(8),
                        ANY_PLASTIC.ingot().count(4)
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_industrial_boiler.get().asItem()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(4),
                        IRON.ingot().count(12),
                        CU.ingot().count(2),
                        cir(EnumCircuitType.VACUUM_TUBE, 2),
                        new ComparableStack(ModItems.sawblade.get())
                }, new AnvilOutput(new ItemStack(item("machine_autosaw")))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(8),
                        IRON.ingot().count(12),
                        CU.ingot().count(2),
                        cir(EnumCircuitType.VACUUM_TUBE, 1),
                }, new AnvilOutput(new ItemStack(ModProductionBlocks.MACHINE_THRESHER.get().asItem()))).setTier(2));

        /*add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.ingot().count(6),
                        IRON.plate().count(8),
                        ANY_PLASTIC.ingot().count(4),
                        new ComparableStack(ModItems.generator_steel.get(), 2),
                        new ComparableStack(ModItems.turbine_titanium.get(), 1),
                        new ComparableStack(ModItems.thermo_element.get(), 3),
                        new ComparableStack(ModItems.crt_display.get(), 1)
                }, new AnvilOutput(new ItemStack(ModPowerGenBlocks.MACHINE_INDUSTRIAL_GENERATOR.get().asItem()))).setTier(2));*/

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModHazardBlocks.brick_concrete.get().asItem(), 64),
                        new ComparableStack(Items.IRON_BARS, 128),
                        new ComparableStack(ModMachineBlocks.machine_condenser.get().asItem(), 4),
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_tower_small.get().asItem()))).setTier(3));
        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModBlocks.concrete_smooth.get().asItem(), 128),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 32),
                        new ComparableStack(ModMachineBlocks.machine_condenser.get().asItem(), 16),
                        STEEL.pipe().count(8),
                }, new AnvilOutput(new ItemStack(ModMachineBlocks.machine_tower_large.get().asItem()))).setTier(4));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.BONE, 16),
                        new ComparableStack(Items.LEATHER, 4),
                        new ComparableStack(Items.FEATHER, 24)
                }, new AnvilOutput(new ItemStack(ModArmorItems.wings_limp.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.sulfur.get(), 12),
                        STEEL.shell().count(4),
                        CU.plateCast().count(6),
                        cir(EnumCircuitType.BASIC, 2)
                },
                new AnvilOutput(new ItemStack(ModMachineBlocks.machine_deuterium_extractor.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.deuterium_filter.get(), 2),
                        STEEL.shell().count(5),
                        STEEL.pipe().count(12),
                        new ComparableStack(ModBlocks.concrete_asbestos.get().asItem(), 8),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        getDict(Fluids.SOURGAS, 1_000, 8),
                },
                new AnvilOutput(new ItemStack(ModMachineBlocks.machine_deuterium_tower.get().asItem()))).setTier(4));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(ANY_CONCRETE, 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 8),
                        new ComparableStack(ModItems.plate_polymer.get(), 8),
                        new ComparableStack(ModItems.coil_copper.get(), 4)
                },
                new AnvilOutput(new ItemStack(ModEnergyNetBlocks.red_pylon_large.get().asItem()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(ANY_CONCRETE, 8),
                        STEEL.ingot().count(8),
                        new ComparableStack(ModItems.plate_polymer.get(), 12),
                        new ComparableStack(ModItems.coil_copper.get(), 8)
                },
                new AnvilOutput(new ItemStack(ModEnergyNetBlocks.substation.get().asItem(), 2))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(4),
                        new ComparableStack(Items.BRICKS, 16),
                        new ComparableStack(ModSteelBlocks.steel_grate.get().asItem(), 2)
                },
                new AnvilOutput(new ItemStack(item("chimney_brick")))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(16),
                        new TagStack(ANY_CONCRETE, 64),
                        new ComparableStack(ModSteelBlocks.steel_grate.get().asItem(), 4),
                        new ComparableStack(ModItems.filter_coal.get(), 4)
                },
                new AnvilOutput(new ItemStack(item("chimney_industrial")))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.tank_steel.get(), 1),
                        PB.plate().count(2),
                        new ComparableStack(ModItems.nuclear_waste.get(), 10)
                }, new AnvilOutput(new ItemStack(ModFluidNetBlocks.yellow_barrel.get().asItem()))).setTier(3));
        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.tank_steel.get(), 1),
                        PB.plate().count(2),
                        new ComparableStack(ModItems.nuclear_waste_vitrified.get(), 10)
                }, new AnvilOutput(new ItemStack(ModFluidNetBlocks.vitrified_barrel.get().asItem()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModBombItems.man_core.get(), 1),
                        BE.ingot().count(4),
                        new ComparableStack(ModToolItems.screwdriver.get(), 1)
                }, new AnvilOutput(new ItemStack(ModNuclearMiscItems.DEMON_CORE_OPEN.get()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {DESH.ingot().count(4), ANY_PLASTIC.dust().count(2), DURA.ingot()},
                new AnvilOutput(new ItemStack(ModItems.plate_desh.get(), 4))).setTier(3));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModItems.nugget_bismuth.get(), 2), U238.billet().count(2), NB.dust()},
                new AnvilOutput(new ItemStack(ModItems.plate_bismuth.get(), 1))).setTier(4));

        add(new AnvilConstructionRecipe(
                new AStack[] {TI.plate().count(2), STEEL.ingot(), STEEL.bolt().count(4)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_titanium.get()))).setTier(2));
        add(new AnvilConstructionRecipe(
                new AStack[] {IRON.plate().count(6), NB.ingot(), new ComparableStack(ModItems.plate_armor_titanium.get(), 1)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_ajr.get(), 2))).setTier(3));
        add(new AnvilConstructionRecipe(
                new AStack[] {DURA.plate().count(4), new ComparableStack(ModItems.plate_armor_titanium.get(), 1), W.wire().count(8)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_hev.get()))).setTier(4));
        add(new AnvilConstructionRecipe(
                new AStack[] {WEAPONSTEEL.plate().count(4), STAR.ingot(), MAGTUNG.wire().count(8)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_lunar.get()))).setTier(4));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModAbilityToolItems.ingot_meteorite_forged.get(), 4), DESH.ingot(), new ComparableStack(ModItems.billet_yharonite.get(), 1)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_fau.get()))).setTier(6));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModItems.plate_dineutronium.get(), 4), new ComparableStack(ModItems.particle_sparkticle.get(), 1), new ComparableStack(ModItems.plate_armor_fau.get(), 6)},
                new AnvilOutput(new ItemStack(ModItems.plate_armor_dnt.get()))).setTier(7));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModMissileItems.missile_doomsday_rusted.get(), 1),
                        ANY_HARDPLASTIC.ingot().count(8),
                        AL.plateWelded().count(2),
                        PU239.billet().count(3)
                }, new AnvilOutput(new ItemStack(ModMissileItems.missile_doomsday.get()))).setTier(5));

        for(int i = 0; i < 15; i += 3) {
            add(new AnvilConstructionRecipe(IRON.plate(), new AnvilOutput(duct(i))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            add(new AnvilConstructionRecipe(CU.plate(), new AnvilOutput(duct(i + 1))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            add(new AnvilConstructionRecipe(AL.plate(), new AnvilOutput(duct(i + 2))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            add(new AnvilConstructionRecipe(new ComparableStack(duct(i).getItem()), new AnvilOutput(new ItemStack(ModItems.plate_iron.get()))).setTier(2).setOverlay(OverlayType.RECYCLING));
            add(new AnvilConstructionRecipe(new ComparableStack(duct(i + 1).getItem()), new AnvilOutput(new ItemStack(ModItems.plate_copper.get()))).setTier(2).setOverlay(OverlayType.RECYCLING));
            add(new AnvilConstructionRecipe(new ComparableStack(duct(i + 2).getItem()), new AnvilOutput(new ItemStack(ModItems.plate_aluminium.get()))).setTier(2).setOverlay(OverlayType.RECYCLING));

            add(new AnvilConstructionRecipe(new AStack[] {IRON.plate(), new ComparableStack(ModItems.plate_polymer.get())}, new AnvilOutput(new ItemStack(item("fluid_duct_exhaust_" + i / 3), 8))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            add(new AnvilConstructionRecipe(new ComparableStack(item("fluid_duct_exhaust_" + i / 3), 8), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.plate_iron.get())), new AnvilOutput(new ItemStack(ModItems.plate_polymer.get()))}).setTier(2));
        }

        for(int i = 0; i < 5; i++) {
            add(new AnvilConstructionRecipe(new AStack[] {MINGRADE.ingot(), new ComparableStack(ModItems.plate_polymer.get())}, new AnvilOutput(new ItemStack(item("red_cable_box_" + i), 16))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            add(new AnvilConstructionRecipe(new ComparableStack(item("red_cable_box_" + i), 16), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_red_copper.get(), 1)), new AnvilOutput(new ItemStack(ModItems.plate_polymer.get()))}).setTier(2));
        }
    }

    private void registerConstructionStamps() {

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_stone_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_stone_plate.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_stone_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_stone_wire.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_stone_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_stone_circuit.get()))).setTier(1));

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_iron_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_iron_plate.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_iron_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_iron_wire.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_iron_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_iron_circuit.get()))).setTier(1));

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_steel_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_steel_plate.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_steel_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_steel_wire.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_steel_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_steel_circuit.get()))).setTier(2));

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_titanium_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_titanium_plate.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_titanium_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_titanium_wire.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_titanium_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_titanium_circuit.get()))).setTier(2));

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_obsidian_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_obsidian_plate.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_obsidian_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_obsidian_wire.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_obsidian_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_obsidian_circuit.get()))).setTier(2));

        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_desh_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_plate.get()))).setTier(3));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_desh_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_wire.get()))).setTier(3));
        add(new AnvilConstructionRecipe(new ComparableStack(ModStampItems.stamp_desh_flat.get(), 1),new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_circuit.get()))).setTier(3));

    }

    private void registerConstructionAmmo() {

        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_iron_flat.get()), GUNMETAL.ingot().count(2)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_9.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_iron_flat.get()), GUNMETAL.ingot().count(2)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_50.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_desh_flat.get()), WEAPONSTEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_9.get()))).setTier(4));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_desh_flat.get()), WEAPONSTEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_50.get()))).setTier(4));

        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), IRON.ingot().count(2)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_c9.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), IRON.ingot().count(2)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_c50.get()))).setTier(1));

        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_barrel_light.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_barrel_heavy.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_receiver_light.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_receiver_heavy.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_mechanism.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_stock.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModItems.mold_base.get()), STEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModMoldItems.mold_grip.get()))).setTier(2));
    }

    private void registerConstructionSirens() {
        for(var track : ModPowerGenItems.tracks().values()) { // 1.7 siren_track metadata 1..20, one item per track
            add(new AnvilConstructionRecipe(new AStack[] {STEEL.plate(), new ComparableStack(ModItems.plate_polymer.get())}, new AnvilOutput(new ItemStack(track.get()))).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
        }
    }

    private void registerConstructionUpgrades() { }

    private void registerConstructionRecycling() {

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModIntermediateItems.chunk(EnumChunkType.RARE)),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.fragment_boron.get())),
                        new AnvilOutput(new ItemStack(ModItems.fragment_boron.get()), 0.5F),
                        new AnvilOutput(new ItemStack(ModItems.fragment_lanthanium.get()), 0.1F),
                        new AnvilOutput(new ItemStack(ModItems.fragment_cobalt.get())),
                        new AnvilOutput(new ItemStack(ModItems.fragment_cobalt.get()), 0.5F),
                        new AnvilOutput(new ItemStack(ModItems.fragment_cerium.get()), 0.1F),
                        new AnvilOutput(new ItemStack(ModItems.fragment_neodymium.get()), 0.5F),
                        new AnvilOutput(new ItemStack(ModItems.fragment_niobium.get()), 0.5F),
                }
        ).setTier(2));

        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_titanium.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_titanium.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_red_copper.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_red_copper.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_tungsten.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_tungsten.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_aluminium.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_aluminium.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_steel.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_rusty_steel.get().asItem(), 8), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_lead.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_lead.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModBlocks.deco_beryllium.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(ModOutgasBlocks.deco_asbestos.get().asItem(), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.ingot_asbestos.get(), 1))}).setTier(1));
        add(new AnvilConstructionRecipe(new ComparableStack(item("platemetal_base"), 4), new AnvilOutput[] {new AnvilOutput(new ItemStack(ModItems.plate_iron.get(), 1))}).setTier(1));

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModMachineBlocks.heater_firebox.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 8)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 6))
                }
        ).setTier(2));

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModMachineBlocks.heater_oven.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.ingot_firebrick.get(), 16)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 8))
                }
        ).setTier(2));

        add(new AnvilConstructionRecipe(
                new CogStack(ModMachineBlocks.machine_stirling.get().asItem(), true), // 1.7 machine_stirling metadata 0, with its gear
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 6)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_copper.get(), 4)),
                        new AnvilOutput(new ItemStack(ModGearItems.gear_large.get(), 1)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new CogStack(ModMachineBlocks.machine_stirling.get().asItem(), false), // 1.7 metadata 1, the gear taken out
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 6)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_copper.get(), 4)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new CogStack(ModMachineBlocks.machine_stirling_steel.get().asItem(), true), // 1.7 machine_stirling_steel metadata 0, with its gear
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 16)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 6)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 4)),
                        new AnvilOutput(new ItemStack(ModItems.coil_gold.get(), 8)),
                        new AnvilOutput(ModGearItems.gear(1)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModGearItems.gear_large_steel.get()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 8)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_titanium.get(), 1)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModGearItems.gear_large.get()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_iron.get(), 8)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 1)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new CogStack(ModMachineBlocks.machine_stirling_steel.get().asItem(), false), // 1.7 metadata 1, the gear taken out
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 16)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 6)),
                        new AnvilOutput(new ItemStack(Items.COPPER_INGOT, 4)),
                        new AnvilOutput(new ItemStack(ModItems.coil_gold.get(), 8)),

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModMachineBlocks.machine_bat9000.get().asItem(), 1),
                new AnvilOutput[] {
                        new AnvilOutput(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_TCALLOY, 4)),
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 16))
                }
        ).setTier(3));

        add(new AnvilConstructionRecipe(
                new ComparableStack(item("deco_computer_ibm_300pl")),
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.crt_display.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 3)),
                    new AnvilOutput(autogen(MaterialShapes.WIRE, Mats.MAT_COPPER, 4)),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.PCB, 2)),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.VACUUM_TUBE, 1), 0.5F),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR, 1), 0.75F),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR, 1), 0.5F),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.ANALOG, 1), 0.1F)

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new RecipeIngredients.AnyOf(List.of(new ComparableStack(item("deco_crt_clean")), new ComparableStack(item("deco_crt_broken")), new ComparableStack(item("deco_crt_blinking")), new ComparableStack(item("deco_crt_bsod"))), 1), // 1.7 deco_crt:*, its four items
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.crt_display.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 2)),
                    new AnvilOutput(autogen(MaterialShapes.WIRE, Mats.MAT_COPPER, 2)),
                    new AnvilOutput(autogen(MaterialShapes.WIRE, Mats.MAT_GOLD, 2), 0.25F),
                    new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.VACUUM_TUBE, 1), 0.25F)

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoContent.deco_toaster_iron.get().asItem()), //iron toaster
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.plate_iron.get(), 3)),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.coil_tungsten.get(), 1)),
                    new AnvilOutput(new ItemStack(Items.BREAD, 1), 0.5F),
                    new AnvilOutput(new ItemStack(ModFinalConsumableItems.fusion_core.get(), 1), 0.01F)

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(item("deco_toaster_steel")), // steel toaster
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 3)),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.coil_tungsten.get(), 2)),
                    new AnvilOutput(new ItemStack(Items.BREAD, 1), 0.5F),
                    new AnvilOutput(new ItemStack(ModBatterySCItems.get(EnumBatterySC.RA226)), 0.1F),
                    new AnvilOutput(new ItemStack(ModFinalConsumableItems.fusion_core.get(), 1), 0.05F)

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(item("deco_toaster_wood")), // wooden toaster
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.powder_sawdust.get(), 4)),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.coil_tungsten.get(), 4)),
                    new AnvilOutput(new ItemStack(Items.BREAD, 1), 0.5F),
                    new AnvilOutput(new ItemStack(ModFinalConsumableItems.fusion_core.get(), 1), 0.5F),
                    new AnvilOutput(new ItemStack(ModFinalConsumableItems.fusion_core.get(), 1), 0.5F),
                    new AnvilOutput(new ItemStack(ModItems.gem_alexandrite.get(), 1), 0.25F),
                    new AnvilOutput(new ItemStack(ModItems.flame_pony.get(), 1), 0.01F)

                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoCrateBlocks.radiorec.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 4)),
                        new AnvilOutput(autogen(MaterialShapes.WIRE, Mats.MAT_COPPER, 1)),
                        new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.VACUUM_TUBE, 1), 0.5F),
                        new AnvilOutput(new ItemStack(ModItems.ingot_polymer.get(), 1), 0.25F),
                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoContent.tape_recorder.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_tungsten.get(), 1), 0.25F),
                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoContent.pole_top.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.ingot_tungsten.get(), 3)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_red_copper.get(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 2)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 1), 0.5F),
                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoContent.pole_satellite_receiver.get().asItem()),
                new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 3)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 2), 0.5F),
                        new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.VACUUM_TUBE, 1), 0.5F),
                        new AnvilOutput(autogen(MaterialShapes.WIRE, Mats.MAT_RED_COPPER, 1)),
                }
        ).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModDecoCrateBlocks.filing_cabinet.get().asItem()),
                new AnvilOutput[] {
                    new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 2)),
                    new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 2), 0.5F),
                    new AnvilOutput(new ItemStack(ModItems.plate_polymer.get(), 2), 0.25F),
                    new AnvilOutput(new ItemStack(ModItems.scrap.get(), 1))

                }
        ).setTier(1));

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.billet_ra226be.get(), 3), new AnvilOutput[] {
                        new AnvilOutput(ModPileBlocks.pile_rod.stack(EnumPileRod.RA226BE)),
                        }).setTier(2));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.billet_po210be.get(), 3), new AnvilOutput[] {
                        new AnvilOutput(ModPileBlocks.pile_rod.stack(EnumPileRod.PO210BE)),
                        }).setTier(2));
        add(new AnvilConstructionRecipe(
                ZR.billet().count(3), new AnvilOutput[] {
                        new AnvilOutput(ModPileBlocks.pile_rod.stack(EnumPileRod.ZR)),
                        }).setTier(2));
        add(new AnvilConstructionRecipe(
                U.billet().count(3), new AnvilOutput[] {
                        new AnvilOutput(ModPileBlocks.pile_rod.stack(EnumPileRod.NU)),
                        }).setTier(2));

        //RBMK
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_moderator.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.block_graphite.get().asItem(), 4))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_absorber.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_boron.get(), 8))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_reflector.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.neutron_reflector.get(), 8))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_control.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_absorber.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModFuelItems.ingot_graphite.get(), 2)),
                        new AnvilOutput(new ItemStack(ModItems.motor.get(), 2))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_control_mod.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_control.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.block_graphite.get().asItem(), 4)),
                        new AnvilOutput(new ItemStack(ModItems.nugget_bismuth.get(), 4))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_control_auto.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_control.get().asItem(), 1)),
                        new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED, 1)),
                        new AnvilOutput(new ItemStack(ModItems.crt_display.get(), 1))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_fuel_rod_reasim.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_zirconium.get(), 4)),
                        new AnvilOutput(autogen(MaterialShapes.SHELL, Mats.MAT_STEEL, 2))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_fuel_rod_reasim_moderated.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_fuel_rod_reasim.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.block_graphite.get().asItem(), 4)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_tcalloy.get(), 4))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_outgasser.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModSteelBlocks.steel_grate.get().asItem(), 6)),
                        new AnvilOutput(new ItemStack(ModItems.tank_steel.get(), 1)),
                        new AnvilOutput(new ItemStack(Items.HOPPER, 1))
                }).setTier(4));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModRBMKBlocks.rbmk_storage.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                        new AnvilOutput(new ItemStack(ModStorageBlocks.crate_steel.get().asItem(), 2))
                }).setTier(4));

        // if(!GeneralConfig.enable528) {
        requires = List.of("!enable_528");

            add(new AnvilConstructionRecipe(
                    new ComparableStack(ModRBMKBlocks.rbmk_fuel_rod.get().asItem()), new AnvilOutput[] {
                            new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                            new AnvilOutput(autogen(MaterialShapes.SHELL, Mats.MAT_STEEL, 2))
                    }).setTier(4));
            add(new AnvilConstructionRecipe(
                    new ComparableStack(ModRBMKBlocks.rbmk_fuel_rod_moderated.get().asItem()), new AnvilOutput[] {
                            new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_fuel_rod.get().asItem(), 1)),
                            new AnvilOutput(new ItemStack(ModRBMKBlocks.block_graphite.get().asItem(), 4)),
                            new AnvilOutput(new ItemStack(ModItems.nugget_bismuth.get(), 4))
                    }).setTier(4));
            add(new AnvilConstructionRecipe(
                    new ComparableStack(ModRBMKBlocks.rbmk_boiler.get().asItem()), new AnvilOutput[] {
                            new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                            new AnvilOutput(autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 6)),
                            new AnvilOutput(autogen(MaterialShapes.SHELL, Mats.MAT_COPPER, 2))
                    }).setTier(4));
            add(new AnvilConstructionRecipe(
                    new ComparableStack(ModRBMKBlocks.rbmk_cooler.get().asItem()), new AnvilOutput[] {
                            new AnvilOutput(new ItemStack(ModRBMKBlocks.rbmk_blank.get().asItem(), 1)),
                            new AnvilOutput(new ItemStack(ModSteelBlocks.steel_grate.get().asItem(), 4)),
                            new AnvilOutput(new ItemStack(ModItems.plate_polymer.get(), 4))
                    }).setTier(4));
            add(new AnvilConstructionRecipe(
                    new ComparableStack(ModNuclearMiscBlocks.REACTOR_RESEARCH.get().asItem()), new AnvilOutput[] {
                            new AnvilOutput(new ItemStack(ModItems.ingot_steel.get(), 8)),
                            new AnvilOutput(new ItemStack(ModItems.ingot_tcalloy.get(), 4)),
                            new AnvilOutput(new ItemStack(ModItems.motor_desh.get(), 2)),
                            new AnvilOutput(new ItemStack(ModItems.ingot_boron.get(), 5)),
                            new AnvilOutput(new ItemStack(ModItems.plate_lead.get(), 8)),
                            new AnvilOutput(new ItemStack(ModItems.crt_display.get(), 3)),
                            new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.BASIC, 1)),
                            new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.BASIC, 1), 0.5F),
                    }).setTier(4));

        requires = List.of();
        // }

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModFluidNetBlocks.yellow_barrel.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.tank_steel.get(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.plate_lead.get(), 2)),
                        new AnvilOutput(new ItemStack(ModItems.nuclear_waste.get(), 10))
                }).setTier(3));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModFluidNetBlocks.vitrified_barrel.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModItems.tank_steel.get(), 1)),
                        new AnvilOutput(new ItemStack(ModItems.plate_lead.get(), 2)),
                        new AnvilOutput(new ItemStack(ModItems.nuclear_waste_vitrified.get(), 10))
                }).setTier(3));

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.egg_glyphid.get()), new AnvilOutput[] {
                        new AnvilOutput(new ItemStack(ModFoodItems.glyphid_meat.get(), 2)),
                        new AnvilOutput(new ItemStack(ModFoodItems.glyphid_meat.get(), 1), 0.5F),
                        new AnvilOutput(new ItemStack(Items.BONE, 1), 0.75F),
                        new AnvilOutput(new ItemStack(Items.EXPERIENCE_BOTTLE, 1), 0.5F)
                }).setTier(1));

        add(new AnvilConstructionRecipe(
                new ComparableStack(ModBlocks.fusion_heater.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(autogen(MaterialShapes.PIPE, Mats.MAT_STEEL, 4)),
                        new AnvilOutput(autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 2)),
                        new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.ANALOG, 1), 0.5F)
                }).setTier(1));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModBlocks.fusion_hatch.get().asItem()), new AnvilOutput[] {
                        new AnvilOutput(autogen(MaterialShapes.PIPE, Mats.MAT_STEEL, 4)),
                        new AnvilOutput(autogen(MaterialShapes.PIPE, Mats.MAT_COPPER, 4)),
                        new AnvilOutput(ModIntermediateItems.circuit(EnumCircuitType.ANALOG, 1), 0.75F)
                }).setTier(1));
    }

    /**
     * 1.7 {@code constructionRecipes.add(recipe)}. The id is the output's item name, or {@code recycle_<input>} for a
     * recipe with several outputs; a second row of one id is {@code <output>_from_<input>} ({@code recycle_<input>_2}).
     */
    private void add(AnvilConstructionRecipe recipe) {
        add(null, recipe);
    }

    private void add(String explicitId, AnvilConstructionRecipe recipe) {
        String id = explicitId != null ? explicitId : id(recipe);
        if (!ids.add(id)) throw new IllegalStateException("two anvil rows " + id);
        List<OutputRow> outputs = new ArrayList<>();
        for (AnvilOutput out : recipe.output) outputs.add(new OutputRow(ItemSpec.of(out.stack).getOrThrow(), out.chance));
        OverlayType overlay = recipe.getOverlay();
        register(id, new ConstructionRow(List.copyOf(recipe.input), outputs, recipe.tierLower,
                recipe.tierUpper == -1 ? Optional.empty() : Optional.of(recipe.tierUpper),
                overlay == AnvilRecipes.defaultOverlay(recipe.input.size(), recipe.output.size()) ? Optional.empty() : Optional.of(overlay),
                requires, position++));
    }

    private String id(AnvilConstructionRecipe recipe) {
        if (recipe.output.size() > 1) {
            String id = "recycle_" + name(recipe.input.get(0));
            return ids.contains(id) ? id + "_2" : id;
        }
        String id = BuiltInRegistries.ITEM.getKey(recipe.output.get(0).stack.getItem()).getPath();
        return ids.contains(id) ? id + "_from_" + name(recipe.input.get(0)) : id;
    }

    private static String name(AStack stack) {
        if (stack instanceof ComparableStack comparable) return BuiltInRegistries.ITEM.getKey(comparable.item).getPath();
        if (stack instanceof CogStack cog) return BuiltInRegistries.ITEM.getKey(cog.item).getPath();
        if (stack instanceof TagStack tag) return tag.tag.location().getPath().replace('/', '_');
        return stack.getDisplayStack().isEmpty() ? "any" : BuiltInRegistries.ITEM.getKey(stack.getDisplayStack().getItem()).getPath();
    }

    /** An {@code hbm:} item by name, for the 1.7 metadata families the port registers one item per type. */
    private static Item item(String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", path));
        if (item == Items.AIR) throw new IllegalStateException("anvil recipe item hbm:" + path + " is not registered");
        return item;
    }

    /** 1.7 {@code new ComparableStack(ModItems.circuit, count, type)}. */
    private static AStack cir(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    /** 1.7 {@code new ItemStack(ModItems.<shape>, count, mat.id)}: the autogen item of that shape. */
    private static ItemStack autogen(MaterialShapes shape, NTMMaterial material, int count) {
        return new ItemStack(ModAutogenItems.get(shape, material), count);
    }

    /**
     * 1.7 {@code OreDictionary.doesOreNameExist(shape.make(mat))}: whether HBM has the material's item in that shape (the
     * vanilla ingots for iron, gold and copper; carbon's ingot is the graphite ingot, {@code c:ingots/carbon}).
     */
    private static boolean doesOreNameExist(MaterialShapes shape, NTMMaterial mat) {
        if (shape == MaterialShapes.INGOT && mat == Mats.MAT_CARBON) return true;
        if (shape == MaterialShapes.INGOT
                && (mat.name.equals("iron") || mat.name.equals("gold") || mat.name.equals("copper"))) return true;
        return BuiltInRegistries.ITEM.containsKey(Ids.of("hbm", shape.itemPrefix + "_" + mat.name));
    }

    /** 1.7 {@code new ItemStack(ModBlocks.fluid_duct_box, 1, m)}: {@code m = 3 * size + material} (silver, copper, white). */
    private static ItemStack duct(int meta) {
        return new ItemStack(item("fluid_duct_box_" + new String[] {"silver", "copper", "white"}[meta % 3] + "_" + meta / 3));
    }

    /** 1.7 {@code new OreDictStack(fluid.getDict(amount), count)}: full containers of the fluid. */
    private static AStack getDict(FluidType fluid, int amount, int count) {
        return new FluidContainerStack(fluid.getName(), amount).count(count);
    }

    /** A 1.7 {@code dust<Name>} ore name the port has no material for: its {@code c:dusts/} tag. */
    private static AStack dust(String name) {
        return new TagStack(tag("c", "dusts/" + name));
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }
}
