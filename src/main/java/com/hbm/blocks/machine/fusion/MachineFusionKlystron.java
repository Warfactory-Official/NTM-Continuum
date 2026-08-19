package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fusion klystron - the microwave emitter that ignites a torus. Power and compressed air come in at
 * the two side ports; the beam leaves from the muzzle port at +2. Behaviour lives in
 * {@link TileEntityFusionKlystron}.
 */
public class MachineFusionKlystron extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFusionKlystron(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionKlystron(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 3, 2, 2}; }

    @Override public int getOffset() { return 3; }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        int cx = core.getX(), cy = core.getY(), cz = core.getZ();
        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, new int[]{4, -3, 4, 3, 1, 1}, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, cx + dir.offsetX * 3, cy + 2, cz + dir.offsetZ * 3);
        makeExtra(level, cx + rot.offsetX * 2, cy, cz + rot.offsetZ * 2);
        makeExtra(level, cx - rot.offsetX * 2, cy, cz - rot.offsetZ * 2);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return isPortCell(world, pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return isPortCell(world, new BlockPos(x, y, z), dir);
    }

    /** A pipe may attach only at a promoted port cell, and only on the face the machine declares. */
    private boolean isPortCell(BlockGetter world, BlockPos cell, ForgeDirection dir) {
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) return false;
        BlockPos outside = cell.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityFusionKlystron te
                && te.getConPos().stream().anyMatch(p -> p.getPos().equals(outside) && p.getDir() == dir);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FUSION_KLYSTRON.get(), TileEntityFusionKlystron::clientTick)
                : createTickerHelper(type, ModBlockEntities.FUSION_KLYSTRON.get(), TileEntityFusionKlystron::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
