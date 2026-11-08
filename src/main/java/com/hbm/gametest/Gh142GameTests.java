package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModSatelliteBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.util.ConveyorInventories;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemExposure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** GitHub issue 142: fuel into the firebox and heating oven through any cell, Fluid Burner heat for every consumer. */
public final class Gh142GameTests {

    private Gh142GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FUEL =
            TESTS.function("gh142_heaters_take_fuel_through_cells", 20, () -> Gh142GameTests::fuel);
    public static final GameTests.Function BURNER =
            TESTS.function("gh142_fluid_burner_heats_every_consumer", 60, 24, () -> Gh142GameTests::burner);

    public static final GameTests.Function CELLS =
            TESTS.function("gh142_inventory_cells_reach_the_core", 20, 70, () -> Gh142GameTests::cells);

    public static void init() {}

    /** The machines 1.7 gave inventory proxies away from the core: an inserter at one reaches the core's container. */
    public static void cells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BlockMultiblockCore> blocks = List.of(ModMachineBlocks.launchpad_lambda.get(), ModMachineBlocks.launchpad_soyuz.get(),
                ModMachineBlocks.machine_liquefactor.get(), ModMachineBlocks.machine_solidifier.get(),
                ModMachineBlocks.machine_wood_burner.get(), ModBatteryBlocks.machine_battery_socket.get(),
                ModSatelliteBlocks.sat_dock.get(), ModRBMKBlocks.rbmk_outgasser.get(), ModRBMKBlocks.rbmk_storage.get(),
                ModTurretBlocks.turret_chekhov.get(), ModTurretBlocks.turret_arty.get());
        List<String> closed = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos core = FoldedTestSupport.assemble(helper, blocks.get(i), new BlockPos(14 + 26 * (i % 4), 2, 14 + 26 * (i / 4)));
            BlockEntity owner = level.getBlockEntity(core);
            int open = 0;
            for (BlockPos pos : BlockPos.betweenClosed(core.offset(-12, -2, -12), core.offset(12, 10, 12))) {
                if (!pos.equals(core) && ConveyorInventories.containerAt(level, pos) == owner) open++;
            }
            if (open == 0) closed.add(blocks.get(i).toString());
            if (blocks.get(i) == ModRBMKBlocks.rbmk_storage.get() && ConveyorInventories.containerAt(level, core.above()) != owner) {
                closed.add("rbmk_storage middle");
            }
        }
        helper.assertTrue(closed.isEmpty(), "no cell reaches the core's inventory: " + closed);
        helper.succeed();
    }

    /** 1.7 proxies the core's inventory (slots 0 and 1, burnable items only) on every cell of both machines. */
    public static void fuel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockMultiblockCore[] heaters = {ModMachineBlocks.heater_firebox.get(), ModMachineBlocks.heater_oven.get()};
        for (int h = 0; h < heaters.length; h++) {
            BlockPos core = FoldedTestSupport.assemble(helper, heaters[h], new BlockPos(2 + 5 * h, 2, 2));
            TileEntityFireboxBase heater = (TileEntityFireboxBase) level.getBlockEntity(core);
            for (Direction out : Direction.Plane.HORIZONTAL) {
                BlockPos cell = core.relative(out);
                Direction face = out;
                heater.inventory.setStackInSlot(0, ItemStack.EMPTY);
                heater.inventory.setStackInSlot(1, ItemStack.EMPTY);

                Container target = ConveyorInventories.containerAt(level, cell);
                helper.assertTrue(target != null, heaters[h] + ": no inventory at the cell " + out + " of the core");
                ItemStack left = ConveyorInventories.addToInventory(target, ConveyorInventories.masquerade(target, face),
                        new ItemStack(Items.COAL, 5), face);
                helper.assertTrue(left.isEmpty() && heater.inventory.getStackInSlot(0).is(Items.COAL),
                        heaters[h] + ": an inserter at the " + out + " cell left " + left);
                ItemStack stone = ConveyorInventories.addToInventory(target, ConveyorInventories.masquerade(target, face),
                        new ItemStack(Items.COBBLESTONE), face);
                helper.assertTrue(stone.getCount() == 1, heaters[h] + ": cobblestone went in");

                int piped = ItemExposure.insert(level, cell, face, new ItemStack(Items.CHARCOAL, 3));
                helper.assertTrue(piped == 3 && heater.inventory.getStackInSlot(1).is(Items.CHARCOAL),
                        heaters[h] + ": another mod's pipe at the " + out + " cell moved " + piped);
            }
        }
        helper.succeed();
    }

    /**
     * Every 1.7 consumer drains the Fluid Burner through the heat proxy above its core: each burner ends below one with
     * nothing on top. A boiler over a firebox cell gets nothing, since 1.7's firebox cells are no heat source.
     */
    public static void burner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BlockMultiblockCore> consumers = List.of(ModMachineBlocks.machine_crucible.get(),
                ModMachineBlocks.machine_stirling.get(), ModMachineBlocks.machine_sawmill.get(),
                ModMachineBlocks.furnace_steel.get(), ModMachineBlocks.heater_electric.get(),
                ModMachineBlocks.heater_oven.get(), ModMachineBlocks.machine_boiler.get(),
                ModMachineBlocks.machine_furnace_combination.get(), ModMachineBlocks.machine_coker.get(),
                ModMachineBlocks.machine_industrial_boiler.get());

        List<TileEntityHeaterOilburner> burners = new ArrayList<>();
        for (int i = 0; i < consumers.size(); i++) {
            BlockPos at = new BlockPos(2 + 5 * (i % 5), 1, 2 + 6 * (i / 5));
            burners.add(burner(helper, at));
            FoldedTestSupport.assemble(helper, consumers.get(i), at.above(2));
        }
        TileEntityHeaterOilburner control = burner(helper, new BlockPos(12, 1, 16));

        BlockPos firebox = FoldedTestSupport.assemble(helper, ModMachineBlocks.heater_firebox.get(), new BlockPos(2, 1, 16));
        ((TileEntityFireboxBase) level.getBlockEntity(firebox)).heatEnergy = 1_000;
        BlockPos offBoiler = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_boiler.get(), new BlockPos(3, 2, 16));

        helper.runAfterDelay(10, () -> {
            List<String> cold = new ArrayList<>();
            for (int i = 0; i < consumers.size(); i++) {
                if (burners.get(i).heatEnergy >= control.heatEnergy) cold.add(consumers.get(i).toString());
            }
            int stray = ((TileEntityHeatBoiler) level.getBlockEntity(offBoiler)).heat;
            helper.assertTrue(cold.isEmpty(), "no heat from the Fluid Burner (" + control.heatEnergy + " TU left alone): " + cold);
            helper.assertTrue(stray == 0, "a boiler over a firebox cell took " + stray + " TU");
            helper.succeed();
        });
    }

    private static TileEntityHeaterOilburner burner(GameTestHelper helper, BlockPos at) {
        BlockPos core = FoldedTestSupport.assemble(helper, ModMachineBlocks.heater_oilburner.get(), at);
        TileEntityHeaterOilburner burner = (TileEntityHeaterOilburner) helper.getLevel().getBlockEntity(core);
        burner.heatEnergy = 1_000;
        return burner;
    }
}
