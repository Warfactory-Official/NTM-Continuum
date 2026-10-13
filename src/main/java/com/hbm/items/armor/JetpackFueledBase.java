package com.hbm.items.armor;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Fixed-fuel jetpack base; fuel is stored as a component rather than legacy item NBT. */
public abstract class JetpackFueledBase extends JetpackBase implements IFillableItem {

    public final FluidType fuel;
    public final int maxFuel;

    protected JetpackFueledBase(FluidType fuel, int maxFuel, Properties properties) {
        super(properties);
        this.fuel = fuel;
        this.maxFuel = maxFuel;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(fuel.getDisplayNameString() + ": " + getFuel(stack) + "mB / "
                + maxFuel + "mB").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == fuel;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack) || amount <= 0) return amount;
        int added = Math.min(amount, maxFuel - getFuel(stack));
        if (added <= 0) return amount;
        setFuel(stack, getFuel(stack) + added);
        return amount - added;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return fuel;
    }

    @Override
    public int getFill(ItemStack stack) {
        return getFuel(stack);
    }

    public int getMaxFill(ItemStack stack) {
        return maxFuel;
    }

    public int getLoadSpeed(ItemStack stack) {
        return 10;
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        // Old JetpackFueledBase reads the module back from the chest slot before showing its fuel line.
        ItemStack jetpack = ArmorModHandler.pryMod(armor,
                ArmorModHandler.plate_only);
        if (jetpack.isEmpty()) return;
        tooltip.add(Component.literal("  ").append(stack.getHoverName().copy().withStyle(ChatFormatting.RED))
                .append(Component.literal(" (" + fuel.getDisplayNameString() + ": " + getFuel(jetpack)
                        + "mB / " + maxFuel + "mB)").withStyle(ChatFormatting.RED)));
    }
}
