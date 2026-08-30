package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.tileentity.machine.TileEntityMachineF6Tank;
import com.hbm.inventory.ModMenus;
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

/** The shared four-slot menu for the UF6 and PuF6 tanks. */
public class ContainerMachineF6Tank extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 4;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineF6Tank tank;

    public ContainerMachineF6Tank(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineF6Tank(int containerId, Inventory playerInventory, TileEntityMachineF6Tank tank) {
        this(containerId, playerInventory, tank, ContainerLevelAccess.create(tank.getLevel(), tank.getBlockPos()));
    }

    private ContainerMachineF6Tank(int containerId, Inventory playerInventory,
                                   @Nullable TileEntityMachineF6Tank tank, ContainerLevelAccess access) {
        super(ModMenus.F6_TANK.get(), containerId);
        this.tank = tank;
        this.machine = tank != null ? tank : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 44, 17));
        this.addSlot(takeOnly(1, 44, 53));
        this.addSlot(validated(2, 116, 17));
        this.addSlot(takeOnly(3, 116, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachineF6Tank.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineF6Tank resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMachineF6Tank tank ? tank : null;
    }

    public @Nullable TileEntityMachineF6Tank getTank() {
        return tank;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockEntity(pos) instanceof TileEntityMachineF6Tank
                        && com.hbm.blocks.BlockDummyable.isWithinInteractionRange(level, pos, player), true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
