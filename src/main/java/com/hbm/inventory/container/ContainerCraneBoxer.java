package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModConveyorMenus;
import com.hbm.tileentity.network.TileEntityCraneBoxer;

/** 1.7 {@code ContainerCraneBoxer}: 3x7 buffer + player inventory. */
public class ContainerCraneBoxer extends ContainerCraneBase<TileEntityCraneBoxer> {

    public ContainerCraneBoxer(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneBoxer.class));
    }

    public ContainerCraneBoxer(int containerId, Inventory playerInventory, @Nullable TileEntityCraneBoxer tile) {
        super(ModConveyorMenus.CRANE_BOXER.get(), containerId, tile, TileEntityCraneBoxer.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(plain(j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(player, index, TileEntityCraneBoxer.SLOT_COUNT, 0, TileEntityCraneBoxer.SLOT_COUNT);
    }
}
