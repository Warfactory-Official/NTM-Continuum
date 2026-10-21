package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerCrate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityCrateIron}, {@code TileEntityCrateSteel} and {@code TileEntityCrateDesh} in one class:
 * a pin-locked slot inventory (iron 36, steel 54, desh 104) whose size and GUI layout come from its {@link CrateType}.
 */
public class TileEntityCrate extends TileEntityCrateBase {

    public final CrateType crateType;

    public TileEntityCrate(CrateType crateType, BlockPos pos, BlockState state) {
        super(crateType.blockEntityType(), pos, state, crateType.slots);
        this.crateType = crateType;
    }

    @Override
    public String getDefaultName() {
        return crateType.defaultName;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrate(containerId, playerInventory, this);
    }
}
