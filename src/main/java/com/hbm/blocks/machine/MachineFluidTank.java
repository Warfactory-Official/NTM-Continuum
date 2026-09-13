package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
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
public class MachineFluidTank extends BlockMultiblockMachine implements IFluidConnectorBlockMK2 {

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
        BlockPos core = MultiblockSurface.coreOfAny(world, new BlockPos(x, y, z));
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

    // --- comparator output from the tank fill (works from any cell via findCore) ---

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineFluidTank tank
                ? tank.tank.getRedstoneComparatorPower() : 0;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    /** The four corners of the base; the front pair face forward and the back pair back. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 1);
        BlockPos back = core.relative(facing, -1);

        visitor.cell(front.relative(rot), MASK_SOUTH);
        visitor.cell(front.relative(rot, -1), MASK_SOUTH);
        visitor.cell(back.relative(rot), MASK_NORTH);
        visitor.cell(back.relative(rot, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 1);
        BlockPos back = core.relative(facing, -1);

        visitor.passiveCell(front.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(front.relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(back.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(back.relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }
}
