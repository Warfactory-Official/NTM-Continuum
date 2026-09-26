package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Drainage-pipe core: receives a fluid network and spills half its contents each server tick. */
public class TileEntityMachineDrain extends SyncedBlockEntity implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 2_000);

    public TileEntityMachineDrain(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DRAIN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDrain drain) {

        // Upstream synchronizes the pre-spill tank state, then performs the destructive/release branch.
        drain.syncState();

        if (drain.tank.getFill() > 0) {
            if (drain.tank.getTankType() == Fluids.AMAT || drain.tank.getTankType() == Fluids.ASCHRAB) {
                level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        10F, true, Level.ExplosionInteraction.TNT);
                return;
            } else {
                int toSpill = Math.max(drain.tank.getFill() / 2, 1);
                drain.tank.setFill(drain.tank.getFill() - toSpill);

                if (toSpill >= 100 && level.getRandom().nextInt(20) == 0
                        && FluidCombustion.isLiquidAndViscous(drain.tank.getTankType())
                        && FluidCombustion.isFlammable(drain.tank.getTankType())) {
                    ForgeDirection dir = MachineCore.facing(state);
                    Vec3 start = new Vec3(pos.getX() + 0.5D - dir.offsetX * 3D,
                            pos.getY() + 0.5D, pos.getZ() + 0.5D - dir.offsetZ * 3D);
                    Vec3 end = start.add(level.getRandom().nextGaussian() * 5D, -25D,
                            level.getRandom().nextGaussian() * 5D);
                    BlockHitResult hit = level.clip(new ClipContext(start, end,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                            (net.minecraft.world.entity.Entity) null));

                    if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP) {
                        BlockPos spillPos = hit.getBlockPos().above();
                        BlockState spillState = level.getBlockState(spillPos);
                        if (!spillState.liquid() && spillState.canBeReplaced()
                                && ModOilBlocks.oil_spill.get().defaultBlockState().canSurvive(level, spillPos)) {
                            level.setBlock(spillPos, ModOilBlocks.oil_spill.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        drain.setChanged();
    }

    public void setTankType(FluidType type) {
        tank.setTankType(type);
        setChanged();
    }

    public DirPos[] getConPos() {
        ForgeDirection dir0 = MachineCore.facing(getBlockState());
        ForgeDirection dir1 = dir0.getRotation(ForgeDirection.UP);
        ForgeDirection dir2 = dir0.getRotation(ForgeDirection.DOWN);
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.getX() + dir0.offsetX, pos.getY(), pos.getZ() + dir0.offsetZ, dir0),
                new DirPos(pos.getX() + dir1.offsetX, pos.getY(), pos.getZ() + dir1.offsetZ, dir1),
                new DirPos(pos.getX() + dir2.offsetX, pos.getY(), pos.getZ() + dir2.offsetZ, dir2)
        };
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != Direction.UP && dir != Direction.DOWN;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "t");
    }
}
