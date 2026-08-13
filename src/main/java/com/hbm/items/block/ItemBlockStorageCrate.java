package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
//? if >=26.1 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * Block item for the storage crates. Adds the "{@code X/Y Slots used (Z%)}" tooltip the 1.12.2
 * {@code BlockStorageCrate.addInformation} showed, reading the stored inventory from the item's
 * {@link DataComponents#CONTAINER} component (which the crate carries its contents in — see
 * {@link com.hbm.tileentity.machine.storage.TileEntityCrate}). Colour-coded like the original:
 * green &lt;25 %, yellow in the middle, red ≥75 %.
 */
public class ItemBlockStorageCrate extends BlockItem {

    private final CrateType crateType;

    public ItemBlockStorageCrate(Block block, Properties properties, CrateType crateType) {
        super(block, properties);
        this.crateType = crateType;
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        java.util.function.Consumer<Component> adder = lines::add;
    *///?}

        int total = crateType.slots;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        int used = contents == null ? 0 : (int) (
                //? if >=26.1 {
                contents.nonEmptyItemCopyStream()
                //?} else {
                /*contents.nonEmptyStream()
                *///?}
                ).count();

        float percent = Math.round(used * 1000.0F / total) / 10.0F; // one decimal, like Library.roundFloat(..,1)
        ChatFormatting color = percent >= 75 ? ChatFormatting.RED
                : percent < 25 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;

        adder.accept(Component.literal(used + "/" + total + " Slots used (" + percent + "%)").withStyle(color));
    }
}
