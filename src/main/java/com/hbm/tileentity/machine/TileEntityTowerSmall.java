package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Auxiliary 5x5x19 cooling tower; exact old 1,000 mB condenser tanks and four side ports. */
public final class TileEntityTowerSmall extends TileEntityCondenser {
    public static final int INPUT_TANK_SIZE = 1_000;
    public static final int OUTPUT_TANK_SIZE = 1_000;

    public TileEntityTowerSmall(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TOWER_SMALL.get(), pos, state, INPUT_TANK_SIZE, OUTPUT_TANK_SIZE);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerSmall tower) { tower.serverTickCommon(level); }

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerSmall tower) {
        if (tower.waterTimer > 0 && level.getGameTime() % 2L == 0L)
            tower.spawnSmoke(level, pos, 18D, 1F, 0.5F, 4F, 250 + level.getRandom().nextInt(250));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_tower_small"; }

    @Override public DirPos[] getConPos() {
        BlockPos p = getBlockPos();
        return new DirPos[]{new DirPos(p.offset(3, 0, 0), ForgeDirection.EAST),
                new DirPos(p.offset(-3, 0, 0), ForgeDirection.WEST),
                new DirPos(p.offset(0, 0, 3), ForgeDirection.SOUTH),
                new DirPos(p.offset(0, 0, -3), ForgeDirection.NORTH)};
    }

}
