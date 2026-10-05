package com.hbm.tileentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;

/**
 * Fusion boiler — turns a torus's plasma output into superheated steam.
 *
 * <p>It is the simplest plasma consumer: water in, superhot steam out, one millibucket per
 * {@link HeatRecipes#getBoilerStep water heat step} of plasma energy. Note it goes straight from water
 * to <b>superhot</b> steam rather than walking the usual boiler ladder — the plasma is hot enough that
 * the intermediate stages do not apply.
 *
 * <p>All the work happens in {@link #receiveFusionPower}, which the torus calls; the tick only keeps
 * the node alive and moves fluid.
 */
public class TileEntityFusionBoiler extends TileEntityLoadedBase
        implements IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 32_000),
            new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32_000),
    };

    /** Plasma received this tick; zeroed every tick and mirrored for the client. */
    public long plasmaEnergy;
    private long plasmaEnergySync;

    /** Last tick's plasma, which 1.7's OpenComputers callbacks reported. */
    public long plasmaEnergySync() { return plasmaEnergySync; }


    public TileEntityFusionBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BOILER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionBoiler te) {
        te.plasmaEnergySync = te.plasmaEnergy;
        te.plasmaEnergy = 0;

        for (DirPos port : te.getConPos()) {
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, port);
        }

        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        FusionLink.claim(level, te, plasmaCell(pos, dir), MachineCaps.PLASMA_IN);

        te.networkPackNT();
    }

    public List<DirPos> getConPos() {
        ForgeDirection dir = FusionNodes.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x - dir.offsetX + rot.offsetX * 2, y, z - dir.offsetZ + rot.offsetZ * 2, rot),
                new DirPos(x - dir.offsetX - rot.offsetX * 2, y, z - dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(x + dir.offsetX * 2 + rot.offsetX * 2, y, z + dir.offsetZ * 2 + rot.offsetZ * 2, rot),
                new DirPos(x + dir.offsetX * 2 - rot.offsetX * 2, y, z + dir.offsetZ * 2 - rot.offsetZ * 2, rot.getOpposite()));
    }

    @Override public boolean receivesFusionPower() { return true; }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;

        HeatRecipes.HeatStep step = HeatRecipes.getBoilerStep(tanks[0].getTankType());
        if (step == null || step.heatReq() <= 0) return;

        int waterCycles = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
        // The min is folded into the division rather than applied afterwards, so a torus putting out a
        // truly absurd amount of power cannot overflow the cast — water cycles are capped by the buffer.
        int steamCycles = (int) Math.min(fusionPower / step.heatReq(), waterCycles);

        if (steamCycles <= 0) return;
        tanks[0].setFill(tanks[0].getFill() - steamCycles);
        tanks[1].setFill(tanks[1].getFill() + steamCycles);

        if (level != null && !level.isClientSide() && level.getRandom().nextInt(200) == 0) {
            level.playSound(null, worldPosition.above(2), ModSounds.BOILER_GROAN.get(),
                    SoundSource.BLOCKS, 2.5F, 1.0F);
        }
    }

    @Override
    public void setRemoved() {
        FusionLink.release(level, plasmaCell(worldPosition,
                FusionNodes.facing(getBlockState()).getOpposite()));
        super.setRemoved();
    }

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
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        tanks[0].readFromNBT(in, "t0");
        tanks[1].readFromNBT(in, "t1");
    }

    @Override protected boolean syncFullTagOnChunkLoad() { return true; }

    /** Where this machine claims its end of the torus beam, 4 blocks out the back at +2. */
    private static BlockPos plasmaCell(BlockPos pos, ForgeDirection dir) {
        return new BlockPos(pos.getX() + dir.offsetX * 4, pos.getY() + 2,
                pos.getZ() + dir.offsetZ * 4);
    }
}
