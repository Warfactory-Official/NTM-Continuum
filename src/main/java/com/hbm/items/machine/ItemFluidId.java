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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A fluid identifier: stamps a tank/duct with one fixed {@link FluidType} (right-click a duct or drop it
 * in a tank's identifier slot). Each fluid has its <b>own dedicated</b> identifier — a single registered
 * item carries the fluid in {@link ModDataComponents#FLUID}, and the creative menu shows one variant
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
     * Sneak-click on a duct or valve: retype the connected pipe network of the clicked pipe's old type (up to 256
     * pipes), the 1.7 sneaking behaviour. Vanilla never passes a sneak-click with an item in hand to the block, so
     * {@code BlockFluidDuct.onUseItem} only ever sees plain clicks; the spread runs here instead, as ntm-next's
     * {@code FluidIdentifierItem.useOn} does it.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isShiftKeyDown()) return super.useOn(context);
        var pos = context.getClickedPos();
        var entity = level.getBlockEntity(pos);
        FluidType before;
        if (entity instanceof com.hbm.tileentity.network.TileEntityFluidDuctNT duct) before = duct.getFluidType();
        else if (entity instanceof com.hbm.tileentity.network.TileEntityFluidValve valve) before = valve.getFluidType();
        else return super.useOn(context);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        FluidType hand = getFluid(context.getItemInHand());
        if (hand != before) {
            com.hbm.blocks.network.BlockFluidDuct.spreadType(level, pos, hand, before, 256);
            player.sendSystemMessage(Component.literal("Duct fluid: ").append(hand.getDisplayName())
                    .withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The tint reads the colour stored beside the fluid id, which only {@link #make} writes. A stack built any
     * other way ({@code /give hbm:fluid_identifier[hbm:fluid_id=22]}, a recipe, another mod) carried the id
     * alone and rendered untinted; the first server tick in an inventory now stores the fluid's colour.
     */
    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity owner,
                                       net.minecraft.world.entity.EquipmentSlot slot, boolean held) {
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
