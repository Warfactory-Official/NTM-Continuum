package com.hbm.tileentity.machine.struct;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for its {@link StructPattern} to be built around it and then transmutes into the finished
 * multiblock. Port of 1.12.2's {@code TileEntityWatzStruct} / {@code TileEntityFusionTorusStruct},
 * whose hand-written {@code cbr(...)} ladders are now the pattern.
 *
 * <p>The scan runs once a second, matching upstream's {@code getTotalWorldTime() % 20}. On a match the
 * struct block becomes the multiblock's core, facing north -- assembly is not direction sensitive
 * upstream -- and the core block fills in its own cells — which necessarily overwrites the component blocks the player just built, exactly
 * as upstream does.
 */
public abstract class TileEntityStructBase extends TileEntityLoadedBase {

    protected TileEntityStructBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The shape that must be present, relative to this block. */
    public abstract StructPattern pattern();

    /** The multiblock this becomes once the shape is complete. */
    protected abstract BlockMultiblockCore target();

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStructBase te) {
        if (level.getGameTime() % 20 != 0) return;
        if (!te.pattern().matches(level, pos)) return;

        BlockMultiblockCore machine = te.target();
        BlockMultiblockCore.withoutTeardown(() ->
                MultiblockHandlerXR.placeAssembled(level, pos, machine, Direction.NORTH));
        // `te` no longer exists — setBlock above destroyed this BlockEntity. Touch nothing after here.
    }
}
