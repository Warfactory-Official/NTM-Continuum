package com.hbm.world.gen.nbt;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1.7 {@code INBTTileEntityTransformable}: like {@link INBTBlockTransformable} but for block entities, e.g. the
 * loot and logic placeholders arming their one-tick replacement when an {@link NBTStructure} places them.
 */
public interface INBTTileEntityTransformable {

    /** Lets the block entity modify itself when spawned in an NBT structure (a live level or world generation). */
    void transformTE(LevelAccessor level, int coordBaseMode);
}
