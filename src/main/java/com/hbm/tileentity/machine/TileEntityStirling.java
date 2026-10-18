package com.hbm.tileentity.machine;

import com.hbm.entity.projectile.EntityCog;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

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
        stirling.setupPowerPorts(stirling.getPorts());
        stirling.updateAllPorts();
        stirling.providePower();

        if (stirling.hasCog) {
            stirling.powerBuffer = 0;
            stirling.tryPullHeat(level, pos);
            stirling.powerBuffer = (long) (stirling.heat * (stirling.isCreative() ? 1D : efficiency));

            if (stirling.warnCooldown > 0) stirling.warnCooldown--;
            if (stirling.heat > stirling.maxHeat() && !stirling.isCreative()) {
                stirling.overspeed++;

                if (stirling.overspeed > 60 && stirling.warnCooldown == 0) {
                    stirling.warnCooldown = 100;
                    level.playSound(null, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D,
                            ModSounds.WARN_OVERSPEED.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                }

                if (stirling.overspeed > overspeedLimit) {
                    stirling.hasCog = false;
                    level.explode(null, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D,
                            5F, false, Level.ExplosionInteraction.NONE);

                    // 1.7: the gear flies off sideways (facing rotated DOWN) and upward with the excess heat.
                    int orientation = MachineCore.facingOrdinal(state, 2);
                    ForgeDirection dir = ForgeDirection.getOrientation(orientation);
                    EntityCog cog = new EntityCog(level,
                            pos.getX() + 0.5D + dir.offsetX, pos.getY() + 1D, pos.getZ() + 0.5D + dir.offsetZ)
                            .setOrientation(orientation).setMeta(stirling.getGearVariant());
                    ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
                    cog.setDeltaMovement(rot.offsetX, 1D + (stirling.heat - stirling.maxHeat()) * 0.0001D, rot.offsetZ);
                    level.addFreshEntity(cog);

                    stirling.setChanged();
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
        if (!stirling.hasCog && stirling.powerBuffer > 0) {
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

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x - 1, y, z, Library.NEG_X),
                    PortDef.make(x + 1, y, z, Library.POS_X),
                    PortDef.make(x, y, z - 1, Library.NEG_Z),
                    PortDef.make(x, y, z + 1, Library.POS_Z),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    @Override
    public long getProviderSpeed() {
        return this.hasCog ? this.getMaxPower() : 0;
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
