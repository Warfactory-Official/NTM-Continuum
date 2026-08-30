package com.hbm.inventory.recipes.anvil;

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ModStampItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.hbm.inventory.OreDictManager.*;

/**
 * The anvil recipe sets, ported (feasibility-curated, like {@code CrystallizerRecipes}) from the 1.12.2
 * {@code AnvilRecipes}. Two independent families:
 * <ul>
 *   <li><b>Smithing</b> — two input slots in the anvil GUI, matched live by the container: the anvil
 *       tier-up matrix (iron/lead anvil + 10 ingots → higher anvil) and the gunmetal recipe. The Mold /
 *       Hot / Cyanide / Rename specials are deferred with their items.</li>
 *   <li><b>Construction</b> — the browsable recipe grid: crafts straight from the player's inventory via
 *       {@link com.hbm.packet.AnvilCraftPayload}. Ported subset: the 13 metal→plate conversions (t3), the
 *       {@code Mats.orderedList} autogen loops (shell / pipe t1, fine wire t4) gated on the autogen item
 *       existing, dust→gem (t3), the deco-block plating recipes (t1), coils/motors, the construction
 *       recipes for the <i>already-ported</i> machines (assembly machine, soldering station, t2), the
 *       armor-plate chain heads that exist ({@code plate_desh}/{@code plate_bismuth}), and the stamp
 *       conversions. <b>Deferred:</b> ammo/siren/recycling blocks, mold recipes, and all
 *       {@code enableLBSM}/{@code enable528}/{@code enableExpensiveMode} non-default alternates (per the
 *       recipe-porting filters).</li>
 * </ul>
 *
 * <p><b>Init is lazy</b> ({@link #register()} on first access): recipes build {@link ItemStack}s, which
 * throw "Components not bound yet" during mod setup. The construction list order is deterministic (client
 * and server build the identical list — the craft packet is index-based).
 */
public final class AnvilRecipes {

    private AnvilRecipes() {}

    private static final List<AnvilSmithingRecipe> smithingRecipes = new ArrayList<>();
    private static final List<AnvilConstructionRecipe> constructionRecipes = new ArrayList<>();
    private static boolean initialized = false;

    /** The 13 materials whose hand-hammered {@code plate_*} items are ported (the c:plates tags populate). */
    private static final Set<NTMMaterial> PLATED = Set.of(
            IRON, GOLD, TI, AL, STEEL, PB, CU, GUNMETAL, WEAPONSTEEL, BIGMT, DURA, SA326, CMB);

    public static List<AnvilSmithingRecipe> getSmithing() {
        register();
        return smithingRecipes;
    }

    public static List<AnvilConstructionRecipe> getConstruction() {
        register();
        return constructionRecipes;
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        registerSmithing();
        registerConstruction();
    }

    private static void registerSmithing() {

        // The anvil tier-up matrix: either tier-1 anvil + 10 ingots of the target material.
        Block[] bases = new Block[]{ModAnvilBlocks.anvil_iron.get(), ModAnvilBlocks.anvil_lead.get()};

        for (Block base : bases) {
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_steel.get()), new ComparableStack(base.asItem()), STEEL.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_desh.get()), new ComparableStack(base.asItem()), DESH.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_saturnite.get()), new ComparableStack(base.asItem()), BIGMT.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_ferrouranium.get()), new ComparableStack(base.asItem()), new ComparableStack(ModItems.ingot_ferrouranium.get(), 10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_bismuth_bronze.get()), new ComparableStack(base.asItem()), BBRONZE.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_arsenic_bronze.get()), new ComparableStack(base.asItem()), ABRONZE.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_schrabidate.get()), new ComparableStack(base.asItem()), SBD.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_dnt.get()), new ComparableStack(base.asItem()), DNT.ingot().count(10)));
            smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_osmiridium.get()), new ComparableStack(base.asItem()), OSMIRIDIUM.ingot().count(10)));
        }

        smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModItems.ingot_gunmetal.get()), CU.ingot(), AL.ingot()));

        // -------- casting molds: a shape example + a blank mold -> the mold (example NOT consumed) --------
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_nugget.get()), GOLD.nugget(), MaterialShapes.NUGGET, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_billet.get()), U.billet(), MaterialShapes.BILLET, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_ingot.get()), IRON.ingot(), MaterialShapes.INGOT, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_plate.get()), IRON.plate(), MaterialShapes.PLATE, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_plate_cast.get()), IRON.plateCast(), MaterialShapes.CASTPLATE, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_plates_cast.get()), IRON.plateCast().count(3), MaterialShapes.CASTPLATE, 3));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_wire.get()), CU.wire(), MaterialShapes.WIRE, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_blade.get()),
                new ComparableStack(ModItems.blade_titanium.get()), ModItems.blade_titanium.get(), ModItems.blade_tungsten.get()));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_blades.get()),
                new ComparableStack(com.hbm.items.ModMachineItems.blades_steel.get()),
                com.hbm.items.ModMachineItems.blades_steel.get(), com.hbm.items.ModMachineItems.blades_titanium.get()));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_stamp.get()),
                new ComparableStack(ModStampItems.stamp_iron_flat.get()),
                ModStampItems.stamp_stone_flat.get(), ModStampItems.stamp_iron_flat.get(), ModStampItems.stamp_steel_flat.get(),
                ModStampItems.stamp_titanium_flat.get(), ModStampItems.stamp_obsidian_flat.get()));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_shell.get()), STEEL.shell(), MaterialShapes.SHELL, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_pipe.get()), STEEL.pipe(), MaterialShapes.PIPE, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_ingots.get()), IRON.ingot().count(9), MaterialShapes.INGOT, 9));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_plates.get()), IRON.plate().count(9), MaterialShapes.PLATE, 9));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_block.get()), IRON.block(), MaterialShapes.BLOCK, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_pipes.get()),
                new ComparableStack(ModItems.pipes_steel.get()), ModItems.pipes_steel.get()));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_wire_dense.get()), MINGRADE.wireDense(), MaterialShapes.DENSEWIRE, 1));
        smithingRecipes.add(new AnvilSmithingMold(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_wires_dense.get()), MINGRADE.wireDense().count(9), MaterialShapes.DENSEWIRE, 9));
    }

    private static void registerConstruction() {

        // -------- metal -> hammered plate (tier 3, 1:1 = SMITHING overlay) --------
        add(new AnvilConstructionRecipe(IRON.ingot(), out(ModItems.plate_iron.get())).setTier(3));
        add(new AnvilConstructionRecipe(GOLD.ingot(), out(ModItems.plate_gold.get())).setTier(3));
        add(new AnvilConstructionRecipe(TI.ingot(), out(ModItems.plate_titanium.get())).setTier(3));
        add(new AnvilConstructionRecipe(AL.ingot(), out(ModItems.plate_aluminium.get())).setTier(3));
        add(new AnvilConstructionRecipe(STEEL.ingot(), out(ModItems.plate_steel.get())).setTier(3));
        add(new AnvilConstructionRecipe(PB.ingot(), out(ModItems.plate_lead.get())).setTier(3));
        add(new AnvilConstructionRecipe(CU.ingot(), out(ModItems.plate_copper.get())).setTier(3));
        add(new AnvilConstructionRecipe(GUNMETAL.ingot(), out(ModItems.plate_gunmetal.get())).setTier(3));
        add(new AnvilConstructionRecipe(WEAPONSTEEL.ingot(), out(ModItems.plate_weaponsteel.get())).setTier(3));
        add(new AnvilConstructionRecipe(BIGMT.ingot(), out(ModItems.plate_saturnite.get())).setTier(3));
        add(new AnvilConstructionRecipe(DURA.ingot(), out(ModItems.plate_dura_steel.get())).setTier(3));
        add(new AnvilConstructionRecipe(SA326.ingot(), out(ModItems.plate_schrabidium.get())).setTier(3));
        add(new AnvilConstructionRecipe(CMB.ingot(), out(ModItems.plate_combine_steel.get())).setTier(3));

        // -------- autogen fine wire: ingot -> 8x wire (tier 4) --------
        for (NTMMaterial mat : Mats.orderedList) {
            Item wire = ModAutogenItems.get(MaterialShapes.WIRE, mat);
            if (mat.autogen.contains(MaterialShapes.WIRE) && wire != null) {
                add(new AnvilConstructionRecipe(new TagStack(mat.tag(MaterialShapes.INGOT)),
                        new AnvilOutput(new ItemStack(wire, 8))).setTier(4));
            }
        }

        // -------- dust -> gem (tier 3; coal is skipped — powder_coal isn't ported) --------
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.powder_quartz.get()), out(Items.QUARTZ)).setTier(3));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.powder_lapis.get()), out(Items.LAPIS_LAZULI)).setTier(3));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.powder_diamond.get()), out(Items.DIAMOND)).setTier(3));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.powder_emerald.get()), out(Items.EMERALD)).setTier(3));

        // -------- ingot -> 4x decorative metal block (tier 1) --------
        add(new AnvilConstructionRecipe(AL.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_aluminium.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(BE.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_beryllium.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(PB.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_lead.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(MINGRADE.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_red_copper.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(STEEL.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_steel.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(TI.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_titanium.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(W.ingot(), new AnvilOutput(new ItemStack(com.hbm.blocks.ModBlocks.deco_tungsten.get(), 4))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));

        // -------- autogen shells / pipes (tier 1) --------
        // Shells: 4x plate. Emitted only where the hand plates exist (PLATED) so the browser lists only
        // craftable recipes (the old list carried dead entries for plate-less materials).
        for (NTMMaterial mat : Mats.orderedList) {
            Item shell = ModAutogenItems.get(MaterialShapes.SHELL, mat);
            if (mat.autogen.contains(MaterialShapes.SHELL) && shell != null && PLATED.contains(mat)) {
                add(new AnvilConstructionRecipe(new TagStack(mat.tag(MaterialShapes.PLATE), 4),
                        new AnvilOutput(new ItemStack(shell))).setTier(1));
            }
        }
        // Pipes: 3x plate, or 3x ingot for materials without plates (the old doesOreNameExist fallback).
        for (NTMMaterial mat : Mats.orderedList) {
            Item pipe = ModAutogenItems.get(MaterialShapes.PIPE, mat);
            if (mat.autogen.contains(MaterialShapes.PIPE) && pipe != null) {
                MaterialShapes in = PLATED.contains(mat) ? MaterialShapes.PLATE : MaterialShapes.INGOT;
                add(new AnvilConstructionRecipe(new TagStack(mat.tag(in), 3),
                        new AnvilOutput(new ItemStack(pipe))).setTier(1));
            }
        }

        // -------- coils / motors --------
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.coil_copper.get(), 2),
                new AnvilOutput(new ItemStack(ModItems.coil_copper_torus.get()))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));
        add(new AnvilConstructionRecipe(
                new ComparableStack(ModItems.coil_gold.get(), 2),
                new AnvilOutput(new ItemStack(ModItems.coil_gold_torus.get()))).setTier(1).setOverlay(OverlayType.CONSTRUCTION));

        add(new AnvilConstructionRecipe(
                new AStack[] {IRON.plate().count(2), new ComparableStack(ModItems.motor.get()), new ComparableStack(ModItems.coil_copper_torus.get())},
                new AnvilOutput(new ItemStack(ModItems.motor.get(), 2))).setTier(1));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModItems.motor.get()), ANY_PLASTIC.ingot().count(2), DESH.ingot().count(2), GOLD.wireDense()},
                new AnvilOutput(new ItemStack(ModItems.motor_desh.get()))).setTier(3));

        // -------- ported machines (tier 2) --------
        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.ingot().count(8),
                        CU.plate().count(4),
                        new ComparableStack(ModItems.motor.get(), 2),
                        cir(EnumCircuitType.VACUUM_TUBE, 4)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_assembly_machine.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "cobblestones")), 8),
                        new TagStack(ItemTags.PLANKS, 16),
                        CU.plate().count(8),
                        PB.pipe().count(2)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_pump_steam.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Blocks.STONE_BRICKS.asItem(), 8),
                        STEEL.plate().count(16),
                        PB.pipe().count(4),
                        new ComparableStack(ModItems.motor.get(), 2),
                        cir(EnumCircuitType.VACUUM_TUBE, 4)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_pump_electric.get()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plateCast().count(2),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        W.bolt().count(4),
                        cir(EnumCircuitType.VACUUM_TUBE, 2)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_soldering_station.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Blocks.STONE_BRICKS.asItem(), 16),
                        IRON.ingot().count(4),
                        STEEL.plate().count(16),
                        CU.ingot().count(8),
                        new ComparableStack(ModSteelBlocks.steel_grate.get().asItem(), 16)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.furnace_steel.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(ItemTags.PLANKS, 16),
                        STEEL.plate().count(6),
                        CU.ingot().count(8),
                        IRON.ingot().count(4),
                        new ComparableStack(ModItems.sawblade.get())
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_sawmill.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new TagStack(ItemTags.PLANKS, 16),
                        STEEL.plate().count(6),
                        CU.ingot().count(8),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        new ComparableStack(com.hbm.items.ModGearItems.gear_large.get().of(0))
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plate().count(16),
                        BE.ingot().count(6),
                        CU.ingot().count(8),
                        new ComparableStack(ModItems.coil_gold.get(), 16),
                        new ComparableStack(com.hbm.items.ModGearItems.gear_large.get().of(1))
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling_steel.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(com.hbm.blocks.ModBlocks.reinforced_stone.get().asItem(), 16),
                        STEEL.plate().count(12),
                        STEEL.shell().count(2),
                        new ComparableStack(ModItems.coil_copper.get(), 4),
                        new ComparableStack(com.hbm.items.ModGearItems.gear_large.get().of(0))
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_steam_engine.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new ComparableStack(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling.get().asItem()),
                new AnvilOutput[]{
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 6)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_copper.get(), 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_copper.get(), 4)),
                        new AnvilOutput(com.hbm.items.ModGearItems.gear_large.get().of(0))
                }).setTier(2));

        add(new AnvilConstructionRecipe(
                new ComparableStack(stirlingItemVariant(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling.get(), 1)),
                new AnvilOutput[]{
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 6)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_copper.get(), 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_copper.get(), 4))
                }).setTier(2));

        add(new AnvilConstructionRecipe(
                new ComparableStack(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling_steel.get().asItem()),
                new AnvilOutput[]{
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 16)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 6)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_copper.get(), 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_gold.get(), 16)),
                        new AnvilOutput(com.hbm.items.ModGearItems.gear_large.get().of(1))
                }).setTier(2));

        add(new AnvilConstructionRecipe(
                new ComparableStack(stirlingItemVariant(com.hbm.blocks.machine.ModMachineBlocks.machine_stirling_steel.get(), 1)),
                new AnvilOutput[]{
                        new AnvilOutput(new ItemStack(ModItems.plate_steel.get(), 16)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_beryllium.get(), 6)),
                        new AnvilOutput(new ItemStack(ModItems.ingot_copper.get(), 8)),
                        new AnvilOutput(new ItemStack(ModItems.coil_gold.get(), 16))
                }).setTier(2));

        // -------- special plates whose items exist --------
        add(new AnvilConstructionRecipe(
                new AStack[] {DESH.ingot().count(4), ANY_PLASTIC.dust().count(2), DURA.ingot()},
                new AnvilOutput(new ItemStack(ModItems.plate_desh.get(), 4))).setTier(3));
        add(new AnvilConstructionRecipe(
                new AStack[] {new ComparableStack(ModItems.nugget_bismuth.get(), 2), U238.billet().count(2), NB.dust()},
                new AnvilOutput(new ItemStack(ModItems.plate_bismuth.get()))).setTier(4));

        // -------- research-reactor plate fuel (old AnvilRecipes L543-549) --------
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.ingot_u233.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.U233))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.ingot_u235.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.U235))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.ingot_mox_fuel.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.MOX))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.ingot_pu239.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.PU239))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.ingot_schrabidium.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.SA326))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.billet_ra226be.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.RA226BE))).setTier(4));
        add(new AnvilConstructionRecipe(new ComparableStack(ModItems.billet_pu238be.get()),
                out(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.PU238BE))).setTier(4));

        // -------- stamp conversions (flat -> plate/wire/circuit) --------
        stampSet(ModStampItems.stamp_stone_flat.get(), 1, ModStampItems.stamp_stone_plate.get(), ModStampItems.stamp_stone_wire.get(), ModStampItems.stamp_stone_circuit.get());
        stampSet(ModStampItems.stamp_iron_flat.get(), 1, ModStampItems.stamp_iron_plate.get(), ModStampItems.stamp_iron_wire.get(), ModStampItems.stamp_iron_circuit.get());
        stampSet(ModStampItems.stamp_steel_flat.get(), 2, ModStampItems.stamp_steel_plate.get(), ModStampItems.stamp_steel_wire.get(), ModStampItems.stamp_steel_circuit.get());
        stampSet(ModStampItems.stamp_titanium_flat.get(), 2, ModStampItems.stamp_titanium_plate.get(), ModStampItems.stamp_titanium_wire.get(), ModStampItems.stamp_titanium_circuit.get());
        stampSet(ModStampItems.stamp_obsidian_flat.get(), 2, ModStampItems.stamp_obsidian_plate.get(), ModStampItems.stamp_obsidian_wire.get(), ModStampItems.stamp_obsidian_circuit.get());
        stampSet(ModStampItems.stamp_desh_flat.get(), 3, ModStampItems.stamp_desh_plate.get(), ModStampItems.stamp_desh_wire.get(), ModStampItems.stamp_desh_circuit.get());

        // -------- the foundry machines (old AnvilRecipes L255-365) --------
        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Items.FURNACE),
                        STEEL.plate().count(8),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.heater_firebox.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        STEEL.plate().count(4),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.heater_oven.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(Blocks.STONE_BRICKS.asItem(), 8),
                        new TagStack(ItemTags.LOGS, 16),
                        CU.plateCast().count(2),
                        new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "ingots/firebrick")), 16)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_furnace_combination.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        ANY_PLASTIC.ingot().count(4),
                        CU.ingot().count(8),
                        STEEL.plate().count(8),   // old STEEL.plate528() = plate() at default config
                        new ComparableStack(ModItems.coil_tungsten.get(), 8),
                        cir(EnumCircuitType.BASIC, 1)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.heater_electric.get()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.ingot_firebrick.get(), 20),
                        CU.ingot().count(8),
                        STEEL.plate().count(8)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_crucible.get()))).setTier(2));

        // -------- oil wave: heat boiler + oilburner heater (old AnvilRecipes L280/372) --------
        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.ingot().count(4),
                        CU.plate().count(16),   // old CU.plate528() = plate() at default config
                        new ComparableStack(ModItems.plate_polymer.get(), 8)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_boiler.get()))).setTier(2));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        STEEL.plateCast().count(8),
                        CU.ingot().count(8),
                        ANY_PLASTIC.ingot().count(4)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.machine_industrial_boiler.get()))).setTier(3));

        add(new AnvilConstructionRecipe(
                new AStack[] {
                        new ComparableStack(ModItems.tank_steel.get(), 4),
                        STEEL.pipe().count(3),
                        TI.ingot().count(12),
                        CU.ingot().count(8)
                }, new AnvilOutput(new ItemStack(com.hbm.blocks.machine.ModMachineBlocks.heater_oilburner.get()))).setTier(2));

        // -------- casing + gun-part molds (old registerConstructionAmmo L609-618) --------
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(com.hbm.items.machine.ModMoldItems.mold_base.get()), IRON.ingot().count(2)},
                new AnvilOutput(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_c9.get()))).setTier(1));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(com.hbm.items.machine.ModMoldItems.mold_base.get()), IRON.ingot().count(2)},
                new AnvilOutput(new ItemStack(com.hbm.items.machine.ModMoldItems.mold_c50.get()))).setTier(1));
        for (var gunMold : new dev.golem.core.registry.RegistryObject[]{
                com.hbm.items.machine.ModMoldItems.mold_barrel_light, com.hbm.items.machine.ModMoldItems.mold_barrel_heavy,
                com.hbm.items.machine.ModMoldItems.mold_receiver_light, com.hbm.items.machine.ModMoldItems.mold_receiver_heavy,
                com.hbm.items.machine.ModMoldItems.mold_mechanism, com.hbm.items.machine.ModMoldItems.mold_stock,
                com.hbm.items.machine.ModMoldItems.mold_grip}) {
            add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(com.hbm.items.machine.ModMoldItems.mold_base.get()), STEEL.ingot().count(4)},
                    new AnvilOutput(new ItemStack((Item) gunMold.get()))).setTier(2));
        }

        // Ammo stamps (the mold-based ammo recipes are deferred with mold_base).
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_iron_flat.get()), GUNMETAL.ingot().count(2)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_9.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_iron_flat.get()), GUNMETAL.ingot().count(2)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_50.get()))).setTier(2));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_desh_flat.get()), WEAPONSTEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_9.get()))).setTier(4));
        add(new AnvilConstructionRecipe(new AStack[]{new ComparableStack(ModStampItems.stamp_desh_flat.get()), WEAPONSTEEL.ingot().count(4)}, new AnvilOutput(new ItemStack(ModStampItems.stamp_desh_50.get()))).setTier(4));
    }

    private static void stampSet(Item flat, int tier, Item... outputs) {
        for (Item o : outputs) {
            add(new AnvilConstructionRecipe(new ComparableStack(flat), new AnvilOutput(new ItemStack(o))).setTier(tier));
        }
    }

    private static void add(AnvilConstructionRecipe recipe) {
        constructionRecipes.add(recipe);
    }

    private static AnvilOutput out(Item item) {
        return new AnvilOutput(new ItemStack(item));
    }

    private static ComparableStack cir(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    private static ItemStack stirlingItemVariant(Block block, int variant) {
        ItemStack stack = new ItemStack(block.asItem());
        com.hbm.items.ModDataComponents.ITEM_VARIANT.set(stack, variant);
        return stack;
    }

    /** A construction recipe: consume the {@link AStack} inputs from the player's inventory, give the
     * (chance-rolled) outputs. Tier-gated by the anvil the player is using. */
    public static class AnvilConstructionRecipe {
        public final List<AStack> input = new ArrayList<>();
        public final List<AnvilOutput> output = new ArrayList<>();
        public int tierLower = 0;
        public int tierUpper = -1;
        private OverlayType overlay = OverlayType.NONE;

        public AnvilConstructionRecipe(AStack input, AnvilOutput output) {
            this.input.add(input);
            this.output.add(output);
            this.setOverlay(OverlayType.SMITHING); // preferred overlay for 1:1 conversions
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput output) {
            Collections.addAll(this.input, input);
            this.output.add(output);
            this.setOverlay(OverlayType.CONSTRUCTION); // preferred overlay for many:1 conversions
        }

        public AnvilConstructionRecipe(AStack input, AnvilOutput[] output) {
            this.input.add(input);
            Collections.addAll(this.output, output);
            this.setOverlay(OverlayType.RECYCLING); // preferred overlay for 1:many conversions
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput[] output) {
            Collections.addAll(this.input, input);
            Collections.addAll(this.output, output);
            this.setOverlay(OverlayType.NONE);
        }

        public AnvilConstructionRecipe setTier(int tier) {
            this.tierLower = tier;
            return this;
        }

        public AnvilConstructionRecipe setTierRange(int lower, int upper) {
            this.tierLower = lower;
            this.tierUpper = upper;
            return this;
        }

        public boolean isTierValid(int tier) {
            if (this.tierUpper == -1) return tier >= this.tierLower;
            return tier >= this.tierLower && tier <= this.tierUpper;
        }

        public AnvilConstructionRecipe setOverlay(OverlayType overlay) {
            this.overlay = overlay;
            return this;
        }

        public OverlayType getOverlay() {
            return this.overlay;
        }

        /** The icon shown in the recipe grid (the first output, or the recycled input for RECYCLING). */
        public ItemStack getDisplay() {
            if (this.overlay == OverlayType.RECYCLING) {
                for (AStack stack : this.input) {
                    if (stack instanceof ComparableStack comp) return comp.getDisplayStack();
                }
            }
            return this.output.get(0).stack.copy();
        }
    }

    /** One construction output with a drop chance (1 = guaranteed). */
    public static class AnvilOutput {
        public final ItemStack stack;
        public final float chance;

        public AnvilOutput(ItemStack stack) {
            this(stack, 1F);
        }

        public AnvilOutput(ItemStack stack, float chance) {
            this.stack = stack;
            this.chance = chance;
        }
    }

    /** The little type icon drawn over a recipe's grid cell. */
    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }
}
