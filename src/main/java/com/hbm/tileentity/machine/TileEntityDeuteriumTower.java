package com.hbm.tileentity.machine;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Large deuterium tower variant with the legacy 50,000/5,000 mB tanks and eight ports. */
public class TileEntityDeuteriumTower extends TileEntityDeuteriumExtractor {

    public TileEntityDeuteriumTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DEUTERIUM_TOWER.get(), pos, state, 50_000, 5_000, 100_000);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityDeuteriumTower tower) {
        TileEntityDeuteriumExtractor.serverTick(level, pos, state, tower);
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rotation = dir.getRotation(ForgeDirection.DOWN);
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.offset(-dir.offsetX * 2, 0, -dir.offsetZ * 2), dir.getOpposite()),
                new DirPos(pos.offset(-dir.offsetX * 2 + rotation.offsetX, 0,
                        -dir.offsetZ * 2 + rotation.offsetZ), dir.getOpposite()),
                new DirPos(pos.offset(dir.offsetX, 0, dir.offsetZ), dir),
                new DirPos(pos.offset(dir.offsetX + rotation.offsetX, 0,
                        dir.offsetZ + rotation.offsetZ), dir),
                new DirPos(pos.offset(-rotation.offsetX, 0, -rotation.offsetZ), rotation.getOpposite()),
                new DirPos(pos.offset(-dir.offsetX - rotation.offsetX, 0,
                        -dir.offsetZ - rotation.offsetZ), rotation.getOpposite()),
                new DirPos(pos.offset(rotation.offsetX * 2, 0, rotation.offsetZ * 2), rotation),
                new DirPos(pos.offset(-dir.offsetX + rotation.offsetX * 2, 0,
                        -dir.offsetZ + rotation.offsetZ * 2), rotation)
        };
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_deuterium_tower";
    }
}
