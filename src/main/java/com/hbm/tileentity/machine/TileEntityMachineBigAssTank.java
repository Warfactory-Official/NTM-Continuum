package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;

/** 16,000,000 mB Big Ass Tank controller. */
public class TileEntityMachineBigAssTank extends TileEntityMachineGiantTank {

    public static final int CAPACITY = 16_000_000;

    public TileEntityMachineBigAssTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BIG_ASS_TANK.get(), pos, state, CAPACITY);
    }

    @Override
    public String getDefaultName() {
        return "container.bigAssTank";
    }

    @Override
    public long getReceiverSpeed(com.hbm.inventory.fluid.FluidType type, int pressure) {
        return Math.max(50_000L, (tankNew.getMaxFill() - tankNew.getFill()) / 100L);
    }

    @Override
    public long getProviderSpeed(com.hbm.inventory.fluid.FluidType type, int pressure) {
        return Math.max(50_000L, tankNew.getFill() / 100L);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineBigAssTank tank) {
        tank.checkTilt(level);
        TileEntityMachineGiantTank.serverTick(level, pos, state, tank);
    }

    /** Unavoidable heavy-machine gravity check, matching TileEntityLoadedBase's 95% support threshold. */
    private int tiltBlocksChecked;
    private int tiltBlocksValid;

    private void checkTilt(Level level) {
        if (!TickPhase.every(level, worldPosition, 20L)) return;
        // hbm-space TileEntityLoadedBase.checkTilt: nothing tilts in orbit (com.hbm.api.environment.Environment)
        if (com.hbm.api.environment.Environment.inOrbit(level)) {
            tilted = false;
            return;
        }
        if (tiltBlocksChecked >= getFloorCount()) {
            boolean wasTilted = tilted;
            tilted = tiltBlocksValid < tiltBlocksChecked * 0.95D;
            if (tilted && !wasTilted) {
                level.playSound(null, worldPosition, ModSounds.METAL_BLOCK_EVENT, SoundSource.BLOCKS, 3F, 1F);
            }
            tiltBlocksChecked = 0;
            tiltBlocksValid = 0;
        }

        BlockPos floor = getFloorPosFromIndex(tiltBlocksChecked++);
        BlockState ground = level.getBlockState(floor);
        if (!ground.isFaceSturdy(level, floor, Direction.UP)
                || !dev.golem.core.world.Compat.isSolidRender(ground, level, floor)
                || ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance()) return;
        tiltBlocksValid++;
    }

    public int getFloorCount() {
        return 4 * 4;
    }

    public BlockPos getFloorPosFromIndex(int index) {
        return new BlockPos(worldPosition.getX() - 3 + (index / 4) * 2, worldPosition.getY() - 1,
                worldPosition.getZ() - 3 + (index % 4) * 2);
    }

    @Override
    protected boolean checkFluidInteraction(Level level) {
        if (tankNew.getTankType() != com.hbm.inventory.fluid.Fluids.AMAT
                && tankNew.getTankType() != com.hbm.inventory.fluid.Fluids.ASCHRAB) return false;
        level.removeBlock(worldPosition, false);
        level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, 10F, true, Level.ExplosionInteraction.TNT);
        return true;
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = getFacing();
        return new DirPos[]{
                new DirPos(worldPosition.getX() + dir.offsetX * 7, worldPosition.getY(),
                        worldPosition.getZ() + dir.offsetZ * 7, dir),
                new DirPos(worldPosition.getX() - dir.offsetX * 7, worldPosition.getY(),
                        worldPosition.getZ() - dir.offsetZ * 7, dir.getOpposite())
        };
    }
}
