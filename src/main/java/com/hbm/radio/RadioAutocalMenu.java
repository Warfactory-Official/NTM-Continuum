package com.hbm.radio;

import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Position-bound, slotless menu for the AUTOCAL screen. */
public final class RadioAutocalMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioAUTOCAL autocal;

    public RadioAutocalMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory, pos));
    }

    public RadioAutocalMenu(int id, Inventory inventory, @Nullable TileEntityRadioAUTOCAL autocal) {
        super(ModRadioMenus.AUTOCAL.get(), id);
        this.autocal = autocal;
    }

    private static @Nullable TileEntityRadioAUTOCAL resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityRadioAUTOCAL value ? value : null;
    }

    public @Nullable TileEntityRadioAUTOCAL autocal() {
        return autocal;
    }

    @Override
    public boolean stillValid(Player player) {
        return autocal != null && !autocal.isRemoved() && autocal.hasPermission(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
