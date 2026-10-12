package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 9x9x13 cooling tower; exact old 10,000 mB condenser tanks and twelve perimeter ports. */
public class TileEntityTowerLarge extends TileEntityCondenser {
    public static final int INPUT_TANK_SIZE = 10_000;
    public static final int OUTPUT_TANK_SIZE = 10_000;

    public TileEntityTowerLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TOWER_LARGE.get(), pos, state, INPUT_TANK_SIZE, OUTPUT_TANK_SIZE);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerLarge tower) { tower.serverTickCommon(level); }

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerLarge tower) {
        if (com.hbm.config.ClientConfig.COOLING_TOWER_PARTICLES && tower.waterTimer > 0 && level.getGameTime() % 4L == 0L)
            tower.spawnSmoke(level, pos, 1D, 0.5F, 1F, 10F, 750 + level.getRandom().nextInt(250));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_tower_large"; }

    @Override public DirPos[] getConPos() {
        BlockPos p = getBlockPos();
        return new DirPos[]{new DirPos(p.offset(5, 0, 0), ForgeDirection.EAST),
                new DirPos(p.offset(5, 0, 3), ForgeDirection.EAST),
                new DirPos(p.offset(5, 0, -3), ForgeDirection.EAST),
                new DirPos(p.offset(-5, 0, 0), ForgeDirection.WEST),
                new DirPos(p.offset(-5, 0, 3), ForgeDirection.WEST),
                new DirPos(p.offset(-5, 0, -3), ForgeDirection.WEST),
                new DirPos(p.offset(0, 0, 5), ForgeDirection.SOUTH),
                new DirPos(p.offset(3, 0, 5), ForgeDirection.SOUTH),
                new DirPos(p.offset(-3, 0, 5), ForgeDirection.SOUTH),
                new DirPos(p.offset(0, 0, -5), ForgeDirection.NORTH),
                new DirPos(p.offset(3, 0, -5), ForgeDirection.NORTH),
                new DirPos(p.offset(-3, 0, -5), ForgeDirection.NORTH)};
    }

}
