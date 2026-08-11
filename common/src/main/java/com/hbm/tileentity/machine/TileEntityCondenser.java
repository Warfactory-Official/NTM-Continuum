package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Passive 100 mB/t spent-steam condenser used to close the ZIRNOX water loop. */
public class TileEntityCondenser extends TileEntityMachineBase implements IFluidStandardTransceiverMK2 {

    public final FluidTankNTM input = new FluidTankNTM(Fluids.SPENTSTEAM, 100);
    public final FluidTankNTM output = new FluidTankNTM(Fluids.WATER, 100);

    public TileEntityCondenser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONDENSER.get(), pos, state, 0);
    }

    @Override public String getDefaultName() { return "container.machineCondenser"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCondenser te) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            te.trySubscribe(Fluids.SPENTSTEAM, level, pos.relative(direction.toDirection()), direction);
            te.tryProvide(te.output, level, pos.relative(direction.toDirection()), direction);
        }
        int amount = Math.min(te.input.getFill(), te.output.getMaxFill() - te.output.getFill());
        if (amount > 0) {
            te.input.setFill(te.input.getFill() - amount);
            te.output.setFill(te.output.getFill() + amount);
            te.setChanged();
        }
        te.networkPackNT();
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{input}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{output}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{input, output}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput outputTag) {
        super.saveAdditional(outputTag);
        input.writeToNBT(outputTag, "input");
        output.writeToNBT(outputTag, "output");
    }

    @Override
    protected void loadAdditional(ValueInput inputTag) {
        super.loadAdditional(inputTag);
        input.readFromNBT(inputTag, "input");
        output.readFromNBT(inputTag, "output");
    }
}
