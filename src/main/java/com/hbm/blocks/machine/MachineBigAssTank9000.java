package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBAT9000;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The legacy 5x5x5 BAT-9000 multiblock. */
public class MachineBigAssTank9000 extends MachineGiantTank {

    public MachineBigAssTank9000(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBAT9000(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        fillAdditional(level, new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset), dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        fillAdditional(level, core, dir);
    }

    private void fillAdditional(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, 1, 1, 2, -2}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, 1, 1, -2, 2}, this, dir);
        makeExtra(level, x + 1, y, z + 2);
        makeExtra(level, x - 1, y, z + 2);
        makeExtra(level, x + 1, y, z - 2);
        makeExtra(level, x - 1, y, z - 2);
        makeExtra(level, x + 2, y, z + 1);
        makeExtra(level, x - 2, y, z + 1);
        makeExtra(level, x + 2, y, z - 1);
        makeExtra(level, x - 2, y, z - 1);
    }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        return com.hbm.handler.MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                        getDimensions(), x, y, z, dir)
                && com.hbm.handler.MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                        new int[]{4, 0, 1, 1, 2, -2}, x, y, z, dir)
                && com.hbm.handler.MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                        new int[]{4, 0, 1, 1, -2, 2}, x, y, z, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_BAT9000.get(), TileEntityMachineBAT9000::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
