package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The block entity that owns a cell, for a net that found a connector block rather than a machine.
 *
 * <p>Continuum has two multiblock systems, and a resolver that knows only one of them misses the other
 * in silence. The dummyable system kept its core at a fixed offset every cell could compute; a folded
 * multiblock's cells carry no offset at all and are resolved through the server's core index. A turret
 * pad is the second kind, and asking it for a dummy offset answers nothing, which is why a
 * cable or a duct laid against one reached the turret through neither.
 */
public final class MachineCore {

    private MachineCore() {}

    /**
     * Which way the machine at this state faces, whichever system its block belongs to.
     *
     * <p>Dozens of block entities recover their own facing this way to lay out their ports, and asking
     * here rather than reading the property is what kept a machine's migration invisible to its tile.
     */
    public static ForgeDirection facing(BlockState state) {
        if (state.hasProperty(BlockMultiblockCore.FACING)) {
            return ForgeDirection.getOrientation(state.getValue(BlockMultiblockCore.FACING));
        }
        return ForgeDirection.UNKNOWN;
    }

    /**
     * {@link #facing} as a {@link ForgeDirection} ordinal, with the fallback a caller used when the
     * property was absent. The renderers all want the ordinal rather than the direction.
     */
    public static int facingOrdinal(BlockState state, int fallback) {
        ForgeDirection facing = facing(state);
        return facing == ForgeDirection.UNKNOWN ? fallback : facing.ordinal();
    }

    public static @Nullable BlockEntity at(Level level, BlockPos cell, BlockState state) {
        if (MultiblockSurface.foldedCore(state) == null && !MultiblockSurface.isFoldedCell(state)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, cell);
        return core == null ? null : level.getBlockEntity(core);
    }
}
