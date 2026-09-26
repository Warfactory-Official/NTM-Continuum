package com.hbm.blocks.turret;

import net.minecraft.core.Direction;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import dev.golem.core.multiblock.MultiblockCell;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The three folded cells of a turret pad, carrying the core's half-block shape. */
public class BlockTurretPad extends MultiblockCell implements IEnergyConnectorBlock {

    public BlockTurretPad(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return TurretBaseNT.padConnects(world, pos, dir);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TurretBaseNT.SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TurretBaseNT.SHAPE;
    }
}
