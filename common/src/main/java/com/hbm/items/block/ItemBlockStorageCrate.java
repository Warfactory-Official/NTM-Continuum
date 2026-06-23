package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class ItemBlockStorageCrate extends BlockItem {

    private final CrateType crateType;

    public ItemBlockStorageCrate(Block block, Properties properties, CrateType crateType) {
        super(block, properties);
        this.crateType = crateType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);

        int total = crateType.slots;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        int used = contents == null ? 0 : (int) contents.nonEmptyItemCopyStream().count();

        float percent = Math.round(used * 1000.0F / total) / 10.0F;
        ChatFormatting color = percent >= 75 ? ChatFormatting.RED
                : percent < 25 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;

        adder.accept(Component.literal(used + "/" + total + " Slots used (" + percent + "%)").withStyle(color));
    }
}
