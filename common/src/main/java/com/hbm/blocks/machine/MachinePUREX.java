package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
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

/** The original five-by-five PUREX plant footprint, with edge power and fluid ports. */
public final class MachinePUREX extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {
    public MachinePUREX(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMachinePUREX(pos, state); }
    @Override public int[] getDimensions() { return new int[]{4, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachinePUREX purex ? purex : null;
    }
    @Override public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachinePUREX purex && IEnergyConnectorBlock.isPort(purex.getConPos(), pos, dir);
    }
    @Override public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachinePUREX purex
                && IEnergyConnectorBlock.isPort(purex.getConPos(), new BlockPos(x, y, z), dir);
    }
    @Override protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (Math.abs(i) == 2 || Math.abs(j) == 2) makeExtra(level, x + i, y, z + j);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? ticker(type, ModBlockEntities.MACHINE_PUREX.get(), TileEntityMachinePUREX::clientTick)
                : ticker(type, ModBlockEntities.MACHINE_PUREX.get(), TileEntityMachinePUREX::serverTick);
    }
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> ticker(BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
