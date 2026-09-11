package com.hbm.inventory.container;

import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
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

/** Menu for the launch complex: the rocket, its payload, the two propellant bays and a battery. */
public class ContainerSoyuzLauncher extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntitySoyuzLauncher launcher;

    public ContainerSoyuzLauncher(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerSoyuzLauncher(int containerId, Inventory playerInventory, TileEntitySoyuzLauncher launcher) {
        this(containerId, playerInventory, launcher,
                ContainerLevelAccess.create(launcher.getLevel(), launcher.getBlockPos()));
    }

    private ContainerSoyuzLauncher(int containerId, Inventory playerInventory,
                                   @Nullable TileEntitySoyuzLauncher launcher, ContainerLevelAccess access) {
        super(ModMenus.SOYUZ_LAUNCHER.get(), containerId);
        this.launcher = launcher;
        this.machine = launcher != null ? launcher : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(TileEntitySoyuzLauncher.SLOT_ROCKET, 98, 80));
        addSlot(validated(TileEntitySoyuzLauncher.SLOT_SATELLITE, 98, 26));
        addSlot(validated(2, 152, 98));
        addSlot(takeOnly(3, 152, 116));
        addSlot(validated(4, 170, 98));
        addSlot(takeOnly(5, 170, 116));
        addSlot(validated(TileEntitySoyuzLauncher.SLOT_BATTERY, 134, 98));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 17, 162);
    }

    private Slot validated(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(machineSlot, stack);
            }
        };
    }

    private Slot takeOnly(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntitySoyuzLauncher resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntitySoyuzLauncher launcher ? launcher : null;
    }

    public @Nullable TileEntitySoyuzLauncher getLauncher() {
        return launcher;
    }

    /** The GUI's launch button. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || launcher == null) return false;
        launcher.startCountdown();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModSoyuzBlocks.soyuz_launcher.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
