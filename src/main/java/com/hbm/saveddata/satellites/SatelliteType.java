package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPools;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
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

    MAPPER("mapper", EnumSatType.SPY, SatelliteMapper::new, SatelliteMapper.CODEC, null),
    SCANNER("scanner", EnumSatType.SCANNER, SatelliteScanner::new, SatelliteScanner.CODEC, null),
    RADAR("radar", EnumSatType.RADAR, SatelliteRadar::new, SatelliteRadar.CODEC, null),
    LASER("laser", EnumSatType.DEATH_RAY, SatelliteLaser::new, SatelliteLaser.CODEC, null),
    RESONATOR("resonator", EnumSatType.XENIUM_RESONATOR, SatelliteResonator::new, SatelliteResonator.CODEC, null),
    RELAY("relay", EnumSatType.RELAY, SatelliteRelay::new, SatelliteRelay.CODEC, null),
    MINER("miner", EnumSatType.MINER_ASTRO, SatelliteMiner::new, SatelliteMiner.CODEC, ItemPools.POOL_SAT_MINER),
    LUNAR_MINER("lunar_miner", EnumSatType.MINER_LUNAR, SatelliteLunarMiner::new, SatelliteLunarMiner.CODEC,
            ItemPools.POOL_SAT_LUNAR),
    HORIZONS("horizons", null, SatelliteHorizons::new, SatelliteHorizons.CODEC, null),
    DETECTOR("detector", EnumSatType.DETECTOR, SatelliteDetector::new, SatelliteDetector.CODEC, null),
    RAY_SCAN("ray_scan", EnumSatType.RAY_SCAN, SatelliteRayScan::new, SatelliteRayScan.CODEC, null);

    public static final Codec<SatelliteType> CODEC = StringRepresentable.fromEnum(SatelliteType::values);
    /**
     * A satellite and its state, 1.7's {@code getID()} plus NBT blob in one. Declared HERE rather than on
     * {@link Satellite}: a static on the base class would be read while this enum's own constants are still
     * being built, and would see null.
     */
    public static final Codec<Satellite> DISPATCH_CODEC =
            dev.golem.core.data.Codecs.dispatch(CODEC, "type", Satellite::type, SatelliteType::codec);

    private final String name;
    private final @Nullable EnumSatType chip;
    private final Supplier<? extends Satellite> factory;
    private final MapCodec<? extends Satellite> codec;
    private final @Nullable String cargo;

    SatelliteType(String name, @Nullable EnumSatType chip, Supplier<? extends Satellite> factory,
                  MapCodec<? extends Satellite> codec, @Nullable String cargo) {
        this.name = name;
        this.chip = chip;
        this.factory = factory;
        this.codec = codec;
        this.cargo = cargo;
    }

    /** The satellite a chip launches, or null where the stack is not a satellite chip. */
    public static @Nullable SatelliteType fromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSatellite satellite)) return null;
        return fromChip(satellite.typeOf(stack));
    }

    public static @Nullable SatelliteType fromChip(@Nullable EnumSatType chip) {
        if (chip == null) return null;
        for (SatelliteType type : values()) {
            if (type.chip == chip) return type;
        }
        return null;
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

    /**
     * The chip that launches this satellite, as a stack. One item carries every satellite here — the chip
     * is a component on it — so a type with no chip (the Horizons probe, which upstream has no chip for)
     * answers empty rather than naming an item that was never registered.
     */
    public ItemStack stack() {
        return chip == null ? ItemStack.EMPTY : ItemSatellite.make(chip);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
