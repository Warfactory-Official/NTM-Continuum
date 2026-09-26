package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * How badly a fluid eats what it touches, 0–100. Above fifty it goes through iron; below, only
 * plastics. Carried by a {@link com.hbm.api.fluid.FluidType} the same way the thermal traits
 * are — a plain object in the type's trait map, with no {@code FluidTrait} base class, because this
 * port's traits are looked up by their own class rather than through a registry.
 */
public final class FT_Corrosive {

    private final int rating;

    public FT_Corrosive(int rating) {
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }

    public boolean isHighlyCorrosive() {
        return rating > 50;
    }

    public void addInfo(List<Component> info) {
        info.add(Component.literal("[")
                .append(Component.translatable(isHighlyCorrosive() ? "trait.corrosiveIron" : "trait.corrosivePlastic"))
                .append("]")
                .withStyle(isHighlyCorrosive() ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
    }
}
