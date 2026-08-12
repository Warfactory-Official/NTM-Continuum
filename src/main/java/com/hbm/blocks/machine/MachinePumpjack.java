package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Pumpjack block — 1:1 port of the 1.12.2 {@code MachinePumpjack}: dims {@code {3,0,0,0,0,6}} (4 tall,
 * 7 long single-row footprint behind the core) plus the two 2-wide ground strips and the 4 "extra"
 * corner ports around the well head (old checkRequirement/fillSpace verbatim). The old meta&ge;6 proxy
 * cells map to the extras.
 */
public class MachinePumpjack extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachinePumpjack(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePumpjack(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 6};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return super.checkRequirement(world, x, y, z, dir, o) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{0, 0, -1, 1, -2, 4}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{0, 0, 1, -1, -1, 5}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        MultiblockHandlerXR.fillSpace(world, x + rot.offsetX * 3, y, z + rot.offsetZ * 3, new int[]{0, 0, -1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + rot.offsetX * 3, y, z + rot.offsetZ * 3, new int[]{0, 0, 1, -1, 2, 2}, this, dir);

        this.makeExtra(world, x + rot.offsetX * 3 + 1, y, z + rot.offsetZ * 3 + 1);
        this.makeExtra(world, x + rot.offsetX * 3 + 1, y, z + rot.offsetZ * 3 - 1);
        this.makeExtra(world, x + rot.offsetX * 3 - 1, y, z + rot.offsetZ * 3 + 1);
        this.makeExtra(world, x + rot.offsetX * 3 - 1, y, z + rot.offsetZ * 3 - 1);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachinePumpjack te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachinePumpjack te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_PUMPJACK.get(), TileEntityMachinePumpjack::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_PUMPJACK.get(),
                        (BlockEntityTicker<TileEntityMachinePumpjack>) com.hbm.tileentity.machine.oil.TileEntityOilDrillBase::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
