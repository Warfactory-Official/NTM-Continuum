package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.multiblock.MultiblockCore;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.client.render.flywheel.FlywheelCompat;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import dev.golem.core.render.Instancing;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The parts of the Flywheel wiring that can be checked without a client.
 *
 * <p>Drawing cannot be, so these cover the two things that would still be wrong on a server: that the
 * engine is genuinely unreachable from one, and that the crumbling hook resolves the multiblocks it
 * was installed for.
 */
public final class FlywheelGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ENGINE_PRESENT =
            TESTS.function("flywheel_engine_present", 20, () -> FlywheelGameTests::enginePresent);
    public static final GameTests.Function ENGINE_CLIENT_ONLY =
            TESTS.function("flywheel_engine_client_only", 20, () -> FlywheelGameTests::engineClientOnly);
    public static final GameTests.Function CRUMBLING_CORE_FOLDED =
            TESTS.function("flywheel_crumbling_core_folded", 60, 8, () -> FlywheelGameTests::crumblingCoreFolded);

    private FlywheelGameTests() {}

    public static void init() {}

    /**
     * The facade and the engine agree about this node. Both are constant-folded, so a mismatch means
     * the guards disagree rather than that anything failed at runtime.
     */
    public static void enginePresent(GameTestHelper helper) {
        if (Instancing.isAvailable()) {
            helper.assertTrue(FlywheelCompat.isSupportedOnThisVersion(),
                    "FlywheelCompat must report Core's available vendored engine");
        }

        // Having an engine is not the same as drawing with one. A server level is never visualized, so
        // a caller choosing between an instanced path and a vanilla renderer must be told no here.
        helper.assertTrue(!Instancing.isVisualizing(helper.getLevel()),
                "A server level must never report as visualized");
        helper.assertTrue(!FlywheelCompat.isAvailable(helper.getLevel()),
                "FlywheelCompat.isAvailable must follow it and refuse a server level");
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), helper.getLevel());
        helper.assertTrue(!FlywheelCompat.isTorexBillboardsActive(torex),
                "A dedicated server must always retain Torex fallback ownership");
        helper.assertTrue(!Instancing.isVisualizing(null), "A null level must not report as visualized");
        helper.succeed();
    }

    /**
     * The engine must be impossible to reach from a dedicated server. It holds GL state and its whole
     * draw path assumes a render thread, so CE gates it on a static initializer that throws off-client
     * — and the value of that gate is precisely that a server-side caller fails loudly here instead of
     * quietly opening a GL context on a headless box. This test is what proves the gate still bites:
     * the suite runs on a dedicated server, so the class must refuse to initialize.
     */
    public static void engineClientOnly(GameTestHelper helper) {
        if (!Instancing.isAvailable()) {
            helper.assertTrue(!Instancing.isVisualizing(helper.getLevel()),
                    "An unavailable Core backend must not visualize a server level");
            helper.succeed();
            return;
        }
        Throwable thrown = null;
        try {
            Class.forName("dev.golem.core.flywheel.Engine", true,
                    FlywheelGameTests.class.getClassLoader());
        } catch (Throwable t) {
            thrown = t;
        }
        helper.assertTrue(thrown != null,
                "Engine initialized on a dedicated server; its client-only gate is gone");
        // Once a class fails initialization the JVM marks it erroneous, so a second attempt in the same
        // session reports NoClassDefFoundError instead. Both mean the gate held.
        helper.assertTrue(thrown instanceof ExceptionInInitializerError
                        || thrown instanceof NoClassDefFoundError,
                "Vendored engine failed to load for the wrong reason: " + thrown);
        helper.succeed();
    }

    /** A folded multiblock's cells carry no block entity, so the hook has to walk them to the core. */
    public static void crumblingCoreFolded(GameTestHelper helper) {
        MultiblockCore block = ModTurretBlocks.turret_chekhov.get();
        BlockPos clicked = helper.absolutePos(new BlockPos(2, 1, 2));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        player.setPos(clicked.getX() - 6D, clicked.getY(), clicked.getZ() - 6D);
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, new ItemStack(block));
        BlockPos core = clicked.above(block.getHeightOffset()).relative(Direction.NORTH, -block.getOffset());

        BlockPos cell = core.east();
        BlockState cellState = helper.getLevel().getBlockState(cell);
        helper.assertTrue(helper.getLevel().getBlockEntity(cell) == null,
                "This test is meaningless unless the cell really has no block entity");
        helper.assertTrue(core.equals(FlywheelCompat.crumblingCore(helper.getLevel(), cell, cellState)),
                "A folded cell must resolve to the core that owns its visual");

        BlockPos stone = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(FlywheelCompat.crumblingCore(helper.getLevel(), stone,
                        helper.getLevel().getBlockState(stone)) == null,
                "A plain block must be rejected on its state alone, never by scanning for a core");
        helper.succeed();
    }

}
