package com.hbm.handler;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The in-world tool conversion table — 1.7's {@code NTMToolHandler}. Each entry says "hitting {@code from} with
 * {@code tool}, while carrying {@code materials}, turns it into {@code to}".
 *
 * <p>The table is the datapack table {@code hbm:tool_conversion}, one file per conversion in
 * {@code data/hbm/recipe/tool_conversion/}:
 *
 * <pre>
 * {"type": "hbm:tool_conversion", "tool": "bolt", "from": "hbm:watz_casing", "to": "hbm:watz_casing_tooled",
 *  "materials": [{"material": "dura_steel", "shape": "bolt", "count": 4}]}
 * </pre>
 *
 * converted once from the former Java table (HBM's four conversions). Datapacks and addons add rows the same way; code
 * may still {@link #register} a row (data rows win for the same tool and block, as a later {@code put} did).
 *
 * <p>Where the original keyed on {@code (ToolType, MetaBlock)}, this keys on {@code (ToolType, Block)}: the port
 * splits 1.7's meta-variant blocks into separate registered blocks, so the meta half of the key has no counterpart.
 * Upstream also registers stone→cobblestone (boltgun); that joke entry is not here.
 */
public final class NTMToolHandler {

    private NTMToolHandler() {}

    /** The material cost and the block a conversion yields. */
    public record Conversion(List<AStack> materials, Supplier<Block> result) {}

    /** One data row. */
    public record Row(ToolType tool, Block from, Block to, List<AStack> materials) {}

    private static final Codec<ToolType> TOOL = Codec.STRING.comapFlatMap(name -> {
        for (ToolType type : ToolType.VALUES) if (type.name().toLowerCase(Locale.ROOT).equals(name)) return DataResult.success(type);
        return DataResult.error(() -> "unknown tool type " + name);
    }, type -> type.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TOOL.fieldOf("tool").forGetter(Row::tool),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("from").forGetter(Row::from),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("to").forGetter(Row::to),
            GenericRecipeCodec.STACK.listOf().optionalFieldOf("materials", List.of()).forGetter(Row::materials)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("tool_conversion", () -> CODEC);

    private record Entry(ToolType tool, Supplier<Block> from, Conversion conversion) {}
    private record Key(ToolType tool, Block from) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static volatile @Nullable Map<Key, Conversion> resolved;

    static {
        TABLE.changed.register(() -> resolved = null);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** A conversion from code (a data row for the same tool and block wins). */
    public static synchronized void register(ToolType tool, Supplier<Block> from, Supplier<Block> to, AStack... materials) {
        ENTRIES.add(new Entry(tool, from, new Conversion(List.of(materials), to)));
        resolved = null;
    }

    public static @Nullable Conversion get(ToolType tool, Block from) {
        Map<Key, Conversion> map = resolved;
        if (map == null) {
            map = new HashMap<>();
            synchronized (NTMToolHandler.class) {
                for (Entry e : ENTRIES) map.put(new Key(e.tool(), e.from().get()), e.conversion());
            }
            for (Row row : TABLE.rows()) {
                Block to = row.to();
                map.put(new Key(row.tool(), row.from()), new Conversion(List.copyOf(row.materials()), () -> to));
            }
            resolved = map;
        }
        return map.get(new Key(tool, from));
    }

    /**
     * Binds HBM's welding and bolting tools to their tool type. Called from the mod bootstrap after items are
     * registered; the conversions themselves are data.
     */
    public static void register() {
        // Bind the tool items to their type (by Item, not ItemStack — components aren't bound yet).
        ToolType.TORCH.register(com.hbm.items.ModToolItems.blowtorch);
        ToolType.BOLT.register(com.hbm.items.ModToolItems.boltgun);
    }
}
