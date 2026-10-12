package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon.AStack;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Codec pieces shared by the item machine tables that are not {@link GenericRecipe}s (anvil, press, ammo press,
 * soldering, ...): their ingredient ({@link GenericRecipeCodec#STACK} plus the stirling engine kind), an empty grid
 * slot, the 1.7 config switches a row can depend on ({@code "requires"}, as in {@link GenericRecipe#requires}), the
 * row order, and read-only views for the tables' public lists.
 *
 * <p>An ingredient is anything {@link GenericRecipeCodec#STACK} reads ({@code {"ingredient": id or #tag, "count"}},
 * {@code "exact"}, autogen {@code {"material", "shape"}}, {@code hbm:fluid_container}, {@code hbm:any_of}, ...), or
 * {@code {"type": "hbm:stirling", "item": "hbm:machine_stirling", "has_cog": false}}: 1.7's
 * {@code new ComparableStack(machine_stirling, 1, 1)}, the engine whose gear was taken out.
 */
public final class TableCodecs {

    private TableCodecs() {}

    private static final String TYPE_STIRLING = "hbm:stirling";

    private record Cog(Item item, boolean hasCog) {}

    private static final MapCodec<Cog> COG = RecordCodecBuilder.mapCodec(i -> i.group(
            dev.golem.core.world.ItemStacks.itemCodec().fieldOf("item").forGetter(Cog::item),
            Codec.BOOL.fieldOf("has_cog").forGetter(Cog::hasCog)
    ).apply(i, Cog::new));

    /** A machine ingredient: {@link GenericRecipeCodec#STACK} or the stirling kind (class comment). */
    public static final Codec<AStack> INGREDIENT = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AStack, T>> decode(DynamicOps<T> ops, T input) {
            Optional<String> type = ops.get(input, "type").flatMap(ops::getStringValue).result();
            if (type.isPresent() && type.get().equals(TYPE_STIRLING)) {
                return COG.codec().decode(ops, input).map(pair -> pair.mapFirst(cog -> (AStack) new CogStack(cog.item(), cog.hasCog())));
            }
            return GenericRecipeCodec.STACK.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AStack stack, DynamicOps<T> ops, T prefix) {
            if (stack instanceof CogStack cog) {
                return COG.codec().encode(new Cog(cog.item, cog.hasCog), ops, prefix)
                        .flatMap(map -> ops.mergeToMap(map, ops.createString("type"), ops.createString(TYPE_STIRLING)));
            }
            return GenericRecipeCodec.STACK.encode(stack, ops, prefix);
        }
    };

    /** A grid position: an ingredient, or {@code ""} for a slot that must stay empty. */
    public static final Codec<Optional<AStack>> SLOT = Codec.either(Codec.STRING.comapFlatMap(
                    text -> text.isEmpty() ? DataResult.success(text) : DataResult.error(() -> "an empty slot is \"\", not " + text),
                    text -> text), INGREDIENT)
            .xmap(either -> either.map(empty -> Optional.<AStack>empty(), Optional::of),
                    slot -> slot.<Either<String, AStack>>map(Either::right).orElse(Either.left("")));

    /** 1.7 {@code new ComparableStack(machine_stirling, 1, meta)}: metadata 1 is the engine without its gear. */
    public static final class CogStack extends AStack {

        public final Item item;
        public final boolean hasCog;

        public CogStack(Item item, boolean hasCog) {
            this.item = item;
            this.hasCog = hasCog;
            this.stacksize = 1;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item
                    && com.hbm.blocks.machine.MachineStirling.hasCog(stack) == hasCog
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = new ItemStack(item, stacksize);
            if (!hasCog) com.hbm.items.ModDataComponents.HAS_COG.set(stack, false);
            return List.of(stack);
        }
    }

    // ---- 1.7 config switches ----

    /**
     * A row's {@code "requires"}: the 1.7 config branches it sat in ({@link RecipeConditions}: {@code enable_528},
     * {@code expensive_mode}, the LBSM switches, any negated with {@code !}), or {@code #tag} (the tag has an item);
     * every one must hold. The same flags as {@link GenericRecipe#requires}, read whenever the table rebuilds, as 1.7
     * read them while registering.
     */
    public static final Codec<List<String>> REQUIRES = Codec.STRING.listOf();

    /**
     * Whether every flag of {@code requires} holds now (an unknown flag: never, with a warning): the flags of
     * {@link RecipeConditions} (528, expensive mode, the LBSM switches, {@code #tag}).
     */
    public static boolean holds(List<String> requires) {
        return requires.isEmpty() || RecipeConditions.holds(requires.toArray(String[]::new), "a table row");
    }

    // ---- order ----

    /** The {@code "order"} default: rows without one come after every ordered row, by recipe id. */
    public static final int LAST = Integer.MAX_VALUE;

    /**
     * The rows sorted by their {@code "order"} (stable, so equal orders keep recipe-id order). Tables whose first match
     * or display order is load-bearing (1.7 kept registration order) carry the old position as {@code "order"}.
     */
    public static <R> List<R> ordered(List<R> rows, ToIntFunction<R> order) {
        List<R> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt(order));
        return sorted;
    }

    // ---- read-only views ----

    /**
     * A read-only list that always shows {@code source}'s current rows: a table's public {@code recipes} list keeps its
     * shape for the machines, GUIs and JEI that read it, while the rows behind it are rebuilt on every reload.
     */
    public static <T> List<T> view(java.util.function.Supplier<List<T>> source) {
        return new java.util.AbstractList<>() {
            @Override
            public T get(int index) {
                return source.get().get(index);
            }

            @Override
            public int size() {
                return source.get().size();
            }
        };
    }

    /** A read-only set that always shows {@code source}'s current members (see {@link #view(java.util.function.Supplier)}). */
    public static <T> java.util.Set<T> setView(java.util.function.Supplier<java.util.Set<T>> source) {
        return new java.util.AbstractSet<>() {
            @Override
            public java.util.Iterator<T> iterator() {
                return java.util.Collections.unmodifiableSet(source.get()).iterator();
            }

            @Override
            public int size() {
                return source.get().size();
            }

            @Override
            public boolean contains(Object o) {
                return source.get().contains(o);
            }
        };
    }

    /** An enum by its lower-case name. */
    public static <E extends Enum<E>> Codec<E> lowerEnum(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "unknown " + type.getSimpleName() + " " + name);
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
