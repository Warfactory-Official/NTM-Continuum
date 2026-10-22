package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A fluid identifier: stamps a tank/duct with one fixed {@link FluidType} (click a duct, sneak-click a barrel or
 * tank, or drop it in a tank's identifier slot). The blocks handle the click, as for 1.7's multi identifier.
 * One registered item carries the fluid in {@link ModDataComponents#FLUID}, and the creative menu shows one
 * variant per fluid (see {@link ModMachineItems}), as the 1.12.2 {@code ItemFluidIDMulti} listed one metadata
 * subtype per fluid.
 *
 * <p>Icon: the base {@code fluid_identifier_multi} sprite under a {@code fluid_identifier_overlay} tinted
 * by the fluid colour (stored in the vanilla {@code custom_model_data} colour list, read by the item-model
 * definition's {@code minecraft:custom_model_data} tint source), in place of the original's
 * {@code IItemColor} handler. No cycling, no per-fluid model.
 */
public class ItemFluidId extends CompatItem implements IItemFluidId {

    public ItemFluidId(Properties properties) {
        super(properties);
    }

    public static FluidType getFluid(ItemStack stack) {
        return ModDataComponents.fluid(stack);
    }

    /** Builds a dedicated identifier for {@code fluid} (FLUID for logic + the tint colour for rendering). */
    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModMachineItems.fluid_identifier.get());
        ModDataComponents.fluid(stack, fluid);
        ModelData.setColor(stack, fluid.getColor());
        return stack;
    }

    /**
     * The tint reads the colour stored beside the fluid id, which only {@link #make} writes. A stack built any
     * other way ({@code /give hbm:fluid_identifier[hbm:fluid_id=22]}, a recipe, another mod) carried the id
     * alone and rendered untinted; the first server tick in an inventory now stores the fluid's colour.
     */
    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level,
                                       Entity owner,
                                       EquipmentSlot slot, boolean held) {
        syncColor(stack);
    }

    /** Stores the fluid's colour for the tint when the stack does not already carry it. */
    public static void syncColor(ItemStack stack) {
        int color = getFluid(stack).getColor();
        if (ModelData.color(stack, ~color) != color) ModelData.setColor(stack, color);
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
