package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.client.sound.LargeTurbineSoundHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

/** Logic for the legacy four-long steam turbine. */
public class TileEntityMachineLargeTurbine extends TileEntityMachineBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, net.minecraft.world.WorldlyContainer, MenuProvider {

    public static final long MAX_POWER = 100_000_000L;
    public static final double EFFICIENCY = 1D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.STEAM, 512_000),
            new FluidTankNTM(Fluids.SPENTSTEAM, 10_240_000)
    };
    public long power;
    public int age;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration;
    public boolean shouldTurn;

    private final float audioDesync;

    public TileEntityMachineLargeTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_LARGE_TURBINE.get(), pos, state, 7);
        audioDesync = new java.util.Random().nextFloat() * 0.05F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLargeTurbine turbine) {
        turbine.age++;
        if (turbine.age >= 2) turbine.age = 0;

        ForgeDirection dir = turbine.getFacing();
        BlockPos powerPos = turbine.getBlockPos().offset(-dir.offsetX * 4, 0, -dir.offsetZ * 4);
        turbine.tryProvide(level, powerPos, dir.getOpposite());
        for (DirPos connection : turbine.getConPos()) {
            turbine.tryProvide(turbine.tanks[1], level, connection);
        }

        turbine.updateTypeFromFluidId();
        turbine.processCanister(turbine.tanks[0], 2, 3);
        turbine.power = Library.chargeItemsFromTE(turbine.inventory, 4, turbine.power, MAX_POWER);
        turbine.convert();
        if (turbine.power > MAX_POWER) turbine.power = MAX_POWER;
        turbine.processCanister(turbine.tanks[1], 5, 6);
        turbine.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLargeTurbine turbine) {
        turbine.lastRotor = turbine.rotor;
        turbine.rotor += turbine.fanAcceleration;
        if (turbine.rotor >= 360F) {
            turbine.rotor -= 360F;
            turbine.lastRotor -= 360F;
        }

        if (turbine.shouldTurn) {
            turbine.fanAcceleration = Math.max(0F, Math.min(15F,
                    turbine.fanAcceleration + 0.075F + turbine.audioDesync));
        } else {
            turbine.fanAcceleration = Math.max(0F, Math.min(15F, turbine.fanAcceleration - 0.1F));
        }
        LargeTurbineSoundHandler.tickLoop(turbine);
    }

    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;

        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type == tanks[0].getTankType()) return;
        tanks[0].setTankType(type);
        tanks[1].setTankType(Fluids.NONE);
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    private void convert() {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(tanks[0].getTankType());
        if (recipe == null || EFFICIENCY <= 0D) {
            tanks[1].setTankType(Fluids.NONE);
            return;
        }

        tanks[1].setTankType(recipe.output());
        int inputOps = tanks[0].getFill() / recipe.inputAmount();
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / recipe.outputAmount();
        int cap = (int) Math.ceil((double) tanks[0].getFill() / recipe.inputAmount() / 5F);
        int ops = Math.min(inputOps, Math.min(outputOps, cap));
        tanks[0].setFill(tanks[0].getFill() - ops * recipe.inputAmount());
        tanks[1].setFill(tanks[1].getFill() + ops * recipe.outputAmount());
        power += (long) (ops * recipe.heatEnergy() * EFFICIENCY);
        shouldTurn = ops > 0;
    }

    private void processCanister(FluidTankNTM tank, int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;

        FluidType tankType = tank.getTankType();
        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held > 0 && heldType != null && heldType != Fluids.NONE && tankType == heldType) {
                int addable = tank.fill(heldType, held, false);
                if (addable > 0) {
                    ItemStack work = single(in);
                    int taken = fillable.tryEmpty(heldType, addable, work);
                    if (taken > 0 && fits(outSlot, work)) {
                        tank.fill(heldType, taken, true);
                        moveToOut(inSlot, outSlot, work);
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
        if (bucketFluid != Fluids.NONE && tankType == bucketFluid
                && tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) >= ItemFluidBucket.VOLUME) {
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (fits(outSlot, empty)) {
                tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
                moveToOut(inSlot, outSlot, empty);
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

    private boolean fits(int slot, ItemStack stack) {
        return inventory.insertItem(slot, stack.copy(), true).isEmpty();
    }

    private void moveToOut(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2, 5 -> isFluidContainer(stack);
            case 4 -> Library.isBattery(stack);
            default -> true;
        };
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    public DirPos[] getConPos() {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos p = getBlockPos();
        return new DirPos[]{
                new DirPos(p.getX() + rot.offsetX * 2, p.getY(), p.getZ() + rot.offsetZ * 2, rot),
                new DirPos(p.getX() - rot.offsetX * 2, p.getY(), p.getZ() - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(p.getX() + dir.offsetX * 2, p.getY(), p.getZ() + dir.offsetZ * 2, dir)
        };
    }

    public DirPos[] getPowerPos() {
        ForgeDirection dir = getFacing();
        BlockPos p = getBlockPos();
        return new DirPos[]{new DirPos(p.getX() - dir.offsetX * 4, p.getY(),
                p.getZ() - dir.offsetZ * 4, dir.getOpposite())};
    }

    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[]{0, 2, 4, 5}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 1 || slot == 3 || slot == 6;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = Math.max(0L, Math.min(MAX_POWER, value)); }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.hbm.inventory.container.ContainerMachineLargeTurbine(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(shouldTurn);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        shouldTurn = buf.readBoolean();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "steam");
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "steam");
        power = input.getLongOr("power", 0L);
    }

    public long getPowerScaled(int size) {
        return power * size / MAX_POWER;
    }

    public float getAudioDesync() {
        return audioDesync;
    }

    public float getRotor(float partialTicks) {
        return lastRotor + (rotor - lastRotor) * partialTicks;
    }

    @Override
    public String getDefaultName() {
        return "container.machineLargeTurbine";
    }
}
