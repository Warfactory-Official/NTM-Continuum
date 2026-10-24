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

/** Auxiliary 5x5x19 cooling tower; 1,000 mB condenser tanks and four side ports. */
public class TileEntityTowerSmall extends TileEntityCondenser {
    public static int inputTankSizeTS = 1_000;
    public static int outputTankSizeTS = 1_000;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "condenserTowerSmall"; }

        @Override public void readIfPresent(JsonObject obj) {
            inputTankSizeTS = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSizeTS);
            outputTankSizeTS = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSizeTS);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:inputTankSize").value(inputTankSizeTS);
            writer.name("I:outputTankSize").value(outputTankSizeTS);
        }
    };

    public TileEntityTowerSmall(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TOWER_SMALL.get(), pos, state, inputTankSizeTS, outputTankSizeTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerSmall tower) { tower.serverTickCommon(level); }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityTowerSmall tower) {
        if (ClientConfig.COOLING_TOWER_PARTICLES && tower.waterTimer > 0 && level.getGameTime() % 2L == 0L)
            tower.spawnSmoke(level, pos, 18D, 1F, 0.5F, 4F, 250 + level.getRandom().nextInt(250));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_tower_small"; }

    @Override
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.auxTower(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

}
