package com.hbm.items.tool;

import com.hbm.blocks.IAnalyzable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.world.CompatItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1.7 {@code ItemAnalysisTool}: right-click an {@link IAnalyzable} block (a multiblock by its core) and its
 * debug lines are printed to chat in yellow.
 */
public class ItemAnalysisTool extends CompatItem {

    public ItemAnalysisTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Block b = level.getBlockState(pos).getBlock();

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null) pos = core;

        if (b instanceof IAnalyzable analyzable) {
            if (level instanceof ServerLevel server && player != null) {
                List<String> debug = analyzable.getDebugInfo(server, pos);
                if (debug != null) {
                    for (String line : debug) {
                        player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
