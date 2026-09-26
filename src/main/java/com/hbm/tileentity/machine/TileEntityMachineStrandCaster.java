package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.material.MaterialStack;
import com.hbm.foundry.FoundryExpansion;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/** Seven-slot Strand Caster core: one mold, six output slots, four fluid ports, and old batching. */
public class TileEntityMachineStrandCaster extends TileEntityFoundryCastingBase
        implements net.minecraft.world.MenuProvider, WorldlyContainer, IFluidStandardTransceiverMK2 {

    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 64_000);
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.SPENTSTEAM, 64_000);

    private long lastProgressTick;
    /** Synced copies of output slots 1..6; the base class already carries slot 1 as syncOutput. */
    public final ItemStack[] syncOutputs = new ItemStack[6];

    public TileEntityMachineStrandCaster(BlockPos pos, BlockState state) {
        super(FoundryExpansion.STRAND_CASTER_BE.get(), pos, state, 7);
        for (int i = 0; i < syncOutputs.length; i++) syncOutputs[i] = ItemStack.EMPTY;
    }

    public String getDefaultName() { return "container.machineStrandCaster"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }
    @Override public int getMoldSize() { return 0; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineStrandCaster caster) {
        if (caster.type != null && caster.amount > caster.getCapacity()) {
            int excess = caster.amount - caster.getCapacity();
            ItemScraps.spawnMats(level, pos.getX() + 0.5D, pos.getY() + 2D, pos.getZ() + 0.5D,
                    new MaterialStack(caster.type, excess));
            caster.amount = caster.getCapacity();
        }

        if (caster.amount == 0) caster.type = null;
        caster.updateConnections(level);

        int moldsToCast = caster.maxProcessable();
        if (moldsToCast > 0 && (moldsToCast >= 9 || level.getGameTime() >= caster.lastProgressTick + 200L)) {
            ItemMold.Mold mold = caster.getInstalledMold();
            ItemStack output = mold == null || caster.type == null ? ItemStack.EMPTY : mold.getOutput(caster.type);
            if (!output.isEmpty()) {
                caster.amount -= moldsToCast * mold.getCost();

                int initialRemaining = output.getCount() * moldsToCast;
                int remaining = initialRemaining;
                int outputMax = output.getMaxStackSize();
                for (int slot = 1; slot < 7 && remaining > 0; slot++) {
                    ItemStack existing = caster.inventory.getStackInSlot(slot);
                    int limit = Math.min(caster.inventory.getSlotLimit(slot), outputMax);
                    if (existing.isEmpty()) {
                        int toDeposit = Math.min(remaining, limit);
                        if (toDeposit > 0) {
                            caster.inventory.setStackInSlot(slot, output.copyWithCount(toDeposit));
                            remaining -= toDeposit;
                        }
                    } else if (ItemStacks.sameItemAndData(existing, output)) {
                        int toDeposit = Math.min(remaining, limit - existing.getCount());
                        if (toDeposit > 0) {
                            existing.grow(toDeposit);
                            caster.inventory.setStackInSlot(slot, existing);
                            remaining -= toDeposit;
                        }
                    }
                }

                int castsMade = (initialRemaining - remaining) / output.getCount();
                if (castsMade > 0) {
                    int waterCost = caster.getWaterRequired() * castsMade;
                    caster.water.setFill(caster.water.getFill() - waterCost);
                    caster.steam.setFill(caster.steam.getFill() + waterCost);
                    caster.lastProgressTick = level.getGameTime();
                    caster.setChanged();
                }
            }
        }

        caster.syncMold = caster.inventory.getStackInSlot(0).copy();
        for (int i = 0; i < caster.syncOutputs.length; i++) {
            caster.syncOutputs[i] = caster.inventory.getStackInSlot(i + 1).copy();
        }
        caster.syncOutput = caster.syncOutputs[0];
        caster.syncState();
    }

    private void updateConnections(Level level) {
        for (DirPos port : getFluidConPos()) {
            tryProvide(steam, level, port.getPos(), port.getDir().toDirection());
        }
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{water}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{steam}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water, steam}; }

    private int maxProcessable() {
        ItemMold.Mold mold = getInstalledMold();
        if (type == null || mold == null) return 0;
        ItemStack output = mold.getOutput(type);
        if (output.isEmpty() || mold.getCost() <= 0 || getWaterRequired() <= 0) return 0;

        int freeSlots = 0;
        int stackLimit = output.getMaxStackSize();
        for (int slot = 1; slot < 7; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            int limit = Math.min(inventory.getSlotLimit(slot), stackLimit);
            if (existing.isEmpty()) freeSlots += limit;
            else if (ItemStacks.sameItemAndData(existing, output)) freeSlots += Math.max(0, limit - existing.getCount());
        }

        int casts = amount / mold.getCost();
        casts = Math.min(casts, freeSlots / output.getCount());
        casts = Math.min(casts, water.getFill() / getWaterRequired());
        casts = Math.min(casts, (steam.getMaxFill() - steam.getFill()) / getWaterRequired());
        return casts;
    }

    public int getWaterRequired() {
        ItemMold.Mold mold = getInstalledMold();
        return mold == null ? 50 : mold.getCost() * 5;
    }

    @Override
    public int getCapacity() {
        ItemMold.Mold mold = getInstalledMold();
        return mold == null ? 50_000 : mold.getCost() * 10;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ,
                                        ForgeDirection side, MaterialStack stack) {
        if (side != ForgeDirection.UP) return false;
        for (int[] pour : getMetalPourPos()) {
            if (pour[0] == pos.getX() && pour[1] == pos.getY() && pour[2] == pos.getZ()) {
                return standardCheck(level, pos, side, stack);
            }
        }
        return false;
    }

    @Override
    public boolean standardCheck(Level level, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        if (type != null && type != stack.material) return false;
        ItemMold.Mold mold = getInstalledMold();
        int limit = mold == null ? getCapacity() : mold.getCost() * 9;
        return mold != null && amount < limit;
    }

    @Override
    public @Nullable MaterialStack standardAdd(Level level, BlockPos pos, ForgeDirection side,
                                                     MaterialStack stack) {
        type = Mats.of(stack.material);
        ItemMold.Mold mold = getInstalledMold();
        int limit = mold == null ? getCapacity() : mold.getCost() * 9;
        if (stack.amount + amount <= limit) {
            amount += stack.amount;
            setChanged();
            return null;
        }
        int required = Math.max(0, limit - amount);
        amount = limit;
        stack.amount -= required;
        lastProgressTick = level.getGameTime();
        setChanged();
        return stack;
    }

    public DirPos[] getFluidConPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos[]{
                new DirPos(worldPosition.getX() + rot.offsetX * 2 - dir.offsetX, worldPosition.getY(),
                        worldPosition.getZ() + rot.offsetZ * 2 - dir.offsetZ, rot),
                new DirPos(worldPosition.getX() - rot.offsetX - dir.offsetX, worldPosition.getY(),
                        worldPosition.getZ() - rot.offsetZ - dir.offsetZ, rot.getOpposite()),
                new DirPos(worldPosition.getX() + rot.offsetX * 2 - dir.offsetX * 5, worldPosition.getY(),
                        worldPosition.getZ() + rot.offsetZ * 2 - dir.offsetZ * 5, rot),
                new DirPos(worldPosition.getX() - rot.offsetX - dir.offsetX * 5, worldPosition.getY(),
                        worldPosition.getZ() - rot.offsetZ - dir.offsetZ * 5, rot.getOpposite())
        };
    }

    public int[][] getMetalPourPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new int[][]{
                {worldPosition.getX() + rot.offsetX - dir.offsetX, worldPosition.getY() + 2,
                        worldPosition.getZ() + rot.offsetZ - dir.offsetZ},
                {worldPosition.getX() - dir.offsetX, worldPosition.getY() + 2,
                        worldPosition.getZ() - dir.offsetZ},
                {worldPosition.getX() + rot.offsetX, worldPosition.getY() + 2,
                        worldPosition.getZ() + rot.offsetZ},
                {worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ()}
        };
    }

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.hbm.inventory.container.ContainerMachineStrandCaster(id, inventory, this);
    }

    @Override public int getContainerSize() { return 7; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{1, 2, 3, 4, 5, 6}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && stack.getItem() instanceof ItemMold;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 1 && slot < 7;
    }
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof ItemMold;
    }

    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && dev.golem.core.multiblock.MultiblockCore.isWithinInteractionRange(level, worldPosition, player);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        water.serialize(buf);
        steam.serialize(buf);
        for (int i = 1; i < syncOutputs.length; i++) {
            dev.golem.core.world.ItemStacks.writeOptional(buf, syncOutputs[i]);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        water.deserialize(buf);
        steam.deserialize(buf);
        syncOutputs[0] = syncOutput;
        for (int i = 1; i < syncOutputs.length; i++) {
            syncOutputs[i] = dev.golem.core.world.ItemStacks.readOptional(buf);
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        water.writeToNBT(output, "w");
        steam.writeToNBT(output, "s");
        output.putLong("t", lastProgressTick);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        water.readFromNBT(input, "w");
        steam.readFromNBT(input, "s");
        lastProgressTick = input.getLongOr("t", 0L);
    }
}
