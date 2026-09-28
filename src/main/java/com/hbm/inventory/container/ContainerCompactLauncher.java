package com.hbm.inventory.container;

import com.hbm.inventory.ModCustomMissileMenus;
import com.hbm.tileentity.bomb.TileEntityCompactLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ContainerCompactLauncher}; the layout lives in {@link ContainerCustomMissileLauncher}. */
public class ContainerCompactLauncher extends ContainerCustomMissileLauncher {

    public ContainerCompactLauncher(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityCompactLauncher te ? te : null);
    }

    public ContainerCompactLauncher(int containerId, Inventory playerInventory, @Nullable TileEntityCompactLauncher launcher) {
        super(ModCustomMissileMenus.COMPACT_LAUNCHER.get(), containerId, playerInventory, launcher);
    }
}
