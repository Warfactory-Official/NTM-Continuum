package com.hbm.tileentity.machine;

import com.hbm.client.sound.ChungusSoundHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * The Leviathan steam turbine, a 5x4x14 machine that drains its whole input tank every tick. Both tanks
 * hold a billion millibuckets and the lever never resizes them, so unlike the industrial turbine it runs
 * every steam grade at the same capacity and simply burns whatever it is fed.
 */
public class TileEntityChungus extends TileEntityTurbineBase {

    public static int inputTankSize = 1_000_000_000;
    public static int outputTankSize = 1_000_000_000;
    public static double efficiency = 0.85D;

    public int turnTimer;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration;

    private final float audioDesync;

    public TileEntityChungus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CHUNGUS.get(), pos, state);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, inputTankSize),
                new FluidTankNTM(Fluids.SPENTSTEAM, outputTankSize)
        };
        audioDesync = new Random().nextFloat() * 0.05F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChungus chungus) {
        chungus.tickServer(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityChungus chungus) {
        chungus.onClientTick();
        chungus.spawnExhaust(level, pos);
        ChungusSoundHandler.tickLoop(chungus);
    }

    @Override public double consumptionPercent() { return 1D; }
    @Override public double getEfficiency() { return efficiency; }

    public ForgeDirection getFacing() {
        BlockState state = getBlockState();
        return MachineCore.facing(state);
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos p = getBlockPos();
        return new DirPos[]{
                new DirPos(p.getX() + dir.offsetX * 5, p.getY() + 2, p.getZ() + dir.offsetZ * 5, dir),
                new DirPos(p.getX() + rot.offsetX * 3, p.getY(), p.getZ() + rot.offsetZ * 3, rot),
                new DirPos(p.getX() - rot.offsetX * 3, p.getY(), p.getZ() - rot.offsetZ * 3, rot.getOpposite())
        };
    }

    @Override
    public DirPos[] getPowerPos() {
        ForgeDirection dir = getFacing();
        BlockPos p = getBlockPos();
        return new DirPos[]{new DirPos(p.getX() - dir.offsetX * 11, p.getY(), p.getZ() - dir.offsetZ * 11,
                dir.getOpposite())};
    }

    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN && dir != ForgeDirection.UNKNOWN;
    }

    @Override
    public void onServerTick() {
        turnTimer--;
        if (operational) turnTimer = 25;
    }

    @Override
    public void onClientTick() {
        lastRotor = rotor;
        rotor += fanAcceleration;
        if (rotor >= 360F) {
            rotor -= 360F;
            lastRotor -= 360F;
        }

        fanAcceleration = turnTimer > 0
                ? Math.min(25F, fanAcceleration + 0.075F + audioDesync)
                : Math.max(0F, fanAcceleration - 0.1F);
    }

    private void spawnExhaust(Level level, BlockPos pos) {
        if (turnTimer <= 0) return;

        net.minecraft.util.RandomSource random = level.getRandom();
        ForgeDirection dir = getFacing();
        ForgeDirection side = dir.getRotation(ForgeDirection.UP);
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.CLOUD,
                    pos.getX() + 0.5D + dir.offsetX * (random.nextDouble() + 1.25D)
                            + random.nextGaussian() * side.offsetX * 0.65D,
                    pos.getY() + 2.5D + random.nextGaussian() * 0.65D,
                    pos.getZ() + 0.5D + dir.offsetZ * (random.nextDouble() + 1.25D)
                            + random.nextGaussian() * side.offsetZ * 0.65D,
                    -dir.offsetX * 0.2D, 0D, -dir.offsetZ * 0.2D);
        }
    }

    /** How far the fan has spooled up, 0 to 1; the sound loop rides this rather than the steam flow. */
    public float getTurbineSpeed() {
        return fanAcceleration / 25F;
    }

    public float getAudioDesync() {
        return audioDesync;
    }

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
        buf.writeInt(turnTimer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        turnTimer = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("turnTimer", turnTimer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        turnTimer = input.getIntOr("turnTimer", 0);
    }
}
