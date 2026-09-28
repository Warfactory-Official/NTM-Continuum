package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerSoyuzCapsule;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntitySoyuzCapsule}: nineteen slots, the eighteen payload stacks the lander carried down and
 * the spent rocket in slot 18. Nothing ticks.
 */
public class TileEntitySoyuzCapsule extends TileEntityMachineBase implements MenuProvider {

    public TileEntitySoyuzCapsule(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.SOYUZ_CAPSULE.get(), pos, state, 19);
    }

    @Override
    public String getDefaultName() {
        return "container.soyuzCapsule";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerSoyuzCapsule(containerId, playerInventory, this);
    }
}
