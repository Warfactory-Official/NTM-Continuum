package com.hbm.items;

import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The recipe-intermediate items ported from the 1.12.2 metadata-subtype {@code ItemEnumMulti} items
 * ({@code circuit} / {@code item_expensive} / {@code chunk_ore}). 1.21 has no metadata, so each enum
 * variant is its own registered item, id {@code <base>_<variant>} (e.g. {@code circuit_basic}) in the
 * MATERIALS tab, matching the pre-existing per-variant textures copied by
 * {@code port-tools/gen_intermediates.py}. Recipes reference them via the typed getters
 * ({@link #circuit}/{@link #expensive}/{@link #chunk}), the tag-system equivalent of the old
 * {@code ModItems.circuit.stackFromEnum(...)}.
 */
public final class ModIntermediateItems {

    private ModIntermediateItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static final Map<EnumChunkType, RegistryObject<Item>> CHUNKS = new EnumMap<>(EnumChunkType.class);
    private static final Map<EnumCircuitType, RegistryObject<Item>> CIRCUITS = new EnumMap<>(EnumCircuitType.class);
    private static final Map<EnumExpensiveType, RegistryObject<Item>> EXPENSIVE = new EnumMap<>(EnumExpensiveType.class);
    private static final Map<EnumDriveType, RegistryObject<ItemDrive>> DRIVES = new EnumMap<>(EnumDriveType.class);

    static {
        for (EnumChunkType v : EnumChunkType.VALUES) CHUNKS.put(v, reg("chunk_ore", v.name()));
        for (EnumCircuitType v : EnumCircuitType.VALUES) CIRCUITS.put(v, reg("circuit", v.name()));
        for (EnumExpensiveType v : EnumExpensiveType.VALUES) EXPENSIVE.put(v, reg("item_expensive", v.name()));
        for (EnumDriveType v : EnumDriveType.values()) {
            String name = v.name().toLowerCase(Locale.US);
            DRIVES.put(v, REG.item("drive_" + name, p -> new ItemDrive(p, v))
                    .transform(Presets.sprite("hbm:items/drive." + name))
                    .tab(HbmTab.MATERIALS));
        }
    }

    /** {@code <base>_<variant>}, drawn with the 1.7 metadata sprite {@code <base>.<variant>}. */
    private static RegistryObject<Item> reg(String base, String variant) {
        String name = variant.toLowerCase(Locale.US);
        return REG.item(base + "_" + name)
                .transform(Presets.sprite("hbm:items/" + base + "." + name))
                .tab(HbmTab.MATERIALS);
    }

    public static Item chunk(EnumChunkType v)         { return CHUNKS.get(v).get(); }
    public static Item circuit(EnumCircuitType v)     { return CIRCUITS.get(v).get(); }
    public static Item expensive(EnumExpensiveType v) { return EXPENSIVE.get(v).get(); }
    public static ItemDrive drive(EnumDriveType v)    { return DRIVES.get(v).get(); }

    public static ItemStack chunk(EnumChunkType v, int count)         { return new ItemStack(chunk(v), count); }
    public static ItemStack circuit(EnumCircuitType v, int count)     { return new ItemStack(circuit(v), count); }
    public static ItemStack expensive(EnumExpensiveType v, int count) { return new ItemStack(expensive(v), count); }

    public static void init() {}
}
