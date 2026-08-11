package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFEL;
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
 * Free Electron Laser (1.12.2 {@code machine_fel}): a long, low emitter that fires a beam up to 24
 * blocks out, driving any correctly-placed {@link com.hbm.blocks.machine.MachineSILEX} in its path and
 * burning through anything else.
 *
 * <p>The footprint is deliberately lopsided — {@code {2, 0, 4, 2, 1, 1}} means it extends <b>4 blocks
 * forward and 2 back</b> from the core, with the core itself sitting {@code getOffset() == 2} blocks in
 * front of where the player clicked. Power enters at the single rear port {@link #fillSpace} places
 * behind the machine, {@code o - 4} along the facing axis at {@code y+1}.
 */
public class MachineFEL extends BlockDummyable implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineFEL(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFEL(pos, state);
    }

    /** Exposes the battery/crystal slots to hoppers, like the other machines. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityFEL te ? te : null;
    }

    /** Cables connect only at the single rear power port the core reports. */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityFEL te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Client ticks too: the FEL runs its own audio ramp and triggers a render update when the
        // beam length changes.
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_FEL.get(), TileEntityFEL::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_FEL.get(), TileEntityFEL::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 4, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        // The rear power port: o - 4 along the facing axis puts it behind the machine, at y+1.
        this.makeExtra(world, x + dir.offsetX * (o - 4), y + 1, z + dir.offsetZ * (o - 4));
    }
}
