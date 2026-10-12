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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raw bedrock ore produced by the excavator. Each stack snapshots all six coordinate-noise channels;
 * the deposit itself is infinite, but moving the stack does not change its composition.
 */
public class ItemBedrockOreBase extends CompatItem {

    public static final GolemComponent<BedrockOreDensity> DENSITY =
            GolemComponent.of(Constants.MOD_ID, "bedrock_ore_density", BedrockOreDensity.CODEC);

    /**
     * The composition of a stack mined where {@link #SOURCE} answered: where it was mined (a translation key, shown as
     * "Mined on: ..."; empty for none) and the amount of each type, by type name. Such a stack carries no
     * {@link #DENSITY}.
     */
    public record Composition(String origin, List<Amount> amounts) {
        public static final com.mojang.serialization.Codec<Composition> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                        com.mojang.serialization.Codec.STRING.optionalFieldOf("origin", "").forGetter(Composition::origin),
                        Amount.CODEC.listOf().fieldOf("amounts").forGetter(Composition::amounts)
                ).apply(i, Composition::new));
    }

    /** One type's amount, the type saved by its registry name. */
    public record Amount(String type, double amount) {
        public static final com.mojang.serialization.Codec<Amount> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                        com.mojang.serialization.Codec.STRING.fieldOf("type").forGetter(Amount::type),
                        com.mojang.serialization.Codec.DOUBLE.fieldOf("amount").forGetter(Amount::amount)
                ).apply(i, Amount::new));
    }

    public static final GolemComponent<Composition> COMPOSITION =
            GolemComponent.of(Constants.MOD_ID, "bedrock_ore_composition", Composition.CODEC);

    /**
     * What lies under a column: the density of each type there, in order, where it is (a translation key, or null),
     * and the bore fluid the deposit needs regardless of density (or null for HBM's tier rule).
     */
    public record OreField(@org.jspecify.annotations.Nullable String origin, Map<BedrockOreType, Double> levels,
                           com.hbm.inventory.fluid.@org.jspecify.annotations.Nullable FluidStack boreFluid) {
        public OreField {
            levels = Collections.unmodifiableMap(new LinkedHashMap<>(levels));
        }

        /** The mean density (1.7 {@code totalLevel / types}): the deposit tier and the scanner's summary read it. */
        public double average() {
            double total = 0D;
            for (double level : levels.values()) total += level;
            return levels.isEmpty() ? 0D : total / levels.size();
        }
    }

    /** A listener's raw bedrock ore field of a column, or null to pass. */
    @FunctionalInterface
    public interface Source {
        @org.jspecify.annotations.Nullable OreField field(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                                         long seed, int x, int z);
    }

    /**
     * hbm-space's per-body bedrock ore ({@code CelestialBedrockOre}): which types lie under a column and how dense they
     * are. The first non-null answer decides; nothing answering is HBM's fixed-seed six-type noise
     * ({@link #getOreLevel(int, int, BedrockOreType)}). Read by the excavator's raw ore, the ore density scanner and
     * the deposit tier. An answered stack saves {@link #COMPOSITION} instead of {@link #DENSITY}.
     */
    public static final dev.golem.core.hook.Hook<Source> SOURCE = dev.golem.core.hook.Hook.of(listeners -> (dimension, seed, x, z) -> {
        for (Source listener : listeners) {
            OreField field = listener.field(dimension, seed, x, z);
            if (field != null) return field;
        }
        return null;
    });

    /** The raw ore field of a column: {@link #SOURCE}'s answer, or HBM's six types from the fixed-seed noise. */
    public static OreField field(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long seed,
                                 int x, int z) {
        OreField answered = SOURCE.invoker().field(dimension, seed, x, z);
        if (answered != null) return answered;
        Map<BedrockOreType, Double> levels = new LinkedHashMap<>();
        for (BedrockOreType type : BedrockOreType.VALUES) levels.put(type, getOreLevel(x, z, type));
        return new OreField(null, levels, null);
    }

    /** {@link #field(net.minecraft.resources.ResourceKey, long, int, int)} of a level (seed 0 where it has none). */
    public static OreField field(net.minecraft.world.level.LevelReader level, int x, int z) {
        long seed = level instanceof net.minecraft.world.level.WorldGenLevel world ? world.getSeed() : 0L;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension =
                level instanceof net.minecraft.world.level.WorldGenLevel world ? world.getLevel().dimension()
                        : level instanceof net.minecraft.world.level.Level plain ? plain.dimension()
                        : net.minecraft.world.level.Level.OVERWORLD;
        return field(dimension, seed, x, z);
    }

    private static final LegacyPerlin LEVEL = new LegacyPerlin(2114043L);
    private static final LegacyPerlin[] ORES = createOreNoises();

    public ItemBedrockOreBase(Properties properties) {
        super(properties);
    }

    public static double getOreAmount(ItemStack stack, BedrockOreType type) {
        Composition composition = COMPOSITION.get(stack).orElse(null);
        if (composition != null) {
            for (Amount amount : composition.amounts()) if (amount.type().equals(type.name())) return amount.amount();
            return 0D;
        }
        return DENSITY.getOr(stack, BedrockOreDensity.ZERO).get(type);
    }

    /** The types a raw ore stack carries amounts of, in order: its composition's, or HBM's six. */
    public static List<BedrockOreType> types(ItemStack stack) {
        Composition composition = COMPOSITION.get(stack).orElse(null);
        if (composition == null) return List.of(BedrockOreType.VALUES);
        List<BedrockOreType> types = new ArrayList<>();
        for (Amount amount : composition.amounts()) {
            BedrockOreType type = BedrockOreType.byName(amount.type());
            if (type != null) types.add(type);
        }
        return types;
    }

    /**
     * hbm-space {@code setOreAmount(world, stack, x, z, mult)}: the column's field ({@link #SOURCE}) times
     * {@code multiplier}; where nothing answers, exactly {@link #setOreAmount(ItemStack, int, int, double)}.
     */
    public static void setOreAmount(net.minecraft.world.level.Level level, ItemStack stack, int x, int z, double multiplier) {
        long seed = level instanceof net.minecraft.world.level.WorldGenLevel world ? world.getSeed() : 0L;
        OreField field = SOURCE.invoker().field(level.dimension(), seed, x, z);
        if (field == null) {
            setOreAmount(stack, x, z, multiplier);
            return;
        }
        List<Amount> amounts = new ArrayList<>();
        for (Map.Entry<BedrockOreType, Double> entry : field.levels().entrySet()) {
            amounts.add(new Amount(entry.getKey().name(), entry.getValue() * multiplier));
        }
        DENSITY.remove(stack);
        COMPOSITION.set(stack, new Composition(field.origin() == null ? "" : field.origin(), amounts));
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
        Composition composition = COMPOSITION.get(stack).orElse(null);
        if (composition != null && !composition.origin().isEmpty()) {
            // hbm-space: "Mined on: " + the body
            tooltip.add(Component.literal("Mined on: ").append(Component.translatable(composition.origin())));
        }
        for (BedrockOreType type : types(stack)) {
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
