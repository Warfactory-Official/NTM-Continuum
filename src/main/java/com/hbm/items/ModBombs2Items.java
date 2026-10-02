package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Rarity;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.items.special.ItemBombPart;

/** The F.L.E.I.J.A. and Blue Rinse components (1.7 {@code ItemFleija} / {@code ItemSolinium}, stack size 1, nuke tab). */
public final class ModBombs2Items {

    private ModBombs2Items() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemBombPart> fleija_igniter = REG.item("fleija_igniter",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    /** 1.7 {@code getRarity}: the propellant alone is rare. */
    public static final RegistryObject<ItemBombPart> fleija_propellant = REG.item("fleija_propellant",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1).rarity(Rarity.RARE))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemBombPart> fleija_core = REG.item("fleija_core",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_fleija), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    public static final RegistryObject<ItemBombPart> solinium_igniter = REG.item("solinium_igniter",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemBombPart> solinium_propellant = REG.item("solinium_propellant",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemBombPart> solinium_core = REG.item("solinium_core",
                    p -> new ItemBombPart(p, ModBombs2Blocks.nuke_solinium), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    public static void init() {}
}
