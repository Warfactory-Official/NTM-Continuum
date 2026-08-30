package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The four-wide legacy turbofan multiblock. */
public class MachineTurbofan extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineTurbofan(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTurbofan(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 3, 3}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x, y, z);
        makeExtra(level, x - rot.offsetX, y, z - rot.offsetZ);
        makeExtra(level, x - dir.offsetX * 2, y, z - dir.offsetZ * 2);
        makeExtra(level, x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX() + dir.offsetX * getOffset();
        int z = core.getZ() + dir.offsetZ * getOffset();
        makeExtra(level, x, core.getY(), z);
        makeExtra(level, x - rot.offsetX, core.getY(), z - rot.offsetZ);
        makeExtra(level, x - dir.offsetX * 2, core.getY(), z - dir.offsetZ * 2);
        makeExtra(level, x - dir.offsetX * 2 - rot.offsetX, core.getY(),
                z - dir.offsetZ * 2 - rot.offsetZ);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineTurbofan fan
                && IEnergyConnectorBlock.isPort(fan.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityMachineTurbofan fan
                && IEnergyConnectorBlock.isPort(fan.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_TURBOFAN.get(), TileEntityMachineTurbofan::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_TURBOFAN.get(), TileEntityMachineTurbofan::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
