package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ItemStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The ordinary 1.12.2 {@code ItemTooling} path for blocks implementing {@link IToolable}. */
public class ItemTooling extends CompatItem {

    private final IToolable.ToolType type;

    public ItemTooling(Properties properties, IToolable.ToolType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Block block = level.getBlockState(context.getClickedPos()).getBlock();
        Player player = context.getPlayer();

        if (player != null && block instanceof IToolable toolable
                && toolable.onScrew(level, player, context.getClickedPos(), context.getClickedFace(),
                context.getClickLocation(), context.getHand(), type)) {
            ItemStack stack = context.getItemInHand();
            if (stack.getMaxDamage() > 0) {
                ItemStacks.hurtAndBreak(stack, 1, player, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }

        // The old item delegated rejected/non-tool blocks to Item.onItemUse.
        return super.useOn(context);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (type == IToolable.ToolType.SCREWDRIVER) {
            tooltip.add(Component.translatable("desc.screwdriver1"));
        }
    }

    public IToolable.ToolType getType() {
        return type;
    }
}
