package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The sunlight-fed solar boiler, a straight port of 1.7.10's {@code TileEntitySolarBoiler}: 100 mB of
 * water and 10,000 mB of steam. Mirrors add their sun value straight to {@link #heat}; every tick the boiler
 * turns {@code heat / 50} operations of 1 mB water into 100 mB steam and then discards all heat. (The
 * 1.12.2 16,000 / 1,600,000 tanks and its heat-accumulation/decay model are not the spec.)
 */
public class TileEntitySolarBoiler extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 100),
            new FluidTankNTM(Fluids.STEAM, 10_000)
    };
    /** Heat delivered by mirrors since the last boiler tick (1.7: {@code boiler.heat += sun}). */
    public int heat;
    /** Operations this tick before the water/steam clamps ({@code heat / 50}); the look overlay's "Too cold!". */
    public int display;

    public TileEntitySolarBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SOLAR_BOILER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySolarBoiler te) {
        te.setupFluidPorts(te.getPorts());
        te.updatePortFIFO();
        te.tryConvert();
        // 1.7 networkPackNT(15) every tick
        te.networkPackNT();
        te.setChanged();
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x, y, z, Library.NEG_Y),
                    PortDef.make(x, y + 2, z, Library.POS_Y),
            };
        }
        return cachedPorts;
    }

    private void tryConvert() {
        // hbm-space: (int)(heat * WorldProviderCelestial.getSunPower(provider, x, z)) / 50, the sun's strength on the
        // body scaling what the mirrors deliver (com.hbm.api.environment.Environment; 1 on plain HBM, so heat / 50)
        float sunPower = level == null ? 1F : (float) Environment.sunlight(level, worldPosition);
        int process = (int) (heat * sunPower) / 50;
        display = process;
        process = Math.min(process, tanks[0].getFill());
        process = Math.min(process, (tanks[1].getMaxFill() - tanks[1].getFill()) / 100);
        if (process < 0) process = 0;

        tanks[0].setFill(tanks[0].getFill() - process);
        tanks[1].fill(Fluids.STEAM, process * 100, true);
        // Deliberately discard the complete pre-input heat value, even when process is zero.
        heat = 0;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tanks[1]};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tanks[0]};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(display);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        display = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "tank0");
        tanks[1].writeToNBT(output, "tank1");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "tank0");
        tanks[1].readFromNBT(input, "tank1");
    }
}
