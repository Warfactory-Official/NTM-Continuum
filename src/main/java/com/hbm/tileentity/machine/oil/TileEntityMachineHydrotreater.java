package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineHydrotreater;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.util.TickPhase;
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
 * Hydrotreater: port of 1.7.10's {@code TileEntityMachineHydrotreater}. 11 slots (0 battery, 1 to 2 input
 * container load, 3 to 4 hydrogen container load, deprecated in the 1.7 menu, which hides them because
 * hydrogen needs pressurisation, but still processed by the tile. 5 to 6 and 7 to 8 are two output-unload pairs,
 * 9 fluid identifier, 10 catalyst). Tanks:
 * {@code [0] OIL 64k} + {@code [1] HYDROGEN 64k p1} inputs (both pipe-fed) to {@code [2] OIL_DS 24k} /
 * {@code [3] SOURGAS 24k}. {@link #reform} runs every 2 ticks: needs {@code power ≥ 20k},
 * {@code tank0 ≥ 100}, enough hydrogen, and a {@code catalytic_converter} in slot 10. maxPower 1M.
 * 1.7 has no automation access (the base tile exposes no sides).
 *
 * <p>Deferred: IPersistentNBT/IFluidCopiable.
 */
public class TileEntityMachineHydrotreater extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final long maxPower = 1_000_000;

    public long power = 0;
    public FluidTankNTM[] tanks;

    public static final int SLOT_COUNT = 11;

    public TileEntityMachineHydrotreater(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_HYDROTREATER.get(), pos, state, SLOT_COUNT);
        tanks = new FluidTankNTM[4];
        tanks[0] = new FluidTankNTM(Fluids.OIL, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.HYDROGEN, 64_000).withPressure(1);
        tanks[2] = new FluidTankNTM(Fluids.OIL_DS, 24_000);
        tanks[3] = new FluidTankNTM(Fluids.SOURGAS, 24_000);
    }

    @Override public String getDefaultName() { return "container.hydrotreater"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHydrotreater te) {
        if (level.getGameTime() % 20 == 0) {
            for (DirPos dp : te.getConPos()) {
                te.trySubscribe(level, dp);
                te.trySubscribe(te.tanks[0].getTankType(), level, dp);
                te.trySubscribe(te.tanks[1].getTankType(), level, dp);
            }
        }
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromFluidId();
        te.loadInput(0, 1, 2);
        te.loadInput(1, 3, 4);

        if (TickPhase.every(level, pos, 2)) te.reform();

        te.unloadTank(2, 5, 6);
        te.unloadTank(3, 7, 8);

        for (DirPos dp : te.getConPos()) {
            for (int i = 2; i < 4; i++) {
                if (te.tanks[i].getFill() > 0) te.tryProvide(te.tanks[i], level, dp);
            }
        }

        te.networkPackNT();
    }


    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z)
        };
    }

    private void reform() {
        HydrotreatingRecipes.HydrotreatingRecipe out = HydrotreatingRecipes.getOutput(tanks[0].getTankType());
        if (out == null) {
            tanks[2].setTankType(Fluids.NONE);
            tanks[3].setTankType(Fluids.NONE);
            return;
        }

        tanks[1].withPressure(out.hydrogen().pressure).setTankType(out.hydrogen().type);
        tanks[2].setTankType(out.out1().type);
        tanks[3].setTankType(out.out2().type);

        if (power < 20_000 || tanks[0].getFill() < 100) return;
        if (tanks[1].getFill() < out.hydrogen().fill) return;
        if (inventory.getStackInSlot(10).isEmpty() || inventory.getStackInSlot(10).getItem() != ModItems.catalytic_converter.get()) return;
        if (tanks[2].getFill() + out.out1().fill > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + out.out2().fill > tanks[3].getMaxFill()) return;

        tanks[0].setFill(tanks[0].getFill() - 100);
        tanks[1].setFill(tanks[1].getFill() - out.hydrogen().fill);
        tanks[2].setFill(tanks[2].getFill() + out.out1().fill);
        tanks[3].setFill(tanks[3].getFill() + out.out2().fill);
        power -= 20_000;
    }

    /** 1.7 {@code tanks[0].setType(9, slots)}: a different identifier switches the type and empties the tank. */
    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(9);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            tanks[0].setFill(0);
            setChanged();
        }
    }

    private void loadInput(int tankIndex, int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;
        FluidTankNTM tank = tanks[tankIndex];
        // 1.7 FluidTank.loadTank: a pressurised tank only accepts the infinite barrel.
        if (tank.getPressure() != 0 && !in.is(ModFluidNetItems.fluid_barrel_infinite.get())) return;

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
            if (taken <= 0 || !inventory.insertItem(outSlot, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(inSlot, 1, false);
            inventory.insertItem(outSlot, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(outSlot, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(inSlot, 1, false);
            inventory.insertItem(outSlot, empty, false);
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
        // Container-output slots stay TRUE: the tile writes them via inventory.insertItem, which validates
        // here; the menu's take-only slots block the player.
        return true;
    }

    // 1.7: TileEntityMachineBase exposes no sides, so hoppers/pipes cannot reach the inventory.
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[2], tanks[3]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() { return Component.translatable("container.hydrotreater"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineHydrotreater(containerId, playerInventory, this);
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
        for (int i = 0; i < 4; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        for (int i = 0; i < 4; i++) tanks[i].readFromNBT(input, "t" + i);
    }
}
