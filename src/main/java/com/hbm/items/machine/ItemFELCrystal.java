package com.hbm.items.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A laser crystal — the interchangeable "lens" that sets a {@code machine_fel}'s wavelength, and with
 * it which SILEX recipes can run and how fast. 1:1 port of 1.12.2's {@code ItemFELCrystal}.
 *
 * <p>Five crystals exist, one per usable wavelength; the sixth enum constant, {@link
 * EnumWavelengths#NULL}, is the "no crystal / powered down" state and has no item.
 */
public class ItemFELCrystal extends Item {

    /**
     * The electromagnetic spectrum as HBM models it. <b>Ordinal is load-bearing</b>: the SILEX gates a
     * recipe on {@code recipe.laserStrength.ordinal() <= mode.ordinal()} and scales its speed by the
     * difference, and the FEL's power draw is {@code powerReq * 4^ordinal}. Do not reorder.
     *
     * <p>{@code VISIBLE} deliberately carries colour 0 for both the world beam and the GUI: every call
     * site special-cases it into an animated rainbow (see {@code GUISILEX}/{@code FELRenderer}).
     *
     * <p>The lang files also carry {@code wavelengths.name.micro/radio/xray} keys with no matching enum
     * constant — vestigial in 1.12.2 as well, not ported.
     */
    public enum EnumWavelengths {
        NULL("la creatura", "6 dollar", 0x010101, 0x010101, ChatFormatting.WHITE),   // upstream's joke placeholder
        IR("wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED),
        VISIBLE("wavelengths.name.visible", "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN),
        UV("wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA),
        GAMMA("wavelengths.name.gamma", "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE),
        DRX("wavelengths.name.drx", "wavelengths.waveRange.drx", 0xFF0000, 0xFF0000, ChatFormatting.DARK_RED);

        public final String name;
        public final String wavelengthRange;
        public final int renderedBeamColor;
        public final int guiColor;
        public final ChatFormatting textColor;

        EnumWavelengths(String name, String wavelengthRange, int renderedBeamColor, int guiColor, ChatFormatting textColor) {
            this.name = name;
            this.wavelengthRange = wavelengthRange;
            this.renderedBeamColor = renderedBeamColor;
            this.guiColor = guiColor;
            this.textColor = textColor;
        }

        public static final EnumWavelengths[] VALUES = values();

        /** Tolerant lookup for save/sync data, so an unknown name degrades to NULL instead of throwing. */
        public static EnumWavelengths byName(String name) {
            for (EnumWavelengths w : VALUES) {
                if (w.name().equals(name)) return w;
            }
            return NULL;
        }
    }

    public final EnumWavelengths wavelength;
    private final boolean digamma;

    public ItemFELCrystal(Properties properties, EnumWavelengths wavelength, boolean digamma) {
        super(properties.stacksTo(1));
        this.wavelength = wavelength;
        this.digamma = digamma;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        // The digamma crystal replaces its description with scrambled text rather than a lang key.
        adder.accept(digamma
                ? Component.literal("THERADIANCEOFATHOUSANDSUNS").withStyle(ChatFormatting.OBFUSCATED)
                : Component.translatable(stack.getItem().getDescriptionId() + ".desc"));
        adder.accept(Component.translatable(wavelength.name)
                .append(" - ")
                .append(Component.translatable(wavelength.wavelengthRange))
                .withStyle(wavelength.textColor));
    }
}
