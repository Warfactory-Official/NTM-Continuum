package com.hbm.blocks.turret;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** The thirty-one folded cells of an artillery emplacement: full cubes, unlike the gun turrets' pad. */
public class BlockTurretEmplacement extends BlockMultiblockCell implements IEnergyConnectorBlock {

    public BlockTurretEmplacement(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return TurretBaseArtillery.emplacementConnects(world, pos, dir);
    }
}
