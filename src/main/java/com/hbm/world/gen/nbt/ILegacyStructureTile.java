package com.hbm.world.gen.nbt;

import net.minecraft.nbt.CompoundTag;

/**
 * A block entity that can read the 1.7 tile-entity NBT a 1.7 structure file stored for it. Block entities without it
 * start from their defaults when a 1.7 structure places them (their 1.7 NBT is not migrated; logged once per id).
 */
public interface ILegacyStructureTile {

    void readLegacyStructureNBT(CompoundTag nbt);
}
