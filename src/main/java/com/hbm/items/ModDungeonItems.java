package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemRebarPlacer;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * The dungeon family's one plain item, the rebar placer (1.7: max stack 1, consumable tab). 1.7 kept its loaded
 * concrete as a one-slot NBT inventory and its first corner as the {@code pos} int array; those are two data
 * components here.
 */
public final class ModDungeonItems {

    private ModDungeonItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7 {@code ItemStackUtil.addStacksToNBT(stack, 1)}: the concrete the placed rebar will set into. */
    public static final GolemComponent<ItemStack> REBAR_CONCRETE = GolemComponent.itemStack(Constants.MOD_ID, "rebar_concrete");
    /** 1.7 {@code pos}: the first corner of the box, the cell in front of the first clicked face. */
    public static final GolemComponent<BlockPos> REBAR_POS = GolemComponent.of(Constants.MOD_ID, "rebar_pos", BlockPos.CODEC);

    public static final RegistryObject<ItemRebarPlacer> rebar_placer =
            ITEMS.registerItem("rebar_placer", ItemRebarPlacer::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, rebar_placer);
    }

    public static void init() {}
}
