package com.hbm.gametest;

import com.hbm.blocks.machine.pwr.BlockPWR;
import com.hbm.blocks.machine.pwr.MachinePWRController;
import com.hbm.blocks.machine.pwr.ModPWRBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.tileentity.machine.TileEntityWasteDrum;
import com.hbm.tileentity.machine.pwr.TileEntityBlockPWR;
import com.hbm.tileentity.machine.pwr.TileEntityPWRController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

/**
 * GameTests for the pressurised water reactor: flood-filling a shell of casing into a running structure,
 * the neutron economy the survey derives from it, the control rods over radio, and taking it apart again.
 */
public final class PWRGameTests {

    private PWRGameTests() {}

    /** The controller, set into the north wall of the shell and facing out of it. */
    private static final BlockPos CONTROLLER = new BlockPos(3, 3, 1);
    /** The coolant port, set into the west wall. */
    private static final BlockPos PORT = new BlockPos(1, 3, 3);
    /** The two fuel rods, with a control block between them. */
    private static final BlockPos ROD_WEST = new BlockPos(2, 3, 3);
    private static final BlockPos ROD_EAST = new BlockPos(4, 3, 3);
    private static final BlockPos CONTROL = new BlockPos(3, 3, 3);
    private static final BlockPos SOURCE = new BlockPos(3, 2, 3);
    private static final BlockPos HEATEX = new BlockPos(3, 4, 3);

    public static void assembly(GameTestHelper helper) {
        TileEntityPWRController controller = assemble(helper);

        helper.assertTrue(controller.assembled, "Reactor refused to assemble");
        helper.assertTrue(controller.rodCount == 2, "Expected two fuel rods, got " + controller.rodCount);
        helper.assertTrue(controller.sourceCount == 1, "Expected one neutron source, got " + controller.sourceCount);
        helper.assertTrue(controller.heatexCount == 1, "Expected one heat exchanger, got " + controller.heatexCount);
        helper.assertTrue(controller.channelCount == 22,
                "Expected twenty-two coolant channels, got " + controller.channelCount);
        helper.assertTrue(controller.connectionsControlled == 1,
                "The rod pair sees each other through a control block, got " + controller.connectionsControlled);
        helper.assertTrue(controller.connections == 0,
                "No pair of rods sees each other unobstructed, got " + controller.connections);

        helper.assertBlockPresent(ModPWRBlocks.pwr_block.get(), ROD_WEST);
        helper.assertBlockPresent(ModPWRBlocks.pwr_block.get(), PORT);

        helper.assertTrue(!state(helper, ROD_WEST).getValue(BlockPWR.IO_ENABLED),
                "A fuel rod cell must not be a port");
        helper.assertTrue(state(helper, PORT).getValue(BlockPWR.IO_ENABLED),
                "The access port cell should carry fluid");

        TileEntityBlockPWR cell = cell(helper, ROD_WEST);
        helper.assertTrue(cell.getCore() == controller, "A cell did not point at its controller");
        helper.assertTrue(cell.originalBlockState != null
                        && cell.originalBlockState.is(ModPWRBlocks.pwr_fuelrod.get()),
                "A cell forgot which part it was built from");
        helper.succeed();
    }

    public static void reaction(GameTestHelper helper) {
        TileEntityPWRController controller = assemble(helper);
        controller.inventory.setStackInSlot(0, ModPWRBlocks.pwr_fuel.get().of(EnumPWRFuel.HEU235, 2));
        controller.tanks[0].setTankType(Fluids.COOLANT);
        controller.tanks[0].setFill(controller.tanks[0].getMaxFill());
        controller.rodLevel = 0;
        controller.rodTarget = 0;

        tick(helper, controller);
        helper.assertTrue(controller.unloadDelay == 0,
                "Reactor idled itself: the chunks around the test plot read as unloaded");
        helper.assertTrue(controller.amountLoaded == 1, "Reactor did not take a pellet from its input slot");
        helper.assertTrue(controller.typeLoaded == EnumPWRFuel.HEU235.ordinal(),
                "Reactor loaded the wrong fuel grade");

        for (int i = 0; i < 60; i++) tick(helper, controller);

        helper.assertTrue(controller.amountLoaded == 2, "Reactor did not take its second pellet");
        helper.assertTrue(controller.flux > 0D, "A fuelled reactor with a neutron source should carry flux");
        helper.assertTrue(controller.coreHeat > 0, "A running reactor should heat its core");
        helper.assertTrue(controller.hullHeat > 0,
                "The heat exchanger should move core heat into the hull");
        helper.assertTrue(controller.tanks[1].getFill() > 0,
                "The coolant channels should be producing hot coolant");
        helper.assertTrue(controller.tanks[1].getTankType() == Fluids.COOLANT_HOT,
                "Hot coolant tank took the wrong type");
        helper.succeed();
    }

    public static void rods(GameTestHelper helper) {
        TileEntityPWRController controller = assemble(helper);
        controller.inventory.setStackInSlot(0, ModPWRBlocks.pwr_fuel.get().of(EnumPWRFuel.HEU235));
        tick(helper, controller);

        helper.assertTrue(controller.rodLevel == 100D, "A fresh reactor should start with its rods in");
        double inserted = controller.getTotalProcessMultiplier();
        helper.assertTrue(inserted == 0D, "Fully inserted rods should cut the controlled coupling to nothing");

        controller.runRORFunction("FUN:setrods", new String[]{"0"});
        helper.assertTrue(controller.rodTarget == 0D, "setrods did not move the target");
        for (int i = 0; i < 120; i++) tick(helper, controller);
        helper.assertTrue(controller.rodLevel == 0D, "Rods did not travel to their target");
        helper.assertTrue("0".equals(controller.provideRORValue("VAL:rods")),
                "VAL:rods should report the rods fully withdrawn");
        helper.assertTrue(controller.getTotalProcessMultiplier() > inserted,
                "Withdrawing the rods should open the controlled coupling");

        helper.assertTrue(controller.provideRORValue("VAL:flux") != null, "VAL:flux went missing");
        helper.assertTrue(controller.provideRORValue("VAL:coreheat") != null, "VAL:coreheat went missing");
        helper.assertTrue(controller.provideRORValue("VAL:depletion") != null, "VAL:depletion went missing");

        controller.runRORFunction("FUN:jettison", new String[0]);
        helper.assertTrue(controller.amountLoaded == 0 && controller.typeLoaded == -1,
                "jettison should empty the core");

        TileEntityBlockPWR port = cell(helper, PORT);
        helper.assertTrue("0".equals(port.provideRORValue("VAL:rods")),
                "The access port should answer for its controller");
        TileEntityBlockPWR plain = cell(helper, ROD_WEST);
        helper.assertTrue(plain.provideRORValue("VAL:rods") == null,
                "A cell that is not a port must not answer");
        helper.succeed();
    }

    public static void disassembly(GameTestHelper helper) {
        TileEntityPWRController controller = assemble(helper);
        TileEntityBlockPWR survivor = cell(helper, ROD_EAST);

        helper.getLevel().destroyBlock(helper.absolutePos(ROD_WEST), false);
        helper.assertTrue(!controller.assembled,
                "Breaking a cell should take the reactor out of service");

        survivor.restoreIfOrphaned(helper.getLevel(), helper.absolutePos(ROD_EAST));
        helper.assertBlockPresent(ModPWRBlocks.pwr_fuelrod.get(), ROD_EAST);
        helper.succeed();
    }

    public static void fuel(GameTestHelper helper) {
        ItemStack pellet = ModPWRBlocks.pwr_fuel.get().of(EnumPWRFuel.HES327);
        helper.assertTrue(ItemPWRFuel.typeOf(pellet) == EnumPWRFuel.HES327, "Fuel pellet lost its grade");
        helper.assertTrue(EnumPWRFuel.HES327.function.effonix(100D) > EnumPWRFuel.MEU.function.effonix(100D),
                "The hotter grade should give back more flux at the same input");
        helper.assertTrue(EnumPWRFuel.BFB_PU241.yield < EnumPWRFuel.MEU.yield,
                "A breeder pellet burns out four times sooner");

        ItemStack hot = ModPWRBlocks.pwr_fuel_hot.get().of(EnumPWRFuel.HES327);
        ItemStack cooled = TileEntityWasteDrum.cooled(hot);
        helper.assertTrue(cooled.getItem() == ModPWRBlocks.pwr_fuel_depleted.get(),
                "A waste drum should cool hot fuel into depleted fuel");
        helper.assertTrue(ModPWRBlocks.pwr_fuel_depleted.get().getType(cooled) == EnumPWRFuel.HES327,
                "Cooling should keep the fuel grade");
        helper.assertTrue(TileEntityWasteDrum.cooled(cooled).isEmpty(),
                "Depleted fuel has nothing left to cool");

        helper.assertTrue(Fluids.HEAVYWATER.hasTrait(com.hbm.inventory.fluid.trait.FT_PWRModerator.class),
                "Heavy water should moderate the reactor");
        helper.assertTrue(Fluids.HEAVYWATER.getTrait(
                        com.hbm.inventory.fluid.trait.FT_PWRModerator.class).getMultiplier() > 1D,
                "Heavy water should raise flux, not lower it");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/blockstates/pwr_block.json",
                "assets/hbm/blockstates/pwr_controller.json",
                "assets/hbm/blockstates/pwr_fuelrod.json",
                "assets/hbm/models/block/pwr_block.json",
                "assets/hbm/models/block/pwr_casing_port.json",
                "assets/hbm/models/block/pwr_controller.json",
                "assets/hbm/models/block/pwr_channel.json",
                "assets/hbm/textures/blocks/pwr_block.png",
                "assets/hbm/textures/blocks/pwr_casing_port.png",
                "assets/hbm/textures/blocks/pwr_controller.png",
                "assets/hbm/textures/blocks/pwr_fuelrod_top.png",
                "assets/hbm/textures/gui/reactors/gui_pwr.png",
                "assets/hbm/textures/items/pwr_fuel_heu235.png",
                "assets/hbm/textures/items/pwr_fuel_hot.png",
                "assets/hbm/textures/items/pwr_fuel_depleted.png",
                "assets/hbm/items/pwr_fuel.json",
                "assets/hbm/items/pwr_controller.json",
                "assets/hbm/sounds/block/reactorloop.ogg",
                "data/hbm/loot_table/blocks/pwr_controller.json",
                "data/hbm/loot_table/blocks/pwr_block.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing PWR asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    /**
     * Builds a 5x5x5 shell of casing around a 3x3x3 core, sets the controller into the north wall and a
     * port into the west one, and assembles it.
     */
    private static TileEntityPWRController assemble(GameTestHelper helper) {
        Block casing = ModPWRBlocks.pwr_casing.get();
        Block channel = ModPWRBlocks.pwr_channel.get();

        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    boolean inside = x >= 2 && x <= 4 && y >= 2 && y <= 4 && z >= 2 && z <= 4;
                    helper.setBlock(new BlockPos(x, y, z), (inside ? channel : casing).defaultBlockState());
                }
            }
        }

        helper.setBlock(ROD_WEST, ModPWRBlocks.pwr_fuelrod.get().defaultBlockState());
        helper.setBlock(ROD_EAST, ModPWRBlocks.pwr_fuelrod.get().defaultBlockState());
        helper.setBlock(CONTROL, ModPWRBlocks.pwr_control.get().defaultBlockState());
        helper.setBlock(SOURCE, ModPWRBlocks.pwr_neutron_source.get().defaultBlockState());
        helper.setBlock(HEATEX, ModPWRBlocks.pwr_heatex.get().defaultBlockState());
        helper.setBlock(PORT, ModPWRBlocks.pwr_port.get().defaultBlockState());

        helper.setBlock(CONTROLLER, ModPWRBlocks.pwr_controller.get().defaultBlockState()
                .setValue(MachinePWRController.FACING, Direction.NORTH));

        TileEntityPWRController controller = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, CONTROLLER, TileEntityPWRController.class);
        helper.assertTrue(controller != null, "PWR controller BlockEntity was not created");

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ModPWRBlocks.pwr_controller.get().assemble(helper.getLevel(), helper.absolutePos(CONTROLLER),
                state(helper, CONTROLLER), player);
        return controller;
    }

    private static void tick(GameTestHelper helper, TileEntityPWRController controller) {
        BlockPos absolute = helper.absolutePos(CONTROLLER);
        Level level = helper.getLevel();
        TileEntityPWRController.serverTick(level, absolute, level.getBlockState(absolute), controller);
    }

    private static TileEntityBlockPWR cell(GameTestHelper helper, BlockPos relative) {
        TileEntityBlockPWR cell = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityBlockPWR.class);
        helper.assertTrue(cell != null, "No reactor cell at " + relative);
        return cell;
    }

    private static BlockState state(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockState(helper.absolutePos(relative));
    }

    private static boolean resourceExists(String path) {
        try (InputStream stream = PWRGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
}
