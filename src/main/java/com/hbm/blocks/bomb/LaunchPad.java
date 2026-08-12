package com.hbm.blocks.bomb;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Silo Launch Pad: a 3×3 flat dummyable pad (core in the centre, surrounding pad blocks). */
public class LaunchPad extends BlockDummyable {

    public LaunchPad(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPad(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        x += dir.offsetX * o;
        z += dir.offsetZ * o;

        this.makeExtra(world, x + 1, y, z + 1);
        this.makeExtra(world, x + 1, y, z - 1);
        this.makeExtra(world, x - 1, y, z + 1);
        this.makeExtra(world, x - 1, y, z - 1);
    }
}
