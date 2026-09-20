package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Heat-to-energy core for the legacy Stirling engine family. */
public class TileEntityStirling extends TileEntityMachineBase implements IEnergyProviderMK2 {

    public static double diffusion = 0.1D;
    public static double efficiency = 0.5D;
    public static int maxHeatNormal = 300;
    public static int maxHeatSteel = 1500;
    public static int overspeedLimit = 300;

    public long powerBuffer;
    public int heat;
    public boolean hasCog = true;
    public float spin;
    public float lastSpin;

    private int warnCooldown;
    private int overspeed;
    private int syncHeat;
    private boolean destroyedByCreativePlayer;

    public TileEntityStirling(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_STIRLING.get(), pos, state, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStirling stirling) {
        if (stirling.hasCog) {
            stirling.powerBuffer = 0;
            stirling.tryPullHeat(level, pos);
            stirling.powerBuffer = (long) (stirling.heat * (stirling.isCreative() ? 1D : efficiency));

            if (stirling.warnCooldown > 0) stirling.warnCooldown--;
            if (stirling.heat > stirling.maxHeat() && !stirling.isCreative()) {
                stirling.overspeed++;
                // The legacy warning sound and flying EntityCog are deferred; the destructive threshold
                // and the missing-gear state remain identical until that entity is ported.
                if (stirling.overspeed > overspeedLimit) {
                    stirling.hasCog = false;
                    level.explode(null, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D,
                            5F, false, Level.ExplosionInteraction.NONE);
                    stirling.setChanged();
                    stirling.overspeed = 0;
                }
            } else {
                stirling.overspeed = 0;
            }
        } else {
            stirling.overspeed = 0;
            stirling.warnCooldown = 0;
        }

        stirling.syncHeat = stirling.heat;
        stirling.networkPackNT();
        if (stirling.hasCog) {
            for (DirPos connection : stirling.getConPos()) {
                stirling.tryProvide(level, connection.getPos().getX(), connection.getPos().getY(),
                        connection.getPos().getZ(), connection.getDir());
            }
        } else if (stirling.powerBuffer > 0) {
            stirling.powerBuffer--;
        }
        stirling.heat = 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityStirling stirling) {
        float momentum = stirling.powerBuffer * 50F / (float) stirling.maxHeat();
        if (stirling.isCreative()) momentum = Math.min(momentum, 45F);
        stirling.lastSpin = stirling.spin;
        stirling.spin += momentum;
        if (stirling.spin >= 360F) {
            stirling.spin -= 360F;
            stirling.lastSpin -= 360F;
        }
    }

    public int getGearVariant() {
        return getBlockState().getBlock() == ModMachineBlocks.machine_stirling.get() ? 0
                : getBlockState().getBlock() == ModMachineBlocks.machine_stirling_creative.get() ? 2 : 1;
    }

    public int maxHeat() {
        return isCreative() ? maxHeatSteel : getGearVariant() == 0 ? maxHeatNormal : maxHeatSteel;
    }

    public boolean isCreative() {
        return getBlockState().getBlock() == ModMachineBlocks.machine_stirling_creative.get();
    }

    public DirPos[] getConPos() {
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.east(2), Library.POS_X),
                new DirPos(pos.west(2), Library.NEG_X),
                new DirPos(pos.south(2), Library.POS_Z),
                new DirPos(pos.north(2), Library.NEG_Z)
        };
    }

    private void tryPullHeat(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos.below()) instanceof IHeatSource source) {
            int heatSource = (int) (source.getHeatStored() * diffusion);
            if (heatSource > 0) {
                source.useUpHeat(heatSource);
                heat += heatSource;
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeInt(syncHeat);
        buf.writeBoolean(hasCog);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        powerBuffer = buf.readLong();
        heat = buf.readInt();
        hasCog = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("powerBuffer", powerBuffer);
        output.putBoolean("hasCog", hasCog);
        output.putInt("overspeed", overspeed);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        powerBuffer = input.getLongOr("powerBuffer", 0L);
        hasCog = input.getBooleanOr("hasCog", true);
        overspeed = input.getIntOr("overspeed", 0);
    }

    public void setDestroyedByCreativePlayer() {
        destroyedByCreativePlayer = true;
    }

    public boolean shouldDrop() {
        return !destroyedByCreativePlayer;
    }

    @Override public long getPower() { return powerBuffer; }
    @Override public void setPower(long power) { powerBuffer = power; }
    @Override public long getMaxPower() { return powerBuffer; }
    @Override public String getDefaultName() { return "block.hbm.machine_stirling"; }
}
