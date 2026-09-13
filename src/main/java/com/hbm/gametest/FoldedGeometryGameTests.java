package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.CapRole;
import com.hbm.capability.MachineCaps;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * A folded machine states its connection geometry once, per cell and per role, and the statement is baked
 * into a flat table. These are the assertions that the baking keeps the roles apart.
 *
 * <p>1.7 never had a role vocabulary: it stated geometry per call site, so the same cell could be a power
 * port in {@code trySubscribe} and no fluid port at all in {@code sendFluid}. Folding both into one face
 * mask would union them and open each on the other's cells, which is silent — the machine works, it just
 * accepts a commodity at a face the original refused. The role planes are what stop that, so their
 * separation is worth asserting directly rather than through a machine that happens to use them.
 */
public final class FoldedGeometryGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function ROLES_SEPARATE =
            TESTS.function("folded_role_planes_separate", 1, () -> FoldedGeometryGameTests::rolesSeparate);
    public static final GameTests.Function ROLES_UNION =
            TESTS.function("folded_roles_default_to_union", 1, () -> FoldedGeometryGameTests::rolesUnion);
    public static final GameTests.Function PASSIVE_PLANE =
            TESTS.function("folded_passive_plane_is_its_own", 1, () -> FoldedGeometryGameTests::passivePlane);

    private FoldedGeometryGameTests() {}

    public static void init() {}

    private static void eq(GameTestHelper helper, int actual, int expected, String what) {
        helper.assertTrue(actual == expected, "Expected " + expected + " but got " + actual + " for " + what);
    }

    private static MultiblockMaskTable bake(MultiblockMaskTable.CellSource cells,
                                            MultiblockMaskTable.PassiveCellSource passive, int declaredCaps) {
        return MultiblockMaskTable.bake(new MultiblockMaskTable.Geometry(BlockMultiblockCore.class, cells, passive,
                BlockMultiblockCore.MASK_NONE, BlockMultiblockCore.MASK_ALL, 0), declaredCaps);
    }

    /** A west port that takes power and an east port that vends fluid must not answer for each other. */
    public static void rolesSeparate(GameTestHelper helper) {
        MultiblockMaskTable table = bake((core, facing, visitor) -> {
            visitor.cell(core.offset(-1, 0, 0), BlockMultiblockCore.MASK_WEST, BlockMultiblockCore.ROLE_POWER_IN);
            visitor.cell(core.offset(1, 0, 0), BlockMultiblockCore.MASK_EAST, BlockMultiblockCore.ROLE_FLUID_OUT);
        }, (core, facing, visitor) -> {}, MachineCaps.POWER_IN | MachineCaps.FLUID_OUT);

        helper.assertTrue(table.hasRolePlanes(), "A geometry that qualified two cells by role baked no role planes");
        eq(helper, table.maskAtLocal(-1, 0, 0, CapRole.POWER_IN), BlockMultiblockCore.MASK_WEST,
                "the power-in face of the west port");
        eq(helper, table.maskAtLocal(-1, 0, 0, CapRole.FLUID_OUT), 0,
                "the fluid-out face of the west port, which takes power only");
        eq(helper, table.maskAtLocal(1, 0, 0, CapRole.FLUID_OUT), BlockMultiblockCore.MASK_EAST,
                "the fluid-out face of the east port");
        eq(helper, table.maskAtLocal(1, 0, 0, CapRole.POWER_IN), 0,
                "the power-in face of the east port, which vends fluid only");
        eq(helper, table.maskAtLocal(-1, 0, 0), BlockMultiblockCore.MASK_WEST,
                "the role-blind union at the west port");
        eq(helper, table.maskAtLocal(4, 0, 0), MultiblockMaskTable.NOT_A_CELL,
                "an offset outside the footprint");
        helper.succeed();
    }

    /** A machine that never names a role pays for no planes, and every role reads the one mask. */
    public static void rolesUnion(GameTestHelper helper) {
        MultiblockMaskTable table = bake((core, facing, visitor) ->
                visitor.cell(core.offset(0, 1, 0), BlockMultiblockCore.MASK_UP),
                (core, facing, visitor) -> {}, MachineCaps.POWER_IN);

        helper.assertFalse(table.hasRolePlanes(), "A role-blind geometry allocated role planes it cannot use");
        for (CapRole role : CapRole.VALUES) {
            eq(helper, table.maskAtLocal(0, 1, 0, role), BlockMultiblockCore.MASK_UP,
                    "the " + role + " face of a cell stated without a role");
        }
        helper.succeed();
    }

    /**
     * The passive plane is the surface a hopper or a pipe finds by asking the block directly; it is not the
     * active plane and the item route must not read their union.
     */
    public static void passivePlane(GameTestHelper helper) {
        MultiblockMaskTable table = bake((core, facing, visitor) ->
                visitor.cell(core.offset(0, 1, 0), BlockMultiblockCore.MASK_NONE),
                (core, facing, visitor) -> visitor.passiveCell(core.offset(0, 1, 0), BlockMultiblockCore.MASK_UP,
                        BlockMultiblockCore.PASSIVE_ITEMS), MachineCaps.ITEMS);

        eq(helper, table.maskAtLocal(0, 1, 0), BlockMultiblockCore.MASK_NONE,
                "the active mask of a cell that is only a proxy");
        eq(helper, table.passiveMaskAtLocal(0, 1, 0, BlockMultiblockCore.PASSIVE_ITEMS),
                BlockMultiblockCore.MASK_UP, "the item proxy's accept face");
        eq(helper, table.passiveMaskAtLocal(0, 1, 0, BlockMultiblockCore.PASSIVE_POWER_IN), 0,
                "the power-in proxy face of a cell that proxies items only");
        helper.succeed();
    }
}
