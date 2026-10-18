package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server tick time over a large power grid and fluid network. Declared only when the GameTest filter names
 * {@code bench_}, so the normal suite never runs it: {@code -Pgametest.filter=hbm:bench_port_networks}. With the
 * environment variable {@code HBM_PORTBENCH_JFR} set to a file, the measured ticks are also recorded with JFR.
 */
public final class PortBenchGameTests {

    private PortBenchGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    private static final int WARMUP = 300;
    private static final int MEASURED = 6000;

    static {
        if (System.getProperty("golem.gametest.filter", "").contains("bench_")) {
            TESTS.function("bench_port_networks", WARMUP + MEASURED + 200, 80, () -> PortBenchGameTests::run);
        }
    }

    public static void init() {}

    private static final class Fixture {
        final List<TileEntityMachineElectricFurnace> furnaces = new ArrayList<>();
        final List<TileEntityMachineDiesel> diesels = new ArrayList<>();
        final List<TileEntityBarrel> sources = new ArrayList<>();
        final List<TileEntityBarrel> sinks = new ArrayList<>();
        final long[] nanos = new long[MEASURED];
        long last;
        Recording recording;
    }

    private static void run(GameTestHelper helper) {
        Fixture f = new Fixture();
        ServerLevel level = helper.getLevel();
        // 8 power strips: a cable row, furnaces and diesels on it, a diesel duct and its barrels behind them
        for (int s = 0; s < 8; s++) {
            int z = -34 + 4 * s;
            for (int x = -32; x < 32; x++) {
                helper.setBlock(new BlockPos(x, 1, z), ModMachineBlocks.red_cable.get().defaultBlockState());
                duct(helper, new BlockPos(x, 1, z + 2), Fluids.DIESEL);
                if ((x & 1) != 0) continue;
                BlockPos at = new BlockPos(x, 1, z + 1);
                if ((x & 2) == 0) {
                    helper.setBlock(at, ModMachineBlocks.machine_electric_furnace_off.get().defaultBlockState());
                    f.furnaces.add(GameTestHelpers.blockEntity(helper, at, TileEntityMachineElectricFurnace.class));
                } else {
                    helper.setBlock(at, ModMachineBlocks.machine_diesel.get().defaultBlockState());
                    f.diesels.add(GameTestHelpers.blockEntity(helper, at, TileEntityMachineDiesel.class));
                    f.sources.add(barrel(helper, new BlockPos(x, 1, z + 3), Fluids.DIESEL, 2));
                }
            }
        }
        // 4 water strips: a duct split by three disabled (passthrough) barrels, barrels on both sides
        for (int s = 0; s < 4; s++) {
            int z = 2 + 4 * s;
            for (int x = -32; x < 32; x++) {
                if (x == -16 || x == 0 || x == 16) {
                    barrel(helper, new BlockPos(x, 1, z + 1), Fluids.WATER, 3);
                    continue;
                }
                duct(helper, new BlockPos(x, 1, z + 1), Fluids.WATER);
                if ((x & 1) != 0) continue;
                int role = (x >> 1) & 3;
                for (int side : new int[] {0, 2}) {
                    TileEntityBarrel barrel = barrel(helper, new BlockPos(x, 1, z + side), Fluids.WATER, role == 0 ? 2 : role == 2 ? 1 : 0);
                    if (role == 0) f.sources.add(barrel);
                    else if (role != 2) f.sinks.add(barrel);
                }
            }
        }
        // 14 multiblocks, each ringed by cable at its base
        BlockMultiblockCore[] kinds = {ModMachineBlocks.machine_assembly_factory.get(), ModMachineBlocks.machine_chemical_plant.get(),
                ModMachineBlocks.machine_turbine_gas.get(), ModMachineBlocks.machine_chemical_factory.get()};
        int k = 0;
        for (int z : new int[] {28, 46}) {
            for (int x = -42; x <= 42; x += 14) {
                BlockPos core = new BlockPos(x, 1, z);
                FoldedTestSupport.assemble(helper, kinds[k++ % kinds.length], core);
                ring(helper, level, core);
            }
        }
        Constants.LOG.info("[portbench] built {} furnaces, {} diesels, {} source and {} sink barrels, 14 multiblocks",
                f.furnaces.size(), f.diesels.size(), f.sources.size(), f.sinks.size());
        f.last = System.nanoTime();
        tick(helper, f, 0);
    }

    private static void tick(GameTestHelper helper, Fixture f, int t) {
        helper.runAfterDelay(1, () -> {
            long now = System.nanoTime();
            if (t >= WARMUP) f.nanos[t - WARMUP] = now - f.last;
            if (t == WARMUP - 1) f.recording = record();
            if (t == WARMUP + MEASURED - 1) {
                report(f);
                if (f.recording != null) dump(f.recording);
                helper.succeed();
                return;
            }
            for (TileEntityMachineElectricFurnace furnace : f.furnaces) furnace.power = 0L;
            for (TileEntityMachineDiesel diesel : f.diesels) diesel.power = TileEntityMachineDiesel.maxPower;
            for (TileEntityBarrel barrel : f.sources) barrel.tank.setFill(barrel.tank.getMaxFill());
            for (TileEntityBarrel barrel : f.sinks) barrel.tank.setFill(0);
            f.last = System.nanoTime();
            tick(helper, f, t + 1);
        });
    }

    /** Tick times exclude the fixture's own refill; the fed counts say the nets moved something in the last round. */
    private static void report(Fixture f) {
        long[] sorted = f.nanos.clone();
        Arrays.sort(sorted);
        double mean = Arrays.stream(sorted).average().orElse(0) / 1e6;
        int fedFurnaces = 0;
        for (TileEntityMachineElectricFurnace furnace : f.furnaces) if (furnace.power > 0L) fedFurnaces++;
        int fedSinks = 0;
        for (TileEntityBarrel barrel : f.sinks) if (barrel.tank.getFill() > 0) fedSinks++;
        Constants.LOG.info(String.format("[portbench] %d ticks: mean %.3f ms, p50 %.3f, p95 %.3f, p99 %.3f, max %.3f; fed %d/%d furnaces, %d/%d sinks",
                MEASURED, mean, at(sorted, 0.50), at(sorted, 0.95), at(sorted, 0.99), sorted[sorted.length - 1] / 1e6,
                fedFurnaces, f.furnaces.size(), fedSinks, f.sinks.size()));
    }

    private static Recording record() {
        String file = System.getenv("HBM_PORTBENCH_JFR");
        if (file == null || file.isEmpty()) return null;
        try {
            Recording recording = new Recording(Configuration.getConfiguration("profile"));
            recording.setDestination(Path.of(file));
            recording.start();
            return recording;
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("JFR profile settings unreadable", e);
        }
    }

    private static void dump(Recording recording) {
        // stop() writes the destination file
        recording.stop();
        recording.close();
        Constants.LOG.info("[portbench] JFR written to {}", System.getenv("HBM_PORTBENCH_JFR"));
    }

    private static double at(long[] sorted, double q) {
        return sorted[Math.min(sorted.length - 1, (int) (q * sorted.length))] / 1e6;
    }

    private static TileEntityBarrel barrel(GameTestHelper helper, BlockPos rel, FluidType type, int mode) {
        helper.setBlock(rel, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        TileEntityBarrel barrel = GameTestHelpers.blockEntity(helper, rel, TileEntityBarrel.class);
        barrel.tank.setTankType(type);
        barrel.mode = (short) mode;
        return barrel;
    }

    private static void duct(GameTestHelper helper, BlockPos rel, FluidType type) {
        helper.setBlock(rel, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState());
        ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(helper.absolutePos(rel))).setFluidType(type);
    }

    /** Cable on every free cell beside the multiblock's bottom layer. */
    private static void ring(GameTestHelper helper, ServerLevel level, BlockPos core) {
        List<BlockPos> ring = new ArrayList<>();
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                BlockPos rel = core.offset(dx, 0, dz);
                if (!level.getBlockState(helper.absolutePos(rel)).isAir()) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (!level.getBlockState(helper.absolutePos(rel.relative(dir))).isAir()) {
                        ring.add(rel);
                        break;
                    }
                }
            }
        }
        for (BlockPos rel : ring) helper.setBlock(rel, ModMachineBlocks.red_cable.get().defaultBlockState());
    }
}
