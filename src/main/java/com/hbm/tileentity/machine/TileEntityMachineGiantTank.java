package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

/** Shared port of the old {@code TileEntityBarrel} behavior used by both giant storage machines. */
public abstract class TileEntityMachineGiantTank extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    private static final int[] SLOTS_TOP = {2};
    private static final int[] SLOTS_BOTTOM = {3, 5};
    private static final int[] SLOTS_SIDE = {4};

    /** 0 = receive, 1 = buffer, 2 = send, 3 = disabled. */
    public short mode;
    public byte lastRedstone;
    public boolean tilted;
    public final FluidTankNTM tankNew;

    protected FluidNode node;
    protected FluidType lastType;

    protected TileEntityMachineGiantTank(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, 6);
        this.tankNew = new FluidTankNTM(Fluids.NONE, capacity);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGiantTank tank) {
        tank.updateTypeFromIdentifier();
        tank.loadTank(2, 3);
        tank.unloadTank(4, 5);

        byte comparator = tank.tankNew.getRedstoneComparatorPower();
        if (comparator != tank.lastRedstone) {
            for (DirPos connection : tank.getConPos()) {
                level.updateNeighborsAt(connection.getPos(), state.getBlock());
            }
            tank.lastRedstone = comparator;
        }

        tank.updateNetwork(level);
        if (tank.tankNew.getFill() > 0 && tank.checkFluidInteraction(level)) return;
        tank.networkPackNT();
        tank.setChanged();
    }

    private void updateNetwork(Level level) {
        FluidType type = tankNew.getTankType();
        if (mode == 1) {
            if (node == null || node.expired || type != lastType) {
                if (node != null) UniNodespace.destroyNode(level, node);
                node = (FluidNode) UniNodespace.getNode(level, getBlockPos(), type.getNetworkProvider());
                if (node == null || node.expired) {
                    node = createNode(type);
                    UniNodespace.createNode(level, node);
                }
                lastType = type;
            }
            if (node != null && node.hasValidNet()) {
                node.net.addProvider(this);
                node.net.addReceiver(this);
            }
            return;
        }

        if (node != null) {
            UniNodespace.destroyNode(level, node);
            node = null;
            lastType = null;
        }

        // Tilted tanks stop external work; buffer mode returned above and still keeps its node alive.
        if (isTilted()) return;

        for (DirPos connection : getConPos()) {
            FluidNode connectionNode = (FluidNode) UniNodespace.getNode(level, connection.getPos(), type.getNetworkProvider());
            if (mode == 2) {
                tryProvide(tankNew, level, connection);
            } else if (connectionNode != null && connectionNode.hasValidNet()) {
                connectionNode.net.removeProvider(this);
            }

            if (connectionNode != null && connectionNode.hasValidNet()) {
                if (mode == 0) connectionNode.net.addReceiver(this);
                else connectionNode.net.removeReceiver(this);
            }
        }
    }

    private void updateTypeFromIdentifier() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty() || !(input.getItem() instanceof IItemFluidId identifier)
                || !inventory.getStackInSlot(1).isEmpty()) return;

        FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), input);
        if (type == tankNew.getTankType()) return;

        setTankType(type);
        inventory.insertItem(1, input.copy(), false);
        inventory.extractItem(0, 1, false);
    }

    /** Retypes the tank and tears down a buffer node before the fluid's nodespace changes. */
    public void setTankType(FluidType type) {
        if (type == null) type = Fluids.NONE;
        if (type == tankNew.getTankType()) return;
        if (node != null && level != null && !level.isClientSide()) {
            UniNodespace.destroyNode(level, node);
            node = null;
            lastType = null;
        }
        tankNew.setTankType(type);
        setChanged();
    }

    /** The directional input-container bay from {@code TileEntityBarrel.loadTank}. */
    protected boolean loadTank(int inputSlot, int outputSlot) {
        if (tankNew.getPressure() != 0) return false;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;

        if (input.getItem() instanceof IFillableItem fillable) {
            FluidType type = fillable.getFirstFluidType(input);
            int held = fillable.getFill(input);
            if (type == null || type == Fluids.NONE || held <= 0) return false;

            ItemStack work = single(input);
            int moved = fillable.tryEmpty(type, Math.min(held, tankNew.getMaxFill() - tankNew.getFill()), work);
            if (moved <= 0) return false;
            boolean wouldEmpty = fillable.getFill(work) <= 0;
            boolean wouldFillTank = tankNew.getFill() + moved >= tankNew.getMaxFill();
            boolean preferOutput = input.getCount() > 1 || wouldEmpty || wouldFillTank;
            boolean toOutput = preferOutput && fits(outputSlot, work);
            if (input.getCount() > 1 && !toOutput) return false;

            tankNew.fill(type, moved, true);
            if (toOutput) {
                inventory.extractItem(inputSlot, 1, false);
                inventory.insertItem(outputSlot, work, false);
            } else {
                inventory.setStackInSlot(inputSlot, work);
            }
            setChanged();
            return true;
        }

        FluidType type = bucketFluid(input);
        if (type == Fluids.NONE || tankNew.fill(type, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return false;
        ItemStack empty = new ItemStack(Items.BUCKET);
        if (!fits(outputSlot, empty)) return false;
        tankNew.fill(type, ItemFluidBucket.VOLUME, true);
        inventory.extractItem(inputSlot, 1, false);
        inventory.insertItem(outputSlot, empty, false);
        setChanged();
        return true;
    }

    /** The directional output-container bay from {@code TileEntityBarrel.unloadTank}. */
    protected boolean unloadTank(int inputSlot, int outputSlot) {
        if (tankNew.getPressure() != 0 || tankNew.getTankType() == Fluids.NONE || tankNew.getFill() <= 0) return false;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;

        if (input.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(tankNew.getTankType(), input)) return false;
            ItemStack work = single(input);
            int remainder = fillable.tryFill(tankNew.getTankType(), tankNew.getFill(), work);
            int moved = tankNew.getFill() - remainder;
            if (moved <= 0) return false;
            boolean full = !fillable.acceptsFluid(tankNew.getTankType(), work);
            boolean emptyTank = moved >= tankNew.getFill();
            boolean preferOutput = input.getCount() > 1 || full || emptyTank;
            boolean toOutput = preferOutput && fits(outputSlot, work);
            if (input.getCount() > 1 && !toOutput) return false;

            tankNew.setFill(tankNew.getFill() - moved);
            if (toOutput) {
                inventory.extractItem(inputSlot, 1, false);
                inventory.insertItem(outputSlot, work, false);
            } else {
                inventory.setStackInSlot(inputSlot, work);
            }
            setChanged();
            return true;
        }

        if (!input.is(Items.BUCKET) || tankNew.getFill() < ItemFluidBucket.VOLUME) return false;
        FluidType type = tankNew.getTankType();
        ItemStack full = type == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                : type == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET) : ItemFluidBucket.make(type);
        if (!fits(outputSlot, full)) return false;
        tankNew.setFill(tankNew.getFill() - ItemFluidBucket.VOLUME);
        inventory.extractItem(inputSlot, 1, false);
        inventory.insertItem(outputSlot, full, false);
        setChanged();
        return true;
    }

    private boolean fits(int slot, ItemStack stack) {
        return inventory.insertItem(slot, stack.copy(), true).isEmpty();
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private boolean isDrainable(ItemStack stack) {
        if (stack.isEmpty() || tankNew.getFill() >= tankNew.getMaxFill()) return false;
        FluidType type;
        int amount;
        if (stack.getItem() instanceof IFillableItem fillable) {
            type = fillable.getFirstFluidType(stack);
            amount = fillable.getFill(stack);
        } else {
            type = bucketFluid(stack);
            amount = ItemFluidBucket.VOLUME;
        }
        return type != null && type != Fluids.NONE && amount > 0
                && (tankNew.getTankType() == Fluids.NONE || tankNew.getTankType() == type)
                && tankNew.fill(type, amount, false) > 0;
    }

    private boolean isFillable(ItemStack stack) {
        return !stack.isEmpty() && ((stack.getItem() instanceof IFillableItem fillable
                && fillable.acceptsFluid(tankNew.getTankType(), stack)) || stack.is(Items.BUCKET))
                && tankNew.getTankType() != Fluids.NONE;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2 -> isDrainable(stack);
            case 4 -> isFillable(stack);
            default -> true;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? SLOTS_TOP : side == Direction.DOWN ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == Direction.UP.ordinal() ? SLOTS_TOP
                : side == Direction.DOWN.ordinal() ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 || slot == 2 || slot == 4 ? isItemValidForSlot(slot, stack) : false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == 3 || slot == 5;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if (isTilted() || mode == 2 || mode == 3 || tankNew.getPressure() != pressure) return 0;
        return type == tankNew.getTankType() ? tankNew.getMaxFill() - tankNew.getFill() : 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        long moved = Math.min(getDemand(type, pressure), amount);
        tankNew.setFill(tankNew.getFill() + (int) moved);
        setChanged();
        return amount - moved;
    }

    protected boolean isTilted() {
        return tilted;
    }

    protected boolean checkFluidInteraction(Level level) {
        return false;
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] connections = getConPos();
        HashSet<BlockPos> positions = new HashSet<>();
        positions.add(getBlockPos());
        for (DirPos connection : connections) {
            BlockPos p = connection.getPos();
            ForgeDirection dir = connection.getDir();
            positions.add(new BlockPos(p.getX() - dir.offsetX, p.getY() - dir.offsetY, p.getZ() - dir.offsetZ));
        }
        return new FluidNode(type.getNetworkProvider(), positions.toArray(new BlockPos[0])).setConnections(connections);
    }

    public abstract DirPos[] getConPos();

    public ForgeDirection getFacing() {
        int meta = getBlockState().hasProperty(BlockDummyable.META)
                ? getBlockState().getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        return ForgeDirection.getOrientation(meta - BlockDummyable.offset);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return mode == 1 || mode == 2 ? new FluidTankNTM[]{tankNew} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return mode == 0 || mode == 1 ? new FluidTankNTM[]{tankNew} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tankNew};
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineGiantTank(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(tilted);
        tankNew.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tilted = buf.readBoolean();
        tankNew.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        output.putBoolean("tilted", tilted);
        tankNew.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = (short) input.getShortOr("mode", (short) 0);
        tilted = input.getBooleanOr("tilted", false);
        tankNew.readFromNBT(input, "tank");
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, node);
            node = null;
        }
        super.setRemoved();
    }
}
