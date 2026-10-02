package com.hbm.items.machine;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemStamp.StampType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The press stamps (dies). Hand-registered (kept out of the generated {@link com.hbm.items.ModItems})
 * because they need a {@link ItemStamp} subclass carrying a {@link StampType}. Durability mirrors the
 * 1.12.2 originals (stone 32 → obsidian 512; desh/casing stamps are unbreakable). All live in the
 * CONTROL creative tab, matching the old {@code controlTab}.
 */
public final class ModStampItems {

    private ModStampItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Every registered stamp, in registration order — used by the press recipe display/JEI later. */
    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    public static final RegistryObject<Item> stamp_stone_flat = stamp("stamp_stone_flat", 32, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_stone_plate = stamp("stamp_stone_plate", 32, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_stone_wire = stamp("stamp_stone_wire", 32, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_stone_circuit = stamp("stamp_stone_circuit", 32, StampType.CIRCUIT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_iron_flat = stamp("stamp_iron_flat", 64, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_iron_plate = stamp("stamp_iron_plate", 64, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_iron_wire = stamp("stamp_iron_wire", 64, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_iron_circuit = stamp("stamp_iron_circuit", 64, StampType.CIRCUIT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_steel_flat = stamp("stamp_steel_flat", 192, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_steel_plate = stamp("stamp_steel_plate", 192, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_steel_wire = stamp("stamp_steel_wire", 192, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_steel_circuit = stamp("stamp_steel_circuit", 192, StampType.CIRCUIT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_titanium_flat = stamp("stamp_titanium_flat", 256, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_titanium_plate = stamp("stamp_titanium_plate", 256, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_titanium_wire = stamp("stamp_titanium_wire", 256, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_titanium_circuit = stamp("stamp_titanium_circuit", 256, StampType.CIRCUIT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_obsidian_flat = stamp("stamp_obsidian_flat", 512, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_obsidian_plate = stamp("stamp_obsidian_plate", 512, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_obsidian_wire = stamp("stamp_obsidian_wire", 512, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_obsidian_circuit = stamp("stamp_obsidian_circuit", 512, StampType.CIRCUIT)
            .transform(Presets.sprite());
    // desh (unbreakable)
    public static final RegistryObject<Item> stamp_desh_flat = stamp("stamp_desh_flat", 0, StampType.FLAT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_plate = stamp("stamp_desh_plate", 0, StampType.PLATE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_wire = stamp("stamp_desh_wire", 0, StampType.WIRE)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_circuit = stamp("stamp_desh_circuit", 0, StampType.CIRCUIT)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_357 = stamp("stamp_desh_357", 0, StampType.C357)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_44 = stamp("stamp_desh_44", 0, StampType.C44)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_9 = stamp("stamp_desh_9", 0, StampType.C9)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_desh_50 = stamp("stamp_desh_50", 0, StampType.C50)
            .transform(Presets.sprite());
    // casing stamps
    public static final RegistryObject<Item> stamp_357 = stamp("stamp_357", 1000, StampType.C357)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_44 = stamp("stamp_44", 1000, StampType.C44)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_9 = stamp("stamp_9", 1000, StampType.C9)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> stamp_50 = stamp("stamp_50", 1000, StampType.C50)
            .transform(Presets.sprite());

    private static Reg.Handle<Item> stamp(String name, int durability, StampType type) {
        UnaryOperator<Item.Properties> props = durability > 0
                ? p -> p.stacksTo(1).durability(durability)
                : p -> p.stacksTo(1);
        Reg.Handle<Item> stamp = REG.item(name, p -> new ItemStamp(p, type), props);
        ALL.add(stamp);
        ModTabs.addTo(HbmTab.CONTROL, stamp);
        return stamp;
    }

    public static void init() {}
}
