package com.hbm.tileentity.machine.struct;

import dev.golem.core.multiblock.MultiblockCore;
import dev.golem.core.multiblock.MultiblockFootprint;
import com.hbm.blocks.machine.struct.StructPattern;
import dev.golem.core.machine.SyncedBlockEntity;
import com.hbm.util.TickPhase;
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
public abstract class TileEntityStructBase extends SyncedBlockEntity {

    protected TileEntityStructBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The shape that must be present, relative to this block. */
    public abstract StructPattern pattern();

    /** The multiblock this becomes once the shape is complete. */
    protected abstract MultiblockCore target();

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStructBase te) {
        if (!TickPhase.every(level, pos, 20)) return;
        if (!te.pattern().matches(level, pos)) return;

        MultiblockCore machine = te.target();
        MultiblockCore.withoutTeardown(() ->
                MultiblockFootprint.placeAssembled(level, pos, machine, Direction.NORTH));
        // `te` no longer exists — setBlock above destroyed this BlockEntity. Touch nothing after here.
    }
}
