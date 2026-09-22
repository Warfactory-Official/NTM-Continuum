package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1.12.2 {@code ItemRangefinder}: reports the exact block hit distance to the action bar. */
public final class ItemRangefinder extends CompatItem {
    public static final int META_POLARIZED = 1;
    private static final double RANGE = 300D;

    public ItemRangefinder(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.rangefinder.desc"));
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            double distance = start.distanceTo(hit.getLocation());
            String message = ((int) (distance * 10D)) / 10D + "m";
            if (player.getItemInHand(hand).getDamageValue() == META_POLARIZED) {
                message = ChatFormatting.LIGHT_PURPLE + message + ChatFormatting.RESET;
            }
            player.sendSystemMessage(Component.literal(message));
        }
        return InteractionResult.SUCCESS;
    }
}
