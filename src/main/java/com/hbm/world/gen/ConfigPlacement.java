package com.hbm.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@code hbm:config}: a placement step that reads an {@code hbm.cfg} value when the chunk generates, so the 1.7 worldgen
 * keys ({@code 2.00_uraniumSpawnrate}, {@code 2.D00_overworldOres}, {@code 4.14_landmineSpawn}, ...) take effect without
 * rebuilding the data pack. The placed-feature files are written at build time (Dejson); this step is where they defer
 * to the config. History §19ej-8.
 *
 * <ul>
 *   <li>{@code count}: the position repeated {@code value * multiplier} times (1.7 {@code for (k < veinCount)}); a
 *       value of 0 or less places nothing.</li>
 *   <li>{@code chance}: kept with probability {@code 1/value} (1.7 {@code rand.nextInt(value) == 0}, drawn as vanilla's
 *       {@code rarity_filter} draws it); 0 or less turns the feature off, as 1.7's {@code > 0} guards did.</li>
 *   <li>{@code switch}: kept while the value is non-zero (a boolean key reads 1/0).</li>
 * </ul>
 *
 * <p>The values come from {@link WorldgenKeys}; an unknown key keeps nothing and logs once, so a typo cannot silently
 * generate at a default.
 */
public final class ConfigPlacement extends PlacementModifier {

    public enum Mode {
        COUNT, CHANCE, SWITCH;

        static Mode parse(String s) {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT));
        }
    }

    private static final java.util.Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /** The current value of {@code key} ({@link WorldgenKeys}); an unknown key reads 0 (nothing placed) and logs once. */
    public static int value(String key) {
        IntSupplier supplier = WorldgenKeys.get(key);
        if (supplier == null) {
            if (WARNED.add(key)) com.hbm.Constants.LOG.warn("hbm:config placement names unknown key '{}'; nothing placed", key);
            return 0;
        }
        return supplier.getAsInt();
    }

    public static final MapCodec<ConfigPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(p -> p.key),
            Codec.STRING.xmap(Mode::parse, m -> m.name().toLowerCase(java.util.Locale.ROOT)).fieldOf("mode").forGetter(p -> p.mode),
            Codec.INT.optionalFieldOf("multiplier", 1).forGetter(p -> p.multiplier)
    ).apply(i, ConfigPlacement::new));

    /** Set by {@code ModFeatures} once the type is registered. */
    public static java.util.function.Supplier<PlacementModifierType<?>> TYPE;

    private final String key;
    private final Mode mode;
    private final int multiplier;

    public ConfigPlacement(String key, Mode mode, int multiplier) {
        this.key = key;
        this.mode = mode;
        this.multiplier = multiplier;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        int v = value(key);
        switch (mode) {
            case COUNT -> {
                int n = v * multiplier;
                return n <= 0 ? Stream.empty() : IntStream.range(0, n).mapToObj(k -> pos);
            }
            case CHANCE -> {
                // vanilla rarity_filter's own draw, so a world generates exactly as with the static file it replaces
                return v > 0 && random.nextFloat() < 1.0F / (float) v ? Stream.of(pos) : Stream.empty();
            }
            default -> {
                return v != 0 ? Stream.of(pos) : Stream.empty();
            }
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE.get();
    }
}
