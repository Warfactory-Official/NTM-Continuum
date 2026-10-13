package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.machine.Floodlight;
import com.hbm.blocks.machine.FloodlightBeam;
import com.hbm.tileentity.ModDecoBlockEntities;

/**
 * 1.7 {@code Floodlight.TileEntityFloodlight}: eats 100 HE/t from the cable behind it and, while lit,
 * re-casts one of its fifteen rays every five ticks (a 5x3 fan, 7.5 deg by 15 deg apart) up to 63 blocks,
 * dropping a {@link FloodlightBeam} light block at the last transparent cell of each ray. Losing power
 * puts the lights out and waits 60 ticks before it may relight.
 */
public class TileEntityFloodlight extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    public float rotation;
    protected final BlockPos[] lightPos = new BlockPos[15];
    public static final long maxPower = 5_000;
    public long power;
    public int delay;
    public boolean isOn;

    public TileEntityFloodlight(BlockPos pos, BlockState state) {
        super(ModDecoBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (power >= 100) {
            power -= 100;
            if (!isOn) {
                this.isOn = true;
                this.castLights();
                setChanged();
                dataChanged();
            } else {
                long timer = level.getGameTime();
                if (timer % 5 == 0) {
                    timer = timer / 5;
                    this.castLight((int) Math.abs(timer % this.lightPos.length));
                }
            }
        } else {
            if (isOn) {
                this.isOn = false;
                this.delay = 60;
                this.destroyLights();
                setChanged();
                dataChanged();
            }
        }
        networkPackMK2();
    }

    private int meta() {
        return Floodlight.meta(getBlockState());
    }

    private void castLight(int index) {
        Level level = getLevel();
        BlockPos newPos = this.getRayEndpoint(index);
        BlockPos oldPos = this.lightPos[index];
        this.lightPos[index] = null;
        if (newPos == null || !newPos.equals(oldPos)) { //if the new end point is null or not equal to the previous, delete the previous spot
            if (oldPos != null) {
                BlockEntity tile = level.getBlockEntity(oldPos);
                if (tile instanceof FloodlightBeam.TileEntityFloodlightBeam beam) {
                    if (beam.cache == this) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        if (newPos == null) return;
        if (level.getBlockState(newPos).isAir()) {
            level.setBlock(newPos, ModDecoContent.floodlight_beam.get().defaultBlockState(), 2);
            BlockEntity tile = level.getBlockEntity(newPos);
            if (tile instanceof FloodlightBeam.TileEntityFloodlightBeam beam) beam.setSource(this, worldPosition, index);
            this.lightPos[index] = newPos;
        }
        if (level.getBlockState(newPos).is(ModDecoContent.floodlight_beam.get())) {
            this.lightPos[index] = newPos;
        }
    }

    @Nullable
    public BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= lightPos.length) return null;
        Level level = getLevel();
        int meta = meta();
        Vec3 dir = new Vec3(1, 0, 0);
        float[] angles = getVariation(index);
        float rotation = this.rotation;
        if (meta == 1 || meta == 7) rotation = 180 - rotation;
        if (meta == 6) rotation = 180 - rotation;
        // Vec3.zRot/yRot are the 1.7 rotateAroundZ/Y formulas, sign and all.
        dir = dir.zRot((float) (rotation / 180D * Math.PI) + angles[0]);
        if (meta == 6) dir = dir.yRot((float) (Math.PI / 2D));
        if (meta == 7) dir = dir.yRot((float) (Math.PI / 2D));
        if (meta == 2) dir = dir.yRot((float) (Math.PI / 2D));
        if (meta == 3) dir = dir.yRot((float) -(Math.PI / 2D));
        if (meta == 4) dir = dir.yRot((float) (Math.PI));
        dir = dir.yRot(angles[1]);
        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
        for (int i = 1; i < 64; i++) {
            int iX = (int) Math.floor(xCoord + 0.5 + dir.x * i);
            int iY = (int) Math.floor(yCoord + 0.5 + dir.y * i);
            int iZ = (int) Math.floor(zCoord + 0.5 + dir.z * i);
            if (iX == xCoord && iY == yCoord && iZ == zCoord) continue;
            BlockPos at = new BlockPos(iX, iY, iZ);
            // 1.7: getLightOpacity < 127 passes (glass, air, leaves); a non-solid-rendering block is the same test here
            if (!Compat.isSolidRender(level.getBlockState(at), level, at)) continue;
            int fX = (int) Math.floor(xCoord + 0.5 + dir.x * (i - 1));
            int fY = (int) Math.floor(yCoord + 0.5 + dir.y * (i - 1));
            int fZ = (int) Math.floor(zCoord + 0.5 + dir.z * (i - 1));
            if (i > 1) return new BlockPos(fX, fY, fZ);
        }
        return null;
    }

    private void castLights() {
        for (int i = 0; i < this.lightPos.length; i++) this.castLight(i);
    }

    private void destroyLight(int index) {
        BlockPos pos = lightPos[index];
        if (pos != null && getLevel().getBlockState(pos).is(ModDecoContent.floodlight_beam.get())) {
            getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public void destroyLights() {
        for (int i = 0; i < this.lightPos.length; i++) destroyLight(i);
    }

    private float[] getVariation(int index) {
        return new float[]{
                (((index / 3) - 2) * 7.5F) / 180F * (float) Math.PI,
                (((index % 3) - 1) * 15F) / 180F * (float) Math.PI
        };
    }

    /** Whether {@code pos} is the current endpoint of ray {@code index} (the beam's liveness test). */
    public boolean holdsLight(int index, BlockPos pos) {
        return index >= 0 && index < lightPos.length && pos.equals(lightPos[index]);
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        this.rotation = in.getFloatOr("rotation", 0F);
        this.power = in.getLongOr("power", 0L);
        this.isOn = in.getBooleanOr("isOn", false);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putFloat("rotation", rotation);
        out.putLong("power", power);
        out.putBoolean("isOn", isOn);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(rotation);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        rotation = buf.readFloat();
        isOn = buf.readBoolean();
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    /** Power only from behind the fixture (1.7 subscribed to the block opposite its facing). */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        Direction facing = getBlockState().getValue(Floodlight.FACING);
        return dir != ForgeDirection.UNKNOWN && dir.toDirection() == facing.getOpposite();
    }
}
