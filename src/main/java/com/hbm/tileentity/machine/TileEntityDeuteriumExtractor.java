package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Converts water to heavy water using the legacy 50:1 ratio and energy cost. */
public class TileEntityDeuteriumExtractor extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {

    public final long maxPower;
    public final FluidTankNTM[] tanks;
    public long power;

    protected TileEntityDeuteriumExtractor(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                           BlockPos pos, BlockState state, int waterCapacity, int heavyWaterCapacity,
                                           long maxPower) {
        super(type, pos, state, 0);
        this.maxPower = maxPower;
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.WATER, waterCapacity),
                new FluidTankNTM(Fluids.HEAVYWATER, heavyWaterCapacity)};
    }

    public TileEntityDeuteriumExtractor(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get(), pos, state, 1_000, 100, 10_000);
    }

    /**
     * 1.7's power use per conversion is {@code maxPower / 20}, needed in full before it runs. Public and assignable so an
     * addon can set its own at start-up (NTM: Space: {@code maxPower / 100}, and the buffer must also exceed 200 HE);
     * HBM never changes them.
     */
    public static int powerDivisor = 20;
    public static long powerFloorExclusive = Long.MIN_VALUE;

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDeuteriumExtractor extractor) {
        for (DirPos connection : extractor.getConPos()) {
            if (extractor.tanks[1].getFill() > 0) extractor.tryProvide(extractor.tanks[1], level, connection);
        }

        if (extractor.hasPower() && extractor.power > powerFloorExclusive && extractor.hasEnoughWater()
                && extractor.tanks[1].getMaxFill() > extractor.tanks[1].getFill()) {
            int convert = Math.min(extractor.tanks[1].getMaxFill(), extractor.tanks[0].getFill()) / 50;
            convert = Math.min(convert, extractor.tanks[1].getMaxFill() - extractor.tanks[1].getFill());
            extractor.tanks[0].setFill(extractor.tanks[0].getFill() - convert * 50);
            extractor.tanks[1].setFill(extractor.tanks[1].getFill() + convert);
            extractor.power -= extractor.getMaxPower() / powerDivisor;
        }

        extractor.setChanged();
        extractor.networkPackNT();
    }

    protected boolean hasPower() {
        return power >= getMaxPower() / powerDivisor;
    }

    protected boolean hasEnoughWater() {
        return tanks[0].getFill() >= 100;
    }

    public DirPos[] getConPos() {
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.east(), ForgeDirection.EAST),
                new DirPos(pos.west(), ForgeDirection.WEST),
                new DirPos(pos.above(), ForgeDirection.UP),
                new DirPos(pos.below(), ForgeDirection.DOWN),
                new DirPos(pos.south(), ForgeDirection.SOUTH),
                new DirPos(pos.north(), ForgeDirection.NORTH)
        };
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
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = Math.max(0, Math.min(power, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_deuterium_extractor";
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "heavyWater");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "heavyWater");
    }
}
