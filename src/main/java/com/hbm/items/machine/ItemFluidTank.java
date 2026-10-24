package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A portable universal fluid container, the 1.12.2 {@code ItemFluidTankV2}. One registered item holds
 * any single {@link FluidType} up to {@link #capacity} mB; the fluid rides {@link ModDataComponents#FLUID}
 * and the amount rides {@link ModDataComponents#FLUID_AMOUNT}. Used for the <b>universal fluid tank</b>
 * (1000&nbsp;mB) and the <b>fluid barrel</b> (16000&nbsp;mB).
 *
 * <p>Implements {@link IFillableItem} so the machine fluid tank's loader slot can fill/empty it. The icon
 * is a base sprite ({@code hbm:items/<base>}) plus an overlay ({@code hbm:items/<base>_overlay}) tinted by
 * the fluid colour via the vanilla {@code custom_model_data} colour list (as the fluid bucket); the fill
 * level shows on the vanilla durability bar. The Forge {@code IFluidHandlerItem} capability and the
 * lead-shielded variant are not ported.
 */
public class ItemFluidTank extends CompatItem implements IFillableItem {

    public final int capacity;
    private final String base; // texture/lang base, e.g. "fluid_tank" / "fluid_barrel"

    public ItemFluidTank(Properties properties, int capacity, String base) {
        super(properties);
        this.capacity = capacity;
        this.base = base;
    }

    // Component access

    public static FluidType getType(ItemStack stack) {
        return ModDataComponents.fluid(stack);
    }

    @Override
    public int getFill(ItemStack stack) {
        return Math.max(0, ModDataComponents.fluidAmount(stack));
    }

    /** Writes the fluid + amount onto the stack (and the tint colour for rendering). When empty, the
     * components are removed so an emptied container is identical to (and stacks with) a pristine one. */
    public void setFluid(ItemStack stack, FluidType type, int amount) {
        if (type == null || type == Fluids.NONE || amount <= 0) {
            ModDataComponents.removeFluid(stack);
            ModDataComponents.removeFluidAmount(stack);
            ModDataComponents.removeCustomModelData(stack);
            return;
        }
        ModDataComponents.fluid(stack, type);
        ModDataComponents.fluidAmount(stack, amount);
        ModelData.setColor(stack, type.getColor());
    }

    /** A container holding {@code amount} mB of {@code type} (for creative variants / recipes). */
    public ItemStack make(FluidType type, int amount) {
        ItemStack stack = new ItemStack(this);
        setFluid(stack, type, amount);
        return stack;
    }

    // IFillableItem

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

    // Display: name, fill bar, tooltip

    @Override
    public Component getName(ItemStack stack) {
        int fill = getFill(stack);
        if (fill <= 0) return Component.translatable("item.hbm." + base + ".empty");
        return Component.translatable("item.hbm." + base + ".not_empty", getType(stack).getDisplayName());
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
        int color = getType(stack).clientMetadata().getColor();
        return color >= 0 ? color : 0x3399FF;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getFill(stack) + " / " + capacity + " mB").withStyle(ChatFormatting.GRAY));
    }

}
