package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.client.sound.IndustrialTurbineSoundHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;
import com.hbm.tileentity.TilePort.PortDef;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/**
 * Industrial steam turbine, ported from the 1.12.2 turbine base without the old Forge fluid/GUI layer.
 * It consumes 20% of the available input steam per tick and turns the released heat into HE.
 */
public class TileEntityMachineIndustrialTurbine extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, IRORValueProvider {

    public static int inputTankSize = 750_000;
    public static int outputTankSize = 3_000_000;
    public static double efficiency = 1D;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "steamturbineIndustrialMk2"; }

        @Override public void readIfPresent(JsonObject obj) {
            inputTankSize = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSize);
            outputTankSize = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSize);
            efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("INFO").value("industrial steam turbine consumes 20% of available steam per tick");
            writer.name("I:inputTankSize").value(inputTankSize);
            writer.name("I:outputTankSize").value(outputTankSize);
            writer.name("D:efficiency").value(efficiency);
        }
    };
    public static final double ACCELERATION = 1D / 400D;

    public final FluidTankNTM[] tanks;
    public long powerBuffer;
    /** 1.7 TileEntityTurbineBase serialises tanks[0] BEFORE the cooling step, so the client sees the pre-consumption steam. */
    private int syncedInputFill;
    public boolean operational;
    public float rotor;
    public float lastRotor;
    public double spin;
    public long lastPowerTarget;

    private final float audioDesync;

    public TileEntityMachineIndustrialTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_INDUSTRIAL_TURBINE.get(), pos, state);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, inputTankSize),
                new FluidTankNTM(Fluids.SPENTSTEAM, outputTankSize)
        };
        audioDesync = new Random().nextFloat() * 0.05F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityMachineIndustrialTurbine turbine) {
        turbine.setupPowerPorts(turbine.getPowerPorts());
        turbine.setupFluidPorts(turbine.getFluidPorts());
        turbine.updatePortPOFIFO();

        turbine.powerBuffer = 0;
        turbine.operational = false;
        turbine.syncedInputFill = turbine.tanks[0].getFill();

        FluidType input = turbine.tanks[0].getTankType();
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input);
        boolean valid = recipe != null && turbine.efficiency > 0D;
        if (valid) {
            turbine.tanks[1].setTankType(recipe.output());
            int inputOps = (int) (Math.min(Math.ceil(turbine.tanks[0].getFill() * turbine.consumptionPercent()),
                    turbine.tanks[0].getFill()) / recipe.inputAmount());
            int outputOps = (turbine.tanks[1].getMaxFill() - turbine.tanks[1].getFill()) / recipe.outputAmount();
            int ops = Math.min(inputOps, outputOps);
            if (ops > 0) {
                turbine.tanks[0].setFill(turbine.tanks[0].getFill() - ops * recipe.inputAmount());
                turbine.tanks[1].setFill(turbine.tanks[1].getFill() + ops * recipe.outputAmount());
                turbine.generatePower((long) (ops * recipe.heatEnergy() * turbine.getEfficiency()),
                        ops * recipe.inputAmount());
                turbine.setChanged();
            }
            turbine.operational = ops > 0;
        }

        turbine.onServerTick();

        if (!valid) turbine.tanks[1].setTankType(Fluids.NONE);
        turbine.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityMachineIndustrialTurbine turbine) {
        turbine.onClientTick();
        IndustrialTurbineSoundHandler.tickLoop(turbine);
    }

    /** Sets the theoretical full-throughput output and eases the flywheel toward actual throughput. */
    public void generatePower(long power, int steamConsumed) {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(tanks[0].getTankType());
        if (recipe == null) return;

        int maxOps = (int) Math.ceil((tanks[0].getMaxFill() * consumptionPercent()) / recipe.inputAmount());
        lastPowerTarget = (long) (maxOps * recipe.heatEnergy() * getEfficiency());
        double fraction = (double) steamConsumed / (double) (recipe.inputAmount() * maxOps);
        if (Math.abs(spin - fraction) <= ACCELERATION) {
            spin = fraction;
        } else if (spin < fraction) {
            spin += ACCELERATION;
        } else {
            spin -= ACCELERATION;
        }
    }

    public void onServerTick() {
        if (!operational) spin -= ACCELERATION;
        if (spin <= 0D) {
            spin = 0D;
        } else {
            powerBuffer = (long) (lastPowerTarget * spin);
        }
    }

    public void onClientTick() {
        lastRotor = rotor;
        float speed = spin >= 0.5D ? 30F : (float) (Math.sqrt(spin * 2D) * 30D);
        rotor += speed;
        if (rotor >= 360F) {
            lastRotor -= 360F;
            rotor -= 360F;
        }
    }

    public void onLeverPull() {
        FluidType type = tanks[0].getTankType();
        if (type == Fluids.STEAM) {
            setStage(Fluids.HOTSTEAM, Fluids.STEAM, true);
        } else if (type == Fluids.HOTSTEAM) {
            setStage(Fluids.SUPERHOTSTEAM, Fluids.HOTSTEAM, true);
        } else if (type == Fluids.SUPERHOTSTEAM) {
            setStage(Fluids.ULTRAHOTSTEAM, Fluids.SUPERHOTSTEAM, true);
        } else if (type == Fluids.ULTRAHOTSTEAM) {
            setStage(Fluids.STEAM, Fluids.SPENTSTEAM, false);
            tanks[0].changeTankSize(tanks[0].getMaxFill() * 1000);
            tanks[1].changeTankSize(tanks[1].getMaxFill() * 1000);
        } else {
            setStage(Fluids.STEAM, Fluids.SPENTSTEAM, false);
        }
        setChanged();
    }

    private void setStage(FluidType input, FluidType output, boolean divideCapacity) {
        tanks[0].setTankType(input);
        tanks[1].setTankType(output);
        if (divideCapacity) {
            tanks[0].changeTankSize(tanks[0].getMaxFill() / 10);
            tanks[1].changeTankSize(tanks[1].getMaxFill() / 10);
        }
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    public boolean canConnect(ForgeDirection dir) {
        return dir == getFacing().getOpposite();
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return (CoolingRecipes.get(type) != null || type == Fluids.SPENTSTEAM)
                && dir != getFacing() && dir != getFacing().getOpposite();
    }

    public double consumptionPercent() { return 0.2D; }
    public double getEfficiency() { return efficiency; }
    public boolean doesResizeCompressor() { return true; }

    protected PortDef[] cachedFluidPorts;

    public PortDef[] getFluidPorts() {
        if (cachedFluidPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedFluidPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 3 + rot.offsetX * 1, y, z + dir.offsetZ * 3 + rot.offsetZ * 1, rot),
                    PortDef.make(x + dir.offsetX * 3 - rot.offsetX * 1, y, z + dir.offsetZ * 3 - rot.offsetZ * 1, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 1 + rot.offsetX * 1, y, z - dir.offsetZ * 1 + rot.offsetZ * 1, rot),
                    PortDef.make(x - dir.offsetX * 1 - rot.offsetX * 1, y, z - dir.offsetZ * 1 - rot.offsetZ * 1, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 3, y + 2, z + dir.offsetZ * 3, ForgeDirection.UP),
                    PortDef.make(x - dir.offsetX * 1, y + 2, z - dir.offsetZ * 1, ForgeDirection.UP),
            };
        }
        return cachedFluidPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getFluidPorts()); }

    protected PortDef[] cachedPowerPorts;
    public PortDef[] getPowerPorts() {
        if (cachedPowerPorts == null) {
            ForgeDirection dir = getFacing();
            cachedPowerPorts = new PortDef[] {
                    PortDef.make(worldPosition.getX() - dir.offsetX * 3, worldPosition.getY() + 1, worldPosition.getZ() - dir.offsetZ * 3, dir.getOpposite())
            };
        }
        return cachedPowerPorts;
    }

    public DirPos[] getPowerPos() { return PortDef.connections(getPowerPorts()); }

    @Override public long getPower() { return powerBuffer; }
    @Override public void setPower(long power) { powerBuffer = power; }
    @Override public long getMaxPower() { return powerBuffer; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "output", PREFIX_VALUE + "flywheel"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) powerBuffer;
        if ((PREFIX_VALUE + "flywheel").equals(name)) return "" + (int) (spin * 100);
        return null;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf, syncedInputFill);
        tanks[1].serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeDouble(spin);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        powerBuffer = buf.readLong();
        spin = buf.readDouble();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "output");
        output.putLong("power", powerBuffer);
        output.putLong("lastPowerTarget", lastPowerTarget);
        output.putDouble("spin", spin);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "output");
        powerBuffer = input.getLongOr("power", 0L);
        lastPowerTarget = input.getLongOr("lastPowerTarget", 0L);
        spin = input.getDoubleOr("spin", 0D);
    }

    public float getAudioDesync() {
        return audioDesync;
    }
}
