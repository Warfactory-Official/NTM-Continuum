package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import com.hbm.particle.helper.HbmEffectNT;

/** Passive 100 mB/t spent-steam condenser used to close the ZIRNOX water loop. */
public class TileEntityCondenser extends TileEntityMachineBase implements IFluidStandardTransceiverMK2 {

    public FluidTankNTM input;
    public FluidTankNTM output;
    /** Legacy array order: [spent steam input, water output]. */
    public FluidTankNTM[] tanks;
    public int age;
    public int waterTimer;
    public int throughput;

    public TileEntityCondenser(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MACHINE_CONDENSER.get(), pos, state, 100, 100);
    }

    protected TileEntityCondenser(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  int inputTankSize, int outputTankSize) {
        super(type, pos, state, 0);
        input = new FluidTankNTM(Fluids.SPENTSTEAM, inputTankSize);
        output = new FluidTankNTM(Fluids.WATER, outputTankSize);
        tanks = new FluidTankNTM[]{input, output};
    }

    @Override public String getDefaultName() { return "container.machineCondenser"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCondenser te) {
        te.serverTickCommon(level);
    }

    /** Shared old-condenser order: convert first, then subscribe and emit the result. */
    protected void serverTickCommon(Level level) {
        age++;
        if (age >= 2) age = 0;
        if (waterTimer > 0) waterTimer--;

        int amount = Math.min(input.getFill(), output.getMaxFill() - output.getFill());
        throughput = amount;
        if (extraCondition(amount)) {
            input.setFill(input.getFill() - amount);
            if (amount > 0) waterTimer = 20;
            output.setFill(output.getFill() + amount);
            if (amount > 0) setChanged();
            postConvert(amount);
        }

        updateConnections(level);
        syncState();
    }

    protected boolean extraCondition(int amount) { return true; }

    protected void postConvert(int amount) {}

    /** Client-side old Tower particle payload; subclasses choose its timing and plume shape. */
    protected final void spawnSmoke(Level level, BlockPos pos, double y, float lift, float base,
                                    float max, int life) {
        CompoundTag data = new CompoundTag();
        data.putFloat("lift", lift);
        data.putFloat("base", base);
        data.putFloat("max", max);
        data.putInt("life", life);
        HbmEffectNT.Tower.summonParticle(level, pos.getX() + 0.5D, pos.getY() + y,
                pos.getZ() + 0.5D, data);
    }

    protected void updateConnections(Level level) {
        for (DirPos connection : getConPos()) {
            tryProvide(output, level, connection.getPos(), connection.getDir().toDirection());
        }
    }

    public DirPos[] getConPos() {
        DirPos[] connections = new DirPos[ForgeDirection.VALID_DIRECTIONS.length];
        for (int i = 0; i < connections.length; i++) {
            ForgeDirection direction = ForgeDirection.VALID_DIRECTIONS[i];
            connections[i] = new DirPos(worldPosition.relative(direction.toDirection()), direction);
        }
        return connections;
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{input}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{output}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        input.serialize(buf);
        output.serialize(buf);
        buf.writeByte(waterTimer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        input.deserialize(buf);
        output.deserialize(buf);
        waterTimer = buf.readByte();
    }

    @Override
    protected void saveData(DataOut outputTag) {
        super.saveData(outputTag);
        input.writeToNBT(outputTag, "input");
        output.writeToNBT(outputTag, "output");
    }

    @Override
    protected void loadData(DataIn inputTag) {
        super.loadData(inputTag);
        input.readFromNBT(inputTag, "input");
        output.readFromNBT(inputTag, "output");
    }
}
