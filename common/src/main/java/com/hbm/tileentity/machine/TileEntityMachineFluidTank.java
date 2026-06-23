package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;

public class TileEntityMachineFluidTank extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IClimbable {

    public static final int CAPACITY = 256_000;

    public short mode = 0;
    public static final short MODES = 4;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, CAPACITY);

    protected FluidNode node;
    protected FluidType lastType;

    private boolean climbRegistered = false;
    private AABB ladderAABB;

    public TileEntityMachineFluidTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, 6);
    }

    @Override
    public String getDefaultName() {
        return "container.fluidtank";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFluidTank tank) {
        tank.ensureClimbRegistered();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFluidTank tank) {
        tank.ensureClimbRegistered();
        tank.updateTypeFromIdentifier();
        tank.processCanister(2, 3);
        tank.processCanister(4, 5);
        FluidType type = tank.tank.getTankType();

        if (tank.mode == 1) {

            if (tank.node == null || tank.node.expired || type != tank.lastType) {
                tank.node = (FluidNode) UniNodespace.getNode(level, pos, type.getNetworkProvider());
                if (tank.node == null || tank.node.expired || type != tank.lastType) {
                    tank.node = tank.createNode(type);
                    UniNodespace.createNode(level, tank.node);
                    tank.lastType = type;
                }
            }
            if (tank.node != null && tank.node.hasValidNet()) {
                tank.node.net.addProvider(tank);
                tank.node.net.addReceiver(tank);
            }
        } else {
            if (tank.node != null) {
                UniNodespace.destroyNode(level, pos, type.getNetworkProvider());
                tank.node = null;
            }
            for (DirPos dp : tank.getConPos()) {
                FluidNode dirNode = (FluidNode) UniNodespace.getNode(level, dp.getPos(), type.getNetworkProvider());
                if (tank.mode == 2) {
                    tank.tryProvide(tank.tank, level, dp.getPos(), dp.getDir());
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeProvider(tank);
                }
                if (tank.mode == 0) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(tank);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(tank);
                }
            }
        }

        tank.setChanged();
        tank.networkPackNT();
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    private void updateTypeFromIdentifier() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidIdentifier ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;

        FluidType t = ident.getType(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), id);
        if (t == tank.getTankType()) return;

        tank.setTankType(t);
        if (node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
            node = null;
        }

        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    private void processCanister(int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;
        FluidType tankType = tank.getTankType();

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);

            if (held > 0 && heldType != null && heldType != Fluids.NONE
                    && (tankType == Fluids.NONE || tankType == heldType)) {
                int addable = tank.fill(heldType, held, false);
                if (addable > 0) {
                    ItemStack work = single(in);
                    int taken = fillable.tryEmpty(heldType, addable, work);
                    if (taken > 0 && fits(outSlot, work)) {
                        FluidType before = tank.getTankType();
                        tank.fill(heldType, taken, true);
                        moveToOut(inSlot, outSlot, work);
                        resetNodeOnTypeChange(before);
                        return;
                    }
                }
            }

            if (tankType != Fluids.NONE && tank.getFill() > 0 && fillable.acceptsFluid(tankType, in)) {
                ItemStack work = single(in);
                int remainder = fillable.tryFill(tankType, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved > 0 && fits(outSlot, work)) {
                    tank.setFill(tank.getFill() - moved);
                    moveToOut(inSlot, outSlot, work);
                }
            }
            return;
        }

        FluidType bucketFluid = bucketFluid(in);
        if (bucketFluid != Fluids.NONE && (tankType == Fluids.NONE || tankType == bucketFluid)
                && tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) >= ItemFluidBucket.VOLUME) {

            ItemStack empty = new ItemStack(Items.BUCKET);
            if (fits(outSlot, empty)) {
                FluidType before = tank.getTankType();
                tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
                moveToOut(inSlot, outSlot, empty);
                resetNodeOnTypeChange(before);
            }
        } else if (in.is(Items.BUCKET) && tankType != Fluids.NONE && tank.getFill() >= ItemFluidBucket.VOLUME) {

            ItemStack filled = tankType == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                    : tankType == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                    : ItemFluidBucket.make(tankType);
            if (fits(outSlot, filled)) {
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveToOut(inSlot, outSlot, filled);
            }
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveToOut(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    private void resetNodeOnTypeChange(FluidType before) {
        if (before != tank.getTankType() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), before.getNetworkProvider());
            node = null;
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidIdentifier;
            case 2, 4 -> isFluidContainer(stack);
            default -> true;
        };
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    private void ensureClimbRegistered() {
        if (!climbRegistered) {
            registerClimbable();
            climbRegistered = true;
        }
    }

    private AABB getLadderAABB() {
        if (ladderAABB == null) {
            ForgeDirection dir = ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            BlockPos p = getBlockPos();
            ladderAABB = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1)
                    .move(dir.offsetX * 0.5 - rot.offsetX * 2.25, 0, dir.offsetZ * 0.5 - rot.offsetZ * 2.25);
        }
        return ladderAABB;
    }

    @Override
    public boolean isEntityInClimbAABB(LivingEntity entity) {
        return entity.getBoundingBox().intersects(getLadderAABB());
    }

    public DirPos[] getConPos() {
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z)};
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();
        HashSet<BlockPos> posSet = new HashSet<>();
        posSet.add(getBlockPos());
        for (DirPos dp : conPos) {
            posSet.add(new BlockPos(dp.getPos().getX() - dp.getDir().offsetX,
                    dp.getPos().getY() - dp.getDir().offsetY, dp.getPos().getZ() - dp.getDir().offsetZ));
        }
        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[0])).setConnections(conPos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return (mode == 1 || mode == 2) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return (mode == 0 || mode == 1) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_fluidtank");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineFluidTank(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort("mode", mode);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = (short) input.getShortOr("mode", (short) 0);
        tank.readFromNBT(input, "tank");
    }
}
