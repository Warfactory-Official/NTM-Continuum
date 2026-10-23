package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

/** Mode-specific menu views over the electrolyser's shared 21-slot inventory. */
public final class ContainerMachineElectrolyser extends AbstractContainerMenu {

    public static final int MODE_FLUID = 0;
    public static final int MODE_METAL = 1;
    public static final int FLUID_MACHINE_SLOTS = 14;
    public static final int METAL_MACHINE_SLOTS = 10;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineElectrolyser electrolyser;
    private final boolean metal;
    private final int machineSlotCount;

    public ContainerMachineElectrolyser(int containerId, Inventory playerInventory, BlockPos pos, boolean metal) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos), metal);
    }

    public ContainerMachineElectrolyser(int containerId, Inventory playerInventory,
                                        TileEntityMachineElectrolyser electrolyser, boolean metal) {
        this(containerId, playerInventory, electrolyser,
                ContainerLevelAccess.create(electrolyser.getLevel(), electrolyser.getBlockPos()), metal);
    }

    private ContainerMachineElectrolyser(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityMachineElectrolyser electrolyser,
                                        ContainerLevelAccess access, boolean metal) {
        super(metal ? ModMenus.MACHINE_ELECTROLYSER_METAL.get() : ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), containerId);
        this.electrolyser = electrolyser;
        this.machine = electrolyser != null ? electrolyser : new SimpleContainer(21);
        this.access = access;
        this.metal = metal;
        this.machineSlotCount = metal ? METAL_MACHINE_SLOTS : FLUID_MACHINE_SLOTS;

        if (metal) addMetalSlots();
        else addFluidSlots();
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private void addFluidSlots() {
        addSlot(validated(0, 186, 109));
        addSlot(validated(1, 186, 140));
        addSlot(validated(2, 186, 158));
        addSlot(validated(3, 6, 18));
        addSlot(takeOnly(4, 6, 54));
        addSlot(validated(5, 24, 18));
        addSlot(takeOnly(6, 24, 54));
        addSlot(validated(7, 78, 18));
        addSlot(takeOnly(8, 78, 54));
        addSlot(validated(9, 134, 18));
        addSlot(takeOnly(10, 134, 54));
        addSlot(takeOnly(11, 154, 18));
        addSlot(takeOnly(12, 154, 36));
        addSlot(takeOnly(13, 154, 54));
    }

    private void addMetalSlots() {
        addSlot(validated(0, 186, 109));
        addSlot(validated(1, 186, 140));
        addSlot(validated(2, 186, 158));
        addSlot(validated(14, 10, 22));
        addSlot(takeOnly(15, 136, 18));
        addSlot(takeOnly(16, 154, 18));
        addSlot(takeOnly(17, 136, 36));
        addSlot(takeOnly(18, 154, 36));
        addSlot(takeOnly(19, 136, 54));
        addSlot(takeOnly(20, 154, 54));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return electrolyser != null ? electrolyser.isItemValidForSlot(index, stack) : machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineElectrolyser resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineElectrolyser electrolyser ? electrolyser : null;
    }

    public @Nullable TileEntityMachineElectrolyser getElectrolyser() { return electrolyser; }
    public boolean isMetal() { return metal; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_electrolyser.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < machineSlotCount) {
                if (!moveItemStackTo(stack, machineSlotCount, machineSlotCount + 36, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 1, 3, false)) return ItemStack.EMPTY;
            } else if (metal) {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (electrolyser == null || !(player instanceof ServerPlayer serverPlayer)
                || !electrolyser.hasPermission(player)) return false;
        if (id != MODE_FLUID && id != MODE_METAL) return false;
        boolean targetMetal = id == MODE_METAL;
        if (targetMetal == metal) return false;
        electrolyser.lastSelectedGUI = targetMetal ? 1 : 0;
        Menus.openWithPos(serverPlayer, electrolyser, electrolyser.getBlockPos());
        return true;
    }
}
