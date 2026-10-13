package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineCatalyticReformer;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
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
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Catalytic Reformer: 1:1 port of the 1.12.2 {@code TileEntityMachineCatalyticReformer}. 11 slots
 * (0 battery, 1 to 2 input container load, 3 to 4/5 to 6/7 to 8 three output-unload pairs, 9 fluid identifier,
 * 10 catalyst). Tanks: {@code [0] NAPHTHA 64k} input + REFORMATE/PETROLEUM/HYDROGEN 24k outputs. Each op
 * needs {@code power ≥ 20k}, {@code tank0 ≥ 100} and a {@code catalytic_converter} in slot 10; drains
 * 100 mB + 20k HE. maxPower 1M.
 *
 * <p>Not ported (IMPLEMENTATION.md §20): IPersistentNBT/IFluidCopiable.
 */
public class TileEntityMachineCatalyticReformer extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final long maxPower = 1_000_000;

    public long power = 0;
    public FluidTankNTM[] tanks;

    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public TileEntityMachineCatalyticReformer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CATALYTIC_REFORMER.get(), pos, state, 11);
        tanks = new FluidTankNTM[4];
        tanks[0] = new FluidTankNTM(Fluids.NAPHTHA, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.REFORMATE, 24_000);
        tanks[2] = new FluidTankNTM(Fluids.PETROLEUM, 24_000);
        tanks[3] = new FluidTankNTM(Fluids.HYDROGEN, 24_000);
    }

    @Override public String getDefaultName() { return "container.catalyticReformer"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCatalyticReformer te) {
        if (level.getGameTime() % 20 == 0) {
            for (DirPos dp : te.getConPos()) {
                te.trySubscribe(level, dp);
                te.trySubscribe(te.tanks[0].getTankType(), level, dp);
            }
        }
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromFluidId();
        te.loadInput();

        te.reform();

        te.unloadTank(1, 3, 4);
        te.unloadTank(2, 5, 6);
        te.unloadTank(3, 7, 8);

        for (DirPos dp : te.getConPos()) {
            for (int i = 1; i < 4; i++) {
                if (te.tanks[i].getFill() > 0) te.tryProvide(te.tanks[i], level, dp);
            }
        }

        te.networkPackNT();
    }


    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ * 2 + rot.offsetZ, dir),
                new DirPos(x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ * 2 - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
                new DirPos(x + rot.offsetX * 3, y, z + rot.offsetZ * 3, rot),
                new DirPos(x - rot.offsetX * 3, y, z - rot.offsetZ * 3, rot.getOpposite())
        };
    }

    private void reform() {
        ReformingRecipes.ReformingRecipe out = ReformingRecipes.getOutput(tanks[0].getTankType());
        if (out == null) {
            tanks[1].setTankType(Fluids.NONE);
            tanks[2].setTankType(Fluids.NONE);
            tanks[3].setTankType(Fluids.NONE);
            return;
        }

        tanks[1].setTankType(out.out1().type);
        tanks[2].setTankType(out.out2().type);
        tanks[3].setTankType(out.out3().type);

        if (power < 20_000 || tanks[0].getFill() < 100) return;
        if (inventory.getStackInSlot(10).isEmpty() || inventory.getStackInSlot(10).getItem() != ModItems.catalytic_converter.get()) return;
        if (tanks[1].getFill() + out.out1().fill > tanks[1].getMaxFill()) return;
        if (tanks[2].getFill() + out.out2().fill > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + out.out3().fill > tanks[3].getMaxFill()) return;

        tanks[0].setFill(tanks[0].getFill() - 100);
        tanks[1].setFill(tanks[1].getFill() + out.out1().fill);
        tanks[2].setFill(tanks[2].getFill() + out.out2().fill);
        tanks[3].setFill(tanks[3].getFill() + out.out3().fill);
        power -= 20_000;
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(9);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            setChanged();
        }
    }

    /** Drains a fluid container in slot 1 INTO tank 0, emptied container goes to slot 2. */
    private void loadInput() {
        ItemStack in = inventory.getStackInSlot(1);
        if (in.isEmpty()) return;
        FluidTankNTM tank = tanks[0];

        // 1.7 FluidLoaderInfinite.emptyItem: an infinite item feeds the tank from its slot and is never moved or consumed
        if (MachineFluidContainerIO.loadInfinite(tank, in)) {
            setChanged();
            return;
        }
        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(2, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(2, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, empty, false);
            setChanged();
        }
    }

    private void unloadTank(int tank, int loadSlot, int outSlot) {
        FluidTankNTM t = tanks[tank];
        FluidType fluid = t.getTankType();
        if (fluid == Fluids.NONE || t.getFill() <= 0) return;
        ItemStack in = inventory.getStackInSlot(loadSlot);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(fluid, in)) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int remainder = fillable.tryFill(fluid, t.getFill(), work);
            int moved = t.getFill() - remainder;
            if (moved <= 0 || !inventory.insertItem(outSlot, work.copy(), true).isEmpty()) return;
            t.setFill(t.getFill() - moved);
            inventory.extractItem(loadSlot, 1, false);
            inventory.insertItem(outSlot, work, false);
            setChanged();
        } else if (in.is(Items.BUCKET) && t.getFill() >= ItemFluidBucket.VOLUME) {
            ItemStack full = ItemFluidBucket.make(fluid);
            if (!inventory.insertItem(outSlot, full.copy(), true).isEmpty()) return;
            t.setFill(t.getFill() - ItemFluidBucket.VOLUME);
            inventory.extractItem(loadSlot, 1, false);
            inventory.insertItem(outSlot, full, false);
            setChanged();
        }
    }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 9) return stack.getItem() instanceof IItemFluidId;
        if (slot == 10) return stack.getItem() == ModItems.catalytic_converter.get();
        return true;
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || slot == 4 || slot == 6 || slot == 8;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2], tanks[3]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() { return Component.translatable("container.catalyticReformer"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineCatalyticReformer(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "o1");
        tanks[2].writeToNBT(output, "o2");
        tanks[3].writeToNBT(output, "o3");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "o1");
        tanks[2].readFromNBT(input, "o2");
        tanks[3].readFromNBT(input, "o3");
    }
}
