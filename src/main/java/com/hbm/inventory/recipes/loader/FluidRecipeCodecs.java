package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.golem.core.hook.Hook;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared pieces of the fluid-processing tables (cracking, reforming, hydrotreating, refinery, coker, solidifier, ...):
 * every one of them is "an input fluid to a few fluid stacks and maybe an item", one row per input fluid. The row
 * codecs are built from {@link GenericRecipeCodec#FLUID}, {@link GenericRecipeCodec#FLUID_STACK} (which already carries
 * {@code pressure}) and {@link GenericRecipeCodec#ITEM_OUT}; this class adds the optional second output, fixed-length
 * output lists, the "solid fuel" rule shared by three tables, and {@link ByFluid}, a {@link DataRecipeTable} indexed by
 * its input fluid.
 */
public final class FluidRecipeCodecs {

    private FluidRecipeCodecs() {}

    /** 1.7's "no output" ({@code new FluidStack(Fluids.NONE, 0)}), e.g. the cracker's second output for kerosene. */
    public static FluidStack none() {
        return new FluidStack(Fluids.NONE, 0);
    }

    /** An output that may be missing: an absent field reads as NONE 0, and NONE is written as an absent field. */
    public static MapCodec<FluidStack> optionalStack(String field) {
        return GenericRecipeCodec.FLUID_STACK.optionalFieldOf(field).xmap(
                stack -> stack.orElseGet(FluidRecipeCodecs::none),
                stack -> stack == null || stack.type == Fluids.NONE ? Optional.empty() : Optional.of(stack));
    }

    /**
     * {@code codec} with a check on every decoded and encoded value ({@code MapCodec.validate} does not exist on the
     * 1.20.1 DataFixerUpper).
     */
    public static <R> MapCodec<R> checked(MapCodec<R> codec, Function<R, DataResult<R>> check) {
        return codec.flatXmap(check, check);
    }

    /** Exactly {@code size} fluid stacks, e.g. the refinery's four fractions. */
    public static Codec<List<FluidStack>> stacks(int size) {
        return GenericRecipeCodec.FLUID_STACK.listOf().comapFlatMap(
                list -> list.size() == size ? DataResult.success(List.copyOf(list))
                        : DataResult.error(() -> "expected " + size + " fluid stacks, got " + list.size()),
                list -> list);
    }

    /**
     * 1.7's {@code registerSFAuto} amount (Coker, PyroOven and SolidificationRecipes): how many mB of a fuel hold
     * {@code energyPerItem} TU, scaled by the machine's {@code factor} and cut to two significant digits the 1.7 way.
     * {@code energyPerBucket} is the fluid's heat energy ({@code flammable} in its {@code fluid_property} file); 0 or
     * less means "not a fuel", and the row is skipped (1.7 would divide by zero or NPE there).
     */
    public static int solidFuelAmount(long energyPerItem, double factor, long energyPerBucket) {
        if (energyPerBucket <= 0) return -1;
        int mB = (int) (energyPerItem * 1000L * factor / energyPerBucket);
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        return mB;
    }

    /**
     * A datapack table whose rows are keyed by their input fluid: one file per input in
     * {@code data/<ns>/recipe/<name>/}, the last row of a fluid (in recipe-id order) wins. {@code value} turns a row into
     * what the machine reads (it may build item stacks: it runs after a load, never while a file is decoded) and may
     * return null to leave a row out. Read {@link #get} / {@link #all} at use time; {@link #changed} fires after a
     * rebuild.
     */
    public static final class ByFluid<R, V> {

        public final DataRecipeTable<R> table;
        private final Function<R, FluidType> key;
        private final Function<R, V> value;
        private volatile Map<FluidType, V> index;

        /** Fires after the index was rebuilt (server reload, client sync). */
        public final Hook<Runnable> changed = Hook.notifying();

        public ByFluid(String name, Supplier<MapCodec<R>> codec, Function<R, FluidType> key, Function<R, V> value) {
            this.table = new DataRecipeTable<>(name, codec);
            this.key = key;
            this.value = value;
            table.changed.register(this::reindex);
        }

        private void reindex() {
            Map<FluidType, V> next = new LinkedHashMap<>();
            for (R row : table.rows()) {
                V v = value.apply(row);
                if (v != null) next.put(key.apply(row), v);
            }
            index = Collections.unmodifiableMap(next);
            for (Runnable listener : changed) listener.run();
        }

        /** Every row, by input fluid, in recipe-id order. */
        public Map<FluidType, V> all() {
            Map<FluidType, V> current = index;
            if (current == null) {
                table.rows();
                if (index == null) reindex();
                current = index;
            }
            return current;
        }

        public V get(FluidType input) {
            return input == null ? null : all().get(input);
        }
    }
}
