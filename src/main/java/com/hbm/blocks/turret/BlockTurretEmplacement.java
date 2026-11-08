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
import org.jspecify.annotations.Nullable;

/** The thirty-one folded cells of an artillery emplacement: full cubes, unlike the gun turrets' pad. */
public class BlockTurretEmplacement extends BlockMultiblockCell implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public BlockTurretEmplacement(Properties properties) {
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
        return TurretBaseArtillery.emplacementConnects(world, pos, dir);
    }
}
