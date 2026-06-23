package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemFluidTank extends Item implements IFillableItem {

    public final int capacity;
    private final String base;

    public ItemFluidTank(Properties properties, int capacity, String base) {
        super(properties);
        this.capacity = capacity;
        this.base = base;
    }

    public static FluidType getType(ItemStack stack) {
        return Fluids.fromID(stack.getOrDefault(ModDataComponents.FLUID_ID.get(), 0));
    }

    @Override
    public int getFill(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(ModDataComponents.FLUID_AMOUNT.get(), 0));
    }

    public void setFluid(ItemStack stack, FluidType type, int amount) {
        if (type == null || type == Fluids.NONE || amount <= 0) {
            stack.remove(ModDataComponents.FLUID_ID.get());
            stack.remove(ModDataComponents.FLUID_AMOUNT.get());
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }
        stack.set(ModDataComponents.FLUID_ID.get(), type.getID());
        stack.set(ModDataComponents.FLUID_AMOUNT.get(), amount);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(type.getColor())));
    }

    public ItemStack make(FluidType type, int amount) {
        ItemStack stack = new ItemStack(this);
        setFluid(stack, type, amount);
        return stack;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        if (type == null || type == Fluids.NONE) return false;
        FluidType held = getType(stack);
        return (held == Fluids.NONE || held == type) && getFill(stack) < capacity;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack) || amount <= 0) return amount;
        int fill = getFill(stack);
        int added = Math.min(amount, capacity - fill);
        setFluid(stack, type, fill + added);
        return amount - added;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return type != null && getType(stack) == type && getFill(stack) > 0;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (!providesFluid(type, stack) || amount <= 0) return 0;
        int taken = Math.min(amount, getFill(stack));
        setFluid(stack, type, getFill(stack) - taken);
        return taken;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getFill(stack) > 0 ? getType(stack) : Fluids.NONE;
    }

    @Override
    public Component getName(ItemStack stack) {
        int fill = getFill(stack);
        if (fill <= 0) return Component.translatable("item.hbm." + base + ".empty");
        return Component.translatable("item.hbm." + base + ".not_empty", prettify(getType(stack).getName()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int fill = getFill(stack);
        return fill > 0 && fill < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getFill(stack) * 13.0F / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int color = getType(stack).getColor();
        return color >= 0 ? color : 0x3399FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal(getFill(stack) + " / " + capacity + " mB").withStyle(ChatFormatting.GRAY));
    }

    private static String prettify(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
