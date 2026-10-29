package com.hbm.tileentity.machine;

import org.jspecify.annotations.Nullable;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;
import com.hbm.tileentity.ModPowerGenBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/**
 * The Leviathan steam turbine, a 1:1 port of 1.7 {@code TileEntityChungus} over {@code TileEntityTurbineBase}.
 * It consumes every drop of steam it holds each tick ({@code consumptionPercent() == 1}) at 85% efficiency
 * and never resizes its billion-mB tanks on a lever pull. The turbine-base loop (cooling step, power buffer
 * reset, port provision) is inlined here exactly as the industrial turbine port did it.
 */
public class TileEntityChungus extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, IRORValueProvider {

    public static int inputTankSize = 1_000_000_000;
    public static int outputTankSize = 1_000_000_000;
    public static double efficiency = 0.85D;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "steamturbineLeviathan"; }

        @Override public void readIfPresent(JsonObject obj) {
            inputTankSize = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSize);
            outputTankSize = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSize);
            efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("INFO").value("leviathan steam turbine consumes all availible steam per tick");
            writer.name("I:inputTankSize").value(inputTankSize);
            writer.name("I:outputTankSize").value(outputTankSize);
            writer.name("D:efficiency").value(efficiency);
        }
    };

    public final FluidTankNTM[] tanks;
    public long powerBuffer;
    /** 1.7 TileEntityTurbineBase serialises tanks[0] BEFORE the cooling step, so the client sees the pre-consumption steam. */
    private int syncedInputFill;
    private @Nullable FluidType syncedInputResource;
    private @Nullable FluidType syncedInputPin;
    public boolean operational = false;
    /** 1.7: counts down from 25 after the last operational tick; the client keeps spinning while it is positive. */
    private int turnTimer;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration = 0F;

    private @org.jspecify.annotations.Nullable AudioWrapper audio;
    private final float audioDesync;

    public TileEntityChungus(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.CHUNGUS.get(), pos, state);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, inputTankSize),
                new FluidTankNTM(Fluids.SPENTSTEAM, outputTankSize)
        };
        audioDesync = new Random().nextFloat() * 0.05F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChungus turbine) {
        turbine.setupPowerPorts(turbine.getPowerPorts());
        turbine.setupFluidPorts(turbine.getFluidPorts());
        turbine.updatePortPOFIFO();

        turbine.powerBuffer = 0;
        turbine.operational = false;
        turbine.syncedInputFill = turbine.tanks[0].getFill();
        turbine.syncedInputResource = turbine.tanks[0].getFluid();
        turbine.syncedInputPin = turbine.tanks[0].isStrict() ? turbine.tanks[0].getDeclaredFluid() : null;

        FluidType input = turbine.tanks[0].getTankType();
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input);
        boolean valid = recipe != null && efficiency > 0D;
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

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityChungus turbine) {
        turbine.onClientTick();
    }

    /** 1.7 {@code TileEntityTurbineBase.generatePower}: the Leviathan has no flywheel; the buffer is the tick's output. */
    public void generatePower(long power, int steamConsumed) {
        this.powerBuffer += power;
    }

    public void onServerTick() {
        turnTimer--;
        if (operational) turnTimer = 25;
    }

    protected final void stopTurning() { turnTimer = 0; }

    public boolean isTurning() { return turnTimer > 0; }

    public void onClientTick() {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;

        if (this.rotor >= 360) {
            this.rotor -= 360;
            this.lastRotor -= 360;
        }

        if (isTurning()) {
            // Fan accelerates with a random offset to ensure the audio doesn't perfectly align, makes for a more pleasant hum
            this.fanAcceleration = Math.max(0F, Math.min(25F, this.fanAcceleration += 0.075F + audioDesync));

            RandomSource rand = level.getRandom();
            ForgeDirection dir = getFacing();
            ForgeDirection side = dir.getRotation(ForgeDirection.UP);
            BlockPos p = getBlockPos();

            for (int i = 0; i < 10; i++) {
                level.addParticle(ParticleTypes.CLOUD,
                        p.getX() + 0.5 + dir.offsetX * (rand.nextDouble() + 1.25) + rand.nextGaussian() * side.offsetX * 0.65,
                        p.getY() + 2.5 + rand.nextGaussian() * 0.65,
                        p.getZ() + 0.5 + dir.offsetZ * (rand.nextDouble() + 1.25) + rand.nextGaussian() * side.offsetZ * 0.65,
                        -dir.offsetX * 0.2, 0, -dir.offsetZ * 0.2);
            }

        } else {
            this.fanAcceleration = Math.max(0F, Math.min(25F, this.fanAcceleration -= 0.1F));
        }
        updateSound();
    }

    /** The audio update is independent of advancing the rotor's animation. */
    public void updateSound() {
        if (isTurning() && audio == null) {
            var p = getBlockPos();
            // 1.7 NTMSounds.TURBINE_LEVI_LOOP is the same chungusTurbine file the industrial turbine plays.
            audio = AudioSystem.getLoopedSound(ModSounds.INDUSTRIAL_TURBINE.get(), SoundSource.BLOCKS,
                    p.getX(), p.getY(), p.getZ(), 1.0F, 20F, 1.0F, 20);
            if (audio != null) audio.startSound();
        }
        if (audio != null) {
            if (isTurning() || fanAcceleration > 0) {
                float turbineSpeed = fanAcceleration / 25F;
                audio.updateVolume(getVolume(0.5F * turbineSpeed));
                audio.updatePitch(0.25F + 0.75F * turbineSpeed);
                if (isTurning()) audio.keepAlive();
            } else {
                audio.stopSound();
                audio = null;
            }
        }
    }

    /** 1.7 {@code invalidate}/{@code onChunkUnload}: the loop dies with the tile. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    /** 1.7 {@code TileEntityTurbineBase.onLeverPull} with {@code doesResizeCompressor() == false}. */
    public void onLeverPull() {
        FluidType type = tanks[0].getTankType();
        if (type == Fluids.STEAM) {
            tanks[0].setTankType(Fluids.HOTSTEAM); tanks[1].setTankType(Fluids.STEAM);
        } else if (type == Fluids.HOTSTEAM) {
            tanks[0].setTankType(Fluids.SUPERHOTSTEAM); tanks[1].setTankType(Fluids.HOTSTEAM);
        } else if (type == Fluids.SUPERHOTSTEAM) {
            tanks[0].setTankType(Fluids.ULTRAHOTSTEAM); tanks[1].setTankType(Fluids.SUPERHOTSTEAM);
        } else if (type == Fluids.ULTRAHOTSTEAM) {
            tanks[0].setTankType(Fluids.STEAM); tanks[1].setTankType(Fluids.SPENTSTEAM);
        } else {
            tanks[0].setTankType(Fluids.STEAM); tanks[1].setTankType(Fluids.SPENTSTEAM);
        }
        setChanged();
    }

    public double consumptionPercent() { return 1D; }
    public double getEfficiency() { return efficiency; }
    public boolean doesResizeCompressor() { return false; }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    public int getTurnTimer() { return turnTimer; }

    /** 1.7: horizontal faces only. */
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN && dir != ForgeDirection.UNKNOWN;
    }

    protected PortDef[] cachedFluidPorts;
    public PortDef[] getFluidPorts() {
        if (cachedFluidPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedFluidPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 4, y + 2, z + dir.offsetZ * 4, dir),
                    PortDef.make(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                    PortDef.make(x - rot.offsetX * 2, y, z - rot.offsetZ * 2, rot.getOpposite()),
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
                    PortDef.make(worldPosition.getX() - dir.offsetX * 10, worldPosition.getY(), worldPosition.getZ() - dir.offsetZ * 10, dir.getOpposite()),
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
        return new String[]{PREFIX_VALUE + "output"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) powerBuffer;
        return null;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        if (syncedInputResource == null) tanks[0].serialize(buf, syncedInputFill);
        else tanks[0].serialize(buf, syncedInputFill, syncedInputResource, syncedInputPin);
        tanks[1].serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeInt(turnTimer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        powerBuffer = buf.readLong();
        turnTimer = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "steam");
        output.putLong("power", powerBuffer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "steam");
        powerBuffer = input.getLongOr("power", 0L);
    }

    public float getAudioDesync() {
        return audioDesync;
    }

    public float getRotor(float partialTicks) {
        return lastRotor + (rotor - lastRotor) * partialTicks;
    }
}
