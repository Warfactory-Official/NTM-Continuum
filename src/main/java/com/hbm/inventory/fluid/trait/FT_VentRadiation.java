package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The 1.7 {@code FT_VentRadiation}: how much chunk radiation one millibucket of a fluid carries once it is out
 * of its container. UF6 0.2, PUF6 0.1, SAS3 1, schrabidic acid 1, Watz mud 0.1, liquid/gaseous waste 0.5,
 * tritium 0.001 (1.7 {@code Fluids.java}).
 *
 * <p>The consumers are 1.7's: {@code EntityMist} adds {@code radPerMB * 2} to its chunk every tick and
 * contaminates a living thing inside it by {@code radPerMB * 5}; {@code EntityChemical} contaminates by
 * {@code radPerMB * 5} and adds the same to the chunk at the hit entity or block.
 *
 * <p>1.7's {@code onFluidRelease} override is not carried, for the reason {@link FT_Polluting} gives: nothing in
 * 1.7 dispatches to a trait's {@code onFluidRelease} (the {@code FluidType} delegate machines call is an empty
 * method), so a tank or drum overflowing UF6 never irradiated its chunk in 1.7 either.
 */
public final class FT_VentRadiation {

    private final float radPerMB;

    public FT_VentRadiation(float rad) {
        this.radPerMB = rad;
    }

    public float getRadPerMB() {
        return radPerMB;
    }

    /** 1.7 {@code addInfo}: the yellow bracketed trait tag. */
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.radioactive")).append("]")
                .withStyle(ChatFormatting.YELLOW));
    }
}
