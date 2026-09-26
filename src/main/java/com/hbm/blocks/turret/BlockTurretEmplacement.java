package com.hbm.blocks.turret;

import net.minecraft.core.Direction;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import dev.golem.core.multiblock.MultiblockCell;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** The thirty-one folded cells of an artillery emplacement — full cubes, unlike the gun turrets' pad. */
public class BlockTurretEmplacement extends MultiblockCell implements IEnergyConnectorBlock {

    public BlockTurretEmplacement(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return TurretBaseArtillery.emplacementConnects(world, pos, dir);
    }
}
