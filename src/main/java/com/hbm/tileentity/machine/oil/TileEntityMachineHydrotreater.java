package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineHydrotreater;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

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
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        if (te.tanks[0].setType(te.inventory, 9, 9)) te.setChanged();
        MachineFluidContainerIO.load(te.tanks[0], te.inventory, 1, 2);
        MachineFluidContainerIO.load(te.tanks[1], te.inventory, 3, 4);

        if (TickPhase.every(level, pos, 2)) te.reform();

        MachineFluidContainerIO.unload(te.tanks[2], te.inventory, 5, 6);
        MachineFluidContainerIO.unload(te.tanks[3], te.inventory, 7, 8);

        te.networkPackNT();
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.refinery(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

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
