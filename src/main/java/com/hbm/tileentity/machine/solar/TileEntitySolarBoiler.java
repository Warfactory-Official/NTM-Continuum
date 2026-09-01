package com.hbm.tileentity.machine.solar;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The sunlight-fed 1.12.2 solar boiler, including its intentionally lossy heat conversion. */
public class TileEntitySolarBoiler extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {

    public static final int maxHeat = 320_000;
    /** Retained legacy constant; the old solar boiler declared it but did not use diffusion in its tick. */
    public static final double diffusion = 0.1D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 16_000),
            new FluidTankNTM(Fluids.STEAM, 1_600_000)
    };
    public int heat;
    public int heatInput;

    private int heatSync;
    private int heatInputSync;

    public TileEntitySolarBoiler(BlockPos pos, BlockState state) {
        super(ModSolarBlockEntities.SOLAR_BOILER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySolarBoiler te) {
        te.updateConnections(level);
        te.tryConvert();
        te.sendFluid(level);

        te.heat += te.heatInput;
        if (te.heat > maxHeat) te.heat = maxHeat;
        te.heatSync = te.heat;
        te.heatInputSync = te.heatInput;
        if (level.getGameTime() % 25L == 0L) te.networkPackNT();

        // This is compound-assignment integer truncation, matching old TileEntitySolarBoiler.update().
        te.heat *= 0.999D;
        te.heatInput = 0;
        te.setChanged();
    }

    private void updateConnections(Level level) {
        for (DirPos connection : getConPos()) {
            trySubscribe(tanks[0].getTankType(), level, connection);
        }
    }

    private void sendFluid(Level level) {
        for (DirPos connection : getConPos()) {
            tryProvide(tanks[1], level, connection);
        }
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.above(3), Library.POS_Y),
                new DirPos(worldPosition.below(), Library.NEG_Y)
        };
    }

    private void tryConvert() {
        int process = heat / 50;
        process = Math.min(process, tanks[0].getFill());
        process = Math.min(process, (tanks[1].getMaxFill() - tanks[1].getFill()) / 100);

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
        buf.writeInt(heatSync);
        buf.writeInt(heatInputSync);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        heatInput = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("heat", heat);
        tanks[0].writeToNBT(output, "tank0");
        tanks[1].writeToNBT(output, "tank1");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        heat = input.getIntOr("heat", 0);
        tanks[0].readFromNBT(input, "tank0");
        tanks[1].readFromNBT(input, "tank1");
    }
}
