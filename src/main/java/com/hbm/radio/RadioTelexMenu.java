package com.hbm.radio;

import com.hbm.tileentity.network.TileEntityRadioTelex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Position-bound, zero-slot menu that authorizes and anchors the TELEX screen. */
public final class RadioTelexMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioTelex telex;

    public RadioTelexMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public RadioTelexMenu(int containerId, Inventory inventory, @Nullable TileEntityRadioTelex telex) {
        super(ModRadioMenus.TELEX.get(), containerId);
        this.telex = telex;
    }

    private static @Nullable TileEntityRadioTelex resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityRadioTelex value ? value : null;
    }

    public @Nullable TileEntityRadioTelex telex() {
        return telex;
    }

    @Override
    public boolean stillValid(Player player) {
        return telex != null && !telex.isRemoved() && telex.hasPermission(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
