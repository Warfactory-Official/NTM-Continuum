package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPools;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 1.7 {@code XSatelliteRegistry.idToClass}, in id order 0..11 ({@link #ordinal()} is 1.7's {@code getID()}).
 * Persistence keys on the serialized name rather than the index, so the order carries no save-format meaning.
 * Which item launches which satellite is {@link XSatelliteRegistry}'s job, as it was in 1.7.
 */
public enum SatelliteType implements StringRepresentable {

    MAPPER("mapper", SatelliteMapper::new, SatelliteMapper.CODEC, null),
    SCANNER("scanner", SatelliteScanner::new, SatelliteScanner.CODEC, null),
    RADAR("radar", SatelliteRadar::new, SatelliteRadar.CODEC, null),
    /** 1.7 {@code SatelliteDeathRay}; "laser" is the name saves already carry. */
    DEATH_RAY("laser", SatelliteDeathRay::new, SatelliteDeathRay.CODEC, null),
    RESONATOR("resonator", SatelliteResonator::new, SatelliteResonator.CODEC, null),
    RELAY("relay", SatelliteRelay::new, SatelliteRelay.CODEC, null),
    MINER("miner", SatelliteMiner::new, SatelliteMiner.CODEC, ItemPools.POOL_SAT_MINER),
    LUNAR_MINER("lunar_miner", SatelliteLunarMiner::new, SatelliteLunarMiner.CODEC, ItemPools.POOL_SAT_LUNAR),
    HORIZONS("horizons", SatelliteHorizons::new, SatelliteHorizons.CODEC, null),
    PRECISION_LASER("precision_laser", SatellitePrecisionLaser::new, SatellitePrecisionLaser.CODEC, null),
    DETECTOR("detector", SatelliteDetector::new, SatelliteDetector.CODEC, null),
    RAY_SCAN("ray_scan", SatelliteRayScan::new, SatelliteRayScan.CODEC, null);

    public static final Codec<SatelliteType> CODEC = StringRepresentable.fromEnum(SatelliteType::values);
    /**
     * A satellite and its state, 1.7's {@code getID()} plus NBT blob in one. Declared HERE rather than on
     * {@link Satellite}: a static on the base class would be read while this enum's own constants are still
     * being built, and would see null.
     */
    public static final Codec<Satellite> DISPATCH_CODEC =
            dev.golem.core.data.Codecs.dispatch(CODEC, "type", Satellite::type, SatelliteType::codec);

    private final String name;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;

    SatelliteType(String name, Supplier<? extends Satellite> factory, MapCodec<? extends Satellite> codec,
                  @Nullable String cargo) {
        this.name = name;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    /** 1.7 {@code XSatelliteRegistry.createFromId}. */
    public static @Nullable SatelliteType fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
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
    public String getSerializedName() {
        return name;
    }
}
