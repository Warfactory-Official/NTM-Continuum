package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineSuperComputer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.module.machine.ModuleMachineSuperComputer;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code TileEntityMachineSuperComputer}. Slots: 0 battery, 1 blueprint, 2-4 inputs, 5-7 outputs. */
public class TileEntityMachineSuperComputer extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer {

    private static final int[] IO_SLOTS = {2, 3, 4, 5, 6, 7};

    public final FluidTankNTM inputTank = new FluidTankNTM(Fluids.NONE, 4_000);
    public final FluidTankNTM outputTank = new FluidTankNTM(Fluids.NONE, 4_000);

    public long power;
    public long maxPower = 100_000;
    public boolean didProcess = false;

    public final ModuleMachineSuperComputer computerModule;
    private PortDef[] cachedPorts;

    public TileEntityMachineSuperComputer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SUPERCOMPUTER.get(), pos, state, 8);
        SuperComputerRecipes.INSTANCE.registerRecipes();
        this.computerModule = new ModuleMachineSuperComputer(0, this, inventory)
                .itemInput(2).itemOutput(5)
                .fluidInput(inputTank).fluidOutput(outputTank);
        this.computerModule.owner(this);
    }

    @Override public String getDefaultName() { return "container.machineSuperComputer"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSuperComputer te) {
        if (te.maxPower <= 0) te.maxPower = 1_000_000;

        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        GenericRecipe recipe = te.computerModule.getRecipe();
        if (recipe != null) te.maxPower = recipe.power * 100;
        te.maxPower = BobMathUtil.max(te.power, te.maxPower, 100_000);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        te.computerModule.update(1D, 1D, true, te.inventory.getStackInSlot(1));
        te.didProcess = te.computerModule.didProcess;
        if (te.computerModule.markDirty) te.setChanged();

        te.networkPackNT();
    }

    /** 1.7 {@code getPorts}: the end of the cable run and both sides of it at 7 and 5 blocks out. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 8, y, z + dir.offsetZ * 8, dir),
                    PortDef.make(x + dir.offsetX * 7 + rot.offsetX, y, z + dir.offsetZ * 7 + rot.offsetZ, rot),
                    PortDef.make(x + dir.offsetX * 7 - rot.offsetX, y, z + dir.offsetZ * 7 - rot.offsetZ, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 5 + rot.offsetX, y, z + dir.offsetZ * 5 + rot.offsetZ, rot),
                    PortDef.make(x + dir.offsetX * 5 - rot.offsetX, y, z + dir.offsetZ * 5 - rot.offsetZ, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        return computerModule.isItemValid(slot, stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2 && slot <= 4 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 5 || computerModule.isSlotClogged(slot); }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {inputTank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {outputTank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {inputTank, outputTank}; }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ContainerMachineSuperComputer(id, inv, this); }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection") && Tags.getIntOr(data, "index", -1) == 0) {
            computerModule.setRecipe(Tags.getStringOr(data, "selection", "null"), false);
            setChanged();
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        computerModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        computerModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inputTank.writeToNBT(output, "i");
        outputTank.writeToNBT(output, "o");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        computerModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        inputTank.readFromNBT(input, "i");
        outputTank.readFromNBT(input, "o");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 100_000L);
        computerModule.load(input);
    }
}
