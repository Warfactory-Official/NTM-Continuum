package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Stores the smoothed chunk dose and exposes the old exponential redstone ladders. */
public class TileEntityRadSensor extends SyncedBlockEntity {

    public double chunkRads;
    public double recievedDose;
    public double lastChunkRads;
    public int redstoneOutput;
    public int comparatorOutput;
    private int lastRedstoneOutput;
    private int lastComparatorOutput;

    public TileEntityRadSensor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAD_SENSOR.get(), pos, state);
    }

    public int getRestoneOutput() {
        if (chunkRads < 0.01) return 0;
        if (chunkRads < 0.5) return 1;
        if (chunkRads < 1) return 2;
        if (chunkRads < 2) return 3;
        if (chunkRads < 4) return 4;
        if (chunkRads < 8) return 5;
        if (chunkRads < 16) return 6;
        if (chunkRads < 32) return 7;
        if (chunkRads < 64) return 8;
        if (chunkRads < 128) return 9;
        if (chunkRads < 256) return 10;
        if (chunkRads < 512) return 11;
        if (chunkRads < 1024) return 12;
        if (chunkRads < 2048) return 13;
        if (chunkRads < 4096) return 14;
        return 15;
    }

    public int getComparatorOutput() {
        if (recievedDose < 0.01) return 0;
        if (recievedDose < 8) return 1;
        if (recievedDose < 16) return 2;
        if (recievedDose < 32) return 3;
        if (recievedDose < 64) return 4;
        if (recievedDose < 128) return 5;
        if (recievedDose < 256) return 6;
        if (recievedDose < 512) return 7;
        if (recievedDose < 1024) return 8;
        if (recievedDose < 2048) return 9;
        if (recievedDose < 4096) return 10;
        if (recievedDose < 8192) return 11;
        if (recievedDose < 16384) return 12;
        if (recievedDose < 32768) return 13;
        if (recievedDose < 65536) return 14;
        return 15;
    }

    private static int getRedstonePower(Level level, BlockPos pos, Direction side) {
        BlockPos source = pos.relative(side);
        int worldPower = level.getSignal(source, side);
        if (worldPower >= 15) return worldPower;
        BlockState sourceState = level.getBlockState(source);
        if (sourceState.getBlock() instanceof RedStoneWireBlock) {
            return Math.max(worldPower, sourceState.getValue(RedStoneWireBlock.POWER));
        }
        return worldPower;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRadSensor sensor) {
        sensor.chunkRads = (ChunkRadiationManager.proxy.getRadiation(level, pos) + sensor.lastChunkRads) / 2.0;
        if (getRedstonePower(level, pos, Direction.DOWN) > 0) {
            sensor.recievedDose = 0;
        } else {
            sensor.recievedDose += sensor.chunkRads / 20.0;
        }

        sensor.redstoneOutput = sensor.getRestoneOutput();
        sensor.comparatorOutput = sensor.getComparatorOutput();
        if (sensor.redstoneOutput != sensor.lastRedstoneOutput
                || sensor.comparatorOutput != sensor.lastComparatorOutput) {
            level.updateNeighborsAt(pos, state.getBlock());
        }
        sensor.lastChunkRads = sensor.chunkRads;
        sensor.lastRedstoneOutput = sensor.redstoneOutput;
        sensor.lastComparatorOutput = sensor.comparatorOutput;
        sensor.setChanged();
        sensor.syncState();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(chunkRads);
        buf.writeDouble(recievedDose);
        buf.writeInt(redstoneOutput);
        buf.writeInt(comparatorOutput);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        chunkRads = buf.readDouble();
        recievedDose = buf.readDouble();
        redstoneOutput = buf.readInt();
        comparatorOutput = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putDouble("chunkRads", chunkRads);
        output.putDouble("recievedDose", recievedDose);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        chunkRads = input.getDoubleOr("chunkRads", 0D);
        recievedDose = input.getDoubleOr("recievedDose", 0D);
        lastChunkRads = chunkRads;
    }
}
