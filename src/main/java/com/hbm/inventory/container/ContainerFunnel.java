package com.hbm.inventory.container;

import com.hbm.inventory.ModProductionMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineFunnel;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** 1.7 funnel menu: nine inputs, nine crafted outputs, then the player inventory. */
public final class ContainerFunnel extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 18;
    private final Container machine;
    private final @Nullable TileEntityMachineFunnel funnel;

    public ContainerFunnel(int id, Inventory player, BlockPos pos) { this(id, player, resolve(player, pos)); }
    public ContainerFunnel(int id, Inventory player, @Nullable TileEntityMachineFunnel funnel) {
        super(ModProductionMenus.FUNNEL.get(), id);
        this.funnel = funnel;
        this.machine = funnel != null ? funnel : new SimpleContainer(MACHINE_SLOTS);
        for (int i = 0; i < 9; i++) {
            final int index = i;
            addSlot(new Slot(machine, index, 8 + 18 * i, 18) {
                @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
            });
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(machine, i + 9, 8 + 18 * i, 54) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        Compat.addPlayerInventory(this::addSlot, player, 8, 86);
    }

    private static @Nullable TileEntityMachineFunnel resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineFunnel funnel ? funnel : null;
    }

    public @Nullable TileEntityMachineFunnel getFunnel() { return funnel; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || funnel == null) return false;
        funnel.cycleMode();
        return true;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack result = source.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 0, 9, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, result);
        return result;
    }
}
