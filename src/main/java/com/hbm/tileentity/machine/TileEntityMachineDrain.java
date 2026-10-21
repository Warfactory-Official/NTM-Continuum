package com.hbm.tileentity.machine;

import dev.golem.core.world.Explosions;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
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
import com.hbm.tileentity.TilePort.PortDef;

/** Drainage-pipe core: receives a fluid network and spills half its contents each server tick. */
public class TileEntityMachineDrain extends TileEntityLoadedBase implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 2_000);

    public TileEntityMachineDrain(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DRAIN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDrain drain) {
        drain.setupFluidPorts(drain.getPorts());
        drain.updatePortFIFO();

        // Upstream synchronizes the pre-spill tank state, then performs the destructive/release branch.
        drain.networkPackNT();

        if (drain.tank.getFill() > 0) {
            if (drain.tank.getTankType() == Fluids.AMAT || drain.tank.getTankType() == Fluids.ASCHRAB) {
                Explosions.explode(level, null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        10F, true, Level.ExplosionInteraction.TNT);
                return;
            } else {
                int toSpill = Math.max(drain.tank.getFill() / 2, 1);
                drain.tank.setFill(drain.tank.getFill() - toSpill);
                FluidTrait.onRelease(level, pos, drain.tank.getTankType(), FluidReleaseType.SPILL, toSpill);
                // hbm-space FT_Gaseous.release: a drained gas goes into the planet's air (com.hbm.api.environment)
                Environment.releaseGas(level, pos, drain.tank.getTankType(), toSpill);

                if (toSpill >= 100 && level.getRandom().nextInt(20) == 0
                        && FluidCombustion.isLiquidAndViscous(drain.tank.getTankType())
                        && FluidCombustion.isFlammable(drain.tank.getTankType())) {
                    ForgeDirection dir = MachineCore.facing(state);
                    Vec3 start = new Vec3(pos.getX() + 0.5D - dir.offsetX * 3D,
                            pos.getY() + 0.5D, pos.getZ() + 0.5D - dir.offsetZ * 3D);
                    Vec3 end = start.add(level.getRandom().nextGaussian() * 5D, -25D,
                            level.getRandom().nextGaussian() * 5D);
                    // Core's clip: a null-entity ClipContext throws on 26.1 (CollisionContext.of(null)).
                    BlockHitResult hit = Compat.clip(level, start, end,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE);

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

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir0 = MachineCore.facing(getBlockState());
            ForgeDirection dir1 = dir0.getRotation(ForgeDirection.UP);
            ForgeDirection dir2 = dir0.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x, y, z, dir0, dir1, dir2),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
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
