package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * The gas centrifuge's two recipe layers (1.7 {@code GasCentrifugeRecipes}), as two datapack tables:
 * <ul>
 *   <li>{@code hbm:pseudo_fluid} in {@code data/hbm/recipe/pseudo_fluid/}: the internal enrichment chain, one file per
 *       pseudo-fluid, {@code {"name": "NUF6", "from": fluid (optional: the real fluid a centrifuge turns into it),
 *       "consumed": mB, "produced": mB, "next": name (optional), "high_speed": bool (optional), "outputs": [items]}};</li>
 *   <li>{@code hbm:gas_centrifuge} in {@code data/hbm/recipe/gas_centrifuge/}: what JEI shows,
 *       {@code {"input": {type, amount}, "outputs": [items], "high_speed": bool (optional), "centrifuges": n}}.</li>
 * </ul>
 * The two stay separate on purpose: the machine converts a real UF6/PUF6/WATZ tank into pseudo-fluid and passes the enriched
 * pseudo-fluid on to the next aligned centrifuge; collapsing that into ordinary fluid recipes loses the cascade. Converted
 * once from the Java tables (7 pseudo-fluids, 3 conversions, 4 display rows).
 *
 * <p>Pseudo-fluids are saved by name. 1.7's seven are constants here because the machine names two of them ({@code NUF6} is
 * a new tank's type, {@code LEUF6} the one that is fed onward), and their values come from the files; a file with a new name
 * makes a new pseudo-fluid (translation key {@code hbmpseudofluid.<name in lower case>}).
 */
public final class GasCentrifugeRecipes {

    private GasCentrifugeRecipes() {}

    public static final class PseudoFluidType {
        private static final Map<String, PseudoFluidType> TYPES = new ConcurrentHashMap<>();

        public static final PseudoFluidType NONE = new PseudoFluidType("NONE");
        public static final PseudoFluidType HEUF6 = new PseudoFluidType("HEUF6");
        public static final PseudoFluidType MEUF6 = new PseudoFluidType("MEUF6");
        public static final PseudoFluidType LEUF6 = new PseudoFluidType("LEUF6");
        public static final PseudoFluidType NUF6 = new PseudoFluidType("NUF6");
        public static final PseudoFluidType PF6 = new PseudoFluidType("PF6");
        public static final PseudoFluidType MUD_HEAVY = new PseudoFluidType("MUD_HEAVY");
        public static final PseudoFluidType MUD = new PseudoFluidType("MUD");

        private final String name;
        private volatile int fluidConsumed;
        private volatile int fluidProduced;
        private volatile PseudoFluidType outputType;
        private volatile boolean highSpeed;
        private volatile ItemStack[] output = new ItemStack[0];

        private PseudoFluidType(String name) {
            this.name = name;
            TYPES.put(name, this);
        }

        public int getFluidConsumed() { load(); return fluidConsumed; }
        public int getFluidProduced() { load(); return fluidProduced; }
        public PseudoFluidType getOutputType() { load(); return outputType == null ? NONE : outputType; }
        public boolean getIfHighSpeed() { load(); return highSpeed; }
        public ItemStack[] getOutput() { load(); return output; }
        public String getName() { return name; }
        public String getNameKey() { return "hbmpseudofluid." + name.toLowerCase(Locale.ROOT); }
        public static PseudoFluidType fromName(String name) {
            load();
            return name == null ? NONE : TYPES.getOrDefault(name, NONE);
        }
    }

    public static final class GasCentrifugeRecipe {
        public final FluidStack input;
        public final ItemStack[] output;
        public final boolean highSpeed;
        public final int centrifuges;

        private GasCentrifugeRecipe(FluidStack input, ItemStack[] output, boolean highSpeed, int centrifuges) {
            this.input = input;
            this.output = output;
            this.highSpeed = highSpeed;
            this.centrifuges = centrifuges;
        }
    }

    public record PseudoRow(String name, Optional<FluidType> from, int consumed, int produced, Optional<String> next,
                            boolean highSpeed, List<GenericRecipeCodec.ItemOut> outputs) {}

    public record DisplayRow(FluidStack input, List<GenericRecipeCodec.ItemOut> outputs, boolean highSpeed, int centrifuges) {}

    public static final MapCodec<PseudoRow> PSEUDO_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(PseudoRow::name),
            GenericRecipeCodec.FLUID.optionalFieldOf("from").forGetter(PseudoRow::from),
            Codec.INT.fieldOf("consumed").forGetter(PseudoRow::consumed),
            Codec.INT.fieldOf("produced").forGetter(PseudoRow::produced),
            Codec.STRING.optionalFieldOf("next").forGetter(PseudoRow::next),
            Codec.BOOL.optionalFieldOf("high_speed", false).forGetter(PseudoRow::highSpeed),
            GenericRecipeCodec.ITEM_OUT.listOf().fieldOf("outputs").forGetter(PseudoRow::outputs)
    ).apply(i, PseudoRow::new));

    public static final MapCodec<DisplayRow> DISPLAY_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID_STACK.fieldOf("input").forGetter(DisplayRow::input),
            GenericRecipeCodec.ITEM_OUT.listOf().fieldOf("outputs").forGetter(DisplayRow::outputs),
            Codec.BOOL.optionalFieldOf("high_speed", false).forGetter(DisplayRow::highSpeed),
            Codec.INT.fieldOf("centrifuges").forGetter(DisplayRow::centrifuges)
    ).apply(i, DisplayRow::new));

    public static final DataRecipeTable<PseudoRow> PSEUDO_FLUIDS = new DataRecipeTable<>("pseudo_fluid", () -> PSEUDO_CODEC);
    public static final DataRecipeTable<DisplayRow> DISPLAY = new DataRecipeTable<>("gas_centrifuge", () -> DISPLAY_CODEC);

    private static volatile Map<FluidType, PseudoFluidType> conversions;
    private static volatile List<GasCentrifugeRecipe> display;

    static {
        PSEUDO_FLUIDS.changed.register(GasCentrifugeRecipes::reindexPseudo);
        DISPLAY.changed.register(GasCentrifugeRecipes::reindexDisplay);
    }

    /** Real fluid to the pseudo-fluid a centrifuge turns it into (a live read-only view of the pseudo-fluid files). */
    public static final Map<FluidType, PseudoFluidType> FLUID_CONVERSIONS = new AbstractMap<>() {
        @Override
        public Set<Entry<FluidType, PseudoFluidType>> entrySet() {
            load();
            return conversions.entrySet();
        }

        @Override
        public PseudoFluidType get(Object key) {
            load();
            return conversions.get(key);
        }
    };

    /** The JEI rows (a live read-only view of the display files). */
    public static final List<GasCentrifugeRecipe> RECIPES = new AbstractList<>() {
        @Override
        public GasCentrifugeRecipe get(int index) {
            return displayRows().get(index);
        }

        @Override
        public int size() {
            return displayRows().size();
        }
    };

    /** Declares the tables at mod construction. */
    public static void declare() {}

    /** Loads the rows if nothing has yet (they load by themselves; kept for callers of the former Java table). */
    public static void register() {
        load();
        displayRows();
    }

    private static void load() {
        if (conversions != null) return;
        PSEUDO_FLUIDS.rows();
        if (conversions == null) reindexPseudo();
    }

    private static synchronized void reindexPseudo() {
        // a pseudo-fluid without a file (any more) has no values, as 1.7's NONE
        for (PseudoFluidType type : PseudoFluidType.TYPES.values()) {
            type.fluidConsumed = 0;
            type.fluidProduced = 0;
            type.outputType = null;
            type.highSpeed = false;
            type.output = new ItemStack[0];
        }
        Map<FluidType, PseudoFluidType> from = new LinkedHashMap<>();
        List<PseudoRow> rows = PSEUDO_FLUIDS.rows();
        for (PseudoRow row : rows) {
            PseudoFluidType known = PseudoFluidType.TYPES.get(row.name());
            PseudoFluidType type = known != null ? known : new PseudoFluidType(row.name());
            type.fluidConsumed = row.consumed();
            type.fluidProduced = row.produced();
            type.highSpeed = row.highSpeed();
            type.output = row.outputs().stream().map(GenericRecipeCodec.ItemOut::stack).toArray(ItemStack[]::new);
            row.from().ifPresent(fluid -> from.put(fluid, type));
        }
        for (PseudoRow row : rows) {
            PseudoFluidType.TYPES.get(row.name()).outputType = row.next().map(next -> PseudoFluidType.TYPES.get(next)).orElse(null);
        }
        conversions = Collections.unmodifiableMap(from);
    }

    private static void reindexDisplay() {
        List<GasCentrifugeRecipe> next = new ArrayList<>();
        for (DisplayRow row : DISPLAY.rows()) {
            next.add(new GasCentrifugeRecipe(row.input(), row.outputs().stream().map(GenericRecipeCodec.ItemOut::stack)
                    .toArray(ItemStack[]::new), row.highSpeed(), row.centrifuges()));
        }
        display = Collections.unmodifiableList(next);
    }

    private static List<GasCentrifugeRecipe> displayRows() {
        List<GasCentrifugeRecipe> current = display;
        if (current == null) {
            DISPLAY.rows();
            if (display == null) reindexDisplay();
            current = display;
        }
        return current;
    }
}
