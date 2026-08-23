package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

/** ReaSim outlet: exposes the steam extracted from adjacent RBMK columns. */
public class BlockEntityRBMKOutlet extends TileEntityMachineBase implements IFluidProviderMK2, IFluidStandardSenderMK2, IRBMKTicking {
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32_000);
    public BlockEntityRBMKOutlet(BlockPos pos, BlockState state) { super(ModRBMKBlockEntities.RBMK_OUTLET.get(), pos, state, 0); }
    @Override public String getDefaultName() { return "container.rbmk_steam_outlet"; }
    @Override public void tickServer() {
        if (RBMKDials.getReasimBoilers(level)) for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = worldPosition.relative(direction);
            if (!(level.getBlockState(adjacent).getBlock() instanceof RBMKBase)) continue;
            BlockPos core = MultiblockSurface.coreOfCell(level, adjacent);
            if (core == null || !(level.getBlockEntity(core) instanceof BlockEntityRBMKBase column)) continue;
            int moved = Math.min(steam.getMaxFill() - steam.getFill(), column.reasimSteam);
            if (moved > 0) { column.reasimSteam -= moved; steam.setFill(steam.getFill() + moved); column.markChanged(); markChanged(); }
        }
        if (steam.getFill() > 0) for (Direction direction : Direction.values()) tryProvide(steam, level, worldPosition.relative(direction), ForgeDirection.getOrientation(direction));
    }
    @Override public long getFluidAvailable(FluidType type, int pressure) { return type == steam.getTankType() && pressure == steam.getPressure() ? steam.getFill() : 0; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{steam}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{steam}; }
    @Override public void useUpFluid(FluidType type, int pressure, long amount) { if (type == steam.getTankType() && pressure == steam.getPressure()) { steam.setFill(steam.getFill() - (int)Math.min(amount, steam.getFill())); setChanged(); } }
    @Override protected void saveData(DataOut out) { super.saveData(out); steam.writeToNBT(out, "tank"); }
    @Override protected void loadData(DataIn in) { super.loadData(in); steam.readFromNBT(in, "tank"); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); steam.serialize(buffer); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); steam.deserialize(buffer); }
}
