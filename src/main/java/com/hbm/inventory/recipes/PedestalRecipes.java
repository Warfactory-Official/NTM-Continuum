package com.hbm.inventory.recipes;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code PedestalRecipes}, as data: the ritual recipes whose sets the clay tablets reveal and the pedestal
 * ({@code BlockPedestal}) performs. The datapack table {@code hbm:pedestal}, one file per ritual in
 * {@code data/hbm/recipe/pedestal/}:
 * <pre>
 * {"output": "hbm:gun_folly", "count": 1, "grid": ["", "#c:plates/lead", "", ...9 slots, row by row],
 *  "extra": "full_moon", "set": 1, "order": 10}
 * </pre>
 * A grid slot is an item id or {@code #tag} (or {@code {"id", "count"}}; the pedestal wants exactly that count), or
 * {@code ""} for a pedestal that must stay empty; {@code extra} names a {@link PedestalExtraCondition} (default
 * {@code none}), {@code set} the tablet set that reveals it (default 0). {@code order} keeps 1.7's registration order,
 * which both the tablet's seeded draw and the pedestal's first-match search depend on. Converted once from the Java
 * table (1.7's 17 rituals; 1.7's enum-variant items are items of their own here). The output is an id resolved when the
 * ritual completes, so a ritual for an item that is not registered simply never completes.
 */
public final class PedestalRecipes {

    /**
     * A ritual's extra condition (1.7 {@code PedestalExtraCondition}), a registry of records so an addon can add one (a
     * condition is a test of the ritual site, plus the icon the clay tablet shows for it). HBM's six keep 1.7's names
     * and tests ({@code BlockPedestal.onNeighborBlockChange}); a ritual file names one in lower case ({@code "full_moon"}),
     * an addon's by its registered name ({@code "ntmspace:eclipse"}). The moon tests read Golem {@code Skies}, so a
     * dimension with its own sky answers them (H-40).
     */
    public static final class PedestalExtraCondition {

        /** The ritual site: the moment and the players whose reputation the karma conditions read (within 20 blocks). */
        public record Site(Level level, BlockPos pos, float celestialAngle,
                           int moonPhase, List<Player> nearbyPlayers) {}

        @FunctionalInterface
        public interface Test {
            boolean holds(Site site);
        }

        /**
         * The tablet's 16x16 icon: on HBM's tablet sheet at v ({@code sheet} null; u follows the tablet), or at (u, v) of
         * an addon's own 256x256 {@code sheet}. Null for a condition the tablet does not show.
         */
        public record Icon(net.minecraft.resources.@org.jspecify.annotations.Nullable Identifier sheet, int u, int v) {}

        public static final NamedRegistry<PedestalExtraCondition> REGISTRY =
                new NamedRegistry<>("pedestal condition", PedestalExtraCondition::name);

        private static boolean night(Site site) {
            return site.celestialAngle() >= 0.35 && site.celestialAngle() <= 0.65;
        }

        private static boolean karma(Site site, IntPredicate reputation) {
            for (Player player : site.nearbyPlayers()) {
                if (reputation.test(HbmPlayerProps.getReputation(player))) return true;
            }
            return false;
        }

        public static final PedestalExtraCondition NONE = register("NONE", null, site -> true);
        public static final PedestalExtraCondition FULL_MOON = register("FULL_MOON", new Icon(null, 0, 32),
                site -> night(site) && site.moonPhase() == 0);
        public static final PedestalExtraCondition NEW_MOON = register("NEW_MOON", new Icon(null, 0, 48),
                site -> night(site) && site.moonPhase() == 4);
        public static final PedestalExtraCondition SUN = register("SUN", new Icon(null, 0, 64),
                site -> site.celestialAngle() <= 0.15 || site.celestialAngle() >= 0.85);
        public static final PedestalExtraCondition GOOD_KARMA = register("GOOD_KARMA", null, site -> karma(site, r -> r >= 10));
        public static final PedestalExtraCondition BAD_KARMA = register("BAD_KARMA", null, site -> karma(site, r -> r <= -10));

        /** By name: HBM's in lower case ({@code "full_moon"}), an addon's as registered. */
        public static final Codec<PedestalExtraCondition> CODEC = Codec.STRING.comapFlatMap(name -> {
            PedestalExtraCondition condition = REGISTRY.get(name);
            if (condition == null) condition = REGISTRY.get(name.toUpperCase(Locale.ROOT));
            PedestalExtraCondition found = condition;
            return found != null ? DataResult.success(found) : DataResult.error(() -> "unknown pedestal condition " + name);
        }, condition -> REGISTRY.get(condition.name().toUpperCase(Locale.ROOT)) == condition
                ? condition.name().toLowerCase(Locale.ROOT) : condition.name());

        private final String name;
        public final @org.jspecify.annotations.Nullable Icon icon;
        private final Test test;

        private PedestalExtraCondition(String name, @org.jspecify.annotations.Nullable Icon icon, Test test) {
            this.name = name;
            this.icon = icon;
            this.test = test;
        }

        /** Registers a condition; an addon names it with its namespace. Call at mod construction, on both sides. */
        public static PedestalExtraCondition register(String name, @org.jspecify.annotations.Nullable Icon icon, Test test) {
            return REGISTRY.register(new PedestalExtraCondition(name, icon, test));
        }

        public boolean holds(Site site) {
            return test.holds(site);
        }

        public String name() {
            return name;
        }

        public static List<PedestalExtraCondition> values() {
            return REGISTRY.values();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** One ingredient: {@code id} is an item id or a {@code #tag}. */
    public record Input(String id, int count) {}

    public static final class PedestalRecipe {
        public final String output;
        public final int outputCount;
        public final Input[] input;
        public PedestalExtraCondition extra = PedestalExtraCondition.NONE;
        public int recipeSet = 0;

        public PedestalRecipe(String output, int outputCount, Input... input) {
            this.output = output;
            this.outputCount = outputCount;
            this.input = input;
        }

        public PedestalRecipe extra(PedestalExtraCondition extra) { this.extra = extra; return this; }
        public PedestalRecipe set(int set) { this.recipeSet = set; return this; }
    }

    private static final Codec<Input> INPUT = Codec.either(Codec.STRING, RecordCodecBuilder.<Input>create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Input::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Input::count)
    ).apply(i, Input::new))).xmap(either -> either.map(id -> new Input(id, 1), input -> input),
            input -> input.count() == 1 ? Either.left(input.id()) : Either.right(input));

    /** A pedestal: an input, or {@code ""} for one that must stay empty. */
    private static final Codec<Optional<Input>> SLOT = INPUT.xmap(
            input -> input.id().isEmpty() ? Optional.<Input>empty() : Optional.of(input),
            slot -> slot.orElse(new Input("", 1)));

    public record Row(String output, int count, List<Optional<Input>> grid, PedestalExtraCondition extra, int set, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("output").forGetter(Row::output),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Row::count),
            SLOT.listOf().comapFlatMap(grid -> grid.size() == 9 ? DataResult.success(grid)
                    : DataResult.error(() -> "a pedestal ritual has 9 pedestals, not " + grid.size()), grid -> grid)
                    .fieldOf("grid").forGetter(Row::grid),
            PedestalExtraCondition.CODEC.optionalFieldOf("extra", PedestalExtraCondition.NONE)
                    .forGetter(Row::extra),
            Codec.INT.optionalFieldOf("set", 0).forGetter(Row::set),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("pedestal", () -> CODEC);

    /** The tablet sets (1.7 had two); a recipe's set is taken modulo this, as 1.7 did. */
    public static final int SETS = 2;

    private record Index(List<PedestalRecipe> recipes, List<List<PedestalRecipe>> sets) {}

    private static volatile Index current = new Index(List.of(), List.of(List.of(), List.of()));

    static {
        TABLE.changed.register(PedestalRecipes::reindex);
    }

    private PedestalRecipes() {}

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<PedestalRecipe> all = new ArrayList<>();
        List<List<PedestalRecipe>> sets = new ArrayList<>();
        for (int set = 0; set < SETS; set++) sets.add(new ArrayList<>());
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            PedestalRecipe recipe = new PedestalRecipe(row.output(), row.count(),
                    row.grid().stream().map(slot -> slot.orElse(null)).toArray(Input[]::new)).extra(row.extra()).set(row.set());
            all.add(recipe);
            sets.get(Math.abs(recipe.recipeSet) % SETS).add(recipe);
        }
        current = new Index(List.copyOf(all), sets.stream().map(List::copyOf).toList());
    }

    private static Index index() {
        if (current.recipes().isEmpty()) TABLE.rows();
        return current;
    }

    /** Every ritual in 1.7's order (the pedestal's first-match search). */
    public static List<PedestalRecipe> recipes() {
        return index().recipes();
    }

    /** The rituals tablet {@code set} reveals, in order (the tablet's seeded draw). */
    public static List<PedestalRecipe> recipeSet(int set) {
        return index().sets().get(Math.abs(set) % SETS);
    }

    /**
     * 1.7 {@code AStack.matchesRecipe(stack, true)} plus the pedestal's exact-count check: the item (or tag) must match and the stack must hold exactly the ingredient's count.
     */
    public static boolean matches(Input input, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != input.count()) return false;
        String id = input.id();
        if (id.startsWith("#")) {
            return stack.is(TagKey.create(Registries.ITEM,
                    Ids.parse(id.substring(1))));
        }
        return ItemStacks.matchesItemId(stack, id);
    }

    /** The recipe's product, or empty if its item is not registered in this port. */
    public static ItemStack output(PedestalRecipe recipe) {
        net.minecraft.resources.Identifier id = Ids.parse(recipe.output);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id),
                recipe.outputCount);
    }
}
