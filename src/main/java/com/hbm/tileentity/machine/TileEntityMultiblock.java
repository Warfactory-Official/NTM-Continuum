package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.machine.struct.TileEntityStructBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMultiblock}, the tile of both launcher core components. Every tick it checks for its
 * shape, and on a match turns into the launcher:
 *
 * <ul>
 *   <li>{@code struct_launcher_core}: the eight {@code struct_launcher} around it become a compact launcher;</li>
 *   <li>{@code struct_launcher_core_large}: all 80 {@code struct_launcher} of the 9x9 ring plus an eleven-high
 *       {@code struct_scaffold} column three blocks out on one side become a launch table facing that side, and
 *       the scaffold is taken down. The four sides are tried in upstream's order (+X, -X, +Z, -Z).</li>
 * </ul>
 *
 * <p>The preview is 1.7 {@code RenderMultiblock}: small translucent cubes for the ring and, for the table, the
 * scaffold column shown on each side in turn, one second each. The port's preview skips cells already built.
 */
public class TileEntityMultiblock extends TileEntityStructBase {

    private static final StructPattern COMPACT = compactPattern();
    /** The ring plus the scaffold column on side 0 (+X), 1 (+Z), 2 (-X), 3 (-Z), matching RenderMultiblock's cycle order. */
    private static final StructPattern[] TABLE = {
            tablePattern(3, 0), tablePattern(0, 3), tablePattern(-3, 0), tablePattern(0, -3)};

    private final boolean large;

    public TileEntityMultiblock(BlockPos pos, BlockState state) {
        super(state.is(ModCustomMissileBlocks.struct_launcher_core_large.get())
                ? ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE_LARGE.get()
                : ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE.get(), pos, state);
        this.large = state.is(ModCustomMissileBlocks.struct_launcher_core_large.get());
    }

    @Override
    public StructPattern pattern() {
        if (!large) return COMPACT;
        return TABLE[(int) (System.currentTimeMillis() % 4000 / 1000)];
    }

    @Override
    protected BlockMultiblockCore target() {
        return large ? ModCustomMissileBlocks.launch_table.get() : ModCustomMissileBlocks.compact_launcher.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMultiblock te) {
        if (!te.large) {
            if (te.isCompact(level, pos)) {
                BlockMultiblockCore.withoutTeardown(() ->
                        MultiblockHandlerXR.placeAssembled(level, pos, ModCustomMissileBlocks.compact_launcher.get(),
                                Direction.NORTH));
            }
            return;
        }

        Direction facing = te.isTable(level, pos);
        if (facing == null) return;

        // 1.7 buildTable: the scaffold column comes down before the table goes in
        for (int i = 1; i < 12; i++) {
            level.setBlockAndUpdate(pos.relative(facing, 3).above(i), Blocks.AIR.defaultBlockState());
        }
        BlockMultiblockCore.withoutTeardown(() ->
                MultiblockHandlerXR.placeAssembled(level, pos, ModCustomMissileBlocks.launch_table.get(), facing));
        // `te` is not valid after this point.
    }

    private boolean isCompact(BlockGetter level, BlockPos pos) {
        Block plate = ModBlocks.struct_launcher.get();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                if (!(i == 0 && j == 0) && !level.getBlockState(pos.offset(i, 0, j)).is(plate)) return false;
        return true;
    }

    /** 1.7 {@code isTable}: the side the scaffold stands on, or null. Meta 0/1/2/3 were +X/-X/+Z/-Z. */
    private Direction isTable(BlockGetter level, BlockPos pos) {
        Block plate = ModBlocks.struct_launcher.get();
        for (int i = -4; i <= 4; i++)
            for (int j = -4; j <= 4; j++)
                if (!(i == 0 && j == 0) && !level.getBlockState(pos.offset(i, 0, j)).is(plate)) return null;

        for (Direction side : new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            boolean flag = true;
            for (int k = 1; k < 12; k++) {
                if (!level.getBlockState(pos.relative(side, 3).above(k)).is(ModBlocks.struct_scaffold.get())) flag = false;
            }
            if (flag) return side;
        }
        return null;
    }

    private static StructPattern compactPattern() {
        StructPattern.Builder b = StructPattern.builder();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                if (i != 0 || j != 0) b.add(i, 0, j, ModBlocks.struct_launcher, "struct_launcher");
        return b.build();
    }

    private static StructPattern tablePattern(int sx, int sz) {
        StructPattern.Builder b = StructPattern.builder();
        for (int i = -4; i <= 4; i++)
            for (int j = -4; j <= 4; j++)
                if (i != 0 || j != 0) b.add(i, 0, j, ModBlocks.struct_launcher, "struct_launcher");
        for (int k = 1; k < 12; k++) b.add(sx, k, sz, ModBlocks.struct_scaffold, "struct_scaffold");
        return b.build();
    }
}
