package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerOilburner;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
 * Fluid burner: burns any flammable fluid (default heating oil, 16k tank) at {@code burnRate} mB/t into a
 * heat reservoir polled by the consumer above. Slots: 0/1 fluid container drain pair, 2 fluid identifier,
 * 3 overdrive upgrade (each level adds 10 mB/t of burn rate and 500k TU of capacity). Every fifth burning
 * tick the fuel's burn pollution goes into the smoke tanks, which leave through the four fuel ports.
 */
public class TileEntityHeaterOilburner extends TileEntityMachinePolluting implements MenuProvider, IHeatSource, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive, IUpgradeInfoProvider {

    /** The highest maxBurnRate, at three overdrive levels. */
    public static final int MAX_SLIDER_RATE = 40;

    public boolean isOn = false;
    public FluidTankNTM tank;
    public int burnRate = 1;
    public int maxBurnRate = 10;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public int heatEnergy;
    public int maxHeatEnergy = 500_000;

    public TileEntityHeaterOilburner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_OILBURNER.get(), pos, state, 4, 100);
        tank = new FluidTankNTM(Fluids.HEATINGOIL, 16_000);
    }

    @Override
    public String getDefaultName() {
        return "container.heaterOilburner";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterOilburner te) {
        te.setupFluidPorts(te.getPorts());
        te.updatePortFIFO();

        if (MachineFluidContainerIO.load(te.tank, te.inventory, 0, 1)) te.setChanged();
        if (te.tank.setType(te.inventory, 2, 2)) te.setChanged();

        te.upgradeManager.checkSlots(te.inventory, 3, 3);
        int od = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        te.maxBurnRate = 10 * (od + 1);
        te.maxHeatEnergy = 500_000 * (od + 1);

        te.burnRate = Math.min(te.burnRate, te.maxBurnRate);
        te.heatEnergy = Math.min(te.heatEnergy, te.maxHeatEnergy);

        boolean shouldCool = true;

        if (te.isOn && te.heatEnergy < te.maxHeatEnergy) {
            long energyPerBucket = FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            int toBurn = Math.min(te.burnRate, te.tank.getFill());
            // hbm-space cools with an empty tank; plain HBM holds its heat. Only positive fuel use consumes air.
            if (energyPerBucket > 0 && (toBurn > 0 ? Environment.breatheAir(level, pos, toBurn)
                    : !Environment.coolEmptyOilburner())) {
                te.tank.setFill(te.tank.getFill() - toBurn);

                int heat = (int) (energyPerBucket / 1000);
                te.heatEnergy += heat * toBurn;

                if (TickPhase.every(level, pos, 5L) && toBurn > 0) {
                    te.pollute(te.tank.getTankType(), FluidReleaseType.BURN, toBurn * 5);
                }

                shouldCool = false;
            }
        }

        if (te.heatEnergy >= te.maxHeatEnergy) shouldCool = false;

        if (shouldCool) {
            te.heatEnergy = Math.max(te.heatEnergy - Math.max(te.heatEnergy / 1000, 1), 0);
        }

        te.networkPackNT();
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.flare(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    /** GUI buttons: 0 = burn toggle, 1000 + rate = slider burn rate, clamped to maxBurnRate on the next tick. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (value == 0) this.isOn = !this.isOn;
        else if (value > 1000 && value <= 1000 + MAX_SLIDER_RATE) this.burnRate = value - 1000;
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Slot 1 is the emptied-container out; the menu's take-only slot keeps players out.
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        if (slot == 3) return Library.isMachineUpgrade(stack);
        return true;
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.heater_oilburner.get()));
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>(1);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // --- IHeatSource ---

    @Override public int getHeatStored() { return heatEnergy; }
    @Override public void useUpHeat(int heat) { this.heatEnergy = Math.max(0, this.heatEnergy - heat); }

    // --- IFluidStandardReceiverMK2 ---

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return this.getSmokeTanks(); }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.heaterOilburner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerOilburner(containerId, playerInventory, this);
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeInt(heatEnergy);
        buf.writeInt(maxHeatEnergy);
        buf.writeByte(burnRate);
        buf.writeByte(maxBurnRate);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        isOn = buf.readBoolean();
        heatEnergy = buf.readInt();
        maxHeatEnergy = buf.readInt();
        burnRate = buf.readByte();
        maxBurnRate = buf.readByte();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "tank");
        output.putBoolean("isOn", isOn);
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("setting", burnRate);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "tank");
        isOn = input.getBooleanOr("isOn", false);
        heatEnergy = input.getIntOr("heatEnergy", 0);
        burnRate = Math.max(1, input.getIntOr("setting", 1));
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "heat", PREFIX_VALUE + "fuel", PREFIX_VALUE + "burnrate", PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active",
                PREFIX_FUNCTION + "setburnrate" + NAME_SEPARATOR + "rate"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "burnrate").equals(name)) return "" + burnRate;
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            isOn = IRORInteractive.parseBool(params[0]);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setburnrate").equals(name) && params.length > 0) {
            burnRate = IRORInteractive.parseInt(params[0], 1, maxBurnRate);
            setChanged();
        }
        return null;
    }
}
