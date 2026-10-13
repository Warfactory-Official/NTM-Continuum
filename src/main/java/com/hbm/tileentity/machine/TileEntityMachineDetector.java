package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.PowerDetector;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** 1.7's {@code TileEntityMachineDetector}: a 5 HE receiver that lights while it is being fed. */
public class TileEntityMachineDetector extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    long power;

    public TileEntityMachineDetector(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.MACHINE_DETECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDetector detector) {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            detector.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
        boolean on = false;
        if (detector.power > 0) {
            on = true;
            detector.power--;
        }
        if (state.getValue(PowerDetector.ON) != on) {
            level.setBlock(pos, state.setValue(PowerDetector.ON, on), 3);
            detector.setChanged();
        }
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return 5; }
    @Override public ConnectionPriority getPriority() { return ConnectionPriority.HIGH; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
    }
}
