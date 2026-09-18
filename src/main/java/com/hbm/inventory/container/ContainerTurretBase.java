package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.ModMachineItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
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

/**
 * Menu shared by every turret: the biometry chip, nine ammo slots and a battery. The layout is
 * 1.12.2's, so the ported {@code gui_turret_*} textures line up unchanged.
 */
public class ContainerTurretBase extends AbstractContainerMenu {

    private final Container turretInventory;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityTurretBaseNT turret;

    public ContainerTurretBase(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerTurretBase(int containerId, Inventory playerInventory, TileEntityTurretBaseNT turret) {
        this(containerId, playerInventory, turret,
                ContainerLevelAccess.create(turret.getLevel(), turret.getBlockPos()));
    }

    private ContainerTurretBase(int containerId, Inventory playerInventory,
                                @Nullable TileEntityTurretBaseNT turret, ContainerLevelAccess access) {
        super(ModMenus.TURRET_BASE.get(), containerId);
        this.turret = turret;
        this.turretInventory = turret != null ? turret : new SimpleContainer(TileEntityTurretBaseNT.SLOT_COUNT);
        this.access = access;

        addSlot(new Slot(turretInventory, TileEntityTurretBaseNT.SLOT_CHIP, 98, 27));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(turretInventory, TileEntityTurretBaseNT.SLOT_AMMO_FIRST + row * 3 + col,
                        80 + col * 18, 63 + row * 18));
            }
        }

        addSlot(new Slot(turretInventory, TileEntityTurretBaseNT.SLOT_BATTERY, 152, 99));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);

        if (turret != null) turret.openInventory();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (turret != null) turret.closeInventory();
    }

    private static @Nullable TileEntityTurretBaseNT resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityTurretBaseNT turret ? turret : null;
    }

    public @Nullable TileEntityTurretBaseNT turret() {
        return turret;
    }

    public long getPower() {
        return turret != null ? turret.getPower() : 0L;
    }

    public long getMaxPower() {
        return turret != null ? turret.getMaxPower() : 1L;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockEntity(pos) instanceof TileEntityTurretBaseNT
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true);
    }

    /**
     * 1.12.2 routes this through a {@code TransferStrategy}: the chip goes to slot 0, a battery to
     * slot 10, and everything else falls through to the ammo block.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int turretEnd = TileEntityTurretBaseNT.SLOT_COUNT;

        if (index < turretEnd) {
            if (!moveItemStackTo(stack, turretEnd, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModMachineItems.turret_chip.get())) {
            if (!moveItemStackTo(stack, TileEntityTurretBaseNT.SLOT_CHIP,
                    TileEntityTurretBaseNT.SLOT_AMMO_FIRST, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Library.isBattery(stack)) {
            if (!moveItemStackTo(stack, TileEntityTurretBaseNT.SLOT_BATTERY, turretEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                TileEntityTurretBaseNT.SLOT_BATTERY, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
