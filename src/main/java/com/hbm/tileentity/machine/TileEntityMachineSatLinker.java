package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerMachineSatLinker;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.ModSatelliteBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMachineSatLinker}, the "Satellite ID Manager": three slots. Every tick, a chip in slot 1
 * takes the frequency of the chip in slot 0, and a chip in slot 2 is given a random frequency below 100,000
 * that no satellite is using (re-rolled every tick, so it keeps changing until taken out).
 */
public class TileEntityMachineSatLinker extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] slots_top = new int[] {0};
    private static final int[] slots_bottom = new int[] {1};
    private static final int[] slots_side = new int[] {2};

    public TileEntityMachineSatLinker(BlockPos pos, BlockState state) {
        super(ModSatelliteBlockEntities.MACHINE_SATLINKER.get(), pos, state, 3);
    }

    @Override
    public String getDefaultName() {
        return "container.satLinker";
    }

    /** A listener copying its own data from the source chip to the copy; true where it changed the copy. */
    @FunctionalInterface
    public interface ChipCopy {
        boolean copy(ItemStack source, ItemStack copy);
    }

    /**
     * Run every tick both chip slots hold a chip, after the frequency is copied: hbm-space also copied a satellite's
     * orbit settings from one satellite item to the other ({@code SatelliteBase.copyItemData}). Every listener runs.
     */
    public static final dev.golem.core.hook.Hook<ChipCopy> CHIP_COPY = dev.golem.core.hook.Hook.of(listeners -> (source, copy) -> {
        boolean changed = false;
        for (ChipCopy listener : listeners) changed |= listener.copy(source, copy);
        return changed;
    });

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSatLinker linker) {
        ItemStack source = linker.inventory.getStackInSlot(0);
        ItemStack copy = linker.inventory.getStackInSlot(1);
        if (!source.isEmpty() && !copy.isEmpty() && source.getItem() instanceof ISatChip
                && copy.getItem() instanceof ISatChip) {
            int freq = ISatChip.getFreqS(source);
            if (ISatChip.getFreqS(copy) != freq) {
                ISatChip.setFreqS(copy, freq);
                linker.setChanged();
            }
            if (CHIP_COPY.invoker().copy(source, copy)) linker.setChanged();
        }

        ItemStack random = linker.inventory.getStackInSlot(2);
        if (!random.isEmpty() && random.getItem() instanceof ISatChip && level instanceof ServerLevel server) {
            SatelliteSavedData satelliteData = SatelliteSavedData.get(server, linker.worldPosition); // hbm-space getData(world, x, z)
            int newId = level.getRandom().nextInt(100000);
            if (!satelliteData.isFreqTaken(newId)) {
                ISatChip.setFreqS(random, newId);
                linker.setChanged();
            }
        }
    }

    /** 1.7: nothing may be piped in; everything may be piped out. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? slots_bottom : (side == 1 ? slots_top : slots_side);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getAccessibleSlotsFromSide(side.get3DDataValue());
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineSatLinker(containerId, playerInventory, this);
    }
}
