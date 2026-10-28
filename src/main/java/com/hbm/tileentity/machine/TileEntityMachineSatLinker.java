package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerMachineSatLinker;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.ModSatelliteBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.hook.Hook;
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
 * takes the frequency of the chip in slot 0, and a chip in slot 2 is given a random frequency below
 * {@link #SAT_ID_RANGE} that no satellite is using (re-rolled every tick, so it keeps changing until taken out).
 * Automation reaches slot 1 only: it puts chips in, and takes one out once it matches slot 0.
 */
public class TileEntityMachineSatLinker extends TileEntityMachineBase
        implements MenuProvider, WorldlyContainer, IRORValueProvider, IRORInteractive {

    public static final int SAT_ID_RANGE = 10_000_000;

    private static final int[] ACCESS = new int[] {1};

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
    public static final Hook<ChipCopy> CHIP_COPY = Hook.of(listeners -> (source, copy) -> {
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
            int newId = level.getRandom().nextInt(SAT_ID_RANGE);
            if (!satelliteData.isFreqTaken(newId)) {
                ISatChip.setFreqS(random, newId);
                linker.setChanged();
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ISatChip;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
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
        return ISatChip.getFreqS(inventory.getStackInSlot(0)) == ISatChip.getFreqS(inventory.getStackInSlot(1));
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "freq",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "freq").equals(name)) return "" + ISatChip.getFreqS(inventory.getStackInSlot(0));
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setfreq").equals(name) && params.length == 1) {
            ISatChip.setFreqS(inventory.getStackInSlot(0), IRORInteractive.parseInt(params[0], 1, SAT_ID_RANGE));
            setChanged();
        }
        return null;
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
