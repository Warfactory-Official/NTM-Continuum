package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;
import com.hbm.blocks.ModDroneContent;
import com.hbm.tileentity.network.IDroneLinkable;

/**
 * 1.7 {@code ItemDroneLinker}: click one {@link IDroneLinkable} to remember it, click the next to link the first
 * to it (the remembered position moves along the chain); right-click the air to forget. The remembered position
 * is a data component here, and 1.7's "Prev pos" HUD readout is a tooltip line.
 */
public class ItemDroneLinker extends CompatItem {

    public ItemDroneLinker(Properties properties) {
        super(properties);
    }

    private MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    private void say(Player player, String text, ChatFormatting color) {
        player.sendSystemMessage(prefix().append(Component.literal(text).withStyle(color)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof IDroneLinkable linkable)) return InteractionResult.PASS;

        if (!level.isClientSide() && player != null) {
            Optional<BlockPos> previous = ModDroneContent.LINKER_POSITION.get(stack);

            if (previous.isEmpty()) {
                ModDroneContent.LINKER_POSITION.set(stack, pos.immutable());
                say(player, "Set initial position!", ChatFormatting.AQUA);
            } else {
                BlockEntity prev = level.getBlockEntity(previous.get());

                if (prev instanceof IDroneLinkable previousLinkable) {
                    previousLinkable.setNextTarget(linkable.getPoint());
                    say(player, "Link set!", ChatFormatting.AQUA);
                } else {
                    say(player, "Previous link lost!", ChatFormatting.RED);
                }

                ModDroneContent.LINKER_POSITION.set(stack, pos.immutable());
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && ModDroneContent.LINKER_POSITION.has(stack)) {
            ModDroneContent.LINKER_POSITION.remove(stack);
            say(player, "Position cleared!", ChatFormatting.GREEN);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        ModDroneContent.LINKER_POSITION.get(stack).ifPresent(pos ->
                tooltip.add(Component.literal("Prev pos: " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ())
                        .withStyle(ChatFormatting.GRAY)));
    }
}
