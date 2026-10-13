package com.hbm.inventory.container;

import com.hbm.tileentity.network.TileEntityRadioTorchController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModMenus;

/** Position-bound zero-slot menu anchoring the controller's standalone configuration screen. */
public final class RadioControllerMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioTorchController controller;

    public RadioControllerMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public RadioControllerMenu(int containerId, Inventory inventory,
                               @Nullable TileEntityRadioTorchController controller) {
        super(ModMenus.RADIO_CONTROLLER.get(), containerId);
        this.controller = controller;
    }

    private static @Nullable TileEntityRadioTorchController resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityRadioTorchController controller ? controller : null;
    }

    public @Nullable TileEntityRadioTorchController controller() {
        return controller;
    }

    @Override
    public boolean stillValid(Player player) {
        return controller != null && !controller.isRemoved()
                && player.distanceToSqr(controller.getBlockPos().getCenter()) < 256D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
