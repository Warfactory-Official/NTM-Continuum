package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCompressor;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Compressor block — 1:1 port of the 1.12.2 {@code MachineCompressor} (dims {@code {2,0,1,2,1,1}},
 * offset 2; two extra body fills + 3 makeExtra power/fluid ports). */
public class MachineCompressor extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineCompressor(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCompressor(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 2, 1, 1}; }
    @Override public int getOffset() { return 2; }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return super.checkRequirement(world, x, y, z, dir, o) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{3, -3, 1, 1, 1, 1}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{8, -4, 0, 0, 1, 1}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{3, -3, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{8, -4, 0, 0, 1, 1}, this, dir);

        x += dir.offsetX * o;
        z += dir.offsetZ * o;

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        this.makeExtra(world, x - dir.offsetX, y, z - dir.offsetZ);
        this.makeExtra(world, x + rot.offsetX, y, z + rot.offsetZ);
        this.makeExtra(world, x - rot.offsetX, y, z - rot.offsetZ);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineCompressor te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineCompressor te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_COMPRESSOR.get(), TileEntityMachineCompressor::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_COMPRESSOR.get(), TileEntityMachineCompressorBase::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
