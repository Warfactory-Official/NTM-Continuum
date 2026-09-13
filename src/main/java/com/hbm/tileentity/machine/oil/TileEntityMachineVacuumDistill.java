package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineVacuumDistill;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
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
 * Vacuum Distiller — 1:1 port of the 1.12.2 {@code TileEntityMachineVacuumDistill}: a 3×3×9 tower that
 * splits OIL/OIL_DS into four vacuum fractions. 10 slots (0 battery, 1→2/3→4/5→6/7→8 four output-unload
 * pairs, 9 fluid identifier). Tanks: {@code [0] OIL 64k p2} input (pipe-fed) + 4×24k outputs conformed to
 * {@link RefineryRecipes#getVacuum}. Each op needs {@code power ≥ 10k} and {@code tank0 ≥ 100}, drains
 * 100 mB + 10k HE. maxPower 1M.
 *
 * <p>Deferred (§20): the boiler audio loop, IPersistentNBT/IFluidCopiable.
 */
public class TileEntityMachineVacuumDistill extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final long maxPower = 1_000_000;

    public long power = 0;
    public boolean isOn;
    public FluidTankNTM[] tanks;

    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public TileEntityMachineVacuumDistill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_VACUUM_DISTILL.get(), pos, state, 10);
        tanks = new FluidTankNTM[5];
        tanks[0] = new FluidTankNTM(Fluids.OIL, 64_000).withPressure(2);
        tanks[1] = new FluidTankNTM(Fluids.HEAVYOIL_VACUUM, 24_000);
        tanks[2] = new FluidTankNTM(Fluids.REFORMATE, 24_000);
        tanks[3] = new FluidTankNTM(Fluids.LIGHTOIL_VACUUM, 24_000);
        tanks[4] = new FluidTankNTM(Fluids.SOURGAS, 24_000);
    }

    @Override public String getDefaultName() { return "container.vacuumDistill"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineVacuumDistill te) {
        te.isOn = false;

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromFluidId();
        te.refine();

        te.unloadTank(1, 1, 2);
        te.unloadTank(2, 3, 4);
        te.unloadTank(3, 5, 6);
        te.unloadTank(4, 7, 8);

        for (DirPos dp : te.getConPos()) {
            for (int i = 1; i < 5; i++) {
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

    private void refine() {
        RefineryRecipes.VacuumRecipe recipe = RefineryRecipes.getVacuum(tanks[0].getTankType());
        if (recipe == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(Fluids.NONE);
            return;
        }

        FluidStack[] stacks = recipe.fractions();
        for (int i = 0; i < stacks.length; i++) tanks[i + 1].setTankType(stacks[i].type);

        if (power < 10_000 || tanks[0].getFill() < 100) return;
        for (int i = 0; i < stacks.length; i++) {
            if (tanks[i + 1].getFill() + stacks[i].fill > tanks[i + 1].getMaxFill()) return;
        }

        this.isOn = true;
        power -= 10_000;
        tanks[0].setFill(tanks[0].getFill() - 100);
        for (int i = 0; i < stacks.length; i++) tanks[i + 1].setFill(tanks[i + 1].getFill() + stacks[i].fill);
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

    /** Fills a container in {@code loadSlot} FROM {@code tanks[tank]}, result → {@code outSlot}. */
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

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2], tanks[3], tanks[4]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() { return Component.translatable("container.vacuumDistill"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineVacuumDistill(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        this.isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "heavy");
        tanks[2].writeToNBT(output, "reformate");
        tanks[3].writeToNBT(output, "light");
        tanks[4].writeToNBT(output, "gas");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "heavy");
        tanks[2].readFromNBT(input, "reformate");
        tanks[3].readFromNBT(input, "light");
        tanks[4].readFromNBT(input, "gas");
    }
}
