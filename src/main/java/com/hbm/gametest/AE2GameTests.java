package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.handler.ae2.AE2CompatHandler;
import com.hbm.handler.ae2.NTMCraftingMachine;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ModEventHandlerHooks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.items.ModMachineItems;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.autocraft.CraftingMachine;
import dev.golem.core.autocraft.CraftingMachines;
import dev.golem.core.autocraft.CraftingPlan;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.storage.CellContents;
import dev.golem.core.storage.ItemStore;
import dev.golem.core.storage.ItemStores;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyView;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** AE2 pattern jobs into the recipe machines, through the neutral plan; the AE2 lookup itself only when AE2 is loaded. */
public final class AE2GameTests {

    private AE2GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PUSH = TESTS.function(
            "ae2_push_selects_recipe", 40, 8, () -> AE2GameTests::pushSelectsRecipe);
    public static final GameTests.Function CELLS = TESTS.function(
            "ae2_machine_reached_through_cells", 40, 8, () -> AE2GameTests::reachedThroughCells);
    public static final GameTests.Function CELL_RADIATION = TESTS.function(
            "ae2_cell_radiation", 20, () -> AE2GameTests::cellRadiation);
    public static final GameTests.Function MASS_STORAGE = TESTS.function(
            "ae2_mass_storage_store", 20, () -> AE2GameTests::massStorageStore);
    public static final GameTests.Function ARC_FURNACE = TESTS.function(
            "ae2_arc_furnace_store", 40, 8, () -> AE2GameTests::arcFurnaceStore);
    public static final GameTests.Function CERTUS = TESTS.function(
            "ae2_certus_cluster_rows", 20, () -> AE2GameTests::certusClusterRows);
    public static final GameTests.Function EXPLOSIVE = TESTS.function(
            "ae2_explosive_energistics", 20, () -> AE2GameTests::explosiveEnergistics);
    public static final GameTests.Function ENERGY = TESTS.function(
            "ae2_energy_acceptor_reached", 20, () -> AE2GameTests::energyAcceptorReached);

    public static void init() {}

    public static void pushSelectsRecipe(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_machine.get(), core);
        TileEntityMachineAssemblyMachine machine = GameTestHelpers.blockEntity(helper, core, TileEntityMachineAssemblyMachine.class);
        CraftingMachine target = AE2CompatHandler.machineOf(machine);
        helper.assertTrue(target instanceof NTMCraftingMachine, "The assembly machine must take AE2 jobs");

        GenericRecipe recipe = null;
        for (GenericRecipe candidate : machine.assemblerModule.getRecipeSet().recipeOrderedList) {
            if (!candidate.isPooled() && candidate.inputFluid == null && candidate.inputItem != null
                    && candidate.outputItem != null && candidate.outputItem.length > 0) {
                recipe = candidate;
                break;
            }
        }
        helper.assertTrue(recipe != null, "Expected an unpooled item-only assembly recipe");
        List<ItemStack> items = new ArrayList<>();
        for (AStack in : recipe.inputItem) items.add(ItemStacks.copyWithCount(in.getDisplayStacks().get(0), in.stacksize));
        ItemStack output = recipe.outputItem[0];

        List<ItemStack> extra = new ArrayList<>(items);
        extra.add(new ItemStack(Items.DIRT));
        helper.assertTrue(!target.push(new CraftingPlan(extra, List.of(), output, null)),
                "A job with an ingredient the recipe has no place for must be refused, not voided");
        helper.assertTrue(target.push(new CraftingPlan(items, List.of(), output, null)), "The matching job was refused");
        helper.assertTrue(recipe.getInternalName().equals(machine.assemblerModule.recipe),
                "The job did not select " + recipe.getInternalName() + " but " + machine.assemblerModule.recipe);
        for (int i = 0; i < recipe.inputItem.length; i++) {
            ItemStack placed = machine.inventory.getStackInSlot(machine.assemblerModule.inputSlots[i]);
            helper.assertTrue(recipe.inputItem[i].matchesRecipe(placed, false), "Ingredient " + i + " was not placed");
        }
        helper.assertTrue(!target.acceptsPlans(), "A machine holding a job must refuse the next one");
        helper.succeed();
    }

    public static void reachedThroughCells(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        BlockPos coreAbs = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_machine.get(), core);
        ServerLevel level = helper.getLevel();
        TileEntityMachineAssemblyMachine machine = GameTestHelpers.blockEntity(helper, core, TileEntityMachineAssemblyMachine.class);
        helper.assertTrue(AE2CompatHandler.owner(level, coreAbs, level.getBlockState(coreAbs), machine, null) == machine,
                "The core must answer");
        if (CraftingMachines.available()) {
            helper.assertTrue(CraftingMachines.exposed(level, coreAbs, Direction.UP), "AE2's own lookup does not see the core");
        }

        int reached = 0;
        for (BlockPos pos : BlockPos.betweenClosed(coreAbs.offset(-2, 0, -2), coreAbs.offset(2, 2, 2))) {
            if (pos.equals(coreAbs) || level.getBlockEntity(pos) != null) continue;
            for (Direction side : Direction.values()) {
                BlockEntity found = AE2CompatHandler.owner(level, pos, level.getBlockState(pos), null, side);
                if (found == null) continue;
                helper.assertTrue(found == machine, "A cell answered for another machine at " + pos);
                if (CraftingMachines.available() && CraftingMachines.reachesBlocksWithoutBlockEntity()) {
                    helper.assertTrue(CraftingMachines.exposed(level, pos.immutable(), side),
                            "AE2's own lookup does not see the machine at " + pos + " " + side);
                }
                reached++;
            }
        }
        helper.assertTrue(reached > 0, "No cell of the assembly machine reaches it");
        helper.succeed();
    }

    /** 1.7 HazardTransformerRadiationME: ten U-235 ingots in a 1k cell are ten ingots' radiation. Needs AE2 1.21+. */
    public static void cellRadiation(GameTestHelper helper) {
        if (!CraftingMachines.available()) {
            helper.succeed();
            return;
        }
        ItemStack ingot = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse("hbm:ingot_u235")));
        double each = HazardSystem.getHazardLevelFromStack(ingot, HazardRegistry.RADIATION);
        helper.assertTrue(each > 0, "U-235 ingots are not radioactive");
        ItemStack empty = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse("ae2:item_storage_cell_1k")));
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(empty, HazardRegistry.RADIATION) == 0, "An empty cell is radioactive");
        ItemStack cell = ItemStacks.withComponents(empty, JsonParser.parseString(
                        "{\"ae2:storage_cell_inv\":[{\"#t\":\"ae2:i\",\"id\":\"hbm:ingot_u235\",\"#\":10}]}").getAsJsonObject(),
                RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess())).orElse(empty);
        if (CellContents.items(cell).isEmpty()) {
            // 1.20.1's AE2 keeps cell contents in NBT this component spelling does not reach
            helper.succeed();
            return;
        }
        double radiation = HazardSystem.getHazardLevelFromStack(cell, HazardRegistry.RADIATION);
        helper.assertTrue(Math.abs(radiation - each * 10) < 1E-3, "Cell radiation " + radiation + ", expected " + each * 10);
        helper.succeed();
    }

    /** 1.7 MassStorageMEInventory: the stockpile and both slots as one store, and AE2's own lookup reaching it. */
    public static void massStorageStore(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModStorageBlocks.mass_storage_iron.get().defaultBlockState());
        TileEntityMassStorage msu = GameTestHelpers.blockEntity(helper, rel, TileEntityMassStorage.class);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        msu.setFilter(iron);
        msu.setStockpile(100);
        msu.inventory.setStackInSlot(TileEntityMassStorage.OUTPUT_SLOT, new ItemStack(Items.IRON_INGOT, 10));
        BlockPos pos = helper.absolutePos(rel);
        ServerLevel level = helper.getLevel();

        ItemStore store = AE2CompatHandler.storeAt(level, pos, level.getBlockState(pos), msu, Direction.UP);
        helper.assertTrue(store != null && store.prefers(iron) && !store.prefers(new ItemStack(Items.GOLD_INGOT)),
                "The mass storage unit must prefer its own type only");
        long[] listed = {0};
        store.list((type, count) -> listed[0] += count);
        helper.assertTrue(listed[0] == 110, "Listed " + listed[0] + ", expected stockpile 100 + output 10");
        helper.assertTrue(store.insert(new ItemStack(Items.GOLD_INGOT), 5, false) == 0, "Took a foreign type");
        helper.assertTrue(store.extract(iron, 105, true) == 105 && msu.getTotalStockpile() == 110, "Simulation changed it");
        helper.assertTrue(store.extract(iron, 105, false) == 105 && msu.getTotalStockpile() == 5, "Extraction mismatch");
        helper.assertTrue(store.insert(iron, 20, false) == 20 && msu.getTotalStockpile() == 25, "Insertion mismatch");

        if (CraftingMachines.available()) {
            ItemStore viaAe2 = ItemStores.find(level, pos, Direction.UP);
            helper.assertTrue(viaAe2 != null, "AE2's own lookup does not find the mass storage unit");
            helper.assertTrue(viaAe2.extract(iron, 5, false) == 5 && msu.getTotalStockpile() == 20,
                    "An extraction through AE2's interface did not reach the stockpile");
        }
        helper.succeed();
    }

    /** 1.7 ArcFurnaceLargeMEInventory: electrodes go into 0..2, products come out of the grid, inputs stay unlisted. */
    public static void arcFurnaceStore(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        BlockPos coreAbs = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_arc_furnace.get(), core);
        ServerLevel level = helper.getLevel();
        TileEntityMachineArcFurnaceLarge afl = GameTestHelpers.blockEntity(helper, core, TileEntityMachineArcFurnaceLarge.class);
        ItemStore store = AE2CompatHandler.storeAt(level, coreAbs, level.getBlockState(coreAbs), afl, null);
        helper.assertTrue(store != null, "The arc furnace core must answer");

        ItemStack electrode = new ItemStack(ModMachineItems.arc_electrode.get());
        helper.assertTrue(store.insert(electrode, 5, true) == 3, "Three electrode slots, three electrodes");
        helper.assertTrue(store.insert(electrode, 5, false) == 3, "Electrodes were not placed");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(afl.inventory.getStackInSlot(i).getItem() == electrode.getItem(), "Slot " + i + " has no electrode");
        }
        helper.assertTrue(store.extract(electrode, 1, false) == 0, "Fresh electrodes must stay in the furnace");

        ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.solidRecipes().get(0);
        ItemStack input = ItemStacks.copyWithCount(recipe.input.getDisplayStacks().get(0), 1);
        helper.assertTrue(store.insert(input, 1, false) == 1, "Recipe input was not taken into the grid");
        long[] listedInput = {0};
        store.list((type, count) -> {
            if (ItemStacks.sameItemAndData(type, input)) listedInput[0] += count;
        });
        helper.assertTrue(listedInput[0] == 0, "Unprocessed grid input must not be listed");

        ItemStack burnt = new ItemStack(ModMachineItems.arc_electrode_burnt.get());
        afl.inventory.setStackInSlot(0, burnt.copy());
        helper.assertTrue(store.extract(burnt, 1, false) == 1 && afl.inventory.getStackInSlot(0).isEmpty(),
                "A burnt electrode must come out");

        if (CraftingMachines.available()) {
            helper.assertTrue(ItemStores.find(level, coreAbs, Direction.UP) != null,
                    "AE2's own lookup does not find the arc furnace");
        }
        helper.succeed();
    }

    /** AE2's quartz clusters stand in for 1.7's certus ore in the centrifuge and the crystallizer. */
    public static void certusClusterRows(GameTestHelper helper) {
        if (!CraftingMachines.available()) {
            helper.succeed();
            return;
        }
        ItemStack cluster = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse("ae2:quartz_cluster")));
        ItemStack crystal = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse("ae2:certus_quartz_crystal")));
        ItemStack[] spun = CentrifugeRecipes.getOutput(cluster);
        helper.assertTrue(spun != null && spun.length == 4 && ItemStack.isSameItem(spun[0], crystal) && spun[0].getCount() == 2,
                "The centrifuge does not turn a quartz cluster into 4 x 2 certus crystals");
        helper.assertTrue(CrystallizerRecipes.isAnyRecipeInput(cluster), "The crystallizer does not take quartz clusters");
        helper.succeed();
    }

    /** 1.7 528 mode: a right-clicked AE2 block is destroyed; without 528 the click passes. */
    public static void explosiveEnergistics(GameTestHelper helper) {
        if (!CraftingMachines.available()) {
            helper.succeed();
            return;
        }
        Block sky = BuiltInRegistries.BLOCK.getOptional(Ids.parse("ae2:sky_stone_block")).orElseThrow();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, sky.defaultBlockState());
        BlockPos pos = helper.absolutePos(rel);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        boolean before = GeneralConfig.enable528ExplosiveEnergistics;
        try {
            GeneralConfig.enable528ExplosiveEnergistics = false;
            helper.assertTrue(!ModEventHandlerHooks.explosiveEnergistics(player, InteractionHand.MAIN_HAND, hit).consumesAction()
                    && helper.getLevel().getBlockState(pos).is(sky), "Without 528 the click must pass");
            GeneralConfig.enable528ExplosiveEnergistics = true;
            helper.assertTrue(ModEventHandlerHooks.explosiveEnergistics(player, InteractionHand.MAIN_HAND, hit).consumesAction()
                    && !helper.getLevel().getBlockState(pos).is(sky), "In 528 mode the AE2 block must be destroyed");
        } finally {
            GeneralConfig.enable528ExplosiveEnergistics = before;
        }
        helper.succeed();
    }

    /** HBM cables feed AE2 through the loader's energy lookup, the one cable arms ask; no AE2-specific code. */
    public static void energyAcceptorReached(GameTestHelper helper) {
        if (!CraftingMachines.available()) {
            helper.succeed();
            return;
        }
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, BuiltInRegistries.BLOCK.getOptional(Ids.parse("ae2:energy_acceptor")).orElseThrow().defaultBlockState());
        BlockPos pos = helper.absolutePos(rel);
        helper.assertTrue(EnergyCompat.exposes(helper.getLevel(), pos, Direction.UP), "Cables would not connect to the acceptor");
        // AE2 grid nodes come up on a later tick; until then the acceptor refuses energy
        helper.succeedWhen(() -> {
            EnergyView view = EnergyCompat.find(helper.getLevel(), pos, Direction.UP);
            helper.assertTrue(view != null && view.canInsert(), "The acceptor takes no energy");
        });
    }
}
