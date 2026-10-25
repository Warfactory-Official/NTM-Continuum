package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Block item of the tungsten crate and the safe, with 1.7 {@code BlockStorageCrate.addInformation}: a crate with
 * spiders only says it skitters; a locked one only whether it feels heavy or empty; otherwise the first ten
 * stacks it carries and how many more there are.
 */
public class ItemBlockLockableCrate extends CompatBlockItem {

    private final int slots;

    public ItemBlockLockableCrate(Block block, Properties properties, int slots) {
        super(block, properties);
        this.slots = slots;
    }

    /** 1.7 {@code ItemBlockStorageCrate}: opened from the hand under {@code /ntmserver CRATE_OPEN_HELD}. */
    @Override
    protected net.minecraft.world.@org.jspecify.annotations.Nullable InteractionResult compatBeforePlace(
            UseOnContext context) {
        return CrateHeldOpening.beforePlace(context);
    }

    @Override
    protected net.minecraft.world.InteractionResult compatUse(Level level,
            Player player, InteractionHand hand) {
        return CrateHeldOpening.use(level, player, hand);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        boolean locked = ItemStacks.carriedInt(stack, "lock") != 0;

        if (ItemStacks.carriedBoolean(stack, "spiders")) {
            if (locked) list.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Skittering emanates from within...") // lamo
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        if (locked) {
            // Sorry people who want to see what's in it while it's locked...
            list.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
            for (int i = 0; i < slots; i++) {
                if (!ItemStacks.carriedItem(stack, "inventory", slots, i).isEmpty()) {
                    list.add(Component.literal("It feels heavy...").withStyle(ChatFormatting.YELLOW));
                    return;
                }
            }
            list.add(Component.literal("It feels empty...").withStyle(ChatFormatting.YELLOW));
            return;
        }

        List<Component> contents = new ArrayList<>();
        int amount = 0;
        for (int i = 0; i < slots; i++) {
            ItemStack content = ItemStacks.carriedItem(stack, "inventory", slots, i);
            if (content.isEmpty()) continue;
            amount++;
            if (contents.size() < 10) {
                contents.add(Component.literal(" - ").append(content.getHoverName())
                        .append(content.getCount() > 1 ? " x" + content.getCount() : "").withStyle(ChatFormatting.AQUA));
            }
        }

        if (!contents.isEmpty()) {
            list.add(Component.literal("Contains:").withStyle(ChatFormatting.AQUA));
            list.addAll(contents);
            amount -= contents.size();
            if (amount > 0) list.add(Component.literal("...and " + amount + " more.").withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
