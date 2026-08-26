package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
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

/** The four-long legacy steam turbine multiblock. */
public class MachineLargeTurbine extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineLargeTurbine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineLargeTurbine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 3, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        addExtraPorts(level, x, y, z, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core.getX(), core.getY(), core.getZ(), dir);
    }

    private void addExtraPorts(Level level, int x, int y, int z, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        // The old placement hook received the clicked base cell. Relative to the core these are
        // the rear/front anchors at -dir*3 and +dir, plus the two lateral anchors.
        makeExtra(level, x - dir.offsetX * 3, y, z - dir.offsetZ * 3);
        makeExtra(level, x + dir.offsetX, y, z + dir.offsetZ);
        makeExtra(level, x + rot.offsetX, y, z + rot.offsetZ);
        makeExtra(level, x - rot.offsetX, y, z - rot.offsetZ);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineLargeTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getPowerPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return (com.hbm.inventory.recipes.CoolingRecipes.get(type) != null
                || type == com.hbm.inventory.fluid.Fluids.SPENTSTEAM)
                && findCoreTE(world, pos) instanceof TileEntityMachineLargeTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_LARGE_TURBINE.get(),
                        TileEntityMachineLargeTurbine::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_LARGE_TURBINE.get(),
                        TileEntityMachineLargeTurbine::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
