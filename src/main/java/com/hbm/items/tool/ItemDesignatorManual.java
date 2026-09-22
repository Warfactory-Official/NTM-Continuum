package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.12.2 {@code ItemDesingatorManual}, including its coordinate-entry screen bridge. */
public final class ItemDesignatorManual extends CompatItem implements IDesignatorItem {
    private static volatile Consumer<ItemStack> clientScreenOpener;

    public ItemDesignatorManual(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Client entrypoint installs the screen without loading client classes on a server. */
    public static void setClientScreenOpener(Consumer<ItemStack> opener) {
        clientScreenOpener = opener;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (ModDataComponents.DESIGNATOR_X.has(stack)) {
            tooltip.add(Component.literal("Target Coordinates:").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("X: " + ModDataComponents.DESIGNATOR_X.getOr(stack, 0))
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("Z: " + ModDataComponents.DESIGNATOR_Z.getOr(stack, 0))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Please select a target.").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            Consumer<ItemStack> opener = clientScreenOpener;
            if (opener != null) opener.accept(player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isReady(Level world, ItemStack stack, BlockPos padPos) {
        return ModDataComponents.DESIGNATOR_X.has(stack);
    }

    @Override
    public Vec3 getCoords(Level world, ItemStack stack, BlockPos padPos) {
        return new Vec3(ModDataComponents.DESIGNATOR_X.getOr(stack, 0), 0,
                ModDataComponents.DESIGNATOR_Z.getOr(stack, 0));
    }
}
