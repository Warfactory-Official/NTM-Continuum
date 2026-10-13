package com.hbm.tileentity.network;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The large four-wire pylon: 100 m, and no cable face at all -- it needs a substation on each end. */
public class TileEntityPylonLarge extends TileEntityPylonBase {

    public TileEntityPylonLarge(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.PYLON_LARGE.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        double topOff = 0.75 + 0.0625;
        double sideOff = 3.375;
        Vec3 vec = new Vec3(sideOff, 0, 0);
        // 1.7 Vec3.rotateAroundY and modern yRot share the formula (x cos + z sin, z cos - x sin).
        switch (facingMeta()) {
            case 2 -> vec = vec.yRot((float) Math.PI * 0.0F);
            case 4 -> vec = vec.yRot((float) Math.PI * 0.25F);
            case 3 -> vec = vec.yRot((float) Math.PI * 0.5F);
            case 5 -> vec = vec.yRot((float) Math.PI * 0.75F);
            default -> { }
        }
        return new Vec3[]{
                new Vec3(0.5 + vec.x, 11.5 + topOff, 0.5 + vec.z),
                new Vec3(0.5 + vec.x, 11.5 - topOff, 0.5 + vec.z),
                new Vec3(0.5 - vec.x, 11.5 + topOff, 0.5 - vec.z),
                new Vec3(0.5 - vec.x, 11.5 - topOff, 0.5 - vec.z),
        };
    }

    @Override
    public double getMaxWireLength() {
        return 100;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return false;
    }

    @Override
    protected int getCellMask(int index) {
        return 0;
    }
}
