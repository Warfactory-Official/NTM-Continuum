package com.hbm.util;

import com.hbm.tileentity.machine.TileEntityBlastDoorDummy;
import com.hbm.tileentity.MachineCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The old {@code CompatExternal.getCoreFromPos}: the block entity a lock tool should act on when the
 * clicked block is part of something bigger: a folded multiblock cell resolves to its core, a blast
 * door dummy to its door, anything else to its own block entity.
 */
public final class LockTargets {
    private LockTargets() {}

    public static @Nullable BlockEntity coreAt(Level level, BlockPos pos) {
        BlockEntity core = MachineCore.at(level, pos, level.getBlockState(pos));
        if (core != null) return core;
        BlockEntity own = level.getBlockEntity(pos);
        if (own instanceof TileEntityBlastDoorDummy dummy) return level.getBlockEntity(dummy.target);
        return own;
    }
}
