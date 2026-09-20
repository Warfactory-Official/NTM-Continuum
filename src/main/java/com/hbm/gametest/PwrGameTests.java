package com.hbm.gametest;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.blocks.machine.MachinePWRController;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.items.ModPwrItems;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityPWRController;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contracts for the PWR: the controller's structure scan turns a bounded core into linked shell cells whose
 * link survives a save/load round trip and whose removal puts the part back, and one fission step behaves as
 * the 1.7 arithmetic says. Registered from {@link HbmGameTestFunctions}.
 *
 * <p>The fixture: a 3x3x3 core (three fuel rods in a row along z, two control rods beside the middle one, a
 * neutron source below it, a heat exchanger above it, four heatsinks and sixteen channels) inside a 5x5x5
 * vessel with one access port, the controller set into the vessel's north wall facing north.
 */
public final class PwrGameTests {

    private PwrGameTests() {}

    private static final BlockPos CONTROLLER = new BlockPos(6, 3, 6);
    private static final BlockPos PORT = new BlockPos(4, 3, 8);

    public static void assembly(GameTestHelper helper) {
        TileEntityPWRController controller = build(helper);
        helper.assertTrue(controller.assembled, "PWR did not assemble");
        helper.assertTrue(controller.rodCount == 3, "PWR counted " + controller.rodCount + " fuel rods, expected 3");
        helper.assertTrue(controller.sourceCount == 1 && controller.heatexCount == 1 && controller.channelCount == 16
                && controller.heatsinkCount == 4, "PWR part counts drifted: src=" + controller.sourceCount + " hx="
                + controller.heatexCount + " ch=" + controller.channelCount + " hs=" + controller.heatsinkCount);
        helper.assertTrue(controller.connections == 2 && controller.connectionsControlled == 0,
                "PWR rod connections drifted: " + controller.connections + " / " + controller.connectionsControlled);
        helper.assertTrue(controller.coreHeatCapacity == 12_000_000L,
                "four heatsinks must raise the core capacity to 12,000,000, got " + controller.coreHeatCapacity);

        BlockPos core = helper.absolutePos(CONTROLLER);
        int cells = 0;
        for (int x = 4; x <= 8; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 6; z <= 10; z++) {
                    BlockPos rel = new BlockPos(x, y, z);
                    if (rel.equals(CONTROLLER)) continue;
                    BlockPos abs = helper.absolutePos(rel);
                    BlockState state = helper.getLevel().getBlockState(abs);
                    if (!reachedByScan(rel)) {
                        // 1.7's flood fill only steps out of core blocks, so vessel edges and corners
                        // (never face-adjacent to a core cell) are left as the casing they were.
                        helper.assertTrue(state.is(expectedPart(rel)), "vessel edge/corner " + rel + " must stay casing");
                        continue;
                    }
                    helper.assertTrue(state.getBlock() instanceof BlockPWR, "cell " + rel + " was not converted");
                    helper.assertTrue(state.getValue(BlockPWR.PORT) == rel.equals(PORT), "port flag wrong at " + rel);
                    BlockEntity be = helper.getLevel().getBlockEntity(abs);
                    helper.assertTrue(be instanceof TileEntityBlockPWR cell && cell.block == expectedPart(rel)
                            && cell.coreX == core.getX() && cell.coreY == core.getY() && cell.coreZ == core.getZ(),
                            "cell " + rel + " is not linked to the controller with its original block");
                    cells++;
                }
            }
        }
        // 27 core cells plus the 54 casing cells on the core's six faces, minus the controller itself
        helper.assertTrue(cells == 27 + 54 - 1, "expected 80 shell cells, saw " + cells);

        // The link survives a save/load round trip.
        BlockPos portAbs = helper.absolutePos(PORT);
        TileEntityBlockPWR port = (TileEntityBlockPWR) helper.getLevel().getBlockEntity(portAbs);
        CompoundTag saved = BlockEntities.save(port, helper.getLevel().registryAccess());
        TileEntityBlockPWR reloaded = new TileEntityBlockPWR(portAbs, helper.getLevel().getBlockState(portAbs));
        reloaded.setLevel(helper.getLevel());
        BlockEntities.loadWithComponents(reloaded, saved, helper.getLevel().registryAccess());
        helper.assertTrue(reloaded.block == ModPwrBlocks.PWR_PORT.get() && reloaded.coreX == core.getX()
                && reloaded.coreY == core.getY() && reloaded.coreZ == core.getZ(),
                "shell cell lost its controller link across save/load");
        helper.assertTrue(reloaded.getCore() == controller, "reloaded cell does not resolve its controller");

        // Breaking a cell puts the part back and disassembles the reactor.
        BlockPos heatex = helper.absolutePos(new BlockPos(6, 4, 8));
        helper.getLevel().removeBlock(heatex, false);
        helper.assertTrue(helper.getLevel().getBlockState(heatex).is(ModPwrBlocks.PWR_HEATEX.get()),
                "breaking a shell cell must put the heat exchanger back");
        helper.assertFalse(controller.assembled, "breaking a shell cell must disassemble the reactor");
        helper.succeed();
    }

    public static void simulationTick(GameTestHelper helper) {
        TileEntityPWRController controller = build(helper);
        helper.assertTrue(controller.assembled, "PWR did not assemble");
        BlockPos pos = controller.getBlockPos();

        controller.inventory.setStackInSlot(0, new ItemStack(ModPwrItems.fuel(EnumPWRFuel.MEU)));
        controller.rodTarget = 50;
        controller.setupTanks();
        controller.runReactor(helper.getLevel(), pos);

        helper.assertTrue(controller.typeLoaded == EnumPWRFuel.MEU.ordinal() && controller.amountLoaded == 1,
                "first step must load the MEU rod: type=" + controller.typeLoaded + " amount=" + controller.amountLoaded);
        helper.assertTrue(controller.inventory.getStackInSlot(0).isEmpty(), "loading must consume the rod");
        helper.assertTrue(controller.rodLevel == 99, "control rods must walk one percent per tick, got " + controller.rodLevel);
        helper.assertTrue(controller.flux == 20, "one neutron source and no prior flux give 20 flux, got " + controller.flux);
        helper.assertTrue(controller.processTime == 1_000_000_000D, "MEU yield is 1e9 per rod, got " + controller.processTime);

        controller.runReactor(helper.getLevel(), pos);
        helper.assertTrue(controller.progress > 0, "the second step must react to the first step's flux");

        // Hull heat boils coolant through the channels: 16 channels over 4 effective rods is 40 %, capped by the
        // 1,000 mB in the cold tank at one heat step of 300 TU per mB.
        controller.hullHeat = 1_000_000;
        controller.coreHeat = 0;
        controller.tanks[0].setFill(1_000);
        controller.setupTanks();
        helper.assertTrue(controller.tanks[1].getTankType() == Fluids.COOLANT_HOT, "hot tank must follow the coolant's heating step");
        controller.runReactor(helper.getLevel(), pos);
        helper.assertTrue(controller.tanks[0].getFill() == 0 && controller.tanks[1].getFill() == 1_000,
                "coolant must convert 1,000 mB: cold=" + controller.tanks[0].getFill() + " hot=" + controller.tanks[1].getFill());
        helper.assertTrue(controller.hullHeat < 1_000_000 && controller.coreHeat > 0,
                "heat must move hull->coolant and core<->hull: hull=" + controller.hullHeat + " core=" + controller.coreHeat);
        helper.succeed();
    }

    // --- fixture ---

    /** The six-way flood fill reaches the core box and any cell face-adjacent to it (outside on at most one axis). */
    private static boolean reachedByScan(BlockPos rel) {
        int outside = 0;
        if (rel.getX() < 5 || rel.getX() > 7) outside++;
        if (rel.getY() < 2 || rel.getY() > 4) outside++;
        if (rel.getZ() < 7 || rel.getZ() > 9) outside++;
        return outside <= 1;
    }

    private static Block expectedPart(BlockPos rel) {
        boolean inCore = rel.getX() >= 5 && rel.getX() <= 7 && rel.getY() >= 2 && rel.getY() <= 4
                && rel.getZ() >= 7 && rel.getZ() <= 9;
        if (!inCore) return rel.equals(PORT) ? ModPwrBlocks.PWR_PORT.get() : ModPwrBlocks.PWR_CASING.get();
        if (rel.getX() == 6 && rel.getY() == 3) return ModPwrBlocks.PWR_FUEL.get();
        if (rel.getY() == 3 && rel.getZ() == 8) return ModPwrBlocks.PWR_CONTROL.get();
        if (rel.equals(new BlockPos(6, 2, 8))) return ModPwrBlocks.PWR_NEUTRON_SOURCE.get();
        if (rel.equals(new BlockPos(6, 4, 8))) return ModPwrBlocks.PWR_HEATEX.get();
        if (rel.getY() == 2 && rel.getX() != 6 && rel.getZ() != 8) return ModPwrBlocks.PWR_HEATSINK.get();
        return ModPwrBlocks.PWR_CHANNEL.get();
    }

    private static TileEntityPWRController build(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 6; z <= 10; z++) {
                    BlockPos rel = new BlockPos(x, y, z);
                    if (rel.equals(CONTROLLER)) continue;
                    helper.setBlock(rel, expectedPart(rel).defaultBlockState());
                }
            }
        }
        MachinePWRController block = ModPwrBlocks.PWR_CONTROLLER.get();
        BlockState state = block.defaultBlockState().setValue(MachinePWRController.FACING, Direction.NORTH);
        helper.setBlock(CONTROLLER, state);
        BlockPos pos = helper.absolutePos(CONTROLLER);
        TileEntityPWRController controller = (TileEntityPWRController) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(controller != null, "controller block entity missing");
        block.assemble(helper.getLevel(), pos, state, null);
        return controller;
    }
}
