package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A fluid identifier: stamps a tank/duct with one fixed {@link FluidType} (right-click a duct or drop it
 * in a tank's identifier slot). Each fluid has its <b>own dedicated</b> identifier — a single registered
 * item carries the fluid in {@link ModDataComponents#FLUID_ID}, and the creative menu shows one variant
 * per fluid (see {@link ModMachineItems}), exactly as the 1.12.2 {@code ItemFluidIDMulti} listed one
 * metadata subtype per fluid.
 *
 * <p>Icon: the base {@code fluid_identifier_multi} sprite under a {@code fluid_identifier_overlay} tinted
 * by the fluid colour (stored in the vanilla {@code custom_model_data} colour list, read by the item-model
 * definition's {@code minecraft:custom_model_data} tint source) — the 26.1 replacement for the original's
 * {@code IItemColor} handler. No cycling, no per-fluid model.
 */
public class ItemFluidId extends CompatItem implements IItemFluidId {

    public ItemFluidId(Properties properties) {
        super(properties);
    }

    public static FluidType getFluid(ItemStack stack) {
        return Fluids.fromID(ModDataComponents.fluidId(stack));
    }

    /** Builds a dedicated identifier for {@code fluid} (FLUID_ID for logic + the tint colour for rendering). */
    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModMachineItems.fluid_identifier.get());
        ModDataComponents.fluidId(stack, fluid.getID());
        ModelData.setColor(stack, fluid.getColor());
        return stack;
    }

    @Override
    public FluidType getType(@Nullable Level world, int x, int y, int z, ItemStack stack) {
        return getFluid(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm.fluid_identifier", getFluid(stack).getDisplayName());
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.fluid_identifier.info").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("   ").append(getFluid(stack).getDisplayName()).withStyle(ChatFormatting.AQUA));
    }
}
