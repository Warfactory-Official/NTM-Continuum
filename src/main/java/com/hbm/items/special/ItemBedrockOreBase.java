package com.hbm.items.special;

import com.hbm.Constants;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.List;

/**
 * Raw bedrock ore produced by the excavator. Each stack snapshots all six coordinate-noise channels;
 * the deposit itself is infinite, but moving the stack does not change its composition.
 */
public class ItemBedrockOreBase extends CompatItem {

    public static final GolemComponent<BedrockOreDensity> DENSITY =
            GolemComponent.of(Constants.MOD_ID, "bedrock_ore_density", BedrockOreDensity.CODEC);

    private static final LegacyPerlin LEVEL = new LegacyPerlin(2114043L);
    private static final LegacyPerlin[] ORES = createOreNoises();

    public ItemBedrockOreBase(Properties properties) {
        super(properties);
    }

    public static double getOreAmount(ItemStack stack, BedrockOreType type) {
        return DENSITY.getOr(stack, BedrockOreDensity.ZERO).get(type);
    }

    public static void setOreAmount(ItemStack stack, int x, int z) {
        setOreAmount(stack, x, z, 1D);
    }

    public static void setOreAmount(ItemStack stack, int x, int z, double multiplier) {
        DENSITY.set(stack, new BedrockOreDensity(
                getOreLevel(x, z, BedrockOreType.LIGHT_METAL) * multiplier,
                getOreLevel(x, z, BedrockOreType.HEAVY_METAL) * multiplier,
                getOreLevel(x, z, BedrockOreType.RARE_EARTH) * multiplier,
                getOreLevel(x, z, BedrockOreType.ACTINIDE) * multiplier,
                getOreLevel(x, z, BedrockOreType.NON_METAL) * multiplier,
                getOreLevel(x, z, BedrockOreType.CRYSTALLINE) * multiplier));
    }

    /** Exact old fixed-seed, four-octave product field, including abs, 0.05 scale and [0,2] clamp. */
    public static double getOreLevel(int x, int z, BedrockOreType type) {
        double scale = 0.01D;
        return Mth.clamp(Math.abs(LEVEL.value(x * scale, z * scale)
                * ORES[type.ordinal()].value(x * scale, z * scale)) * 0.05D, 0D, 2D);
    }

    private static LegacyPerlin[] createOreNoises() {
        LegacyPerlin[] noises = new LegacyPerlin[BedrockOreType.VALUES.length];
        for (int i = 0; i < noises.length; i++) noises[i] = new LegacyPerlin(2082127L + i);
        return noises;
    }

    public static double averageOreLevel(int x, int z) {
        double total = 0D;
        for (BedrockOreType type : BedrockOreType.VALUES) total += getOreLevel(x, z, type);
        return total / BedrockOreType.VALUES.length;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.hbm.bedrock_ore_base", "Raw Bedrock Ore");
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            double amount = getOreAmount(stack, type);
            double truncated = ((int) (amount * 100D)) / 100D;
            tooltip.add(Component.translatableWithFallback(
                            "item.bedrock_ore.type." + type.suffix + ".name", type.displayName)
                    .append(": " + truncated + " (")
                    .append(Component.translatableWithFallback(densityKey(amount), densityFallback(amount))
                            .withStyle(densityColor(amount)))
                    .append(")"));
        }
    }

    private static String densityKey(double density) {
        if (density <= 0.1D) return "item.ore_density_scanner.verypoor";
        if (density <= 0.35D) return "item.ore_density_scanner.poor";
        if (density <= 0.75D) return "item.ore_density_scanner.low";
        if (density >= 1.9D) return "item.ore_density_scanner.excellent";
        if (density >= 1.65D) return "item.ore_density_scanner.veryhigh";
        if (density >= 1.25D) return "item.ore_density_scanner.high";
        return "item.ore_density_scanner.moderate";
    }

    private static String densityFallback(double density) {
        if (density <= 0.1D) return "Very Poor";
        if (density <= 0.35D) return "Poor";
        if (density <= 0.75D) return "Low";
        if (density >= 1.9D) return "Excellent";
        if (density >= 1.65D) return "Very High";
        if (density >= 1.25D) return "High";
        return "Moderate";
    }

    private static ChatFormatting densityColor(double density) {
        if (density <= 0.1D) return ChatFormatting.DARK_RED;
        if (density <= 0.35D) return ChatFormatting.RED;
        if (density <= 0.75D) return ChatFormatting.GOLD;
        if (density >= 1.9D) return ChatFormatting.AQUA;
        if (density >= 1.65D) return ChatFormatting.BLUE;
        if (density >= 1.25D) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }

    /** The old NoiseGeneratorPerlin's four-octave loop over legacy simplex noise. */
    private static final class LegacyPerlin {
        private final com.hbm.world.feature.LegacyPerlinNoise noise;

        private LegacyPerlin(long seed) {
            noise = new com.hbm.world.feature.LegacyPerlinNoise(new LegacyRandomSource(seed), 4);
        }

        private double value(double x, double z) {
            return noise.getValue(x, z);
        }
    }

    /** Forces the component declaration to exist before registry flushing starts. */
    public static void init() {}
}
