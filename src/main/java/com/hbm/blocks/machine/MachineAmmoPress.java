package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The positional 3-wide x 2-high Ammo Press from the 1.12.2 machine. */
public class MachineAmmoPress extends BlockDummyable implements WorldlyContainerHolder {

    public MachineAmmoPress(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineAmmoPress press ? press : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAmmoPress(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_AMMO_PRESS.get(),
                TileEntityMachineAmmoPress::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_AMMO_PRESS.get(),
                TileEntityMachineAmmoPress::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
