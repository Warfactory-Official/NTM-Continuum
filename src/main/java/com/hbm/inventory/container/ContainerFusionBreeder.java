package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Pos-synchronised menu for the fusion breeder. */
public class ContainerFusionBreeder extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityFusionBreeder.SLOTS;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFusionBreeder breeder;

    public ContainerFusionBreeder(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerFusionBreeder(int containerId, Inventory playerInventory, TileEntityFusionBreeder breeder) {
        this(containerId, playerInventory, breeder,
                ContainerLevelAccess.create(breeder.getLevel(), breeder.getBlockPos()));
    }

    private ContainerFusionBreeder(int containerId, Inventory playerInventory, @Nullable TileEntityFusionBreeder breeder,
                                   ContainerLevelAccess access) {
        super(ModMenus.FUSION_BREEDER.get(), containerId);
        this.breeder = breeder;
        this.machine = breeder != null ? breeder : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 26, 72));
        this.addSlot(validated(1, 48, 45));
        this.addSlot(takeOnly(2, 112, 45));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 118);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityFusionBreeder resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFusionBreeder breeder ? breeder : null;
    }

    public @Nullable TileEntityFusionBreeder getBreeder() { return breeder; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.fusion_breeder.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof IItemFluidId) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(ModAbilityToolItems.meteorite_sword_irradiated.get())) {
            // 1.7 transferStackInSlot sent every non-identifier to slot 1 through mergeItemStack, which never asked
            // isItemValid: shift-click was the only way the irradiated sword (no outgasser recipe) got in.
            if (!this.slots.get(1).hasItem()) {
                this.slots.get(1).set(stack.split(1));
            } else {
                return ItemStack.EMPTY;
            }
        } else if (this.machine.canPlaceItem(1, stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INV_START + 27) {
            if (!moveItemStackTo(stack, PLAYER_INV_START + 27, PLAYER_INV_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
