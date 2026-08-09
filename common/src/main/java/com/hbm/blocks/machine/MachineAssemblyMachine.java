package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
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

/** Assembly Machine: a 3×3×3 dummyable multiblock (core + 8 ringed "extra" dummies + fill). */
public class MachineAssemblyMachine extends BlockDummyable implements WorldlyContainerHolder, IEnergyConnectorBlock {

    public MachineAssemblyMachine(Properties properties) {
        super(properties);
    }

    /**
     * Cables connect only at the machine's true power ports (the {@code getConPos()} edge positions, two
     * per horizontal side at the core's Y) — not every footprint face. The core's BlockEntity isn't at
     * the dummy cells, so we walk to the core and match the cell/side against its connection points.
     */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineAssemblyMachine te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAssemblyMachine(pos, state);
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers/pipes. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineAssemblyMachine te ? te : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server: power + recipe processing. Client: the arm/ring animation (gated by didProcess).
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), TileEntityMachineAssemblyMachine::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), TileEntityMachineAssemblyMachine::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        x -= dir.offsetX;
        z -= dir.offsetZ;

        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) this.makeExtra(world, x + i, y, z + j);
        }
    }
}
