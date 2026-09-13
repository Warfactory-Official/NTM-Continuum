package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A migrated machine's baked face masks must answer exactly what its own {@code getConPos} answers.
 *
 * <p>Before folding, both {@code canConnect} overloads on a {@code BlockDummyable} ran
 * {@link IEnergyConnectorBlock#isPort} over the core tile's {@code DirPos[]}; after folding the answer
 * comes from the table instead. Those two must agree cell for cell and face for face, or a migration has
 * quietly moved a port — which nothing else would catch, because a machine with a port on the wrong face
 * still runs.
 *
 * <p>No machine is placed. Not one {@code getConPos} implementation in the tree reads the level, so the
 * block entity is built off-world at a fixed position and asked directly; that is what lets this cover
 * every registered machine at every facing rather than the handful a plot has room for.
 */
public final class FoldedPortParityGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function PORTS_MATCH =
            TESTS.function("folded_ports_match_conpos", 20, () -> FoldedPortParityGameTests::portsMatchConPos);

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    private FoldedPortParityGameTests() {}

    public static void init() {}

    public static void portsMatchConPos(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        int covered = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockMultiblockMachine machine)) continue;
            boolean any = false;
            for (Direction facing : Direction.values()) {
                if (facing.getAxis().isVertical()) continue;
                DirPos[] declared = conPos(machine, facing);
                if (declared == null) continue;
                any = true;
                compare(machine, facing, declared, failures);
            }
            if (any) covered++;
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures.subList(0, Math.min(8, failures.size())))
                + (failures.size() > 8 ? " (and " + (failures.size() - 8) + " more)" : ""));
        helper.assertTrue(covered > 0, "No folded machine exposed a getConPos to compare against");
        helper.succeed();
    }

    /** The core tile's declared connections, or null where this machine states none to compare against. */
    private static DirPos @org.jspecify.annotations.Nullable [] conPos(BlockMultiblockMachine machine,
                                                                      Direction facing) {
        BlockState state = machine.defaultBlockState().setValue(BlockMultiblockMachine.FACING, facing);
        BlockEntity core;
        try {
            core = machine.newBlockEntity(ORIGIN, state);
        } catch (RuntimeException e) {
            return null;
        }
        if (core == null) return null;
        try {
            Method method = core.getClass().getMethod("getConPos");
            return (DirPos[]) method.invoke(core);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void compare(BlockMultiblockMachine machine, Direction facing, DirPos[] declared,
                                List<String> failures) {
        String id = BuiltInRegistries.BLOCK.getKey(machine) + "/" + facing.getName();
        MultiblockMaskTable table = machine.maskTable();
        table.forEachLocalCell((lx, ly, lz) -> {
            int dx = MultiblockMaskTable.worldX(lx, ly, lz, facing);
            int dz = MultiblockMaskTable.worldZ(lx, ly, lz, facing);
            BlockPos cell = ORIGIN.offset(dx, ly, dz);
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                Direction side = dir.toDirection();
                if (buried(table, facing, dx + side.getStepX(), ly + side.getStepY(), dz + side.getStepZ())) continue;
                boolean was = IEnergyConnectorBlock.isPort(declared, cell, dir);
                boolean now = machine.isOpenAt(facing, dx, ly, dz, side);
                if (was == now) continue;
                failures.add(id + " at " + dx + "," + ly + "," + dz + " face " + dir
                        + (was ? " was a port and the table closes it" : " is opened by the table and was not a port"));
            }
        });
    }

    /**
     * Is the position on the far side of this face another cell of the same machine? Nothing can stand
     * there to ask, so 1.7 declaring a connection into its own body and the table closing that face are the
     * same machine. Stating it once here is what lets a footprint be transcribed as the tighter of the two.
     */
    private static boolean buried(MultiblockMaskTable table, Direction facing, int dx, int dy, int dz) {
        return table.maskAtLocal(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing)) != MultiblockMaskTable.NOT_A_CELL;
    }
}
