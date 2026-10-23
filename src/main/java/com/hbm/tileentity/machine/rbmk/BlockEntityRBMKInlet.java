package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/** ReaSim inlet: transfers its complete water buffer to adjacent RBMK columns. */
public class BlockEntityRBMKInlet extends TileEntityMachineBase implements IFluidReceiverMK2, IFluidStandardReceiverMK2, IRBMKTicking {
    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 32_000);
    public BlockEntityRBMKInlet(BlockPos pos, BlockState state) { super(ModRBMKBlockEntities.RBMK_INLET.get(), pos, state, 0); }
    @Override public String getDefaultName() { return "container.rbmk_steam_inlet"; }
    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    @Override public void tickServer() {
        this.setupFluidPorts(getPorts());
        this.updatePortFIFO();
        if (!RBMKDials.getReasimBoilers(level)) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = worldPosition.relative(direction);
            if (!(level.getBlockState(adjacent).getBlock() instanceof RBMKBase)) continue;
            BlockPos core = MultiblockSurface.coreOfAny(level, adjacent);
            if (core == null || !(level.getBlockEntity(core) instanceof BlockEntityRBMKBase column)) continue;
            int moved = Math.min(BlockEntityRBMKBase.maxWater - column.reasimWater, water.getFill());
            if (moved > 0) { column.reasimWater += moved; water.setFill(water.getFill() - moved); column.markChanged(); markChanged(); }
        }
    }
    @Override public long getDemand(FluidType type, int pressure) { return type == Fluids.WATER && pressure == water.getPressure() ? water.getMaxFill() - water.getFill() : 0; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{water}; }
    @Override public long transferFluid(FluidType type, int pressure, long amount) { if (type != Fluids.WATER || pressure != water.getPressure()) return amount; int accepted = water.fill(type, (int)Math.min(amount, Integer.MAX_VALUE), true); if (accepted > 0) setChanged(); return amount - accepted; }
    @Override protected void saveData(DataOut out) { super.saveData(out); water.writeToNBT(out, "tank"); }
    @Override protected void loadData(DataIn in) { super.loadData(in); water.readFromNBT(in, "tank"); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); water.serialize(buffer); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); water.deserialize(buffer); }
}
