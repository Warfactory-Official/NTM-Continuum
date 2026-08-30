package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The small and polonium RTGs share the old one-cell generator implementation. */
public class TileEntityMachineMiniRTG extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IEnergyConnectorMK2 {

    public long power;

    public TileEntityMachineMiniRTG(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_MINI_RTG.get(), pos, state);
    }

    public boolean isPolonium() {
        return getBlockState().is(ModMachineBlocks.rtg_polonium.get());
    }

    public long getGeneration() {
        return isPolonium() ? 2500L : 70L;
    }

    @Override
    public long getMaxPower() {
        return isPolonium() ? 50000L : 10000L;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMiniRTG generator) {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            generator.tryProvide(level, pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ), dir);
        }
        generator.power = Math.min(generator.power + generator.getGeneration(), generator.getMaxPower());
        generator.setChanged();
        generator.networkPackNT();
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
    }

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
