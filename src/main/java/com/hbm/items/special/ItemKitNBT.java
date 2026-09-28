package com.hbm.items.special;

import com.hbm.items.ModMeleeKitItems;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.ItemContents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemKitNBT}: a kit whose give-list is carried on the stack ({@code ItemStackUtil.addStacksToNBT},
 * here the {@code hbm:kit_contents} component). Right-click hands every stack over and uses up the kit.
 * No subclass declares a container item, so 1.7's container branch never ran and is not carried.
 */
public class ItemKitNBT extends CompatItem {

    public ItemKitNBT(Properties properties) {
        super(properties);
    }

    public static List<ItemStack> contents(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack item : ModMeleeKitItems.KIT_CONTENTS.getOr(stack, ItemContents.EMPTY).copies()) {
            if (!item.isEmpty()) out.add(item);
        }
        return out;
    }

    public static void setContents(ItemStack stack, ItemStack... contents) {
        ModMeleeKitItems.KIT_CONTENTS.set(stack, new ItemContents(List.of(contents)));
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            for (ItemStack item : contents(stack)) player.getInventory().add(item.copy());
            world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_UNPACK.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        List<ItemStack> stacks = contents(stack);
        if (!ModMeleeKitItems.KIT_CONTENTS.has(stack)) return;

        list.add(Component.literal("Contains:"));
        for (ItemStack item : stacks) {
            list.add(Component.literal("-").append(item.getHoverName())
                    .append(item.getCount() > 1 ? " x" + item.getCount() : ""));
        }
    }

    /** 1.7 {@code ItemKitNBT.create}: a {@code kit_custom} holding {@code contents}. */
    public static ItemStack create(ItemStack... contents) {
        ItemStack stack = new ItemStack(ModMeleeKitItems.kit_custom.get());
        setContents(stack, contents);
        return stack;
    }
}
