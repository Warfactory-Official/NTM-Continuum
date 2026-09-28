package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerSafe;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code TileEntitySafe}: fifteen slots behind a pin lock, blast resistance 10000 on the block. */
public class TileEntitySafe extends TileEntityCrateBase {

    public TileEntitySafe(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.SAFE.get(), pos, state, 15);
    }

    @Override
    public String getDefaultName() {
        return "container.safe";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerSafe(containerId, inventory, this);
    }
}
