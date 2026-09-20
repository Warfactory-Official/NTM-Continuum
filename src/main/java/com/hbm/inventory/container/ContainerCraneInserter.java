package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModConveyorMenus;
import com.hbm.tileentity.network.TileEntityCraneInserter;

/** 1.7 {@code ContainerCraneInserter}: 3x7 buffer + player inventory. */
public class ContainerCraneInserter extends ContainerCraneBase<TileEntityCraneInserter> {

    public ContainerCraneInserter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneInserter.class));
    }

    public ContainerCraneInserter(int containerId, Inventory playerInventory, @Nullable TileEntityCraneInserter tile) {
        super(ModConveyorMenus.CRANE_INSERTER.get(), containerId, tile, TileEntityCraneInserter.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(plain(j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(player, index, TileEntityCraneInserter.SLOT_COUNT, 0, TileEntityCraneInserter.SLOT_COUNT);
    }
}
