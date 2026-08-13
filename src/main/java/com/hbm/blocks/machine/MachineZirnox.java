package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=26.1 {
import net.minecraft.world.level.redstone.Orientation;
//?}
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The five-block-tall ZIRNOX reactor multiblock and its original five access cells. */
public class MachineZirnox extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {
    public MachineZirnox(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityReactorZirnox(pos, state); }
    @Override public int[] getDimensions() { return new int[]{1, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(META) < extra) return null;
        return findCoreTE(level, pos) instanceof TileEntityReactorZirnox reactor ? reactor : null;
    }
    @Override public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection direction) {
        BlockPos cell = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) return false;
        BlockPos outside = cell.offset(direction.offsetX, direction.offsetY, direction.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityReactorZirnox reactor
                && reactor.getConPos().stream().anyMatch(port -> port.getPos().equals(outside) && port.getDir() == direction);
    }
    @Override public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection direction, int offset) {
        int coreX = x + direction.offsetX * offset;
        int coreY = y + direction.offsetY * offset;
        int coreZ = z + direction.offsetZ * offset;
        return super.checkRequirement(level, x, y, z, direction, offset)
                && MultiblockHandlerXR.checkSpace(level, coreX, coreY, coreZ, new int[]{4, -2, 1, 1, 1, 1}, x, y, z, direction)
                && MultiblockHandlerXR.checkSpace(level, coreX, coreY, coreZ, new int[]{4, -2, 0, 0, 2, -2}, x, y, z, direction)
                && MultiblockHandlerXR.checkSpace(level, coreX, coreY, coreZ, new int[]{4, -2, 0, 0, -2, 2}, x, y, z, direction);
    }
    @Override protected void fillSpace(Level level, int x, int y, int z, ForgeDirection direction, int offset) {
        BlockPos core = new BlockPos(x + direction.offsetX * offset, y + direction.offsetY * offset, z + direction.offsetZ * offset);
        formStructure(level, core, direction);
    }
    /** Forms the live dummy shell around an existing controller. Used by normal placement and structure tests. */
    public void formStructure(Level level, BlockPos core, ForgeDirection direction) {
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), getDimensions(), this, direction);
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), new int[]{4, -2, 1, 1, 1, 1}, this, direction);
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), new int[]{4, -2, 0, 0, 2, -2}, this, direction);
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), new int[]{4, -2, 0, 0, -2, 2}, this, direction);
        ForgeDirection rot = direction.getRotation(ForgeDirection.UP);
        makeExtra(level, core.getX() + rot.offsetX * 2, core.getY() + 1, core.getZ() + rot.offsetZ * 2);
        makeExtra(level, core.getX() + rot.offsetX * 2, core.getY() + 3, core.getZ() + rot.offsetZ * 2);
        makeExtra(level, core.getX() - rot.offsetX * 2, core.getY() + 1, core.getZ() - rot.offsetZ * 2);
        makeExtra(level, core.getX() - rot.offsetX * 2, core.getY() + 3, core.getZ() - rot.offsetZ * 2);
        makeExtra(level, core.getX(), core.getY() + 4, core.getZ());
    }
    //? if >=26.1 {
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                              @Nullable Orientation orientation, boolean movedByPiston) {
    //?} else {
    /*protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos hbm$fromPos, boolean movedByPiston) {
    *///?}
        //? if >=26.1 {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        //?} else {
        /*super.neighborChanged(state, level, pos, block, hbm$fromPos, movedByPiston);
        *///?}
        if (level.isClientSide()) return;
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityReactorZirnox reactor)) return;
        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 4 && !powered; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((dx == -2 || dx == 2 || dy == 0 || dy == 4 || dz == -2 || dz == 2)
                            && level.hasNeighborSignal(core.offset(dx, dy, dz))) {
                        powered = true;
                        break;
                    }
                }
            }
        }
        reactor.setRedstonePowered(powered);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MACHINE_ZIRNOX.get(), TileEntityReactorZirnox::serverTick);
    }
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
