package com.hbm.blocks;

import com.hbm.inventory.ModNuclearMiscMenus;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.special.ItemDemonCore;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;


/**
 * One common bootstrap hook for the miscellaneous nuclear machines and fuel items: the RTG radiolysis
 * chamber, the legacy breeding and research reactors with their remote control, the legacy graphite
 * pile, breeding rods, pile rods, spent plate fuel and the odd nuclear specials.
 */
public final class ModNuclearMiscContent {
    private ModNuclearMiscContent() {}

    public static void init() {
        ModNuclearMiscBlocks.init();
        ModNuclearMiscItems.init();
        ModNuclearMiscBlockEntities.init();
        ModNuclearMiscMenus.init();
        // Components declared as static fields of item/tile classes only exist once the class is
        // initialised; a constructor reference does not do that, so touch them here, before the
        // loaders flush the data-component register (same reason MainRegistry calls ItemHot.init()).
        java.util.Objects.requireNonNull(com.hbm.items.machine.ItemReactorSensor.TARGET);
        java.util.Objects.requireNonNull(com.hbm.items.special.ItemUnstable.TIMER);
        java.util.Objects.requireNonNull(TileEntityMachineRadiolysis.CONTAGION);
        // 1.7 ItemDemonCore.onEntityItemUpdate is driven by the loaders' per-item tick hooks
        // (com.hbm.platform.*Init) next to ItemSingularity/ItemCell/ItemAntimatterPellet.
    }
}
