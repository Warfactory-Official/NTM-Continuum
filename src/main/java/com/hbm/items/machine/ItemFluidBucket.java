package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Locale;

/**
 * A full fluid bucket — 1000&nbsp;mB of one {@link FluidType}, used to fill a fluid tank for testing.
 * A single registered item carries the fluid in the {@link ModDataComponents#FLUID_ID} component; its
 * icon is the vanilla {@code minecraft:item/bucket} with a {@code fluid_bucket_overlay} fill tinted by
 * the fluid colour (stored in the vanilla {@code custom_model_data} colour list, read by the model's
 * {@code minecraft:custom_model_data} tint source — no per-fluid model or client colour handler needed).
 * Dropping it in a tank's fill-in slot adds 1000&nbsp;mB and returns an empty {@code Items.BUCKET} to the
 * fill-out slot (see {@code TileEntityMachineFluidTank.fillFromBucket}).
 */
public class ItemFluidBucket extends CompatItem {

    public static final int VOLUME = 1000;

    public ItemFluidBucket(Properties properties) {
        super(properties);
    }

    /** Builds a full bucket stack for {@code fluid} (FLUID_ID for logic + the tint colour for rendering). */
    public static ItemStack make(FluidType fluid) {
        ItemStack stack = new ItemStack(ModFluidItems.fluid_bucket.get());
        ModDataComponents.fluidId(stack, fluid.getID());
        ModelData.setColor(stack, fluid.getColor());
        return stack;
    }

    public static FluidType getFluid(ItemStack stack) {
        return Fluids.fromID(ModDataComponents.fluidId(stack));
    }

    /** Title-cases the internal fluid name, e.g. {@code SCHRABIDIC_ACID} → {@code "Schrabidic Acid"}. */
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
        return Component.translatable("item.hbm.fluid_bucket", prettify(getFluid(stack).getName()));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(VOLUME + " mB").withStyle(ChatFormatting.GRAY));
    }
}
