package fissionworks;

import fissionworks.sim.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hall: a 7 x 7 slab, one block tall. The outer ring is casing with the panel at the middle of the
 * front side. The 5 x 5 inside is reactor columns. Cell (i, j): i runs to the player's right, j runs away
 * from the panel, so (3, 0) is the panel and inner column (x, z) is cell (x + 1, z + 1).
 */
public final class Hall {
    public static final int SIZE = 7;
    public static final int INNER = SIZE - 2;

    private Hall() {}

    public static BlockPos at(BlockPos panel, Direction facing, int i, int j) {
        Direction back = facing.getOpposite();
        return panel.relative(back, j).relative(back.getClockWise(), i - 3);
    }

    /** Cell (i, j) of pos inside this hall, or null. */
    public static int[] locate(BlockPos panel, Direction facing, BlockPos pos) {
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                if (at(panel, facing, i, j).equals(pos)) {
                    return new int[] {i, j};
                }
            }
        }
        return null;
    }

    /** Forms the hall around the panel. Returns null on success, else a message naming the problem. */
    public static Component tryForm(Level level, BlockPos panel, Direction facing) {
        Reactor reactor = new Reactor(INNER, INNER);
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                if (i == 3 && j == 0) {
                    continue;
                }
                BlockPos p = at(panel, facing, i, j);
                BlockState state = level.getBlockState(p);
                boolean border = i == 0 || j == 0 || i == SIZE - 1 || j == SIZE - 1;
                Reactor.Kind kind = border ? Reactor.Kind.EMPTY : FissionWorks.kindOf(state.getBlock());
                boolean ok = border ? state.is(FissionWorks.CASING.get()) : kind != null;
                if (!ok || (state.getBlock() instanceof PileBlock && state.getValue(PileBlock.FORMED))) {
                    return Component.translatable("message.fissionworks.bad_block", p.getX(), p.getY(), p.getZ());
                }
                if (!border) {
                    reactor.set(i - 1, j - 1, kind);
                }
            }
        }
        if (reactor.count(Reactor.Kind.FUEL) == 0 || !(level.getBlockEntity(panel) instanceof ReactorBE be)) {
            return Component.translatable("message.fissionworks.need_fuel");
        }
        be.start(reactor, facing);
        setFormed(level, panel, facing, true);
        return null;
    }

    /** Called when a formed block is removed: unforms the hall it belonged to. */
    public static void unform(Level level, BlockPos removedPos, BlockState removed) {
        BlockPos panel = panelFor(level, removedPos, removed);
        if (panel == null) {
            return;
        }
        BlockState state = panel.equals(removedPos) ? removed : level.getBlockState(panel);
        setFormed(level, panel, state.getValue(PanelBlock.FACING), false);
        if (level.getBlockEntity(panel) instanceof ReactorBE be) {
            be.stop();
        }
    }

    /** The formed panel whose hall contains near. removed stands in for near's old state. */
    public static BlockPos panelFor(Level level, BlockPos near, BlockState removed) {
        for (BlockPos c : BlockPos.betweenClosed(near.offset(-6, 0, -6), near.offset(6, 0, 6))) {
            BlockState state = removed != null && c.equals(near) ? removed : level.getBlockState(c);
            if (state.is(FissionWorks.PANEL.get()) && state.getValue(PileBlock.FORMED)
                    && locate(c, state.getValue(PanelBlock.FACING), near) != null) {
                return c.immutable();
            }
        }
        return null;
    }

    private static void setFormed(Level level, BlockPos panel, Direction facing, boolean formed) {
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                BlockPos p = at(panel, facing, i, j);
                BlockState state = level.getBlockState(p);
                if (state.getBlock() instanceof PileBlock && state.getValue(PileBlock.FORMED) != formed) {
                    BlockState next = state.setValue(PileBlock.FORMED, formed);
                    level.setBlock(p, formed ? next : resetLook(next), 3);
                }
            }
        }
    }

    private static BlockState resetLook(BlockState state) {
        if (state.getBlock() instanceof RodBlock) {
            return state.setValue(RodBlock.LEVEL, 8);
        }
        return state.getBlock() instanceof FuelBlock ? state.setValue(FuelBlock.GLOW, 0) : state;
    }
}
