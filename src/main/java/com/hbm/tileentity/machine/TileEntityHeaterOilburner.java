package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerOilburner;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
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

/**
 * Oil-Fired Heater, a 1:1 port of the 1.12.2 {@code TileEntityHeaterOilburner}: burns any
 * {@code FT_Flammable} fluid (default HEATINGOIL, 16k tank) at 1-100 mB/t ({@code setting}) into a
 * 100k TU {@link IHeatSource} reservoir; heat is polled by the consumer above (boiler/crucible).
 * 3 slots: 0 to 1 fluid container drain pair, 2 fluid identifier (types the tank). Passively cools
 * ({@code max(heat/1000, 1)}) when not burning and below cap. isOn toggles via the GUI burn button;
 * the mB/t setting wraps 1 to 100 in both directions (the 1.12.2 screwdriver IToolable is GUI clicks here, a
 * documented deviation). Every fifth burning tick the fuel's {@code FT_Polluting} burn table goes into the 100 mB smoke
 * tanks, which leave through the four fuel ports.
 */
public class TileEntityHeaterOilburner extends TileEntityMachinePolluting implements MenuProvider, IHeatSource, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public static final int maxHeatEnergy = 100_000;
    public boolean isOn = false;
    public FluidTankNTM tank;
    public int setting = 1;
    public int heatEnergy;

    public TileEntityHeaterOilburner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_OILBURNER.get(), pos, state, 3, 100);
        tank = new FluidTankNTM(Fluids.HEATINGOIL, 16_000);
    }

    @Override
    public String getDefaultName() {
        return "container.heaterOilburner";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterOilburner te) {
        if (MachineFluidContainerIO.load(te.tank, te.inventory, 0, 1)) te.setChanged();
        te.setTypeFromFluidId();

        for (DirPos connection : te.getConPos()) {
            te.trySubscribe(te.tank.getTankType(), level, connection);
            te.sendSmoke(connection);
        }

        boolean shouldCool = true;

        if (te.isOn && te.heatEnergy < maxHeatEnergy) {
            long energyPerBucket = FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            int toBurn = Math.min(te.setting, te.tank.getFill());
            // hbm-space breatheAir(toBurn): without air nothing burns and the heater cools
            // (com.hbm.api.environment.Environment). hbm-space also cooled with an empty tank (toBurn 0); that is
            // not 1.7, so an empty tank still holds its heat here.
            if (energyPerBucket > 0 && (toBurn <= 0 || Environment.breatheAir(level, pos, toBurn))) {
                te.tank.setFill(te.tank.getFill() - toBurn);

                int heat = (int) (energyPerBucket / 1000);
                te.heatEnergy += heat * toBurn;

                if (TickPhase.every(level, pos, 5L) && toBurn > 0) {
                    te.pollute(te.tank.getTankType(), FluidReleaseType.BURN, toBurn * 5);
                }

                shouldCool = false;
            }
        }

        if (te.heatEnergy >= maxHeatEnergy) shouldCool = false;

        if (shouldCool) {
            te.heatEnergy = Math.max(te.heatEnergy - Math.max(te.heatEnergy / 1000, 1), 0);
        }

        te.networkPackNT();
    }

    /** Slot 2 fluid identifier retypes the tank (old tank.setType(2, inventory)). */
    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(2);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tank.getTankType()) {
            tank.setTankType(t);
            setChanged();
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x, y, z - 2, Library.NEG_Z)
        };
    }

    /** GUI buttons: 0 = burn toggle, 1 = setting up, 2 = setting down (old IControlReceiver+IToolable). */
    @Override
    public void handleButtonPacket(int value, int meta) {
        switch (value) {
            case 0 -> this.isOn = !this.isOn;
            case 1 -> toggleSettingUp();
            case 2 -> toggleSettingDown();
        }
        setChanged();
    }

    public void toggleSettingUp() {
        setting++;
        if (setting > 100) setting = 1;
    }

    public void toggleSettingDown() {
        setting--;
        if (setting < 1) setting = 100;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Slot 1 is the emptied-container out; the menu's take-only slot keeps players out.
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        return true;
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
        buf.writeByte((byte) this.setting);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        isOn = buf.readBoolean();
        heatEnergy = buf.readInt();
        setting = buf.readByte();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "tank");
        output.putBoolean("isOn", isOn);
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("setting", setting);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "tank");
        isOn = input.getBooleanOr("isOn", false);
        heatEnergy = input.getIntOr("heatEnergy", 0);
        setting = Math.max(1, input.getIntOr("setting", 1));
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
        if ((PREFIX_VALUE + "burnrate").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            isOn = params[0].equals("1");
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setburnrate").equals(name) && params.length > 0) {
            setting = IRORInteractive.parseInt(params[0], 1, 10);
            setChanged();
        }
        return null;
    }
}
