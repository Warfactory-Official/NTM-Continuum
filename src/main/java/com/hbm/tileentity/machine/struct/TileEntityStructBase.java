package com.hbm.tileentity.machine.struct;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for its {@link StructPattern} to be built around it and then transmutes into the finished
 * multiblock. Port of 1.12.2's {@code TileEntityWatzStruct} / {@code TileEntityFusionTorusStruct},
 * whose hand-written {@code cbr(...)} ladders are now the pattern.
 *
 * <p>The scan runs once a second, matching upstream's {@code getTotalWorldTime() % 20}. On a match the
 * struct block becomes the multiblock's core (META 12 = facing NORTH) and the core block fills in its
 * own dummy shell — which necessarily overwrites the component blocks the player just built, exactly
 * as upstream does.
 */
public abstract class TileEntityStructBase extends TileEntityLoadedBase {

    protected TileEntityStructBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The shape that must be present, relative to this block. */
    public abstract StructPattern pattern();

    /** The multiblock this becomes once the shape is complete. */
    protected abstract BlockDummyable target();

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStructBase te) {
        if (level.getGameTime() % 20 != 0) return;
        if (!te.pattern().matches(level, pos)) return;

        BlockDummyable machine = te.target();
        BlockDummyable.safeRem = true;
        try {
            // META 12 = ForgeDirection.NORTH (2) + BlockDummyable.offset (10). Assembly is not direction
            // sensitive upstream, so a struct-built machine always ends up facing north.
            level.setBlock(pos, machine.defaultBlockState().setValue(BlockDummyable.META, 12), 3);
            machine.formStructure(level, pos, ForgeDirection.NORTH);
        } finally {
            BlockDummyable.safeRem = false;
        }
        // `te` no longer exists — setBlock above destroyed this BlockEntity. Touch nothing after here.
    }
}
