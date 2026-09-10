package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.sound.IndustrialTurbineSoundHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Industrial steam turbine, ported from the 1.12.2 turbine base without the old Forge fluid/GUI layer.
 * It consumes 20% of the available input steam per tick and turns the released heat into HE.
 */
public class TileEntityMachineIndustrialTurbine extends TileEntityTurbineBase {

    public static int inputTankSize = 750_000;
    public static int outputTankSize = 3_000_000;
    public static double efficiency = 1D;
    public static final double ACCELERATION = 1D / 400D;

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
        turbine.tickServer(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityMachineIndustrialTurbine turbine) {
        turbine.onClientTick();
        IndustrialTurbineSoundHandler.tickLoop(turbine);
    }

    /** Sets the theoretical full-throughput output and eases the flywheel toward actual throughput. */
    @Override
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

    @Override
    public void onServerTick() {
        if (!operational) spin -= ACCELERATION;
        if (spin <= 0D) {
            spin = 0D;
        } else {
            powerBuffer = (long) (lastPowerTarget * spin);
        }
    }

    @Override
    public void onClientTick() {
        lastRotor = rotor;
        float speed = spin >= 0.5D ? 30F : (float) (Math.sqrt(spin * 2D) * 30D);
        rotor += speed;
        if (rotor >= 360F) {
            lastRotor -= 360F;
            rotor -= 360F;
        }
    }

    public ForgeDirection getFacing() {
        int meta = getBlockState().hasProperty(BlockDummyable.META)
                ? getBlockState().getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        return ForgeDirection.getOrientation(meta - BlockDummyable.offset);
    }

    public boolean canConnect(ForgeDirection dir) {
        return dir == getFacing().getOpposite();
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return (CoolingRecipes.get(type) != null || type == Fluids.SPENTSTEAM)
                && dir != getFacing() && dir != getFacing().getOpposite();
    }

    @Override public double consumptionPercent() { return 0.2D; }
    @Override public double getEfficiency() { return efficiency; }
    @Override public boolean doesResizeCompressor() { return true; }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = getFacing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos p = getBlockPos();
        return new DirPos[]{
                new DirPos(p.getX() + dir.offsetX * 3 + rot.offsetX * 2, p.getY(),
                        p.getZ() + dir.offsetZ * 3 + rot.offsetZ * 2, rot),
                new DirPos(p.getX() + dir.offsetX * 3 - rot.offsetX * 2, p.getY(),
                        p.getZ() + dir.offsetZ * 3 - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(p.getX() - dir.offsetX + rot.offsetX * 2, p.getY(),
                        p.getZ() - dir.offsetZ + rot.offsetZ * 2, rot),
                new DirPos(p.getX() - dir.offsetX - rot.offsetX * 2, p.getY(),
                        p.getZ() - dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(p.getX() + dir.offsetX * 3, p.getY() + 3,
                        p.getZ() + dir.offsetZ * 3, ForgeDirection.UP),
                new DirPos(p.getX() - dir.offsetX, p.getY() + 3,
                        p.getZ() - dir.offsetZ, ForgeDirection.UP)
        };
    }

    @Override
    public DirPos[] getPowerPos() {
        ForgeDirection dir = getFacing();
        BlockPos p = getBlockPos();
        return new DirPos[]{new DirPos(p.getX() - dir.offsetX * 4, p.getY() + 1,
                p.getZ() - dir.offsetZ * 4, dir.getOpposite())};
    }

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
        buf.writeDouble(spin);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        spin = buf.readDouble();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("lastPowerTarget", lastPowerTarget);
        output.putDouble("spin", spin);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        lastPowerTarget = input.getLongOr("lastPowerTarget", 0L);
        spin = input.getDoubleOr("spin", 0D);
    }

    public float getAudioDesync() {
        return audioDesync;
    }
}
