package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code TileEntityConverterHeRf} ("HE to RF Converter"): an HE receiver with a 5 MHE buffer at LOW
 * priority, turning {@link #heInput} HE into {@link #rfOutput} RF (default 5:1) into a 1 MRF store, which it pushes
 * into every adjacent foreign energy handler (1.7 CoFH {@code IEnergyReceiver}) and lets foreign machines pull.
 *
 * <p>RF here is the platform's energy capability, reached through Core's {@link EnergyCompat} (Forge Energy on
 * Forge/NeoForge, the optional Team Reborn Energy on Fabric); on a node with none loaded (Fabric without TR Energy)
 * the store fills and stays full. Our own conductors expose that capability
 * too, but 1.7 cables were never RF receivers, so conductors are skipped: the converter would otherwise feed the
 * net it drains.
 */
public class TileEntityConverterHeRf extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    // 1.7 IConfigurableMachine "HEToRFConverter" defaults
    public static long heInput = 5;
    public static long rfOutput = 1;
    public static double inputDecay = 0.0;

    /** 1.7 {@code EnergyStorage(1_000_000, 1_000_000, 1_000_000)}. */
    public static final int RF_CAPACITY = 1_000_000;
    public static final int RF_MAX_EXTRACT = 1_000_000;

    public long power;
    public final long maxPower = 5_000_000;
    /** The CoFH {@code EnergyStorage}'s stored RF. */
    public int stored;

    public TileEntityConverterHeRf(BlockPos pos, BlockState state) {
        super(ModEnergyBedrockBlockEntities.CONVERTER_HE_RF.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityConverterHeRf converter) {
        long rfCreated = Math.min(RF_CAPACITY - converter.stored, converter.power / heInput * rfOutput);
        converter.power -= rfCreated * heInput / rfOutput;
        converter.stored = (int) (converter.stored + rfCreated);
        if (converter.power > 0) converter.power = (long) (converter.power * (1D - inputDecay));
        if (rfCreated > 0) converter.setChanged();

        for (com.hbm.lib.ForgeDirection dir : com.hbm.lib.ForgeDirection.VALID_DIRECTIONS) {
            converter.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }

        if (EnergyCompat.supported()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbour = pos.relative(dir);
                if (level.getBlockEntity(neighbour) instanceof IEnergyConductorMK2) continue;
                EnergyView receiver = EnergyCompat.find(level, neighbour, dir.getOpposite());
                if (receiver == null) continue;
                int maxAvailable = Math.min(converter.stored, RF_MAX_EXTRACT);
                if (maxAvailable <= 0) break;
                long transferred = receiver.insert(maxAvailable);
                if (transferred > 0) {
                    converter.stored -= (int) Math.min(transferred, maxAvailable);
                    converter.setChanged();
                }
            }
        }

        converter.networkPackNT();
    }

    /** CoFH {@code extractEnergy}: what a foreign machine pulling from us may take. */
    public int extractRf(long max) {
        int taken = (int) Math.min(Math.min(max, RF_MAX_EXTRACT), stored);
        if (taken > 0) {
            stored -= taken;
            setChanged();
        }
        return Math.max(taken, 0);
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public ConnectionPriority getPriority() { return ConnectionPriority.LOW; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("Energy", stored); // CoFH EnergyStorage.writeToNBT
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
