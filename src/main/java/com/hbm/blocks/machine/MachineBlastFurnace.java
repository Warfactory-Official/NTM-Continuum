package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
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

/** The 3x3x8 blast furnace multiblock and its fluid-network port cells. */
public class MachineBlastFurnace extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineBlastFurnace(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBlastFurnace(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{6, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineBlastFurnace furnace
                ? furnace : null;
    }

    @Override
    public boolean canConnect(com.hbm.inventory.fluid.FluidType type, BlockGetter level,
                              int x, int y, int z, ForgeDirection dir) {
        BlockEntity entity = findCoreTE(level, new BlockPos(x, y, z));
        return entity instanceof TileEntityMachineBlastFurnace furnace
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(furnace.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_BLAST_FURNACE.get(), TileEntityMachineBlastFurnace::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_BLAST_FURNACE.get(), TileEntityMachineBlastFurnace::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x -= dir.offsetX;
        z -= dir.offsetZ;

        makeExtra(level, x + 1, y, z);
        makeExtra(level, x - 1, y, z);
        makeExtra(level, x, y, z + 1);
        makeExtra(level, x, y, z - 1);
        makeExtra(level, x + dir.offsetX, y + 3, z + dir.offsetZ);
        makeExtra(level, x + dir.offsetX, y + 5, z + dir.offsetZ);
        makeExtra(level, x, y + 6, z);
    }
}
