package com.hbm.api.data;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import dev.golem.core.hook.Hook;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * The fluid deposits the oil drills (oil well, pumpjack, fracking tower) draw from, as data: {@code hbm:fluid_deposit},
 * keyed by the deposit block ({@code data/<ns>/value_maps/block/fluid_deposit.json}).
 *
 * <p>1.7 hard-wired two blocks into {@code TileEntityOilDrillBase} and its three drills: {@code ore_oil} (oil plus gas,
 * depleting into {@code ore_oil_empty}, which the drills search through) and {@code ore_bedrock_oil} (fracking tower only,
 * a tenth of the yield, never depletes). Each drill keeps its own 1.7 amounts (oil per deposit, gas range, drain chance);
 * a deposit names the two fluids it gives and scales those amounts, so HBM's two entries ({@code yield} 1 and 0.1) give
 * exactly 1.7's numbers. NTM: Space's per-planet oil, gas and brine reservoirs are entries of their own.
 */
public final class DepositDataMaps {

    private DepositDataMaps() {}

    /**
     * An absolute reservoir bound to one physical state. Metadata still comes from the merged deposit maps;
     * its amounts may come from an independently loaded subsystem configuration instead of proportional yields.
     */
    public interface Reserve {
        Deposit deposit();
        int primaryAmount();
        int secondaryAmount(RandomSource random);
        double drainChance();

        default FluidType primaryFluid() { return deposit().primaryFluid(); }
        default @Nullable FluidType secondaryFluid() {
            var fluid = deposit().secondaryFluid();
            return fluid == Fluids.NONE ? null : fluid;
        }
        default boolean requiresFracking() { return deposit().frackingOnly(); }

        /** An emptied state retains the live deposit's shared properties, including an addon's BODY. */
        default void drain(Level level, BlockPos pos, RandomSource random, double multiplier) {
            var empty = deposit().emptyBlock();
            if (empty != null && random.nextDouble() < drainChance() * multiplier)
                level.setBlock(pos, emptyState(level.getBlockState(pos), empty), Block.UPDATE_ALL);
        }
    }

    @FunctionalInterface
    public interface ReserveProvider {
        /** Null leaves this state on the existing multiplicative deposit path. */
        @Nullable Reserve reserve(BlockState state, Deposit metadata);
    }

    public static final Hook<ReserveProvider> RESERVES = Hook.notifying();

    public static @Nullable Reserve reserve(BlockState state) {
        var metadata = deposit(state);
        return metadata == null ? null : reserve(state, metadata);
    }

    /** Uses the caller's resolved metadata too, so an explicit caller override is not replaced by another lookup. */
    public static @Nullable Reserve reserve(BlockState state, Deposit metadata) {
        for (var provider : RESERVES) {
            var reserve = provider.reserve(state, metadata);
            if (reserve != null) return reserve;
        }
        return null;
    }

    /**
     * One deposit block. {@code primary} fills the drill's first tank and {@code secondary} its second
     * ({@code "minecraft:empty"} for none); {@code empty} is the block a drained deposit becomes (absent: it never
     * drains, as bedrock oil); {@code yield} scales the drill's primary amount and {@code secondary_yield} (default
     * {@code yield}) its secondary range; {@code drain} scales its drain chance; a {@code fracking_only} deposit is
     * reached only by the fracking tower.
     */
    public record Deposit(String primary, String secondary, Optional<String> empty, double yield,
                          Optional<Double> secondaryYield, double drain, boolean frackingOnly) {
        public static final Codec<Deposit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("primary").forGetter(Deposit::primary),
                Codec.STRING.optionalFieldOf("secondary", "minecraft:empty").forGetter(Deposit::secondary),
                Codec.STRING.optionalFieldOf("empty").forGetter(Deposit::empty),
                Codec.DOUBLE.optionalFieldOf("yield", 1D).forGetter(Deposit::yield),
                Codec.DOUBLE.optionalFieldOf("secondary_yield").forGetter(Deposit::secondaryYield),
                Codec.DOUBLE.optionalFieldOf("drain", 1D).forGetter(Deposit::drain),
                Codec.BOOL.optionalFieldOf("fracking_only", false).forGetter(Deposit::frackingOnly)
        ).apply(i, Deposit::new));

        public FluidType primaryFluid() {
            return Fluids.byRegistryName(primary);
        }

        public FluidType secondaryFluid() {
            return Fluids.byRegistryName(secondary);
        }

        /** The block a drained deposit becomes, or null when it never drains. */
        public @Nullable Block emptyBlock() {
            return empty.map(WorldDataMaps::blockOf).filter(block -> block != Blocks.AIR).orElse(null);
        }

        /** A drill's amount scaled by {@code yield}: HBM's yields of 1 and 0.1 give 1.7's integers exactly. */
        public int primaryAmount(int base) {
            return (int) Math.round(base * yield);
        }

        public int secondaryAmount(int base) {
            return (int) Math.round(base * secondaryYield.orElse(yield));
        }
    }

    public static final DataMap<Block, Deposit> FLUID_DEPOSIT =
            DataMaps.map(DepositDataMaps.class, Constants.MOD_ID, "fluid_deposit", "block", () -> BuiltInRegistries.BLOCK,
                    Deposit.CODEC);

    /** The deposit {@code block} is, or null. */
    public static @Nullable Deposit deposit(Block block) {
        return FLUID_DEPOSIT.get(block);
    }

    /**
     * Deposits that differ by one block-state property of a single block: {@code hbm:fluid_deposit_states}
     * ({@code data/<ns>/value_maps/block/fluid_deposit_states.json}), {@code {"property": "body", "values": {"duna":
     * {deposit}, ...}}}. A block with such an entry is looked up by its state first ({@link #deposit(BlockState)}); a
     * value without an entry falls back to {@link #FLUID_DEPOSIT}. HBM has none; NTM: Space's per-body oil, gas and brine
     * ores (one block with a {@code body} state, 1.7 metadata) are its example.
     */
    public record StateDeposits(String property, Map<String, Deposit> values) {
        public static final Codec<StateDeposits> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("property").forGetter(StateDeposits::property),
                Codec.unboundedMap(Codec.STRING, Deposit.CODEC).fieldOf("values").forGetter(StateDeposits::values)
        ).apply(i, StateDeposits::new));

        /** The deposit of {@code state}'s value of {@link #property}, or null. */
        public @Nullable Deposit of(BlockState state) {
            Property<?> prop = state.getBlock().getStateDefinition().getProperty(property);
            return prop == null ? null : values.get(valueName(state, prop));
        }
    }

    public static final DataMap<Block, StateDeposits> FLUID_DEPOSIT_STATES =
            DataMaps.map(DepositDataMaps.class, Constants.MOD_ID, "fluid_deposit_states", "block", () -> BuiltInRegistries.BLOCK,
                    StateDeposits.CODEC);

    /** The deposit {@code state} is: its {@link #FLUID_DEPOSIT_STATES} entry, else its block's {@link #FLUID_DEPOSIT}. */
    public static @Nullable Deposit deposit(BlockState state) {
        StateDeposits byState = FLUID_DEPOSIT_STATES.get(state.getBlock());
        if (byState != null) {
            Deposit deposit = byState.of(state);
            if (deposit != null) return deposit;
        }
        return deposit(state.getBlock());
    }

    /** Whether {@code block} is a drained deposit (the drills search through such blocks for a live one). */
    public static boolean isEmptyDeposit(Block block) {
        for (Map.Entry<String, Deposit> entry : FLUID_DEPOSIT.byId().entrySet()) {
            if (entry.getValue().emptyBlock() == block) return true;
        }
        for (Map.Entry<String, Deposit> entry : FLUID_DEPOSIT.byTag()) {
            if (entry.getValue().emptyBlock() == block) return true;
        }
        for (Map.Entry<String, StateDeposits> entry : FLUID_DEPOSIT_STATES.byId().entrySet()) {
            for (Deposit deposit : entry.getValue().values().values()) if (deposit.emptyBlock() == block) return true;
        }
        for (Map.Entry<String, StateDeposits> entry : FLUID_DEPOSIT_STATES.byTag()) {
            for (Deposit deposit : entry.getValue().values().values()) if (deposit.emptyBlock() == block) return true;
        }
        return false;
    }

    /** Whether {@code block} has a deposit in any of its states (a plain entry or a state entry). */
    public static boolean hasDeposit(Block block) {
        return deposit(block) != null || FLUID_DEPOSIT_STATES.get(block) != null;
    }

    /**
     * The state a drained {@code full} becomes: {@code empty}'s default state carrying every property value
     * {@code full} shares with it by name (1.7 kept the metadata: {@code setBlock(x, y, z, empty, meta, 3)}).
     */
    public static BlockState emptyState(BlockState full, Block empty) {
        BlockState out = empty.defaultBlockState();
        for (Property<?> prop : full.getProperties()) {
            Property<?> target = empty.getStateDefinition().getProperty(prop.getName());
            if (target != null) out = copy(out, target, valueName(full, prop));
        }
        return out;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> prop) {
        return prop.getName(state.getValue(prop));
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(state);
    }

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
