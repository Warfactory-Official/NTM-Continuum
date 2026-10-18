package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * The 1.7 {@code TileEntityConverterRfHe} ("RF to HE Converter"): foreign machines push RF (the platform energy
 * capability, published by {@code ConverterEnergySource}) into a 1 MRF store, which it turns into HE at
 * {@link #rfInput} RF to {@link #heOutput} HE (default 2:5) into a 5 MHE provider buffer. The HE goes out through
 * any adjacent cable's network and, like 1.7's {@code tryProvide}, straight into any adjacent HE receiver.
 *
 * <p>Odd but ported: the conversion floors per tick ({@code rfCreated * 5 / 2}), so an odd RF amount loses half
 * an HE.
 */
public class TileEntityConverterRfHe extends TileEntityLoadedBase implements IEnergyProviderMK2 {

    // 1.7 IConfigurableMachine "RFToHEConverter" defaults
    public static long rfInput = 2;
    public static long heOutput = 5;
    public static double inputDecay = 0.0;

    public static final int RF_CAPACITY = 1_000_000;
    public static final int RF_MAX_RECEIVE = 1_000_000;

    public long power;
    public final long maxPower = 5_000_000;
    public int stored;

    public TileEntityConverterRfHe(BlockPos pos, BlockState state) {
        super(ModEnergyBedrockBlockEntities.CONVERTER_RF_HE.get(), pos, state);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityConverterRfHe converter) {
        long rfCreated = Math.min(converter.stored, (converter.maxPower - converter.power) * rfInput / heOutput);
        converter.stored = (int) (converter.stored - rfCreated);
        converter.power += rfCreated * heOutput / rfInput;
        if (converter.stored > 0) {
            int decay = (int) Math.min(Math.ceil(converter.stored * inputDecay), RF_MAX_RECEIVE);
            converter.stored -= Math.min(decay, converter.stored);
        }
        if (rfCreated > 0) converter.setChanged();

        converter.setupPowerPorts(converter.getPorts());
        converter.updateAllPorts();
        converter.providePower();

        converter.networkPackNT();
    }

    /** What a foreign pusher may put in right now (CoFH {@code receiveEnergy} simulated). */
    public int insertableRf() {
        return Math.max(0, Math.min(RF_CAPACITY - stored, RF_MAX_RECEIVE));
    }

    /** CoFH {@code receiveEnergy}. */
    public int receiveRf(long max) {
        int accepted = (int) Math.min(max, insertableRf());
        if (accepted > 0) {
            stored += accepted;
            setChanged();
        }
        return Math.max(accepted, 0);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("Energy", stored);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        stored = Math.max(0, Math.min(RF_CAPACITY, input.getIntOr("Energy", 0)));
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(stored);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        stored = buf.readInt();
    }
}
