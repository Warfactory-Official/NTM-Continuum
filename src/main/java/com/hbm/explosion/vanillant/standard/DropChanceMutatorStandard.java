package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IDropChanceMutator;
import net.minecraft.world.level.block.Block;

/** A flat drop chance for every block, ignoring the explosion's size. */
public class DropChanceMutatorStandard implements IDropChanceMutator {

    protected float chance;

    public DropChanceMutatorStandard(float chance) {
        this.chance = chance;
    }

    @Override
    public float mutateDropChance(ExplosionVNT explosion, Block block, int x, int y, int z, float dropChance) {
        return this.chance;
    }
}
