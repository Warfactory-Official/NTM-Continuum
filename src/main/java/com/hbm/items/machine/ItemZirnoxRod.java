package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}

/** A single active, component-backed ZIRNOX fuel variant. */
public class ItemZirnoxRod extends Item {

    private final ZirnoxFuel fuel;

    public ItemZirnoxRod(Properties properties, ZirnoxFuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    public ZirnoxFuel fuel() {
        return fuel;
    }

    public int getLife(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ZIRNOX_LIFE.get(), 0);
    }

    public void setLife(ItemStack stack, int life) {
        stack.set(ModDataComponents.ZIRNOX_LIFE.get(), Math.max(0, life));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLife(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round(13F * (1F - Math.min(1F, (float) getLife(stack) / fuel.maxLife))));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x53C653;
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> hbm$lines, TooltipFlag flag) {
        java.util.function.Consumer<Component> adder = hbm$lines::add;
    *///?}
        adder.accept(Component.translatable("item.hbm.zirnox_rod.life", getLife(stack), fuel.maxLife)
                .withStyle(ChatFormatting.GRAY));
        if (fuel.breeding) {
            adder.accept(Component.translatable("desc.item.zirnoxBreedingRod").withStyle(ChatFormatting.GOLD));
        }
    }
}
