package com.hbm.tileentity.machine;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.environment.Environment;
import com.hbm.client.sound.GasTurbineSoundHandler;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.container.ContainerMachineTurbineGas;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/** The legacy gas turbine: a slow-starting gas generator with a steam heat exchanger. */
public class TileEntityMachineTurbineGas extends TileEntityMachinePolluting
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, MenuProvider, IRORValueProvider, IRORInteractive {

    public static final long MAX_POWER = 1_000_000L;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.GAS, 100_000),
            new FluidTankNTM(Fluids.LUBRICANT, 16_000),
            new FluidTankNTM(Fluids.WATER, 16_000),
            new FluidTankNTM(Fluids.HOTSTEAM, 160_000)
    };
    public long power;
    public int rpm;
    public int temp;
    public int rpmIdle = 10;
    public int tempIdle = 300;
    public int powerSliderPos;
    public int throttle;
    public boolean autoMode;
    public int state;
    public int counter;
    public int instantPowerOutput;
    public double waterToBoil;

    private int rpmLast;
    private int tempLast;
    private double fuelToConsume;
    private long powerBeforeNet;

    /**
     * 1.7 {@code fuelMaxCons}: mB burned per tick at full throttle, now the value map
     * {@code data/hbm/value_maps/fluid/gas_turbine_fuel_consumption.json}; 5 for a fuel without a value (1.7's default).
     */
    static double maxFuelConsumption(FluidType fuel) {
        Double value = FluidDataMaps.GAS_TURBINE_FUEL.get(fuel.getFluid());
        return value == null ? 5D : value;
    }

    public TileEntityMachineTurbineGas(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBINE_GAS.get(), pos, state, 2, 150);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbineGas turbine) {
        turbine.setupPowerPorts(turbine.getPowerPorts());
        turbine.setupFluidPorts(turbine.getPorts());
        turbine.updatePortPOFIFO();

        turbine.waterToBoil = 0D;
        turbine.throttle = turbine.powerSliderPos * 100 / 60;
        turbine.updateFuelType();

        if (turbine.autoMode) {
            int target;
            // 1.7: above 10% fuel the slider follows the power gauge; below it, consumption falls off linearly
            if (turbine.tanks[0].getFill() * 10 > turbine.tanks[0].getMaxFill()) {
                target = 60 - (int) (60D * turbine.power / MAX_POWER);
            } else {
                target = (int) (turbine.tanks[0].getFill() * 0.0001D * (60 - (int) (60D * turbine.power / MAX_POWER)));
            }
            if (target > turbine.powerSliderPos) turbine.powerSliderPos++;
            else if (target < turbine.powerSliderPos) turbine.powerSliderPos--;
        }

        switch (turbine.state) {
            case 0 -> turbine.shutdown();
            case -1 -> {
                turbine.stopIfNotReady();
                turbine.startup(level);
            }
            case 1 -> {
                turbine.stopIfNotReady();
                turbine.run(level);
            }
            default -> { }
        }

        ForgeDirection dir = turbine.getFacing();
        turbine.powerBeforeNet = Math.min(turbine.power, MAX_POWER);
        turbine.power = Library.chargeItemsFromTE(turbine.inventory, 0, turbine.power, MAX_POWER);
        turbine.power = Math.min(turbine.power, MAX_POWER);

        turbine.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbineGas turbine) {
        GasTurbineSoundHandler.tickLoop(turbine);
    }

    private void updateFuelType() {
        ItemStack id = inventory.getStackInSlot(1);
        if (!(id.getItem() instanceof IItemFluidId identifier)) return;
        FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(type);
        if (fuel != null && fuel.grade() == FluidCombustion.FuelGrade.GAS) tanks[0].setTankType(type);
    }

    private void stopIfNotReady() {
        if (tanks[0].getFill() == 0 || tanks[1].getFill() == 0 || !hasAcceptableFuel()) state = 0;
    }

    public boolean hasAcceptableFuel() {
        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(tanks[0].getTankType());
        return fuel != null && fuel.grade() == FluidCombustion.FuelGrade.GAS;
    }

    /**
     * Menu controls: 0 ignition, 1 automatic load-following, 1000..1060 slider position.
     * 1.7 gated the ignition button on the client only ({@code GUIMachineTurbineGas.mouseClicked}: counter 0 or 579,
     * where the client's counter freezes at 579 once state 1 stops syncing it); its {@code receiveControl} applied the
     * state unconditionally. The server counter is 225 while running, so a server-side gate here would block stop.
     */
    public void handleButton(int id) {
        if (id == 0) state = state - 1;
        else if (id == 1 && state == 1) autoMode = !autoMode;
        else if (id == 2 && state == 1) autoMode = false;
        else if (id >= 1000 && id <= 1060 && state == 1) {
            powerSliderPos = id - 1000;
            autoMode = false;
        } else return;
        setChanged();
    }

    private void startup(Level level) {
        // hbm-space: no air, no ignition; the turbine drops straight back to off (com.hbm.api.environment.Environment)
        if (!Environment.breatheAir(level, worldPosition, 0)) {
            state = 0;
            return;
        }

        counter++;
        if (counter <= 20) rpm = 5 * counter;
        else if (counter <= 40) rpm = 100 - 5 * (counter - 20);
        else if (counter > 50) {
            rpm = rpmIdle * (counter - 50) / 530;
            temp = tempIdle * (counter - 50) / 530;
        }
        if (counter == 50) level.playSound(null, worldPosition.above(2), ModSounds.TURBINE_GAS_STARTUP.get(),
                SoundSource.BLOCKS, 1F, 1F);
        if (counter == 580) {
            counter = 225; // 1.7: ensures it shuts down properly when stopped immediately after startup
            state = 1;
        }
    }

    private void shutdown() {
        autoMode = false;
        instantPowerOutput = 0;
        if (powerSliderPos > 0) powerSliderPos--;

        if (rpm <= 10 && counter > 0) {
            if (counter == 225) {
                if (level != null) level.playSound(null, worldPosition.above(2), ModSounds.TURBINE_GAS_SHUTDOWN.get(),
                        SoundSource.BLOCKS, 1F, 1F);
                rpmLast = rpm;
                tempLast = temp;
            }
            counter--;
            rpm = rpmLast * counter / 225;
            temp = tempLast * counter / 225;
        } else if (rpm > 11) {
            counter = 42069;
            rpm--;
        } else if (rpm == 11) {
            counter = 225;
            rpm--;
        }
    }

    protected int getFluidBurnTemp(FluidType type) {
        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(type);
        double energy = fuel == null ? 0D : fuel.energyPerBucket();
        return (int) Math.floor(800D - Math.exp(-energy / 100_000D) * 300D);
    }

    private void run(Level level) {
        if ((int) (throttle * 0.9D) > rpm - rpmIdle) {
            if (TickPhase.every(level, worldPosition, 5)) rpm++;
        } else if ((int) (throttle * 0.9D) < rpm - rpmIdle && TickPhase.every(level, worldPosition, 2)) {
            rpm--;
        }

        int maxTemp = getFluidBurnTemp(tanks[0].getTankType());
        int heatTarget = throttle * 5 * (maxTemp - tempIdle) / 500;
        if (heatTarget > temp - tempIdle && TickPhase.every(level, worldPosition, 2)) temp++;
        else if (heatTarget < temp - tempIdle && TickPhase.every(level, worldPosition, 2)) temp--;
        makePower(level, maxFuelConsumption(tanks[0].getTankType()), throttle);
    }

    private void makePower(Level level, double maxConsumption, int throttle) {
        double consumption = maxConsumption * 0.05D + maxConsumption * throttle / 100D;
        if (TickPhase.every(level, worldPosition, 5)) pollute(tanks[0].getTankType(), FluidReleaseType.BURN, (float) consumption * 0.5F);
        fuelToConsume += consumption;
        // hbm-space: the running turbine breathes as much oxygen as it burns fuel, and stops without it
        if (!Environment.breatheAir(level, worldPosition, (int) Math.floor(fuelToConsume))) {
            state = 0;
            return;
        }
        int consumed = (int) Math.floor(fuelToConsume);
        tanks[0].setFill(tanks[0].getFill() - consumed);
        fuelToConsume -= consumed;
        if (TickPhase.every(level, worldPosition, 10)) tanks[1].setFill(tanks[1].getFill() - 1);
        if (tanks[0].getFill() < 0) {
            tanks[0].setFill(0);
            state = 0;
        }
        if (tanks[1].getFill() < 0) {
            tanks[1].setFill(0);
            state = 0;
        }

        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(tanks[0].getTankType());
        long energy = fuel == null ? 0L : fuel.energyPerBucket() / 1000L;
        int rpmEfficiency = rpm - rpmIdle;
        double target = maxConsumption * energy * rpmEfficiency / 90D;
        if (instantPowerOutput < target) {
            instantPowerOutput += Math.random() * 0.005D * maxConsumption * energy;
            if (instantPowerOutput > target) instantPowerOutput = (int) target;
        } else if (instantPowerOutput > target) {
            instantPowerOutput -= Math.random() * 0.011D * maxConsumption * energy;
            if (instantPowerOutput < target) instantPowerOutput = (int) target;
        }
        power += instantPowerOutput;

        waterToBoil = maxConsumption * energy * (temp - tempIdle) / 220_000D;
        int heatCycles = (int) Math.floor(waterToBoil);
        int waterCycles = tanks[2].getFill();
        int steamCycles = (tanks[3].getMaxFill() - tanks[3].getFill()) / 10;
        int cycles = Math.min(heatCycles, Math.min(waterCycles, steamCycles));
        tanks[2].setFill(tanks[2].getFill() - cycles);
        tanks[3].setFill(tanks[3].getFill() + cycles * 10);
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    protected PortDef[] cachedPowerPorts;
    public PortDef[] getPowerPorts() {
        if (cachedPowerPorts == null) {
            ForgeDirection rot = getFacing().getRotation(ForgeDirection.UP);
            cachedPowerPorts = new PortDef[] {
                    PortDef.make(worldPosition.getX() + rot.offsetX * 4, worldPosition.getY() + 1, worldPosition.getZ() + rot.offsetZ * 4, rot),
            };
        }
        return cachedPowerPorts;
    }

    /** One passthrough group for all four tanks: fuel, lube, water and steam share every fluid port. */
    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x - dir.offsetX * 1 + rot.offsetX, y, z - dir.offsetZ * 1 + rot.offsetZ, dir.getOpposite()),
                    PortDef.make(x + dir.offsetX * 1 + rot.offsetX, y, z + dir.offsetZ * 1 + rot.offsetZ, dir),
                    PortDef.make(x - dir.offsetX * 1 + rot.offsetX * -4, y, z - dir.offsetZ * 1 + rot.offsetZ * -4, dir.getOpposite()),
                    PortDef.make(x + dir.offsetX * 1 + rot.offsetX * -4, y, z + dir.offsetZ * 1 + rot.offsetZ * -4, dir),
                    PortDef.make(x - rot.offsetX * 5, y + 1, z - rot.offsetZ * 5, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public DirPos getPowerPos() {
        ForgeDirection dir = getFacing();
        return new DirPos(worldPosition.getX() - dir.offsetZ * 5, worldPosition.getY() + 1,
                worldPosition.getZ() + dir.offsetX * 5, dir.getRotation(ForgeDirection.UP));
    }

    public DirPos[] getFuelPos() { return getInputPair(2, 1); }
    public DirPos[] getLubePos() { return getInputPair(2, 1); }

    private DirPos[] getInputPair(int distance, int rotationDistance) {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos[]{
                new DirPos(worldPosition.getX() - dir.offsetX * distance + rot.offsetX * rotationDistance, worldPosition.getY(),
                        worldPosition.getZ() - dir.offsetZ * distance + rot.offsetZ * rotationDistance, dir.getOpposite()),
                new DirPos(worldPosition.getX() + dir.offsetX * distance + rot.offsetX * rotationDistance, worldPosition.getY(),
                        worldPosition.getZ() + dir.offsetZ * distance + rot.offsetZ * rotationDistance, dir)
        };
    }

    public DirPos[] getWaterPos() {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos[]{
                new DirPos(worldPosition.getX() - dir.offsetX * 2 - rot.offsetX * 4, worldPosition.getY(),
                        worldPosition.getZ() - dir.offsetZ * 2 - rot.offsetZ * 4, dir.getOpposite()),
                new DirPos(worldPosition.getX() + dir.offsetX * 2 - rot.offsetX * 4, worldPosition.getY(),
                        worldPosition.getZ() + dir.offsetZ * 2 - rot.offsetZ * 4, dir)
        };
    }

    public DirPos getSteamPos() {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos(worldPosition.getX() + dir.offsetZ * 6, worldPosition.getY() + 1,
                worldPosition.getZ() - dir.offsetX * 6, rot.getOpposite());
    }

    /** All connection anchors, in legacy order: power, fuel/lube, water, steam. */
    public DirPos[] getConPos() {
        DirPos[] fuel = getFuelPos();
        DirPos[] water = getWaterPos();
        return new DirPos[]{getPowerPos(), fuel[0], fuel[1], water[0], water[1], getSteamPos()};
    }

    public boolean isPort(BlockPos pos, ForgeDirection dir) {
        for (DirPos connection : getConPos()) {
            if (connection.getPos().equals(pos) && connection.getDir() == dir) return true;
        }
        return false;
    }

    public boolean isFluidPort(BlockPos pos, ForgeDirection dir) {
        DirPos[] ports = {
                getFuelPos()[0], getFuelPos()[1], getWaterPos()[0], getWaterPos()[1], getSteamPos()
        };
        return IEnergyConnectorBlock.isPort(ports, pos, dir);
    }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return dir != ForgeDirection.DOWN; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[3], smoke, smoke_leaded, smoke_poison}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = value; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 ? Library.isBattery(stack) : stack.getItem() instanceof IItemFluidId;
    }
    @Override public String getDefaultName() { return "container.turbinegas"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineTurbineGas(id, inventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "turbinepercent", PREFIX_VALUE + "turbinespeed", PREFIX_VALUE + "output",
                PREFIX_VALUE + "state", PREFIX_VALUE + "automode", PREFIX_VALUE + "temp", PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel", PREFIX_VALUE + "lubricant", PREFIX_VALUE + "water", PREFIX_VALUE + "steam",
                PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "auto",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "turbinepercent").equals(name)) return "" + (int) (powerSliderPos * 100D / 60D);
        if ((PREFIX_VALUE + "turbinespeed").equals(name)) return "" + rpm;
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (instantPowerOutput * 20);
        if ((PREFIX_VALUE + "state").equals(name)) return "" + state;
        if ((PREFIX_VALUE + "automode").equals(name)) return "" + (autoMode ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name)) return "" + temp;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "lubricant").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "water").equals(name)) return "" + tanks[2].getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + tanks[3].getFill();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length > 0) {
            try {
                autoMode = Integer.parseInt(params[0]) == 1;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                int percent = Integer.parseInt(params[0]);
                percent = Math.max(0, Math.min(100, percent));
                powerSliderPos = percent * 60 / 100;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                int newState = Integer.parseInt(params[0]);
                if (newState == 1) {
                    if (state == 0) state = -1;
                } else if (newState == 0) {
                    if (state == 1) state = 0;
                }
                setChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(powerBeforeNet);
        buf.writeInt(rpm);
        buf.writeInt(temp);
        buf.writeInt(state);
        buf.writeBoolean(autoMode);
        buf.writeInt(throttle);
        buf.writeInt(powerSliderPos);
        buf.writeInt(state == 1 ? instantPowerOutput : counter);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        rpm = buf.readInt();
        temp = buf.readInt();
        state = buf.readInt();
        autoMode = buf.readBoolean();
        throttle = buf.readInt();
        powerSliderPos = buf.readInt();
        if (state == 1) instantPowerOutput = buf.readInt(); else counter = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }
    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, switch (i) {
            case 0 -> "gas"; case 1 -> "lube"; case 2 -> "water"; default -> "densesteam";
        });
        output.putBoolean("automode", autoMode);
        output.putLong("power", power);
        output.putInt("state", state == 1 ? state : 0);
        output.putInt("rpm", state == 1 ? rpm : 0);
        output.putInt("temperature", state == 1 ? temp : 20);
        output.putInt("slidPos", state == 1 ? powerSliderPos : 0);
        output.putInt("instPwr", state == 1 ? instantPowerOutput : 0);
        output.putInt("counter", state == 1 ? 225 : 0);
    }
    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "gas"); tanks[1].readFromNBT(input, "lube");
        tanks[2].readFromNBT(input, "water"); tanks[3].readFromNBT(input, "densesteam");
        autoMode = input.getBooleanOr("automode", false);
        power = input.getLongOr("power", 0L); state = input.getIntOr("state", 0);
        rpm = input.getIntOr("rpm", 0); temp = input.getIntOr("temperature", 20);
        powerSliderPos = input.getIntOr("slidPos", 0); instantPowerOutput = input.getIntOr("instPwr", 0);
        counter = input.getIntOr("counter", 0);
    }
}
