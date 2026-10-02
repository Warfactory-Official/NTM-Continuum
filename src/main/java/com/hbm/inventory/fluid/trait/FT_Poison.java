package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The 1.7 {@code FT_Poison} (deprecated there in favour of {@link FT_Toxin}, but still carried by six fluids):
 * contact with a cloud or spray of the fluid poisons, or withers, for {@code 5 * 20 * intensity} ticks at
 * amplifier 0. Applied by {@code EntityMist} and {@code EntityChemical} exactly where 1.7 does.
 *
 * <p>{@link #getLevel()} is carried but, as in 1.7, no consumer reads it: the effect is always amplifier 0.
 */
public final class FT_Poison {

    private final boolean withering;
    private final int level;

    public FT_Poison(boolean withering, int level) {
        this.withering = withering;
        this.level = level;
    }

    public boolean isWithering() {
        return withering;
    }

    public int getLevel() {
        return level;
    }

    /** 1.7 {@code addInfoHidden}: a literal, untranslated green {@code [Toxic Fumes]}. */
    public void addInfoHidden(List<Component> info) {
        info.add(Component.literal("[Toxic Fumes]").withStyle(ChatFormatting.GREEN));
    }
}
