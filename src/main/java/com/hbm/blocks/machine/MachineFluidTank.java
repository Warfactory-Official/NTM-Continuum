package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Tank: a 5×3×5 dummyable multiblock (the 1.12.2 {@code MachineFluidTank}), dims {2,0,1,1,2,2}
 * with the four corner cells filled. Drawn by the generic OBJ renderer ({@code fluidtank.obj}); the core
 * is {@link TileEntityMachineFluidTank}. Implements {@link IFluidConnectorBlockMK2} so fluid ducts
 * connect to any face of the footprint without a BlockEntity at each cell.
 */
public class MachineFluidTank extends BlockDummyable implements IFluidConnectorBlockMK2 {

    public MachineFluidTank(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFluidTank(pos, state);
    }

    /**
     * Ducts connect only at the tank's actual fluid ports (the {@code getConPos} positions: two per
     * horizontal side, at the core's Y), NOT every footprint face — so a duct alongside the tank body
     * doesn't sprout a phantom connection. {@code dir} is the tank's outward face toward the duct.
     */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos core = findCore(world, new BlockPos(x, y, z));
        return core != null && isPort(core, x, y, z, dir);
    }

    /**
     * Whether tank cell {@code (x,y,z)} exposes a fluid port on face {@code dir}. The tank's network node
     * occupies the boundary cells {@code (±2, 0, ±1)} or {@code (±1, 0, ±2)} relative to the core
     * (= each {@code getConPos} entry's {@code pos − dir}); a duct sits one block beyond that boundary
     * and the port face points outward from it.
     */
    private static boolean isPort(BlockPos c, int x, int y, int z, ForgeDirection dir) {
        int dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
        if (dy != 0) return false;
        return switch (dir) {
            case EAST -> dx == 2 && (dz == -1 || dz == 1);   // +X (Library.POS_X)
            case WEST -> dx == -2 && (dz == -1 || dz == 1);  // -X (Library.NEG_X)
            case SOUTH -> dz == 2 && (dx == -1 || dx == 1);  // +Z (Library.POS_Z)
            case NORTH -> dz == -2 && (dx == -1 || dx == 1); // -Z (Library.NEG_Z)
            default -> false;
        };
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ + 1);
        this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ - 1);
        this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ + 1);
        this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ - 1);
    }

    // --- comparator output from the tank fill (works from any cell via findCore) ---

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineFluidTank tank
                ? tank.tank.getRedstoneComparatorPower() : 0;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Both sides: the side-ladder must be registered on the client too (onClimbable is checked there
        // for the local player). The server tick also runs the network/identifier logic.
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FLUID_TANK.get(), TileEntityMachineFluidTank::clientTick)
                : createTickerHelper(type, ModBlockEntities.FLUID_TANK.get(), TileEntityMachineFluidTank::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
