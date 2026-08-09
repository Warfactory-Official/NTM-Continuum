package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Oil Derrick block — 1:1 port of the 1.12.2 {@code MachineOilWell}: a 10-tall structure with a 1-high
 * 3×3 base at ground level, a 9-high 1×1 tower above the core, and the 4 base corners one block up
 * (the old custom checkRequirement/fillSpace, copied verbatim). Core at the clicked position (offset 0).
 */
public class MachineOilWell extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineOilWell(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineOilWell(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return MultiblockHandlerXR.checkSpace(world, x, y, z, new int[]{1, -1, 0, 0, 0, 0}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x, y + 1, z, new int[]{8, 0, 1, 1, 1, 1}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + 1, y + 1, z + 1, new int[]{-1, 1, 0, 0, 0, 0}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + 1, y + 1, z - 1, new int[]{-1, 1, 0, 0, 0, 0}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x - 1, y + 1, z + 1, new int[]{-1, 1, 0, 0, 0, 0}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x - 1, y + 1, z - 1, new int[]{-1, 1, 0, 0, 0, 0}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        MultiblockHandlerXR.fillSpace(world, x, y, z, new int[]{1, -1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x, y + 1, z, new int[]{8, 0, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + 1, y + 1, z + 1, new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + 1, y + 1, z - 1, new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x - 1, y + 1, z + 1, new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x - 1, y + 1, z - 1, new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineOilWell te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineOilWell te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_OIL_WELL.get(), TileEntityMachineOilWell::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
