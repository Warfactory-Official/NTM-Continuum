package com.hbm.tileentity.machine.solar;

import dev.golem.core.data.Tags;

import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A solar mirror that feeds heat into the boiler at its recorded target coordinates. */
public class TileEntitySolarMirror extends TileEntityLoadedBase {

    public int tX;
    public int tY;
    public int tZ;
    public boolean isOn;

    public TileEntitySolarMirror(BlockPos pos, BlockState state) {
        super(ModSolarBlockEntities.SOLAR_MIRROR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySolarMirror te) {
        if (TickPhase.every(level, pos, 20L)) te.networkPackNT();

        if (te.tY < pos.getY()) {
            te.isOn = false;
            return;
        }

        int sun = getSun(level, pos);
        BlockPos skyCheck = pos.above();
        // 1.12's canSeeSky(pos.up()) rejected a block occupying pos.up(); modern heightmap
        // semantics consider the topmost solid cell sky-visible, so retain the explicit obstruction.
        if (!isSolarActive(sun, level.getBlockState(skyCheck).isAir() && level.canSeeSky(skyCheck))) {
            te.isOn = false;
            return;
        }

        te.isOn = true;
        BlockEntity target = level.getBlockEntity(new BlockPos(te.tX, te.tY - 1, te.tZ));
        if (target instanceof TileEntitySolarBoiler boiler) {
            boiler.heat += sun;
        }
    }

    /** The old {@code skyLight - skylightSubtracted - 11} heat contribution. */
    public static int getSun(Level level, BlockPos pos) {
        return calculateSun(level.getBrightness(LightLayer.SKY, pos), level.getSkyDarken());
    }

    /** Pure form of the legacy skylight calculation, exposed for deterministic tests. */
    public static int calculateSun(int skyLight, int skyDarken) {
        return skyLight - skyDarken - 11;
    }

    /** Pure sunlight/sky visibility gate used by the server tick. */
    public static boolean isSolarActive(int sun, boolean canSeeSky) {
        return sun > 0 && canSeeSky;
    }

    public void setTarget(BlockPos target) {
        setTarget(target.getX(), target.getY(), target.getZ());
    }

    public void setTarget(int x, int y, int z) {
        tX = x;
        tY = y;
        tZ = z;
        setChanged();
        networkPackNT();
    }

    public BlockPos getTarget() {
        return new BlockPos(tX, tY, tZ);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(tX);
        buf.writeInt(tY);
        buf.writeInt(tZ);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tX = buf.readInt();
        tY = buf.readInt();
        tZ = buf.readInt();
        isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("targetX", tX);
        output.putInt("targetY", tY);
        output.putInt("targetZ", tZ);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tX = input.getIntOr("targetX", 0);
        tY = input.getIntOr("targetY", 0);
        tZ = input.getIntOr("targetZ", 0);
    }
}
