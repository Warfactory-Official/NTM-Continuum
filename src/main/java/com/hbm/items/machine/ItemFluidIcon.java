package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * The fluid "droplet" icon — a single item that visually <b>represents</b> any {@link FluidType} (the
 * 1.12.2 {@code ItemFluidIcon}). It is never used as a real ingredient; recipes use it purely as a
 * display icon for fluid inputs/outputs (e.g. {@link com.hbm.inventory.recipes.loader.GenericRecipe
 * #getIcon} falls back to it for a fluid-output recipe, and the recipe selector shows it).
 *
 * <p>Like {@link ItemFluidBucket} it carries the fluid in the {@link ModDataComponents#FLUID_ID}
 * component and tints the grayscale {@code fluid_icon.png} droplet by the fluid colour via the vanilla
 * {@code custom_model_data} colour list (read by the model's {@code minecraft:custom_model_data} tint
 * source) — so one item + one model covers all 157 fluids, no per-fluid model or client colour handler
 * (the 26.1 replacement for the 1.12.2 metadata subtypes + {@code IItemColor}).
 */
public class ItemFluidIcon extends Item {

    public ItemFluidIcon(Properties properties) {
        super(properties);
    }

    /** Builds a droplet icon for {@code fluid} (FLUID_ID for identity + the tint colour for rendering). */
    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModFluidItems.fluid_icon.get());
        ModDataComponents.fluidId(stack, fluid.getID());
        ModelData.setColor(stack, fluid.getColor());
        return stack;
    }

    /** Convenience: a droplet for a {@link FluidStack}'s type (amount/pressure are shown by the recipe). */
    public static ItemStack make(FluidStack stack) {
        return make(stack.type);
    }

    public static FluidType getFluid(ItemStack stack) {
        return Fluids.fromID(ModDataComponents.fluidId(stack));
    }

    /** Title-cases the internal fluid name, e.g. {@code SULFURIC_ACID} → {@code "Sulfuric Acid"}. */
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
        return Component.literal(prettify(getFluid(stack).getName()));
    }
}
