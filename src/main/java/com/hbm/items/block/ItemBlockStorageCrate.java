package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.world.level.block.Block;

/** Block item of the iron, steel and desh crates; the tooltip and the held-opening path are the lockable crates'. */
public class ItemBlockStorageCrate extends ItemBlockLockableCrate {

    private final CrateType crateType;

    public ItemBlockStorageCrate(Block block, Properties properties, CrateType crateType) {
        super(block, properties, crateType.slots);
        this.crateType = crateType;
    }

    public CrateType crateType() {
        return crateType;
    }
}
