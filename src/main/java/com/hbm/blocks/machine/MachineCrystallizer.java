package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
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

/**
 * Ore Acidizer (1.12.2 {@code machine_crystallizer}): a 3×3×6 dummyable tower that dissolves ores in
 * acid to produce crystals. Power and fluid connect only at the core's 8 side ports ({@code getConPos});
 * the plus-shaped base is widened to the full 3×3 by {@link #fillSpace} corner extras, like the original.
 */
public class MachineCrystallizer extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCrystallizer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCrystallizer(pos, state);
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers/pipes (input 0, outputs 2/4). */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineCrystallizer te ? te : null;
    }

    /** Cables connect only at the machine's true power ports (the core's {@code getConPos} side positions). */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineCrystallizer te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    /** Ducts connect at the same 8 side ports (the geometry is fluid-agnostic — reuse the energy helper). */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineCrystallizer te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server: power/fluid/processing. Client: the spinner animation + looping sound (gated by isOn).
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_CRYSTALLIZER.get(), TileEntityMachineCrystallizer::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_CRYSTALLIZER.get(), TileEntityMachineCrystallizer::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        // The dimensions above only fill a plus shape at ground level; add the 4 base corners (1.12.2 verbatim).
        x += dir.offsetX * o;
        z += dir.offsetZ * o;

        this.makeExtra(world, x + 1, y, z + 1);
        this.makeExtra(world, x + 1, y, z - 1);
        this.makeExtra(world, x - 1, y, z + 1);
        this.makeExtra(world, x - 1, y, z - 1);
    }
}
