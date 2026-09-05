package com.hbm.radio;

import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Position-bound, zero-slot menu used only to authorize and anchor the radio configuration screen. */
public final class RadioTorchMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioTorchBase radio;

    public RadioTorchMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public RadioTorchMenu(int containerId, Inventory inventory, @Nullable TileEntityRadioTorchBase radio) {
        super(ModRadioMenus.RADIO_TORCH.get(), containerId);
        this.radio = radio;
    }

    private static @Nullable TileEntityRadioTorchBase resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityRadioTorchBase radio ? radio : null;
    }

    public @Nullable TileEntityRadioTorchBase radio() {
        return radio;
    }

    @Override
    public boolean stillValid(Player player) {
        return radio != null && !radio.isRemoved()
                && player.distanceToSqr(radio.getBlockPos().getCenter()) <= 64D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
