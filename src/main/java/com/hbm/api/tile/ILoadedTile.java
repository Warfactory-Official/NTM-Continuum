package com.hbm.api.tile;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface ILoadedTile {

    default boolean isLoaded() {
        return !(this instanceof BlockEntity be) || !be.isRemoved();
    }
}
