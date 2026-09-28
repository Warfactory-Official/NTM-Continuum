package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.items.ModItems;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code BlockBallsSpawner}, the Mechanist's Circle: using the mechanist's key on it spends the key and drops
 * Balls-O-Tron Prime from y 300 above the circle, head first at one block per tick, and the circle cracks into
 * a plain cracked enargite brick. 1.7 answered false even then, so the click is not consumed.
 */
public class BlockBallsSpawner extends CompatBlock {

    public BlockBallsSpawner(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.is(ModItems.mech_key.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        stack.shrink(1);

        if (level instanceof ServerLevel server) {
            EntityBOTPrimeHead bot = Entities.createTriggered(ModNtmEntities.BALLS_O_TRON.get(), server);
            if (bot != null) {
                bot.setPos(pos.getX() + 0.5, 300, pos.getZ() + 0.5);
                bot.setYRot(0);
                bot.setXRot(0);
                bot.setDeltaMovement(0, -1.0, 0);
                Spawns.finalizeTriggered(bot, server);
                server.addFreshEntity(bot);
            }
            level.setBlockAndUpdate(pos, ModBlocks.brick_jungle_cracked.get().defaultBlockState());
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
