package com.hbm.saveddata.satellites;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.itempool.ItemPools;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.golem.core.data.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A kind of satellite: 1.7 {@code XSatelliteRegistry.idToClass}, as a registry of records keyed by the name saves
 * carry ({@code "mapper"}, {@code "laser"}, ...). HBM registers 1.7's twelve in 1.7 id order; an addon registers its
 * own under its namespace ({@code SatelliteType.register("ntmspace:dyson_relay", ...)}) at mod construction, and
 * {@code SatelliteSavedData} saves and loads it by that name. Which item launches which satellite is
 * {@link XSatelliteRegistry}'s job (the {@code hbm:satellite} item data map), as it was in 1.7.
 *
 * <p>1.7's numeric {@code getID()}/{@code createFromId(int)} are gone: nothing persisted or sent them (the saved form
 * has always been the name), and an index would collide across addons.
 */
public final class SatelliteType {

    /** Every satellite type, HBM's 1.7 twelve first. */
    public static final NamedRegistry<SatelliteType> REGISTRY = new NamedRegistry<>("satellite type", SatelliteType::name);

    public static final SatelliteType MAPPER = register("mapper", SatelliteMapper::new, SatelliteMapper.CODEC, null);
    public static final SatelliteType SCANNER = register("scanner", SatelliteScanner::new, SatelliteScanner.CODEC, null);
    public static final SatelliteType RADAR = register("radar", SatelliteRadar::new, SatelliteRadar.CODEC, null);
    /** 1.7 {@code SatelliteDeathRay}; "laser" is the name saves already carry. */
    public static final SatelliteType DEATH_RAY = register("laser", SatelliteDeathRay::new, SatelliteDeathRay.CODEC, null);
    public static final SatelliteType RESONATOR = register("resonator", SatelliteResonator::new, SatelliteResonator.CODEC, null);
    public static final SatelliteType RELAY = register("relay", SatelliteRelay::new, SatelliteRelay.CODEC, null);
    public static final SatelliteType MINER = register("miner", SatelliteMiner::new, SatelliteMiner.CODEC, ItemPools.POOL_SAT_MINER);
    public static final SatelliteType LUNAR_MINER = register("lunar_miner", SatelliteLunarMiner::new, SatelliteLunarMiner.CODEC, ItemPools.POOL_SAT_LUNAR);
    public static final SatelliteType HORIZONS = register("horizons", SatelliteHorizons::new, SatelliteHorizons.CODEC, null);
    public static final SatelliteType PRECISION_LASER = register("precision_laser", SatellitePrecisionLaser::new, SatellitePrecisionLaser.CODEC, null);
    public static final SatelliteType DETECTOR = register("detector", SatelliteDetector::new, SatelliteDetector.CODEC, null);
    public static final SatelliteType RAY_SCAN = register("ray_scan", SatelliteRayScan::new, SatelliteRayScan.CODEC, null);

    /** A satellite type by its saved name; an unknown name fails to decode. */
    public static final Codec<SatelliteType> CODEC = REGISTRY.codec();
    /**
     * A satellite and its state, 1.7's {@code getID()} plus NBT blob in one. Declared HERE rather than on
     * {@link Satellite}: a static on the base class would be read while this class's own constants are still
     * being built, and would see null.
     */
    public static final Codec<Satellite> DISPATCH_CODEC =
            Codecs.dispatch(CODEC, "type", Satellite::type, SatelliteType::codec);

    private final String name;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;

    private SatelliteType(String name, Supplier<? extends Satellite> factory, MapCodec<? extends Satellite> codec,
                          @Nullable String cargo) {
        this.name = name;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    /**
     * Registers a satellite type. {@code name} is its saved name (an addon uses {@code "<namespace>:<path>"}),
     * {@code codec} its saved state, {@code cargo} the {@code ItemPool} it delivers, or null.
     */
    public static SatelliteType register(String name, Supplier<? extends Satellite> factory,
                                         MapCodec<? extends Satellite> codec, @Nullable String cargo) {
        return REGISTRY.register(new SatelliteType(name, factory, codec, cargo));
    }

    /** Every satellite type in registration order (1.7 id order first). */
    public static List<SatelliteType> values() {
        return REGISTRY.values();
    }

    /** The type saved as {@code name}, or null. */
    public static @Nullable SatelliteType byName(String name) {
        return REGISTRY.get(name);
    }

    public String name() {
        return name;
    }

    public Satellite create() {
        return factory.get();
    }

    public MapCodec<? extends Satellite> codec() {
        return codec;
    }

    /**
     * The {@code ItemPool} name this satellite delivers, or null where it mines nothing (1.7
     * {@code SatelliteMiner.CARGO}).
     */
    public @Nullable String cargo() {
        return cargo;
    }

    @Override
    public String toString() {
        return name;
    }
}
