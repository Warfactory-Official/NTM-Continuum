package com.hbm.blocks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code IAnalyzable}: a block that answers the analysis tool with debug lines. */
public interface IAnalyzable {

    @Nullable List<String> getDebugInfo(ServerLevel level, BlockPos pos);
}
