package com.hbm.items.machine;

import com.hbm.blocks.StandardInfo;
import dev.golem.core.data.ModelData;
import dev.golem.core.viewer.FluidIcons;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.util.BobMathUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The fluid "droplet" icon: a single item that visually <b>represents</b> any {@link FluidType} (the
 * 1.12.2 {@code ItemFluidIcon}). It is never used as a real ingredient; recipes use it purely as a
 * display icon for fluid inputs/outputs (e.g. {@link com.hbm.inventory.recipes.loader.GenericRecipe
 * #getIcon} falls back to it for a fluid-output recipe, and the recipe selector shows it).
 *
 * <p>Like {@link ItemFluidBucket} it carries the fluid in the {@link ModDataComponents#FLUID}
 * component, and stacks made from a {@link FluidStack} retain their amount in
 * {@link ModDataComponents#FLUID_AMOUNT}. It tints the grayscale {@code fluid_icon.png} droplet by the fluid colour via the vanilla
 * {@code custom_model_data} colour list (read by the model's {@code minecraft:custom_model_data} tint
 * source), so one item and one model covers all 156 fluids, no per-fluid model or client colour handler
 * (the 26.1 replacement for the 1.12.2 metadata subtypes + {@code IItemColor}).
 *
 * <p>This is the droplet wherever there is an item slot to put it in, which is nearly everywhere JEI
 * shows a fluid. Where there is not, as with a fixed corner of a screen, an overlay, or a category icon, the same
 * droplet is on the atlas per fluid, drawn by {@link com.hbm.client.FluidSprites#draw}.
 */
public class ItemFluidIcon extends CompatItem {

    public ItemFluidIcon(Properties properties) {
        super(properties);
    }

    /** Builds a droplet icon for {@code fluid} (FLUID for identity + the tint colour for rendering). */
    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModFluidItems.fluid_icon.get());
        ModDataComponents.fluid(stack, fluid);
        ModelData.setColor(stack, fluid.getColor());
        return stack;
    }

    /** Convenience: a droplet for a {@link FluidStack}'s type, retaining its amount for the tooltip. */
    public static ItemStack make(FluidStack stack) {
        ItemStack icon = make(stack.type);
        ModDataComponents.fluidAmount(icon, stack.fill);
        ModDataComponents.fluidPressure(icon, stack.pressure);
        return icon;
    }

    public static ItemStack make(FluidType fluid, int amount) {
        return make(new FluidStack(fluid, amount));
    }

    public static ItemStack make(FluidType fluid, int amount, int pressure) {
        return make(new FluidStack(fluid, amount, pressure));
    }

    /** {@code stack} at {@code times} its amount: 1.7's oil tables show per 1,000 mB of input, ten operations. */
    public static ItemStack make(FluidStack stack, int times) {
        return make(stack.type, stack.fill * times, stack.pressure);
    }

    public static FluidType getFluid(ItemStack stack) {
        return ModDataComponents.fluid(stack);
    }

    /** The fluid a recipe viewer files the icon under, so the fluid's own entry finds the recipe. */
    public static FluidIcons.Amount viewerFluid(ItemStack icon) {
        return new FluidIcons.Amount(getFluid(icon).getFluid(), ModDataComponents.fluidAmount(icon));
    }

    @Override
    public Component getName(ItemStack stack) {
        return getFluid(stack).getDisplayName();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        int amount = ModDataComponents.fluidAmount(stack);
        if (amount > 0) tooltip.add(Component.literal(amount + "mB"));
        int pressure = ModDataComponents.fluidPressure(stack);
        if (pressure > 0) {
            tooltip.add(Component.literal(pressure + "PU").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Pressurized, use compressor!")
                    .withStyle(BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_RED));
        }
        getFluid(stack).clientMetadata().addInfo(tooltip, StandardInfo.shiftHeld());
    }
}
