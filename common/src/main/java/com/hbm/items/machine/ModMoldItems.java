package com.hbm.items.machine;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ItemMold.MoldBlock;
import com.hbm.items.machine.ItemMold.MoldMulti;
import com.hbm.items.machine.ItemMold.MoldShape;
import com.hbm.items.machine.ItemMold.MoldSingle;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The casting molds — the 1.12.2 {@code ItemMold} metadata table as one registered item per mold
 * (registry id {@code mold_<name>}), plus the {@code mold_base} crafting blank. Sizes/costs verbatim;
 * the two weaponsteel casing entries are dropped (their enum-casing items aren't ported).
 */
public final class ModMoldItems {

    private ModMoldItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** All registered mold items, in display order (for recipes/creative). */
    public static final List<RegistryObject<ItemMold>> MOLDS = new ArrayList<>();

    /** The blank used to craft molds on the anvil (registered by the generated {@link ModItems}). */
    public static final RegistryObject<Item> mold_base = ModItems.mold_base;

    private static RegistryObject<ItemMold> mold(Mold mold) {
        RegistryObject<ItemMold> ro = ITEMS.registerItem("mold_" + mold.name, p -> new ItemMold(p, mold), p -> p.stacksTo(64));
        MOLDS.add(ro);
        ModTabs.addTo(HbmTab.TEMPLATES, ro);
        return ro;
    }

    private static final int S = 0;
    private static final int L = 1;

    // --- small molds (foundry_mold) ---
    public static final RegistryObject<ItemMold> mold_nugget = mold(new MoldShape(0, S, "nugget", MaterialShapes.NUGGET));
    public static final RegistryObject<ItemMold> mold_billet = mold(new MoldShape(1, S, "billet", MaterialShapes.BILLET));
    public static final RegistryObject<ItemMold> mold_ingot = mold(new MoldShape(2, S, "ingot", MaterialShapes.INGOT));
    public static final RegistryObject<ItemMold> mold_plate = mold(new MoldShape(3, S, "plate", MaterialShapes.PLATE));
    public static final RegistryObject<ItemMold> mold_wire = mold(new MoldShape(4, S, "wire", MaterialShapes.WIRE, 8));

    public static final RegistryObject<ItemMold> mold_blade = mold(new MoldMulti(5, S, "blade", MaterialShapes.INGOT.quantity * 3)
            .add(Mats.MAT_TITANIUM, () -> new ItemStack(ModItems.blade_titanium.get()))
            .add(Mats.MAT_TUNGSTEN, () -> new ItemStack(ModItems.blade_tungsten.get())));

    public static final RegistryObject<ItemMold> mold_blades = mold(new MoldMulti(6, S, "blades", MaterialShapes.INGOT.quantity * 4)
            .add(Mats.MAT_STEEL, () -> new ItemStack(ModMachineItems.blades_steel.get()))
            .add(Mats.MAT_TITANIUM, () -> new ItemStack(ModMachineItems.blades_titanium.get())));

    public static final RegistryObject<ItemMold> mold_stamp = mold(new MoldMulti(7, S, "stamp", MaterialShapes.INGOT.quantity * 4)
            .add(Mats.MAT_STONE, () -> new ItemStack(ModStampItems.stamp_stone_flat.get()))
            .add(Mats.MAT_IRON, () -> new ItemStack(ModStampItems.stamp_iron_flat.get()))
            .add(Mats.MAT_STEEL, () -> new ItemStack(ModStampItems.stamp_steel_flat.get()))
            .add(Mats.MAT_TITANIUM, () -> new ItemStack(ModStampItems.stamp_titanium_flat.get()))
            .add(Mats.MAT_OBSIDIAN, () -> new ItemStack(ModStampItems.stamp_obsidian_flat.get())));

    public static final RegistryObject<ItemMold> mold_shell = mold(new MoldShape(8, S, "shell", MaterialShapes.SHELL));
    public static final RegistryObject<ItemMold> mold_pipe = mold(new MoldShape(9, S, "pipe", MaterialShapes.PIPE));

    // --- large molds (foundry_basin) ---
    public static final RegistryObject<ItemMold> mold_ingots = mold(new MoldShape(10, L, "ingots", MaterialShapes.INGOT, 9));
    public static final RegistryObject<ItemMold> mold_plates = mold(new MoldShape(11, L, "plates", MaterialShapes.PLATE, 9));
    public static final RegistryObject<ItemMold> mold_block = mold(new MoldBlock(12, L, "block", MaterialShapes.BLOCK)
            .override(Mats.MAT_STONE, () -> new ItemStack(Items.STONE))
            .override(Mats.MAT_OBSIDIAN, () -> new ItemStack(Items.OBSIDIAN)));
    public static final RegistryObject<ItemMold> mold_pipes = mold(new MoldSingle(13, L, "pipes",
            () -> new ItemStack(ModItems.pipes_steel.get()), Mats.MAT_STEEL, MaterialShapes.BLOCK.quantity * 3));
    public static final RegistryObject<ItemMold> mold_plates_cast = mold(new MoldShape(15, L, "plates_cast", MaterialShapes.CASTPLATE, 3));

    // --- more small molds ---
    public static final RegistryObject<ItemMold> mold_c9 = mold(new MoldMulti(16, S, "c9", MaterialShapes.PLATE.quantity / 4)
            .add(Mats.MAT_GUNMETAL, () -> new ItemStack(ModItems.casing_9.get())));
    public static final RegistryObject<ItemMold> mold_c50 = mold(new MoldMulti(17, S, "c50", MaterialShapes.PLATE.quantity / 2)
            .add(Mats.MAT_GUNMETAL, () -> new ItemStack(ModItems.casing_50.get())));

    public static final RegistryObject<ItemMold> mold_plate_cast = mold(new MoldShape(19, S, "plate_cast", MaterialShapes.CASTPLATE));
    public static final RegistryObject<ItemMold> mold_wire_dense = mold(new MoldShape(20, S, "wire_dense", MaterialShapes.DENSEWIRE));
    public static final RegistryObject<ItemMold> mold_wires_dense = mold(new MoldShape(21, L, "wires_dense", MaterialShapes.DENSEWIRE, 9));

    public static final RegistryObject<ItemMold> mold_barrel_light = mold(new MoldShape(22, S, "barrel_light", MaterialShapes.LIGHTBARREL));
    public static final RegistryObject<ItemMold> mold_barrel_heavy = mold(new MoldShape(23, S, "barrel_heavy", MaterialShapes.HEAVYBARREL));
    public static final RegistryObject<ItemMold> mold_receiver_light = mold(new MoldShape(24, S, "receiver_light", MaterialShapes.LIGHTRECEIVER));
    public static final RegistryObject<ItemMold> mold_receiver_heavy = mold(new MoldShape(25, S, "receiver_heavy", MaterialShapes.HEAVYRECEIVER));
    public static final RegistryObject<ItemMold> mold_mechanism = mold(new MoldShape(26, S, "mechanism", MaterialShapes.MECHANISM));
    public static final RegistryObject<ItemMold> mold_stock = mold(new MoldShape(27, S, "stock", MaterialShapes.STOCK));
    public static final RegistryObject<ItemMold> mold_grip = mold(new MoldShape(28, S, "grip", MaterialShapes.GRIP));


    public static void init() {}
}
