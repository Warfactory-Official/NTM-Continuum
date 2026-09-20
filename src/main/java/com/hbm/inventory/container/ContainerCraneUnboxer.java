package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModConveyorMenus;
import com.hbm.tileentity.network.TileEntityCraneUnboxer;

/** 1.7 {@code ContainerCraneUnboxer}: 3x7 buffer, two upgrade slots, player inventory. */
public class ContainerCraneUnboxer extends ContainerCraneBase<TileEntityCraneUnboxer> {

    public ContainerCraneUnboxer(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneUnboxer.class));
    }

    public ContainerCraneUnboxer(int containerId, Inventory playerInventory, @Nullable TileEntityCraneUnboxer tile) {
        super(ModConveyorMenus.CRANE_UNBOXER.get(), containerId, tile, 23);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(plain(j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        // upgrades
        addSlot(upgrade(21, 152, 23));
        addSlot(upgrade(22, 152, 47));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    /** 1.7 merged player stacks into slots 0..22, upgrades included. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(player, index, 23, 0, 23);
    }
}
