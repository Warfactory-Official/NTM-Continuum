package com.hbm.tileentity.machine;

import com.hbm.config.ClientConfig;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 9x9x13 cooling tower; 10,000 mB condenser tanks and twelve perimeter ports. */
public class TileEntityTowerLarge extends TileEntityCondenser {
    public static final int INPUT_TANK_SIZE = 10_000;
    public static final int OUTPUT_TANK_SIZE = 10_000;

    public TileEntityTowerLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TOWER_LARGE.get(), pos, state, INPUT_TANK_SIZE, OUTPUT_TANK_SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerLarge tower) { tower.serverTickCommon(level); }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerLarge tower) {
        if (ClientConfig.COOLING_TOWER_PARTICLES && tower.waterTimer > 0 && level.getGameTime() % 4L == 0L)
            tower.spawnSmoke(level, pos, 1D, 0.5F, 1F, 10F, 750 + level.getRandom().nextInt(250));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_tower_large"; }

    @Override
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.bigTower(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

}
