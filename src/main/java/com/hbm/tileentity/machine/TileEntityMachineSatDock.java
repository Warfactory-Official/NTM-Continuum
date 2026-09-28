package com.hbm.tileentity.machine;

import com.hbm.entity.missile.EntityMinerRocket;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.inventory.container.ContainerSatDock;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteMiner;
import com.hbm.tileentity.ModSatelliteBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code TileEntityMachineSatDock}, the "Cargo Landing Pad": fifteen cargo slots and a chip slot (15). With a
 * chip tuned to a mining satellite, a miner rocket is dropped from y=300 every ten minutes; on landing it unloads
 * 10..15 draws from the satellite's pool, and the pad pushes its cargo one item at a time into any inventory two
 * blocks out along the four horizontal axes. A rocket whose frequency is not the chip's detonates on the pad.
 */
public class TileEntityMachineSatDock extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] access = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };

    /** 1.7: {@code 10 * 60 * 1000} ms between launches. */
    public static final int DELAY = 10 * 60 * 1000;

    public TileEntityMachineSatDock(BlockPos pos, BlockState state) {
        super(ModSatelliteBlockEntities.SAT_DOCK.get(), pos, state, 16);
    }

    @Override
    public String getDefaultName() {
        return "container.satDock";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSatDock dock) {
        if (level instanceof ServerLevel server) dock.update(server);
    }

    private void update(ServerLevel level) {
        SatelliteSavedData data = SatelliteSavedData.get(level);
        BlockPos pos = getBlockPos();
        ItemStack chip = inventory.getStackInSlot(15);

        if (!chip.isEmpty()) {
            int freq = ISatChip.getFreqS(chip);

            Satellite sat = data.getSatFromFreq(freq);

            if (sat instanceof SatelliteMiner miner) {
                // 1.7 System.currentTimeMillis(): wall-clock, so a fresh miner (lastOp 0) sends its first rocket
                // at once and pausing a singleplayer game does not hold the next one back
                long now = System.currentTimeMillis();
                if (miner.lastOp + DELAY < now) {
                    EntityMinerRocket rocket = new EntityMinerRocket(level);
                    rocket.setPos(pos.getX() + 0.5, 300, pos.getZ() + 0.5);
                    rocket.setSatellite(freq);
                    level.addFreshEntity(rocket);
                    miner.lastOp = now;
                    data.setDirty();
                }
            }
        }

        List<EntityMinerRocket> list = level.getEntitiesOfClass(EntityMinerRocket.class,
                new AABB(pos.getX() - 0.25 + 0.5, pos.getY() + 0.75, pos.getZ() - 0.25 + 0.5,
                        pos.getX() + 0.25 + 0.5, pos.getY() + 2, pos.getZ() + 0.25 + 0.5));

        for (EntityMinerRocket rocket : list) {
            if (!chip.isEmpty() && ISatChip.getFreqS(chip) != rocket.getSatellite()) {
                rocket.discard();
                ExplosionNukeSmall.explode(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ExplosionNukeSmall.PARAMS_TOTS);
                break;
            }

            if (rocket.getMode() == 1 && rocket.timer == 50) {
                // 1.7 getFreqS(null) answers 0, so an empty pad asks frequency 0
                Satellite sat = data.getSatFromFreq(ISatChip.getFreqS(chip));
                if (sat instanceof SatelliteMiner miner) unloadCargo(level, miner);
            }
        }

        ejectInto(level, pos.offset(2, 0, 0));
        ejectInto(level, pos.offset(-2, 0, 0));
        ejectInto(level, pos.offset(0, 0, 2));
        ejectInto(level, pos.offset(0, 0, -2));
    }

    private void unloadCargo(ServerLevel level, SatelliteMiner satellite) {
        int itemAmount = level.getRandom().nextInt(6) + 10;

        List<ItemPool.Entry> cargo = ItemPool.getPool(satellite.getCargo());

        for (int i = 0; i < itemAmount; i++) {
            addToInv(ItemPool.getStack(cargo, level.getRandom()));
        }
    }

    /**
     * 1.7 {@code addToInv}: tops up matching stacks first; failing that, an empty slot receives ONE item of the
     * draw and the rest of it is lost. Ported as found.
     */
    private void addToInv(ItemStack stack) {
        if (stack.isEmpty()) return;

        for (int i = 0; i < 15; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStacks.sameItemAndData(slot, stack)
                    && slot.getCount() < slot.getMaxStackSize()) {
                int toAdd = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());

                slot.grow(toAdd);
                stack.shrink(toAdd);
                setChanged();

                if (stack.getCount() <= 0) return;
            }
        }

        for (int i = 0; i < 15; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, ItemStacks.copyWithCount(stack, 1));
                return;
            }
        }
    }

    /** 1.7 {@code ejectInto}: moves a single item per call, into a matching stack if one exists, else a free slot. */
    private void ejectInto(ServerLevel level, BlockPos target) {
        BlockEntity te = level.getBlockEntity(target);

        if (te instanceof Container chest) {

            for (int i = 0; i < 15; i++) {
                ItemStack mine = inventory.getStackInSlot(i);
                if (!mine.isEmpty()) {
                    for (int j = 0; j < chest.getContainerSize(); j++) {
                        ItemStack theirs = chest.getItem(j);

                        if (!theirs.isEmpty() && ItemStacks.sameItemAndData(theirs, mine)
                                && theirs.getCount() < theirs.getMaxStackSize()) {
                            mine.shrink(1);
                            if (mine.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
                            theirs.grow(1);
                            chest.setChanged();
                            setChanged();
                            return;
                        }
                    }
                }
            }

            for (int i = 0; i < 15; i++) {
                ItemStack mine = inventory.getStackInSlot(i);
                if (!mine.isEmpty()) {
                    for (int j = 0; j < chest.getContainerSize(); j++) {
                        ItemStack sta = ItemStacks.copyWithCount(mine, 1);

                        if (chest.getItem(j).isEmpty() && chest.canPlaceItem(j, sta)) {
                            mine.shrink(1);
                            if (mine.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
                            chest.setItem(j, sta);
                            setChanged();
                            return;
                        }
                    }
                }
            }
        }
    }

    /** 1.7: only the chip slot accepts automation. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 15;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return access;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return access;
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
        return new ContainerSatDock(containerId, playerInventory, this);
    }
}
