package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPools;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 1.7 {@code Satellite.satellites}, in {@code Satellite.register()} order. 1.7 keyed persistence and the wire
 * on this list's INDEX; here both key on the serialized name, so the order carries no meaning beyond matching
 * the legacy's player-visible ordering.
 *
 * <p>Item pairing is 1.7's own and is not name-symmetric: {@link #RELAY} launches from {@code sat_foeq}, while
 * {@code sat_relay} and {@code sat_chip} pair with no satellite at all.
 */
public enum SatelliteType implements StringRepresentable {

    MAPPER("mapper", "sat_mapper", SatelliteMapper::new, SatelliteMapper.CODEC, null),
    SCANNER("scanner", "sat_scanner", SatelliteScanner::new, SatelliteScanner.CODEC, null),
    RADAR("radar", "sat_radar", SatelliteRadar::new, SatelliteRadar.CODEC, null),
    LASER("laser", "sat_laser", SatelliteLaser::new, SatelliteLaser.CODEC, null),
    RESONATOR("resonator", "sat_resonator", SatelliteResonator::new, SatelliteResonator.CODEC, null),
    RELAY("relay", "sat_foeq", SatelliteRelay::new, SatelliteRelay.CODEC, null),
    MINER("miner", "sat_miner", SatelliteMiner::new, SatelliteMiner.CODEC, ItemPools.POOL_SAT_MINER),
    LUNAR_MINER("lunar_miner", "sat_lunar_miner", SatelliteLunarMiner::new, SatelliteLunarMiner.CODEC,
            ItemPools.POOL_SAT_LUNAR),
    HORIZONS("horizons", "sat_gerald", SatelliteHorizons::new, SatelliteHorizons.CODEC, null),
    DETECTOR("detector", "sat_detector", SatelliteDetector::new, SatelliteDetector.CODEC, null),
    RAY_SCAN("ray_scan", "sat_ray_scan", SatelliteRayScan::new, SatelliteRayScan.CODEC, null);

    public static final Codec<SatelliteType> CODEC = StringRepresentable.fromEnum(SatelliteType::values);
    /**
     * A satellite and its state, 1.7's {@code getID()} plus NBT blob in one. Declared HERE rather than on
     * {@link Satellite}: a static on the base class would be read while this enum's own constants are still
     * being built, and would see null.
     */
    public static final Codec<Satellite> DISPATCH_CODEC =
            dev.golem.core.data.Codecs.dispatch(CODEC, "type", Satellite::type, SatelliteType::codec);

    private final String name;
    private final String itemPath;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;
    private @Nullable Item resolved;

    SatelliteType(String name, String itemPath, Supplier<? extends Satellite> factory,
                  MapCodec<? extends Satellite> codec, @Nullable String cargo) {
        this.name = name;
        this.itemPath = itemPath;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    public static @Nullable SatelliteType fromItem(Item item) {
        if (item == Items.AIR) return null;
        for (SatelliteType type : values()) {
            if (type.item() == item) return type;
        }
        return null;
    }

    public static @Nullable SatelliteType fromStack(ItemStack stack) {
        return stack.isEmpty() ? null : fromItem(stack.getItem());
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

    public Item item() {
        Item cached = resolved;
        if (cached == null) {
            cached = BuiltInRegistries.ITEM.getValue(Library.id(itemPath));
            if (cached == Items.AIR) throw new IllegalStateException(itemPath + " is not registered");
            resolved = cached;
        }
        return cached;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
