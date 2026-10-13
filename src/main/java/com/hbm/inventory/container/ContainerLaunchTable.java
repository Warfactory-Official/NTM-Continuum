package com.hbm.inventory.container;

import com.hbm.inventory.ModCustomMissileMenus;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ContainerLaunchTable}; the layout lives in {@link ContainerCustomMissileLauncher}. */
public class ContainerLaunchTable extends ContainerCustomMissileLauncher {

    public ContainerLaunchTable(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityLaunchTable te ? te : null);
    }

    public ContainerLaunchTable(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchTable launcher) {
        super(ModCustomMissileMenus.LAUNCH_TABLE.get(), containerId, playerInventory, launcher);
    }

    /** 1.7 AuxButtonPacket from the three pad-size buttons: the value is the PartSize ordinal. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(getLauncher() instanceof TileEntityLaunchTable table)) return false;
        table.handleButtonPacket(id, 0);
        return true;
    }
}
