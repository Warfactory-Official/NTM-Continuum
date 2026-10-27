package com.hbm.items.machine;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A laser crystal: the interchangeable "lens" that sets a {@code machine_fel}'s wavelength, and with
 * it which SILEX recipes can run and how fast. 1:1 port of 1.12.2's {@code ItemFELCrystal}.
 *
 * <p>Five crystals exist, one per usable wavelength; the sixth, {@link EnumWavelengths#NULL}, is the
 * "no crystal / powered down" state and has no item.
 */
public class ItemFELCrystal extends CompatItem {

    /**
     * The electromagnetic spectrum as HBM models it (1.7 {@code EnumWavelengths}), a registry of records so an addon can
     * add a wavelength (NTM: Space's X-ray) with {@link #register}.
     *
     * <p>1.7 compared wavelengths by ordinal: the SILEX gates a recipe on {@code recipe.laserStrength <= mode} and scales
     * its speed by {@code 2^(mode - recipe)}, the FEL draws {@code powerReq * 3^mode} and shows a beam above
     * {@code powerReq * 2^mode}. An addon wavelength has no ordinal between HBM's, so every wavelength carries that
     * number explicitly as {@link #strength}: HBM's six start at 1.7's ordinals (0..5), and an addon
     * picks any value above 0, a fractional one to sit between two of HBM's. The live spectrum follows strength;
     * {@link #legacy} retains HBM's original ordinal identities. Machines save and sync a wavelength by
     * {@link #name()} (1.7's constant name); recipes name HBM's in lower case.
     *
     * <p>{@code VISIBLE} carries colour 0 for both the world beam and the GUI: every call site special-cases
     * it into an animated rainbow (see {@code GUISILEX}/{@code FELRenderer}). The lang files also carry
     * {@code wavelengths.name.micro/radio/xray} keys with no 1.7 constant, vestigial there as well.
     */
    public static final class EnumWavelengths {

        /** What the FEL's beam does to a living entity standing in it (1.7 {@code TileEntityFEL.updateEntity}'s switch). */
        @FunctionalInterface
        public interface BeamEffect {
            void apply(LivingEntity entity);
        }

        public static final NamedRegistry<EnumWavelengths> REGISTRY =
                new NamedRegistry<>("FEL wavelength", EnumWavelengths::name);

        private static final BeamEffect IGNITE = entity -> Effects.ignite(entity, 10);

        // upstream's joke placeholder: the "no crystal / powered down" state
        public static final EnumWavelengths NULL = register("NULL", 0, "la creatura", "6 dollar", 0x010101, 0x010101,
                ChatFormatting.WHITE, entity -> {});
        public static final EnumWavelengths IR = register("IR", 1, "wavelengths.name.ir", "wavelengths.waveRange.ir",
                0xBB1010, 0xCC4040, ChatFormatting.RED, IGNITE);
        // 1.7's switch falls through: VISIBLE blinds AND ignites
        public static final EnumWavelengths VISIBLE = register("VISIBLE", 2, "wavelengths.name.visible",
                "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN, entity -> {
                    Effects.add(entity, MobEffects.BLINDNESS, 60 * 60 * 65536, 0);
                    Effects.ignite(entity, 10);
                });
        public static final EnumWavelengths UV = register("UV", 3, "wavelengths.name.uv", "wavelengths.waveRange.uv",
                0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA, IGNITE);
        public static final EnumWavelengths GAMMA = register("GAMMA", 4, "wavelengths.name.gamma",
                "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE,
                entity -> ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION,
                        ContaminationUtil.ContaminationType.CREATIVE, 25));
        public static final EnumWavelengths DRX = register("DRX", 5, "wavelengths.name.drx", "wavelengths.waveRange.drx",
                0xFF0000, 0xFF0000, ChatFormatting.DARK_RED,
                entity -> ContaminationUtil.applyDigammaData(entity, 0.1F));

        private static final List<EnumWavelengths> LEGACY = REGISTRY.values();
        private static volatile List<EnumWavelengths> spectrum = List.of();
        private static volatile List<EnumWavelengths> spectrumSource;

        private final String key;
        /** 1.7's ordinal, the number the SILEX and the FEL compute with; 0 only for {@link #NULL}. */
        private volatile double strength;
        public final String name;
        public final String wavelengthRange;
        public final int renderedBeamColor;
        public final int guiColor;
        public final ChatFormatting textColor;
        public final BeamEffect beamEffect;

        private EnumWavelengths(String key, double strength, String name, String wavelengthRange, int renderedBeamColor,
                                int guiColor, ChatFormatting textColor, BeamEffect beamEffect) {
            this.key = key;
            this.strength = strength;
            this.name = name;
            this.wavelengthRange = wavelengthRange;
            this.renderedBeamColor = renderedBeamColor;
            this.guiColor = guiColor;
            this.textColor = textColor;
            this.beamEffect = beamEffect;
        }

        /**
         * Registers a wavelength. {@code key} is its saved name (an addon may use its namespace, {@code "addon:microwave"});
         * {@code strength} must be above 0 ({@link #NULL} is the only 0); {@code name}/{@code wavelengthRange} are
         * translation keys; colour 0 means the rainbow. Call at mod construction, on both sides.
         */
        public static EnumWavelengths register(String key, double strength, String name, String wavelengthRange,
                                               int renderedBeamColor, int guiColor, ChatFormatting textColor,
                                               BeamEffect beamEffect) {
            if (!REGISTRY.values().isEmpty() && !(strength > 0D)) {
                throw new IllegalArgumentException("FEL wavelength " + key + " needs a strength above 0, got " + strength);
            }
            return REGISTRY.register(new EnumWavelengths(key, strength, name, wavelengthRange, renderedBeamColor, guiColor,
                    textColor, beamEffect));
        }

        /**
         * Moves a registered wavelength to another strength. HBM never calls it (its six keep 1.7's ordinals); an addon that
         * reproduces a fork's spectrum does, e.g. NTM: Space, whose {@code EnumWavelengths} has {@code XRAY} between
         * {@code UV} and {@code GAMMA}, so its GAMMA is 5 and its DRX 6. {@link #NULL} stays 0 and every other strength
         * must be above 0. Call at mod construction, on both sides, before any machine ticks.
         */
        public static synchronized void setStrength(EnumWavelengths wavelength, double strength) {
            if (wavelength == NULL) throw new IllegalArgumentException("NULL's strength is fixed at 0");
            if (!(strength > 0D)) {
                throw new IllegalArgumentException("FEL wavelength " + wavelength.key + " needs a strength above 0, got " + strength);
            }
            wavelength.strength = strength;
            spectrumSource = null;
        }

        /** 1.7's ordinal for HBM's six unless an addon moved it ({@link #setStrength}); 0 only for {@link #NULL}. */
        public double strength() {
            return strength;
        }

        /** Every wavelength in the live spectrum, from weakest to strongest. */
        public static List<EnumWavelengths> values() {
            if (spectrumSource != REGISTRY.values()) rebuildSpectrum();
            return spectrum;
        }

        private static synchronized void rebuildSpectrum() {
            var source = REGISTRY.values();
            if (spectrumSource == source) return;
            var ordered = new ArrayList<>(source);
            ordered.sort(Comparator.comparingDouble(EnumWavelengths::strength));
            spectrum = List.copyOf(ordered);
            spectrumSource = source;
        }

        /** HBM's original ordinal, independent of addon insertions or strength changes. */
        public static EnumWavelengths legacy(int ordinal) {
            return LEGACY.get(ordinal);
        }

        /** Tolerant lookup for save/sync data, so an unknown name degrades to NULL instead of throwing. */
        public static EnumWavelengths byName(String name) {
            EnumWavelengths wavelength = REGISTRY.get(name);
            return wavelength != null ? wavelength : NULL;
        }

        /** The saved name (1.7's constant name for HBM's). */
        public String name() {
            return key;
        }

        @Override
        public String toString() {
            return key;
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
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        // The digamma crystal replaces its description with scrambled text rather than a lang key.
        tooltip.add(digamma
                ? Component.literal("THERADIANCEOFATHOUSANDSUNS").withStyle(ChatFormatting.OBFUSCATED)
                : Component.translatable(stack.getItem().getDescriptionId() + ".desc"));
        tooltip.add(Component.translatable(wavelength.name)
                .append(" - ")
                .append(Component.translatable(wavelength.wavelengthRange))
                .withStyle(wavelength.textColor));
    }
}
