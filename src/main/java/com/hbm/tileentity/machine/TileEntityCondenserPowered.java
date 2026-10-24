package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/** The powered condenser; conversion is gated by its stored HE buffer. */
public class TileEntityCondenserPowered extends TileEntityCondenser implements IEnergyReceiverMK2 {

    public static long maxPower = 10_000_000L;
    public static int inputTankSizeP = 1_000_000;
    public static int outputTankSizeP = 1_000_000;
    public static int powerConsumption = 10;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "condenserPowered"; }

        @Override public void readIfPresent(JsonObject obj) {
            maxPower = IConfigurableMachine.grab(obj, "L:maxPower", maxPower);
            inputTankSizeP = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSizeP);
            outputTankSizeP = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSizeP);
            powerConsumption = IConfigurableMachine.grab(obj, "I:powerConsumption", powerConsumption);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("L:maxPower").value(maxPower);
            writer.name("I:inputTankSize").value(inputTankSizeP);
            writer.name("I:outputTankSize").value(outputTankSizeP);
            writer.name("I:powerConsumption").value(powerConsumption);
        }
    };

    public long power;
    public float spin;
    public float lastSpin;

    public TileEntityCondenserPowered(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONDENSER_POWERED.get(), pos, state, inputTankSizeP, outputTankSizeP);
        vacuumOptimised = true; // hbm-space: the powered condenser keeps condensing in a vacuum
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCondenserPowered condenser) {
        condenser.serverTickCommon(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCondenserPowered condenser) {
        condenser.lastSpin = condenser.spin;
        if (condenser.waterTimer <= 0) return;

        condenser.spin += 30F;
        if (condenser.spin >= 360F) {
            condenser.spin -= 360F;
            condenser.lastSpin -= 360F;
        }

        if (!TickPhase.every(level, pos, 4)) return;
        ForgeDirection direction = condenser.getFacing();
        level.addParticle(ParticleTypes.CLOUD,
                pos.getX() + 0.5D + direction.offsetX * 1.5D,
                pos.getY() + 1.5D,
                pos.getZ() + 0.5D + direction.offsetZ * 1.5D,
                direction.offsetX * 0.1D, 0D, direction.offsetZ * 0.1D);
        level.addParticle(ParticleTypes.CLOUD,
                pos.getX() + 0.5D - direction.offsetX * 1.5D,
                pos.getY() + 1.5D,
                pos.getZ() + 0.5D - direction.offsetZ * 1.5D,
                direction.offsetX * -0.1D, 0D, direction.offsetZ * -0.1D);
    }

    @Override
    protected boolean extraCondition(int amount) {
        // 5% tolerance, so a grid that can never top the buffer up exactly still runs it
        return power >= (amount * powerConsumption) * 0.95;
    }

    @Override
    protected void postConvert(int amount) {
        power -= (long) amount * powerConsumption;
        if (power < 0L) power = 0L;
    }

    public ForgeDirection getFacing() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        return ForgeDirection.getOrientation(meta);
    }

    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.condenser(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getFacing()); return cachedPorts; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = Math.max(0L, Math.min(maxPower, value)); }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeFloat(spin);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        spin = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
    }
}
