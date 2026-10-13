package com.hbm.tileentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Magnetohydrodynamic turbine: the fusion chain's electrical output, converting plasma straight to HE
 * with no steam loop in between.
 *
 * <p>Conversion is better than unity ({@link #PLASMA_EFFICIENCY}), which is the point of the machine:
 * plasma is worth more through an MHD turbine than through a boiler and a steam turbine. But it only
 * pays out in full above {@link #MINIMUM_PLASMA}; a trickle halves the yield, so the
 * turbine rewards one big reactor over several small ones.
 *
 * <p>It also has to be actively cooled: without perfluoromethyl to run through, it produces nothing at
 * all rather than merely less.
 */
public class TileEntityFusionMHDT extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    public static final double PLASMA_EFFICIENCY = 1.35D;
    public static final int COOLANT_USE = 50;
    /** Below this, the turbine yields half. Configurable upstream; a constant here (no config file). */
    public static final long MINIMUM_PLASMA = 5_000_000L;
    public static final float ROTOR_ACCELERATION = 0.125F;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000),
            new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000),
    };

    public long plasmaEnergy;
    private long plasmaEnergySync;

    /** Last tick's plasma, which 1.7's OpenComputers callbacks reported. */
    public long plasmaEnergySync() { return plasmaEnergySync; }
    public long power;

    /** Client-side rotor spin. */
    public float rotor;
    public float prevRotor;
    public float rotorSpeed;


    public TileEntityFusionMHDT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_MHDT.get(), pos, state);
    }

    public boolean hasMinimumPlasma() {
        return plasmaEnergy >= MINIMUM_PLASMA;
    }

    /** The turbine only runs while there is cold coolant to spend and room for the warmed return. */
    public boolean isCool() {
        return tanks[0].getFill() >= COOLANT_USE && tanks[1].getFill() + COOLANT_USE <= tanks[1].getMaxFill();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionMHDT te) {
        te.plasmaEnergySync = te.plasmaEnergy;

        if (te.isCool()) {
            te.power = (long) Math.floor(te.plasmaEnergy * PLASMA_EFFICIENCY);
            if (!te.hasMinimumPlasma()) te.power /= 2;
            te.tanks[0].setFill(te.tanks[0].getFill() - COOLANT_USE);
            te.tanks[1].setFill(te.tanks[1].getFill() + COOLANT_USE);
        }

        for (DirPos port : te.getConPos()) {
            te.tryProvide(level, port.getPos().getX(), port.getPos().getY(), port.getPos().getZ(), port.getDir());
            if (te.tanks[0].getTankType() != Fluids.NONE) te.trySubscribe(te.tanks[0].getTankType(), level, port);
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, port);
        }

        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        FusionLink.claim(level, te, plasmaCell(pos, dir), MachineCaps.PLASMA_IN);

        te.networkPackNT();
        te.plasmaEnergy = 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFusionMHDT te) {
        if (te.plasmaEnergy > 0 && te.isCool()) te.rotorSpeed += ROTOR_ACCELERATION;
        else te.rotorSpeed -= ROTOR_ACCELERATION;
        // The rotor visibly tops out lower when the turbine is under-fed, which is the only cue that
        // the halved-output threshold has not been met.
        te.rotorSpeed = Mth.clamp(te.rotorSpeed, 0F, te.hasMinimumPlasma() ? 15F : 10F);

        te.prevRotor = te.rotor;
        te.rotor += te.rotorSpeed;
        if (te.rotor >= 360F) {
            te.rotor -= 360F;
            te.prevRotor -= 360F;
        }
    }

    public List<DirPos> getConPos() {
        ForgeDirection dir = FusionNodes.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x + dir.offsetX * 4 + rot.offsetX * 4, y, z + dir.offsetZ * 4 + rot.offsetZ * 4, rot),
                new DirPos(x + dir.offsetX * 4 - rot.offsetX * 4, y, z + dir.offsetZ * 4 - rot.offsetZ * 4, rot.getOpposite()),
                new DirPos(x + dir.offsetX * 8, y + 1, z + dir.offsetZ * 8, dir));
    }

    @Override public boolean receivesFusionPower() { return true; }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;
    }

    @Override
    public void setRemoved() {
        FusionLink.release(level, plasmaCell(worldPosition,
                FusionNodes.facing(getBlockState()).getOpposite()));
        super.setRemoved();
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return Long.MAX_VALUE; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(plasmaEnergySync);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        plasmaEnergy = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        tanks[0].writeToNBT(out, "t0");
        tanks[1].writeToNBT(out, "t1");
        out.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        tanks[0].readFromNBT(in, "t0");
        tanks[1].readFromNBT(in, "t1");
        power = in.getLongOr("power", 0L);
    }

    @Override protected boolean syncFullTagOnChunkLoad() { return true; }

    /** Where this machine claims its end of the torus beam, 6 blocks out the back at +2. */
    private static BlockPos plasmaCell(BlockPos pos, ForgeDirection dir) {
        return new BlockPos(pos.getX() + dir.offsetX * 6, pos.getY() + 2,
                pos.getZ() + dir.offsetZ * 6);
    }
}
