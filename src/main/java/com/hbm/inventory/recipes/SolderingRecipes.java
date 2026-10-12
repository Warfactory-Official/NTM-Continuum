package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Soldering station recipes. A recipe has three grouped ingredient sets — up to 3 <b>toppings</b> (slots 0–2), up to
 * 2 <b>pcb</b> (slots 3–4) and 1 <b>solder</b> (slot 5) — an optional fluid, and an output; {@link #getRecipe}
 * exact-set matches the six input slots against those groups. The datapack table {@code hbm:soldering}, one file per
 * recipe in {@code data/hbm/recipe/soldering/} ({@code {"output": item, "duration": ticks, "consumption": HE/t,
 * "fluid": {type, amount}, "toppings": [...], "pcb": [...], "solder": [...], "requires": ["!enable_528"], "order": n}};
 * the first match by {@code order} wins). {@link #toppings}, {@link #pcb} and {@link #solder} (what each slot group
 * accepts) are rebuilt from the rows.
 *
 * <p>Converted once from the Java port of 1.7's {@code SolderingRecipes}: the circuit chain (ANALOG, BASIC, ADVANCED,
 * CAPACITOR_BOARD, BISMOID, QUANTUM), the control units and the tier II/III upgrades (both inside 1.7's
 * {@code if(no528)}, so {@code "requires": ["!enable_528"]}: 528 mode makes them in the precision assembler), the tier-1
 * upgrades, the satellite ID chip and the two forcefield upgrades (formerly
 * {@code RadarUtilityAssemblyRecipes.registerSoldering}). 1.7's {@code enableLBSM && enableLBSMSimpleCrafting}
 * ingredient reductions are second rows with {@code "requires": ["lbsm_simple_crafting"]} beside the default rows
 * ({@code "!lbsm_simple_crafting"}).
 */
public final class SolderingRecipes {

    private SolderingRecipes() {}

    public record Row(ItemSpec output, int duration, long consumption, Optional<FluidStack> fluid, List<AStack> toppings,
                      List<AStack> pcb, List<AStack> solder, List<String> requires, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("duration").forGetter(Row::duration),
            Codec.LONG.fieldOf("consumption").forGetter(Row::consumption),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("fluid").forGetter(Row::fluid),
            TableCodecs.INGREDIENT.listOf().optionalFieldOf("toppings", List.of()).forGetter(Row::toppings),
            TableCodecs.INGREDIENT.listOf().optionalFieldOf("pcb", List.of()).forGetter(Row::pcb),
            TableCodecs.INGREDIENT.listOf().optionalFieldOf("solder", List.of()).forGetter(Row::solder),
            TableCodecs.REQUIRES.optionalFieldOf("requires", List.of()).forGetter(Row::requires),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("soldering", () -> CODEC);

    /** {@code flags}: {@link RecipeConditions#fingerprint} when the index was built. */
    private record Index(List<SolderingRecipe> recipes, Set<AStack> toppings, Set<AStack> pcb, Set<AStack> solder, String flags) {}

    private static volatile Index current = new Index(List.of(), Set.of(), Set.of(), Set.of(), null);

    /** The recipes in match order, always the current rows (read-only). */
    public static final List<SolderingRecipe> recipes = TableCodecs.view(() -> index().recipes());
    /** The union of every group's ingredients — the TE/container use these to validate slot placement. */
    public static final Set<AStack> toppings = TableCodecs.setView(() -> index().toppings());
    public static final Set<AStack> pcb = TableCodecs.setView(() -> index().pcb());
    public static final Set<AStack> solder = TableCodecs.setView(() -> index().solder());

    static {
        TABLE.changed.register(SolderingRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** One soldering recipe. Arrays, so a plain class (not a record) — matched by {@link #getRecipe}. */
    public static final class SolderingRecipe {
        public final ItemStack output;
        public final int duration;
        public final long consumption;
        public final FluidStack fluid; // may be null
        public final AStack[] toppings;
        public final AStack[] pcb;
        public final AStack[] solder;

        SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                        AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.toppings = toppings;
            this.pcb = pcb;
            this.solder = solder;
        }
    }

    private static synchronized void reindex() {
        String flags = RecipeConditions.fingerprint();
        List<SolderingRecipe> next = new ArrayList<>();
        Set<AStack> top = new HashSet<>();
        Set<AStack> board = new HashSet<>();
        Set<AStack> sold = new HashSet<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            // 1.7 read GeneralConfig.enable528 and the LBSM switches while registering; a reload reads them again
            if (!TableCodecs.holds(row.requires())) continue;
            next.add(new SolderingRecipe(row.output().stack(), row.duration(), row.consumption(), row.fluid().orElse(null),
                    row.toppings().toArray(AStack[]::new), row.pcb().toArray(AStack[]::new), row.solder().toArray(AStack[]::new)));
            top.addAll(row.toppings());
            board.addAll(row.pcb());
            sold.addAll(row.solder());
        }
        current = new Index(List.copyOf(next), Collections.unmodifiableSet(top), Collections.unmodifiableSet(board),
                Collections.unmodifiableSet(sold), flags);
    }

    private static Index index() {
        if (current.recipes().isEmpty()) TABLE.rows();
        // a config flag changed since the last build (1.7 read them while registering): test the rows again
        if (!RecipeConditions.fingerprint().equals(current.flags())) reindex();
        return current;
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        index();
    }

    /** Exact-set match on the 6 input slots (0–2 toppings, 3–4 pcb, 5 solder). */
    public static SolderingRecipe getRecipe(ItemStack[] inputs) {
        for (SolderingRecipe recipe : index().recipes()) {
            if (matches(new ItemStack[]{inputs[0], inputs[1], inputs[2]}, recipe.toppings)
                    && matches(new ItemStack[]{inputs[3], inputs[4]}, recipe.pcb)
                    && matches(new ItemStack[]{inputs[5]}, recipe.solder)) return recipe;
        }
        return null;
    }

    /** Every required ingredient is satisfied by a distinct slot with enough count, and no slot is left over. */
    private static boolean matches(ItemStack[] slots, AStack[] required) {
        boolean[] used = new boolean[slots.length];
        for (AStack req : required) {
            boolean found = false;
            for (int i = 0; i < slots.length; i++) {
                if (!used[i] && req.matchesRecipe(slots[i], true) && slots[i].getCount() >= req.stacksize) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        for (int i = 0; i < slots.length; i++) if (!used[i] && !slots[i].isEmpty()) return false;
        return true;
    }
}
