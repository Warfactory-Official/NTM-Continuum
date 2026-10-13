package com.hbm.inventory.container;

import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModMenus;

public final class RadioReaderMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioTorchReader reader;
    public RadioReaderMenu(int id, Inventory inventory, BlockPos pos) { this(id, inventory, resolve(inventory, pos)); }
    public RadioReaderMenu(int id, Inventory inventory, @Nullable TileEntityRadioTorchReader reader) {
        super(ModMenus.RADIO_READER.get(), id); this.reader = reader;
    }
    private static @Nullable TileEntityRadioTorchReader resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityRadioTorchReader reader ? reader : null;
    }
    public @Nullable TileEntityRadioTorchReader reader() { return reader; }
    @Override public boolean stillValid(Player player) {
        return reader != null && !reader.isRemoved() && player.distanceToSqr(reader.getBlockPos().getCenter()) <= 64D;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
