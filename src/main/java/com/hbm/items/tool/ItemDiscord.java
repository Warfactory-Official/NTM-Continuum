package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 {@code ItemDiscord}, the Rod of Discord: teleports the user onto the block face it looks at, up to
 * 100 blocks away ({@code Library.rayTrace(player, 100, 1)}), one block out along the hit face and one
 * down, with the enderman teleport sound at both ends and 32 portal particles.
 */
public class ItemDiscord extends CompatItem {

    public ItemDiscord(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        HitResult pos = player.pick(100, 1.0F, false);

        if (pos.getType() == HitResult.Type.BLOCK && pos instanceof BlockHitResult hit) {
            if (!world.isClientSide()) {
                if (player.isPassenger()) player.stopRiding();

                Direction dir = hit.getDirection();
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 1.0F);

                Vec3 at = hit.getLocation();
                player.teleportTo(at.x + dir.getStepX(), at.y + dir.getStepY() - 1, at.z + dir.getStepZ());

                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                player.resetFallDistance();
            }

            for (int i = 0; i < 32; ++i) {
                world.addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0D,
                        player.getZ(), player.getRandom().nextGaussian(), 0.0D, player.getRandom().nextGaussian());
            }
        }

        // 1.7 returned the stack without starting a use: no swing, no consumption
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("I've seen the Rod of Discord and honestly").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal("it's not as amazing as people say.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal(""));
        list.add(Component.literal("Rod of Discord is crucial in so many boss fights.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        list.add(Component.literal("Imagine getting coiled by worm bosses.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        list.add(Component.literal(""));
        list.add(Component.literal("Oh, you mean the Terraria item.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal("Idk about that thing.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
