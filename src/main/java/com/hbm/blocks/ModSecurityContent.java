package com.hbm.blocks;

import com.hbm.Constants;
import dev.golem.core.data.GolemComponent;
import com.hbm.inventory.ModSecurityMenus;
import com.hbm.items.ModSecurityItems;
import com.hbm.items.tool.ItemCounterfeitKeys;
import com.hbm.items.tool.ItemLock;
import com.hbm.sound.ModSecuritySounds;
import com.hbm.tileentity.ModSecurityBlockEntities;

/**
 * One common bootstrap hook for the security / perimeter family: barbed wire, spikes, the sand-bag
 * barricade, the chain-link fence, the wooden barrier, the locksmith table and the padlock / key items.
 */
public final class ModSecurityContent {
    private ModSecurityContent() {}

    /** The old {@code pins} NBT integer on keys and padlocks. */
    public static final GolemComponent<Integer> KEY_PINS = GolemComponent.integer(Constants.MOD_ID, "key_pins");

    public static void init() {
        // The lockable doors let these items' own use handlers run instead of toggling the door.
        com.hbm.tileentity.machine.TileEntityLockableBase.LOCK_TOOLS =
                stack -> stack.getItem() instanceof ItemLock || stack.getItem() instanceof ItemCounterfeitKeys;
        ModSecuritySounds.init();
        ModSecurityBlocks.init();
        ModSecurityItems.init();
        ModSecurityBlockEntities.init();
        ModSecurityMenus.init();
    }
}
