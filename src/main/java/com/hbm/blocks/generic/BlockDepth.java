package com.hbm.blocks.generic;

import com.hbm.api.item.IDepthRockTool;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Depth rock: the 1.12.2 {@code BlockDepth}. Set <b>unbreakable</b> ({@code setBlockUnbreakable()})
 * with an explosion resistance of only 10, so it cannot be mined by hand at any tool tier but is
 * trivially removed by explosives. The sole exception is a held {@link IDepthRockTool} (the depth
 * drill): it gets 1.7's fixed relative progress of {@code 1/50} (50 ticks to break; 1.12.2 had
 * {@code 1/100}). Ordinary tools keep the unbreakable destroy speed.
 */
public class BlockDepth extends CompatBlock {

    public static final float DEPTH_TOOL_PROGRESS = (float) (1D / 50D);

    public BlockDepth(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IDepthRockTool tool
                && level instanceof Level realLevel
                && tool.canBreakRock(realLevel, player, held, state, pos)) {
            return DEPTH_TOOL_PROGRESS;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
