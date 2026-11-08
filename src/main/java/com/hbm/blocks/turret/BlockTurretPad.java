package com.hbm.blocks.turret;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The three folded cells of a turret pad, carrying the core's half-block shape. */
public class BlockTurretPad extends BlockMultiblockCell implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public BlockTurretPad(Properties properties) {
        super(properties);
    }

    /** 1.7 proxies the turret's ammo slots on every cell. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityTurretBaseNT turret ? turret : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
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
