package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.client.sound.ChemPlantSoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineChemplant;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TileEntityMachineChemicalPlant extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer {

    private static final int[] IO_SLOTS = {4, 5, 6, 7, 8, 9};

    public long power;
    public long maxPower = 100_000;
    public boolean didProcess = false;

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[3];

    public int anim;
    public int prevAnim;

    public final ModuleMachineChemplant chemplantModule;

    public TileEntityMachineChemicalPlant(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), pos, state, 22);
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000);
        }
        this.chemplantModule = new ModuleMachineChemplant(0, this, this.inventory)
                .itemInput(4, 5, 6).itemOutput(7, 8, 9)
                .fluidInput(inputTanks[0], inputTanks[1], inputTanks[2])
                .fluidOutput(outputTanks[0], outputTanks[1], outputTanks[2]);
    }

    @Override
    public String getDefaultName() {
        return "container.machine_chemical_plant";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalPlant te) {

        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            for (FluidTankNTM tank : te.inputTanks) if (tank.getTankType() != Fluids.NONE) te.trySubscribe(tank.getTankType(), level, dp);
            for (FluidTankNTM tank : te.outputTanks) if (tank.getFill() > 0) te.tryProvide(tank, level, dp);
        }
        te.fillInputTanks();
        te.unloadOutputTanks();

        ItemStack blueprint = te.inventory.getStackInSlot(1);
        te.chemplantModule.update(1.0D, 1.0D, true, blueprint);
        te.didProcess = te.chemplantModule.didProcess;

        if (te.chemplantModule.markDirty) te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalPlant te) {
        ChemPlantSoundHandler.tickLoop(te);
        te.prevAnim = te.anim;
        if (te.didProcess) te.anim++;
    }

    private void fillInputTanks() {
        for (int i = 0; i < 3; i++) {
            ItemStack in = inventory.getStackInSlot(10 + i);
            if (in.isEmpty()) continue;
            FluidTankNTM tank = inputTanks[i];

            if (in.getItem() instanceof IFillableItem fillable) {
                int held = fillable.getFill(in);
                FluidType heldType = fillable.getFirstFluidType(in);
                if (held <= 0 || heldType == Fluids.NONE) continue;
                int addable = tank.fill(heldType, held, false);
                if (addable <= 0) continue;
                ItemStack work = single(in);
                int taken = fillable.tryEmpty(heldType, addable, work);
                if (taken <= 0 || !fits(13 + i, work)) continue;
                tank.fill(heldType, taken, true);
                moveResult(10 + i, 13 + i, work);
            } else {
                FluidType fluid = bucketFluid(in);
                if (fluid == Fluids.NONE) continue;
                if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) continue;
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!fits(13 + i, empty)) continue;
                tank.fill(fluid, ItemFluidBucket.VOLUME, true);
                moveResult(10 + i, 13 + i, empty);
            }
        }
    }

    private void unloadOutputTanks() {
        for (int i = 0; i < 3; i++) {
            FluidTankNTM tank = outputTanks[i];
            FluidType fluid = tank.getTankType();
            if (fluid == Fluids.NONE || tank.getFill() <= 0) continue;
            ItemStack in = inventory.getStackInSlot(16 + i);
            if (in.isEmpty()) continue;

            if (in.getItem() instanceof IFillableItem fillable) {
                if (!fillable.acceptsFluid(fluid, in)) continue;
                ItemStack work = single(in);
                int remainder = fillable.tryFill(fluid, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved <= 0 || !fits(19 + i, work)) continue;
                tank.setFill(tank.getFill() - moved);
                moveResult(16 + i, 19 + i, work);
            } else if (in.is(Items.BUCKET) && tank.getFill() >= ItemFluidBucket.VOLUME) {
                ItemStack full = fluid == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                        : fluid == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                        : ItemFluidBucket.make(fluid);
                if (!fits(19 + i, full)) continue;
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveResult(16 + i, 19 + i, full);
            }
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveResult(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z),
        };
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= 2 && slot <= 3) return false;
        if (slot >= 7 && slot <= 9) return false;
        if (slot >= 10 && slot <= 12) return isFluidContainer(stack);
        if (slot >= 13 && slot <= 15) return true;
        if (slot >= 16 && slot <= 18) return isFluidContainer(stack);
        if (slot >= 19 && slot <= 21) return true;
        return chemplantModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1));
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 4 && slot <= 6 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 7 && slot <= 9; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0], outputTanks[1], outputTanks[2]};
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) == 0) {
                this.chemplantModule.recipe = data.getStringOr("selection", "null");
                this.chemplantModule.progress = 0D;
                setChanged();
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_chemical_plant");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineChemicalPlant(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        chemplantModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        chemplantModule.deserialize(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 3; i++) {
            inputTanks[i].writeToNBT(output, "i" + i);
            outputTanks[i].writeToNBT(output, "o" + i);
        }
        output.putLong("power", power);
        chemplantModule.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 3; i++) {
            inputTanks[i].readFromNBT(input, "i" + i);
            outputTanks[i].readFromNBT(input, "o" + i);
        }
        power = input.getLongOr("power", 0L);
        chemplantModule.load(input);
    }
}
