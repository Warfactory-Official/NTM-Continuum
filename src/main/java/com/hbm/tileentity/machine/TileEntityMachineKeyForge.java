package com.hbm.tileentity.machine;

import com.hbm.items.tool.ItemKeyPin;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerMachineKeyForge;
import com.hbm.tileentity.ModSecurityBlockEntities;

/**
 * The locksmith table's block entity, port of 1.7.10's {@code TileEntityMachineKeyForge}. Three
 * slots: every server tick the pins of a transferable key/lock in {@link #SLOT_SOURCE} are pasted onto
 * a transferable one in {@link #SLOT_COPY}, and a transferable key/lock in {@link #SLOT_RANDOM} gets a
 * fresh random 100..999 pin configuration. Automation sees slot 0 from the top, 1 from the bottom and
 * 2 from the sides, may extract anything and insert nothing (old {@code isItemValidForSlot = false}).
 */
public class TileEntityMachineKeyForge extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_COPY = 1;
    public static final int SLOT_RANDOM = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] SLOTS_TOP = {SLOT_SOURCE};
    private static final int[] SLOTS_BOTTOM = {SLOT_COPY};
    private static final int[] SLOTS_SIDE = {SLOT_RANDOM};

    public TileEntityMachineKeyForge(BlockPos pos, BlockState state) {
        super(ModSecurityBlockEntities.MACHINE_KEYFORGE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.keyForge";
    }

    /** The old {@code isItemValidForSlot} refused everything — the player slots are unrestricted in the menu itself. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineKeyForge te) {
        ItemStack source = te.inventory.getStackInSlot(SLOT_SOURCE);
        ItemStack copy = te.inventory.getStackInSlot(SLOT_COPY);
        if (!source.isEmpty() && !copy.isEmpty()
                && source.getItem() instanceof ItemKeyPin sourcePin && copy.getItem() instanceof ItemKeyPin copyPin
                && sourcePin.canTransfer() && copyPin.canTransfer()) {
            ItemKeyPin.setPins(copy, ItemKeyPin.pins(source));
        }

        ItemStack random = te.inventory.getStackInSlot(SLOT_RANDOM);
        if (!random.isEmpty() && random.getItem() instanceof ItemKeyPin randomPin && randomPin.canTransfer()) {
            ItemKeyPin.setPins(random, level.getRandom().nextInt(900) + 100);
        }
    }

    // --- WorldlyContainer (old ISidedInventory) ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_BOTTOM : (side == Direction.UP ? SLOTS_TOP : SLOTS_SIDE);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineKeyForge(containerId, playerInventory, this);
    }
}
