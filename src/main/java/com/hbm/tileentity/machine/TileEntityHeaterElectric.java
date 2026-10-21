package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.client.sound.HeaterSoundHandler;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * Electric heating unit (1.12.2 {@code TileEntityHeaterElectric}): turns HE into heat, and also relays
 * heat from an {@link IHeatSource} below at 85% efficiency (so it can boost a firebox). The constants
 * are 1.7's: consumption = {@code setting^1.4 × 200} HE/t, heat generated = {@code setting × 100} TU/t,
 * buffer = 20 ticks of consumption, reservoir decays ×0.999/t, setting 0–10. The port has no
 * screwdriver/hand-drill tools, so the setting is cycled by right-clicking the block (sneak = down).
 */
public class TileEntityHeaterElectric extends TileEntityLoadedBase implements IHeatSource, IEnergyReceiverMK2 {

    public long power;
    public int heatEnergy;
    public boolean isOn;
    public int setting = 0;

    public TileEntityHeaterElectric(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_ELECTRIC.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterElectric te) {
        te.setupPowerPorts(te.getPorts());
        te.updateAllPorts();
        te.receivePower();
        te.heatEnergy *= 0.999;

        te.tryPullHeat(level, pos);

        te.isOn = false;
        if (te.setting > 0 && te.power >= te.getConsumption()) {
            te.power -= te.getConsumption();
            te.heatEnergy += te.getHeatGen();
            te.isOn = true;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterElectric te) {
        HeaterSoundHandler.tickLoop(te);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            cachedPorts = new PortDef[] { PortDef.make(worldPosition.getX() + dir.offsetX * 2, worldPosition.getY(), worldPosition.getZ() + dir.offsetZ * 2, dir) };
        }
        return cachedPorts;
    }

    /** Heat relay: fully drains an {@link IHeatSource} below at 0.85 efficiency (old behaviour). */
    private void tryPullHeat(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof IHeatSource source) {
            this.heatEnergy += source.getHeatStored() * 0.85;
            source.useUpHeat(source.getHeatStored());
        }
    }

    public void toggleSettingUp() {
        setting++;
        if (setting > 10) setting = 0;
        setChanged();
    }

    public void toggleSettingDown() {
        setting--;
        if (setting < 0) setting = 10;
        setChanged();
    }

    public long getConsumption() {
        return (long) (Math.pow(setting, 1.4D) * 200D);
    }

    public int getHeatGen() {
        return this.setting * 100;
    }

    // --- IEnergyReceiverMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return getConsumption() * 20; }

    // --- IHeatSource ---

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        this.heatEnergy = Math.max(0, this.heatEnergy - heat);
    }

    // --- sync + persistence (1.12.2 keys) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte(setting);
        buf.writeInt(heatEnergy);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        setting = buf.readByte();
        heatEnergy = buf.readInt();
        isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("setting", setting);
        output.putInt("heatEnergy", heatEnergy);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        setting = input.getIntOr("setting", 0);
        heatEnergy = input.getIntOr("heatEnergy", 0);
    }
}
