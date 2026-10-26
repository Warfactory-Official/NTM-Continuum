package com.hbm.capability;

import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Shared block contracts, including the folded core behind a proxy cell. */
public final class NtmContracts {
    public static final Toolable TOOLABLE = new Toolable();

    private NtmContracts() {}

    public static final class Toolable {
        private Toolable() {}
        public @Nullable IToolable at(Level level, BlockPos pos) { return IToolable.at(level, pos); }
    }
}
