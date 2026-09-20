package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Rarity;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.items.special.ItemBombPart;

/** The F.L.E.I.J.A. and Blue Rinse components (1.7 {@code ItemFleija} / {@code ItemSolinium}, stack size 1, nuke tab). */
public final class ModBombs2Items {

    private ModBombs2Items() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemBombPart> fleija_igniter = ITEMS.registerItem("fleija_igniter",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1));
    /** 1.7 {@code getRarity}: the propellant alone is rare. */
    public static final RegistryObject<ItemBombPart> fleija_propellant = ITEMS.registerItem("fleija_propellant",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final RegistryObject<ItemBombPart> fleija_core = ITEMS.registerItem("fleija_core",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1));

    public static final RegistryObject<ItemBombPart> solinium_igniter = ITEMS.registerItem("solinium_igniter",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1));
    public static final RegistryObject<ItemBombPart> solinium_propellant = ITEMS.registerItem("solinium_propellant",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1));
    public static final RegistryObject<ItemBombPart> solinium_core = ITEMS.registerItem("solinium_core",
            p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.BOMBS, fleija_igniter);
        ModTabs.addTo(HbmTab.BOMBS, fleija_propellant);
        ModTabs.addTo(HbmTab.BOMBS, fleija_core);
        ModTabs.addTo(HbmTab.BOMBS, solinium_igniter);
        ModTabs.addTo(HbmTab.BOMBS, solinium_propellant);
        ModTabs.addTo(HbmTab.BOMBS, solinium_core);
    }

    public static void init() {}
}
