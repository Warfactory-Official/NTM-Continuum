package com.hbm.radio;

import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class RadioLogicMenu extends AbstractContainerMenu {
    private final @Nullable TileEntityRadioTorchLogic logic;
    public RadioLogicMenu(int id, Inventory inventory, BlockPos pos) { this(id, inventory, resolve(inventory, pos)); }
    public RadioLogicMenu(int id, Inventory inventory, @Nullable TileEntityRadioTorchLogic logic) {
        super(ModRadioMenus.RADIO_LOGIC.get(), id); this.logic = logic;
    }
    private static @Nullable TileEntityRadioTorchLogic resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityRadioTorchLogic logic ? logic : null;
    }
    public @Nullable TileEntityRadioTorchLogic logic() { return logic; }
    @Override public boolean stillValid(Player player) {
        return logic != null && !logic.isRemoved() && player.distanceToSqr(logic.getBlockPos().getCenter()) <= 64D;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
