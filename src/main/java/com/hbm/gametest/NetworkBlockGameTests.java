package com.hbm.gametest;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.BasePipeAnchor;
import com.hbm.blocks.network.CableDiode;
import com.hbm.blocks.network.FluidPump;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityFluidPipeAnchor;
import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import com.hbm.tileentity.network.TileEntityPipeExhaustAnchor;
import com.hbm.tileentity.network.TileEntityPipelineBase;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumaticPipeAnchor;
import com.hbm.uninos.PneumaticGraph;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 X5846's network blocks: the flow control pump's per-tick limit, the network separator's limit and log, the
 * deprecated pump and diode as plain conductors, the anchors of all three networks and the counter valve's count.
 */
public final class NetworkBlockGameTests {

    private NetworkBlockGameTests() {}

    /** Ports are built on the first ticks and a round needs a tick more; a window then runs at full rate. */
    private static final int WARM = 20;
    private static final int WINDOW = 20;

    /**
     * Water from a sending barrel to a receiving one through the pump: 100 mB per round at the default buffer, 250 mB
     * per round once set to 250, nothing while redstone reaches any of its three blocks.
     */
    public static void pumpLimit(GameTestHelper helper) {
        BlockPos core = new BlockPos(6, 1, 6);
        BlockPos abs = FoldedTestSupport.assemble(helper, ModFluidNetBlocks.flow_control_pump.get(), core);
        TileEntityFlowControlPump pump = GameTestHelpers.blockEntity(helper, core, TileEntityFlowControlPump.class);
        pump.tank[0].setTankType(Fluids.WATER);
        ForgeDirection out = pump.outSide();
        helper.assertTrue(out == ForgeDirection.WEST, "a north-facing pump sends west, got " + out);

        TileEntityBarrel source = barrel(helper, core.east(2), 2, 16_000);
        TileEntityBarrel sink = barrel(helper, core.west(2), 0, 0);

        helper.runAfterDelay(WARM, () -> {
            int s0 = sink.tank.getFill();
            helper.runAfterDelay(WINDOW, () -> {
                int moved = sink.tank.getFill() - s0;
                // other tests may run extra rounds in the same tick; each round still moves exactly one buffer
                helper.assertTrue(moved >= 100 * WINDOW && moved % 100 == 0, "100 mB per round for " + WINDOW + " ticks, moved " + moved);
                CompoundTag control = new CompoundTag();
                control.putInt("capacity", 250);
                pump.receiveControl(control);
                helper.runAfterDelay(10, () -> {
                    int s2 = sink.tank.getFill();
                    helper.runAfterDelay(WINDOW, () -> {
                        int fast = sink.tank.getFill() - s2;
                        helper.assertTrue(fast >= 250 * WINDOW && fast % 250 == 0, "250 mB per round for " + WINDOW + " ticks, moved " + fast);
                        helper.setBlock(core.north(), Blocks.REDSTONE_BLOCK);
                        helper.runAfterDelay(5, () -> {
                            int s4 = sink.tank.getFill();
                            int left = source.tank.getFill();
                            helper.runAfterDelay(WINDOW, () -> {
                                helper.assertTrue(pump.redstone, "a redstone block beside the pump disables it");
                                helper.assertTrue(sink.tank.getFill() == s4 && source.tank.getFill() == left,
                                        "a disabled pump moves nothing: sink " + s4 + " -> " + sink.tank.getFill());
                                helper.assertTrue(abs.equals(pump.getBlockPos()), "the core holds the pump");
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    /**
     * 1,000 HE/t from a battery through the separator into another: it logs 1,000 HE/t and 20,000 HE/s, the sink gains
     * 20,000 HE in twenty ticks; set to 250 over RoR it logs 250 and 5,000; redstone on the base cuts it off.
     */
    public static void separatorLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = new BlockPos(6, 1, 6);
        FoldedTestSupport.assemble(helper, ModEnergyNetBlocks.network_separator.get(), core);
        TileEntityNetworkSeparator sep = GameTestHelpers.blockEntity(helper, core, TileEntityNetworkSeparator.class);
        TileEntityMachineBattery source = battery(level, helper.absolutePos(core.east(2)), TileEntityMachineBattery.MODE_OUTPUT,
                TileEntityMachineBattery.MAX_POWER);
        TileEntityMachineBattery sink = battery(level, helper.absolutePos(core.west(2)), TileEntityMachineBattery.MODE_INPUT, 0L);

        helper.runAfterDelay(WARM + WINDOW, () -> {
            helper.assertTrue(sep.deltaTick[19] == 1_000 && sep.deltaSecond == 20_000,
                    "1,000 HE/t: logged " + sep.deltaTick[19] + " HE/t and " + sep.deltaSecond + " HE/s");
            long s0 = sink.power;
            helper.runAfterDelay(WINDOW, () -> {
                helper.assertTrue(sink.power - s0 == 20_000, "the sink gained " + (sink.power - s0) + " HE in 20 ticks");
                helper.assertTrue(source.power < TileEntityMachineBattery.MAX_POWER, "the source paid for it");
                sep.runRORFunction("FUN:setlimit", new String[]{"250"});
                sep.runRORFunction("FUN:setpriority", new String[]{"2"});
                helper.assertTrue(sep.configuredLimit == 250 && sep.priority == ConnectionPriority.HIGH,
                        "RoR set the limit to " + sep.configuredLimit + " and the priority to " + sep.priority);
                helper.runAfterDelay(WARM + WINDOW, () -> {
                    helper.assertTrue(sep.deltaTick[19] == 250 && sep.deltaSecond == 5_000,
                            "250 HE/t: logged " + sep.deltaTick[19] + " HE/t and " + sep.deltaSecond + " HE/s");
                    helper.assertTrue("250".equals(sep.provideRORValue("VAL:deltatick"))
                            && "5000".equals(sep.provideRORValue("VAL:deltasecond")), "RoR reads the log");
                    helper.setBlock(core.east().north(), Blocks.REDSTONE_BLOCK);
                    helper.runAfterDelay(WARM + WINDOW, () -> {
                        helper.assertTrue(sep.redstone && sep.deltaTick[19] == 0 && sep.deltaSecond == 0,
                                "redstone on an end block cuts it off: " + sep.deltaTick[19] + " HE/t, " + sep.deltaSecond + " HE/s");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The deprecated pump is a passthrough: both barrels share one net, so far more than 100 mB/t passes. */
    public static void deprecatedPump(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModFluidNetBlocks.fluid_pump.get().defaultBlockState().setValue(FluidPump.FACING, Direction.NORTH));
        TileEntityFluidPump pump = GameTestHelpers.blockEntity(helper, pos, TileEntityFluidPump.class);
        pump.tank[0].setTankType(Fluids.WATER);
        TileEntityBarrel sink = barrel(helper, pos.west(), 0, 0);
        barrel(helper, pos.east(), 2, 16_000);
        helper.runAfterDelay(WARM, () -> {
            helper.assertTrue(sink.tank.getFill() > 100 * WARM, "a limiter would allow 2,000 mB, the pipe moved " + sink.tank.getFill());
            helper.succeed();
        });
    }

    /** The deprecated diode is a plain cable: power crosses it either way round. */
    public static void deprecatedDiode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TileEntityMachineBattery[] sinks = new TileEntityMachineBattery[2];
        Direction[] facings = {Direction.EAST, Direction.WEST};
        for (int i = 0; i < 2; i++) {
            BlockPos diode = new BlockPos(4, 1, 2 + 3 * i);
            helper.setBlock(diode, ModEnergyNetBlocks.cable_diode.get().defaultBlockState().setValue(CableDiode.FACING, facings[i]));
            battery(level, helper.absolutePos(diode.east()), TileEntityMachineBattery.MODE_OUTPUT, TileEntityMachineBattery.MAX_POWER);
            sinks[i] = battery(level, helper.absolutePos(diode.west()), TileEntityMachineBattery.MODE_INPUT, 0L);
        }
        helper.runAfterDelay(WARM, () -> {
            helper.assertTrue(sinks[0].power > 0 && sinks[1].power > 0,
                    "power crosses a diode facing either way: " + sinks[0].power + ", " + sinks[1].power);
            helper.succeed();
        });
    }

    /**
     * Anchors of each network link only to their own kind, within ten metres, and a linked pair is one net. An exhaust
     * anchor carries the smokes through its mounted face only.
     */
    public static void anchors(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fluidPos = new BlockPos(1, 2, 1);
        BlockPos exA = new BlockPos(1, 2, 4), exB = new BlockPos(7, 2, 4);
        BlockPos pnA = new BlockPos(1, 2, 7), pnB = new BlockPos(7, 2, 7);
        helper.setBlock(fluidPos, ModFluidNetBlocks.pipe_anchor.get().defaultBlockState());
        for (BlockPos p : new BlockPos[]{exA, exB}) {
            helper.setBlock(p, ModFluidNetBlocks.pipe_anchor_exhaust.get().defaultBlockState().setValue(BasePipeAnchor.FACING, Direction.UP));
        }
        for (BlockPos p : new BlockPos[]{pnA, pnB}) {
            helper.setBlock(p, ModPneumaticBlocks.PIPE_ANCHOR_PNEUMATIC.get().defaultBlockState().setValue(BasePipeAnchor.FACING, Direction.UP));
        }
        TileEntityFluidPipeAnchor fluid = GameTestHelpers.blockEntity(helper, fluidPos, TileEntityFluidPipeAnchor.class);
        TileEntityPipeExhaustAnchor exhaustA = GameTestHelpers.blockEntity(helper, exA, TileEntityPipeExhaustAnchor.class);
        TileEntityPipeExhaustAnchor exhaustB = GameTestHelpers.blockEntity(helper, exB, TileEntityPipeExhaustAnchor.class);
        TileEntityPneumaticPipeAnchor pneuA = GameTestHelpers.blockEntity(helper, pnA, TileEntityPneumaticPipeAnchor.class);
        TileEntityPneumaticPipeAnchor pneuB = GameTestHelpers.blockEntity(helper, pnB, TileEntityPneumaticPipeAnchor.class);

        helper.assertTrue(TileEntityPipelineBase.canConnect(fluid, exhaustA) == 1, "a fluid anchor does not link to an exhaust anchor");
        helper.assertTrue(TileEntityPipelineBase.canConnect(exhaustA, pneuA) == 1, "an exhaust anchor does not link to a pneumatic one");
        helper.assertTrue(TileEntityPipelineBase.canConnect(exhaustA, exhaustB) == 0, "two exhaust anchors 6 m apart link");
        helper.assertTrue(TileEntityPipelineBase.canConnect(pneuA, pneuB) == 0, "two pneumatic anchors 6 m apart link");

        ForgeDirection down = ForgeDirection.DOWN;
        helper.assertTrue(exhaustA.canConnect(Fluids.SMOKE, down) && exhaustA.canConnect(Fluids.SMOKE_POISON, down)
                        && !exhaustA.canConnect(Fluids.WATER, down) && !exhaustA.canConnect(Fluids.SMOKE, ForgeDirection.UP),
                "an exhaust anchor on a floor takes the three smokes from below only");

        exhaustA.addConnection(exhaustB.getBlockPos());
        exhaustB.addConnection(exhaustA.getBlockPos());
        pneuA.addConnection(pneuB.getBlockPos());
        pneuB.addConnection(pneuA.getBlockPos());
        var fluidGraph = FluidGraph.get(level);
        var exNet = fluidGraph.networkAt(exhaustA.getBlockPos().asLong());
        helper.assertTrue(exNet != null && exNet == fluidGraph.networkAt(exhaustB.getBlockPos().asLong()),
                "the exhaust pipeline joins the two anchors' nets");
        var pnNet = PneumaticGraph.netAt(level, pneuA.getBlockPos());
        helper.assertTrue(pnNet != null && pnNet == PneumaticGraph.netAt(level, pneuB.getBlockPos()),
                "the pneumatic pipeline joins the two anchors' nets");

        level.removeBlock(pneuB.getBlockPos(), false);
        helper.assertTrue(pneuA.getConnected().isEmpty(), "breaking a pneumatic anchor tears it out of its peer's list");
        helper.succeed();
    }

    /** Every mB that crossed the open valve is on its counter, the closing tick too. */
    public static void counterValve(GameTestHelper helper) {
        BlockPos valvePos = new BlockPos(4, 1, 4);
        helper.setBlock(valvePos, ModFluidNetBlocks.fluid_counter_valve.get().defaultBlockState());
        TileEntityFluidCounterValve valve = GameTestHelpers.blockEntity(helper, valvePos, TileEntityFluidCounterValve.class);
        valve.setFluidType(Fluids.WATER);
        valve.setState(1);
        barrel(helper, valvePos.east(), 2, 16_000);
        TileEntityBarrel sink = barrel(helper, valvePos.west(), 0, 0);

        helper.runAfterDelay(WARM, () -> {
            valve.setState(0);
            long counted = valve.getCounter();
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(sink.tank.getFill() > 0, "nothing crossed the valve");
                helper.assertTrue(counted == sink.tank.getFill() && valve.getCounter() == counted,
                        "the counter reads " + counted + " for " + sink.tank.getFill() + " mB moved");
                helper.succeed();
            });
        });
    }

    /** A steel barrel given a highly corrosive fluid becomes a corroded barrel holding the same fluid. */
    public static void steelBarrelCorrodes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        TileEntityBarrel barrel = barrel(helper, pos, 0, 0);
        barrel.tank.setTankType(Fluids.SCHRABIDIC);
        barrel.tank.setFill(4_000);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(pos).is(ModFluidNetBlocks.barrel_corroded.get()), "the steel barrel corroded");
            TileEntityBarrel corroded = GameTestHelpers.blockEntity(helper, pos, TileEntityBarrel.class);
            helper.assertTrue(corroded.tank.getTankType() == Fluids.SCHRABIDIC && corroded.tank.getMaxFill() == 16_000
                            && corroded.tank.getFill() <= 4_000 && corroded.tank.getFill() >= 3_990,
                    "the corroded barrel keeps the fluid in 16,000 mB: " + corroded.tank.getFill());
            helper.succeed();
        });
    }

    /** A battery at one of the catalytic reformer's six ports charges it. */
    public static void reformerPower(GameTestHelper helper) {
        BlockPos core = new BlockPos(5, 1, 5);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_catalytic_reformer.get(), core);
        TileEntityMachineCatalyticReformer reformer = GameTestHelpers.blockEntity(helper, core, TileEntityMachineCatalyticReformer.class);
        // facing north, the first port sits at core + north + east and connects north
        battery(helper.getLevel(), helper.absolutePos(core.north(2).east()), TileEntityMachineBattery.MODE_OUTPUT,
                TileEntityMachineBattery.MAX_POWER);
        helper.runAfterDelay(WARM, () -> {
            helper.assertTrue(reformer.power > 0, "the reformer took no power at its port");
            helper.succeed();
        });
    }

    private static TileEntityBarrel barrel(GameTestHelper helper, BlockPos rel, int mode, int fill) {
        helper.setBlock(rel, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        TileEntityBarrel barrel = GameTestHelpers.blockEntity(helper, rel, TileEntityBarrel.class);
        barrel.tank.setTankType(Fluids.WATER);
        barrel.tank.setFill(fill);
        barrel.mode = (short) mode;
        return barrel;
    }

    private static TileEntityMachineBattery battery(ServerLevel level, BlockPos abs, int mode, long power) {
        TileEntityMachineBattery battery = PortedBattery.place(level, abs);
        battery.redLow = (short) mode;
        battery.redHigh = (short) mode;
        battery.power = power;
        return battery;
    }
}
