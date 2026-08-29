package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;

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

/** The large Exposure Chamber multiblock and its five external power ports. */
public class MachineExposureChamber extends BlockDummyable implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineExposureChamber(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineExposureChamber(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public void formStructure(Level world, BlockPos core, ForgeDirection dir) {
        int x = core.getX();
        int y = core.getY();
        int z = core.getZ();
        MultiblockHandlerXR.fillSpace(world, x, y, z, getDimensions(), this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP).getOpposite();

        MultiblockHandlerXR.fillSpace(world, x, y, z, new int[]{3, 0, 0, 0, -3, 8}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x, y + 2, z, new int[]{0, 0, 1, -1, -3, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x, y + 2, z, new int[]{0, 0, -1, 1, -3, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + rot.offsetX * 7, y, z + rot.offsetZ * 7,
                new int[]{3, 0, 1, -1, 0, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + rot.offsetX * 7, y, z + rot.offsetZ * 7,
                new int[]{3, 0, -1, 1, 0, 1}, this, dir);

        makeExtra(world, x + rot.offsetX * 7 + dir.offsetX, y, z + rot.offsetZ * 7 + dir.offsetZ);
        makeExtra(world, x + rot.offsetX * 7 - dir.offsetX, y, z + rot.offsetZ * 7 - dir.offsetZ);
        makeExtra(world, x + rot.offsetX * 8 + dir.offsetX, y, z + rot.offsetZ * 8 + dir.offsetZ);
        makeExtra(world, x + rot.offsetX * 8 - dir.offsetX, y, z + rot.offsetZ * 8 - dir.offsetZ);
        makeExtra(world, x + rot.offsetX * 8, y, z + rot.offsetZ * 8);
    }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(world, x, y, z, dir, o)) return false;

        x += dir.offsetX * o;
        z += dir.offsetZ * o;
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP).getOpposite();

        if (!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[]{3, 0, 0, 0, -3, 8}, x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x, y + 2, z, new int[]{0, 0, 1, -1, -3, 6}, x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x, y + 2, z, new int[]{0, 0, -1, 1, -3, 6}, x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x + rot.offsetX * 7, y, z + rot.offsetZ * 7,
                new int[]{3, 0, 1, -1, 0, 1}, x, y, z, dir)) return false;
        return MultiblockHandlerXR.checkSpace(world, x + rot.offsetX * 7, y, z + rot.offsetZ * 7,
                new int[]{3, 0, -1, 1, 0, 1}, x, y, z, dir);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineExposureChamber chamber
                ? chamber : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineExposureChamber chamber
                && IEnergyConnectorBlock.isPort(chamber.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(),
                TileEntityMachineExposureChamber::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(),
                TileEntityMachineExposureChamber::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
