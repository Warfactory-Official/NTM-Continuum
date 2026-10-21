package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Range Designator, ported from 1.12.2's {@code ItemDesignatorRange}. Same job as the plain
 * {@link ItemDesignator}, but it targets whatever the player is <em>looking at</em> up to 300 blocks
 * away instead of what they can reach, which is how a target is picked from a hilltop.
 */
public class ItemDesignatorRange extends CompatItem implements IDesignatorItem {

    /** Upstream's reach. Anything past the loaded chunks misses. */
    private static final double RANGE = 300D;

    public ItemDesignatorRange(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        ItemDesignator.appendTargetTooltip(stack, tooltip);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        if (level.getBlockState(pos).getBlock() instanceof LaunchPad) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ItemDesignator.setTarget(level, player, player.getItemInHand(hand), pos.getX(), pos.getZ(),
                Component.literal("Position set to X:" + pos.getX() + ", Z:" + pos.getZ()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isReady(Level world, ItemStack stack, BlockPos padPos) {
        return ModDataComponents.DESIGNATOR_X.has(stack);
    }

    @Override
    public Vec3 getCoords(Level world, ItemStack stack, BlockPos padPos) {
        return new Vec3(ModDataComponents.DESIGNATOR_X.getOr(stack, 0), 0, ModDataComponents.DESIGNATOR_Z.getOr(stack, 0));
    }
}
