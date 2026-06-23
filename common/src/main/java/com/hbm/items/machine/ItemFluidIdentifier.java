package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemFluidIdentifier extends Item implements IItemFluidIdentifier {

    public ItemFluidIdentifier(Properties properties) {
        super(properties);
    }

    public static FluidType getFluid(ItemStack stack) {
        return Fluids.fromID(stack.getOrDefault(ModDataComponents.FLUID_ID.get(), 0));
    }

    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModMachineItems.fluid_identifier.get());
        stack.set(ModDataComponents.FLUID_ID.get(), fluid.getID());
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(fluid.getColor())));
        return stack;
    }

    @Override
    public FluidType getType(@Nullable Level world, int x, int y, int z, ItemStack stack) {
        return getFluid(stack);
    }

    private static String prettify(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm.fluid_identifier", prettify(getFluid(stack).getName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                               Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.fluid_identifier.info").withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("   " + prettify(getFluid(stack).getName())).withStyle(ChatFormatting.AQUA));
    }
}
