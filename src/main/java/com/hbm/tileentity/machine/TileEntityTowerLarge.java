package com.hbm.tileentity.machine;

import com.hbm.config.ClientConfig;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/** 9x9x13 cooling tower; 10,000 mB condenser tanks and twelve perimeter ports. */
public class TileEntityTowerLarge extends TileEntityCondenser {
    public static int inputTankSizeTL = 10_000;
    public static int outputTankSizeTL = 10_000;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "condenserTowerLarge"; }

        @Override public void readIfPresent(JsonObject obj) {
            inputTankSizeTL = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSizeTL);
            outputTankSizeTL = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSizeTL);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:inputTankSize").value(inputTankSizeTL);
            writer.name("I:outputTankSize").value(outputTankSizeTL);
        }
    };

    public TileEntityTowerLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TOWER_LARGE.get(), pos, state, inputTankSizeTL, outputTankSizeTL);
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
