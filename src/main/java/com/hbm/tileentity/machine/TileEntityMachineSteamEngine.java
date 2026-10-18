package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/** Server logic and client animation state for the legacy steam engine. */
public class TileEntityMachineSteamEngine extends TileEntityMachineBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2 {

    public static final int STEAM_CAPACITY = 2_000;
    public static final int SPENT_STEAM_CAPACITY = 20;
    public static final double EFFICIENCY = 0.85D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.STEAM, STEAM_CAPACITY),
            new FluidTankNTM(Fluids.SPENTSTEAM, SPENT_STEAM_CAPACITY)
    };
    public long powerBuffer;
    public float rotor;
    public float lastRotor;

    private float syncRotor;
    private int turnProgress;
    private float acceleration;

    public TileEntityMachineSteamEngine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_STEAM_ENGINE.get(), pos, state, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSteamEngine engine) {
        engine.setupAllPorts(engine.getPorts());
        engine.updatePortPOFIFO();

        engine.powerBuffer = 0;
        engine.tanks[0].setTankType(Fluids.STEAM);
        engine.tanks[1].setTankType(Fluids.SPENTSTEAM);

        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(engine.tanks[0].getTankType());
        int ops = 0;
        if (recipe != null) {
            int inputOps = engine.tanks[0].getFill() / recipe.inputAmount();
            int outputOps = (engine.tanks[1].getMaxFill() - engine.tanks[1].getFill()) / recipe.outputAmount();
            ops = Math.min(inputOps, outputOps);
            engine.tanks[0].setFill(engine.tanks[0].getFill() - ops * recipe.inputAmount());
            engine.tanks[1].setFill(engine.tanks[1].getFill() + ops * recipe.outputAmount());
            engine.powerBuffer = (long) (ops * recipe.heatEnergy() * EFFICIENCY);
        }

        if (ops > 0) {
            engine.acceleration += 0.1F;
        } else {
            engine.acceleration -= 0.1F;
        }
        engine.acceleration = Mth.clamp(engine.acceleration, 0F, 40F);
        engine.rotor += engine.acceleration;
        if (engine.rotor >= 360F) {
            engine.rotor -= 360F;
            level.playSound(null, pos, ModSounds.STEAM_ENGINE_OPERATE.get(),
                    SoundSource.BLOCKS, 1F, 0.5F + engine.acceleration / 80F);
        }

        engine.setChanged();
        engine.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSteamEngine engine) {
        engine.lastRotor = engine.rotor;
        if (engine.turnProgress > 0) {
            double d = Mth.wrapDegrees(engine.syncRotor - engine.rotor);
            engine.rotor = (float) (engine.rotor + d / engine.turnProgress);
            --engine.turnProgress;
        } else {
            engine.rotor = engine.syncRotor;
        }
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX * 1, y + 1, z + rot.offsetZ * 1, rot),
                    PortDef.make(x + rot.offsetX * 1 + dir.offsetX, y + 1, z + rot.offsetZ * 1 + dir.offsetZ, rot),
                    PortDef.make(x + rot.offsetX * 1 - dir.offsetX, y + 1, z + rot.offsetZ * 1 - dir.offsetZ, rot),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    @Override public long getPower() { return powerBuffer; }
    @Override public void setPower(long power) { powerBuffer = power; }
    @Override public long getMaxPower() { return powerBuffer; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    public float getRotor(float partialTicks) {
        return lastRotor + (rotor - lastRotor) * partialTicks;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeFloat(rotor);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        powerBuffer = buf.readLong();
        syncRotor = buf.readFloat();
        tanks[1].deserialize(buf);
        turnProgress = 3;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "s");
        tanks[1].writeToNBT(output, "w");
        output.putLong("powerBuffer", powerBuffer);
        output.putFloat("acceleration", acceleration);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "s");
        tanks[1].readFromNBT(input, "w");
        powerBuffer = input.getLongOr("powerBuffer", 0L);
        acceleration = input.getFloatOr("acceleration", 0F);
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_steam_engine";
    }
}
