package com.hbm.handler;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.inventory.RecipesCommon.AStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The in-world tool conversion table — 1.12.2's {@code NTMToolHandler}. Each entry says "hitting
 * {@code from} with {@code tool}, while carrying {@code materials}, turns it into {@code to}".
 *
 * <p>Where the original keyed on {@code (ToolType, MetaBlock)}, this keys on {@code (ToolType, Block)}:
 * the port splits 1.12.2's meta-variant blocks into separate registered blocks, so the meta half of the
 * key has no counterpart.
 *
 * <p>Registration is by {@link Supplier} and the lookup map is built on first use, because the entries
 * are declared while the block registry is still being filled.
 */
public final class NTMToolHandler {

    private NTMToolHandler() {}

    /** The material cost and the block a conversion yields. */
    public record Conversion(List<AStack> materials, Supplier<Block> result) {}

    private record Entry(ToolType tool, Supplier<Block> from, Conversion conversion) {}
    private record Key(ToolType tool, Block from) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static @Nullable Map<Key, Conversion> resolved;

    public static void register(ToolType tool, Supplier<Block> from, Supplier<Block> to, AStack... materials) {
        ENTRIES.add(new Entry(tool, from, new Conversion(List.of(materials), to)));
        resolved = null;
    }

    public static @Nullable Conversion get(ToolType tool, Block from) {
        Map<Key, Conversion> map = resolved;
        if (map == null) {
            map = new HashMap<>();
            for (Entry e : ENTRIES) map.put(new Key(e.tool(), e.from().get()), e.conversion());
            resolved = map;
        }
        return map.get(new Key(tool, from));
    }

    /**
     * The conversions HBM ships. Called from the mod bootstrap after blocks are registered.
     *
     * <p>Upstream also registers stone→cobblestone (boltgun) and the two ICF component steps; the
     * former is a joke entry and the latter waits on the ICF, so neither is here yet.
     */
    public static void register() {
        // Bind the tool items to their type (by Item, not ItemStack — components aren't bound yet).
        ToolType.TORCH.register(com.hbm.items.ModToolItems.blowtorch);
        ToolType.BOLT.register(com.hbm.items.ModToolItems.boltgun);

        // Watz casing must be bolted before the reactor structure accepts it (4 dura bolts).
        register(ToolType.BOLT,
                com.hbm.blocks.machine.ModMachineBlocks.watz_casing::get,
                com.hbm.blocks.machine.ModMachineBlocks.watz_casing_tooled::get,
                new com.hbm.inventory.RecipesCommon.MaterialStack(
                        com.hbm.inventory.material.Mats.MAT_DURA,
                        com.hbm.inventory.material.MaterialShapes.BOLT, 4));
        // A raw fusion component is welded into a BSCCO magnet coil with a blowtorch + a steel plate.
        register(ToolType.TORCH,
                com.hbm.blocks.machine.ModMachineBlocks.fusion_component::get,
                com.hbm.blocks.machine.ModMachineBlocks.fusion_component_bscco_welded::get,
                new com.hbm.inventory.RecipesCommon.MaterialStack(
                        com.hbm.inventory.material.Mats.MAT_STEEL,
                        com.hbm.inventory.material.MaterialShapes.CASTPLATE, 1));
    }
}
