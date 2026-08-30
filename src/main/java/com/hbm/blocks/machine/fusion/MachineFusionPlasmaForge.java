package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The irregular 1.12.2 plasma-forge shell and its ten external machine ports. */
public class MachineFusionPlasmaForge extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineFusionPlasmaForge(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionPlasmaForge(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 2, 2, 5, 5}; }
    @Override public int getOffset() { return 5; }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        int cx = x + dir.offsetX * offset;
        int cy = y + dir.offsetY * offset;
        int cz = z + dir.offsetZ * offset;
        return super.checkRequirement(level, x, y, z, dir, offset)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, 3, -2, 4, 4}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, -2, 3, 4, 4}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, 4, -3, 3, 3}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, -3, 4, 3, 3}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, 5, -4, 2, 2}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{2, 0, -4, 5, 2, 2}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{3, -2, 1, 1, 5, 5}, x, y, z, dir)
                && MultiblockHandlerXR.checkSpace(level, cx, cy, cz, new int[]{4, -3, 0, 0, 4, 4}, x, y, z, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), getDimensions(), this, dir);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        int[][] shapes = {
                {2, 0, 3, -2, 4, 4}, {2, 0, -2, 3, 4, 4},
                {2, 0, 4, -3, 3, 3}, {2, 0, -3, 4, 3, 3},
                {2, 0, 5, -4, 2, 2}, {2, 0, -4, 5, 2, 2},
                {3, -2, 1, 1, 5, 5}, {4, -3, 0, 0, 4, 4}
        };
        for (int[] shape : shapes) MultiblockHandlerXR.fillSpace(level, x, y, z, shape, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        for (int i = -2; i <= 2; i++) {
            makeExtra(level, x + dir.offsetX * 5 + rot.offsetX * i, y,
                    z + dir.offsetZ * 5 + rot.offsetZ * i);
            makeExtra(level, x - dir.offsetX * 5 + rot.offsetX * i, y,
                    z - dir.offsetZ * 5 + rot.offsetZ * i);
        }
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection direction) {
        return findCoreTE(level, pos) instanceof TileEntityFusionPlasmaForge forge
                && IEnergyConnectorBlock.isPort(forge.getConPos().toArray(com.hbm.lib.DirPos[]::new), pos, direction);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection direction) {
        return canConnect(level, new BlockPos(x, y, z), direction);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityFusionPlasmaForge forge ? forge : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FUSION_PLASMA_FORGE.get(), TileEntityFusionPlasmaForge::clientTick)
                : createTickerHelper(type, ModBlockEntities.FUSION_PLASMA_FORGE.get(), TileEntityFusionPlasmaForge::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
