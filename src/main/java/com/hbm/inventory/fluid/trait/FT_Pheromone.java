package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The 1.7 {@code FT_Pheromone}: type 1 is glyphid pheromone (buffs glyphids), type 2 the modified pheromone
 * (buffs players). Read by {@code EntityMist} and {@code EntityChemical}, whose buff tables differ — 1.7 keeps
 * them separate and so does the port.
 */
public final class FT_Pheromone {

    private final int type;

    public FT_Pheromone(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    /** 1.7 {@code addInfo}: aqua glyphid pheromones for type 1, blue modified pheromones otherwise. */
    public void addInfo(List<Component> info) {
        if (type == 1) {
            info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.glyphidPheromones"))
                    .append("]").withStyle(ChatFormatting.AQUA));
        } else {
            info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.modifiedPheromones"))
                    .append("]").withStyle(ChatFormatting.BLUE));
        }
    }
}
