package com.hbm.items.block;

import com.hbm.blocks.generic.RedBarrel;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import com.hbm.blocks.ModFluidNetBlocks;

/**
 * Block item for the fluid-network blocks: the fluid barrels' hard-coded capacity/material lines and, for a barrel
 * picked up with fluid in it, the 1.7 {@code IPersistentInfoProvider} line showing what it holds.
 */
public final class FluidNetBlockItem extends CompatBlockItem {

    public FluidNetBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getBlock() instanceof BlockFluidBarrel barrel) {
            int fill = ItemStacks.carriedInt(stack, "fill");
            if (fill > 0) {
                int max = ItemStacks.carriedInt(stack, "max");
                                tooltip.add(Component.literal(fill + "/" + max + "mB ").withStyle(ChatFormatting.YELLOW)
                        .append(Fluids.byRegistryName(ItemStacks.carriedString(stack, "type")).getDisplayName()));
            }
            if (barrel == ModFluidNetBlocks.barrel_plastic.get()) {
                tooltip.add(Component.literal("Capacity: 12,000mB").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Cannot store hot fluids").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Cannot store corrosive fluids").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Cannot store antimatter").withStyle(ChatFormatting.YELLOW));
            } else if (barrel == ModFluidNetBlocks.barrel_corroded.get()) {
                tooltip.add(Component.literal("Capacity: 16,000mB").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Leaky").withStyle(ChatFormatting.RED));
            } else if (barrel == ModFluidNetBlocks.barrel_steel.get()) {
                tooltip.add(Component.literal("Capacity: 16,000mB").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Can store hot fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Can store corrosive fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Cannot store highly corrosive fluids properly").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Cannot store antimatter").withStyle(ChatFormatting.YELLOW));
            } else if (barrel == ModFluidNetBlocks.barrel_antimatter.get()) {
                tooltip.add(Component.literal("Capacity: 16,000mB").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Can store hot fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Can store highly corrosive fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Can store antimatter").withStyle(ChatFormatting.GREEN));
            } else if (barrel == ModFluidNetBlocks.barrel_tcalloy.get()) {
                tooltip.add(Component.literal("Capacity: 24,000mB").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Can store hot fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Can store highly corrosive fluids").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Cannot store antimatter").withStyle(ChatFormatting.YELLOW));
            }
        }
        // 1.7 ItemBlockLore: the red/pink/lox/taint barrels carry this one line.
        if (getBlock() instanceof RedBarrel) {
            tooltip.add(Component.literal("Static fluid barrel"));
        }
    }
}
