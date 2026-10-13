package com.hbm.inventory.container;

import com.hbm.items.machine.ItemMold;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
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
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;

/** The old seven-slot Strand Caster menu: one mold and six output slots. */
public class ContainerMachineStrandCaster extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineStrandCaster caster;

    public ContainerMachineStrandCaster(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineStrandCaster(int id, Inventory playerInventory, TileEntityMachineStrandCaster caster) {
        this(id, playerInventory, caster, ContainerLevelAccess.create(caster.getLevel(), caster.getBlockPos()));
    }

    private ContainerMachineStrandCaster(int id, Inventory playerInventory,
                                         @Nullable TileEntityMachineStrandCaster caster,
                                         ContainerLevelAccess access) {
        super(ModMenus.MACHINE_STRAND_CASTER.get(), id);
        this.caster = caster;
        this.machine = caster == null ? new SimpleContainer(MACHINE_SLOTS) : caster;
        this.access = access;

        this.addSlot(new Slot(machine, 0, 57, 62) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ItemMold; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 2; col++) {
            int index = 1 + row * 2 + col;
            this.addSlot(new Slot(machine, index, 125 + col * 18, 26 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 132);
    }

    private static @Nullable TileEntityMachineStrandCaster resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMachineStrandCaster caster ? caster : null;
    }

    public @Nullable TileEntityMachineStrandCaster getCaster() { return caster; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_strand_caster.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
