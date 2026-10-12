package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The original electrolyser footprint and its six network ports. */
public class MachineElectrolyser extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    /** The seven upper/side fill volumes from the 1.12.2 implementation. */
    private static final int[][] EXTRA_VOLUMES = {
            {2, -1, 5, 5, 1, 1},
            {3, -3, 5, 5, 0, 0},
            {3, -1, 4, -4, -3, 3},
            {3, -1, 2, -2, -3, 3},
            {3, -1, 0, 0, -3, 3},
            {3, -1, -2, 2, -3, 3},
            {3, -1, -4, 4, -3, 3}
    };
    private static final int[] RING_OFFSETS = {4, 2, 0, -2, -4};
    private static final int[] RING_VOLUME = {0, 0, 0, 0, -1, 2};

    public MachineElectrolyser(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineElectrolyser(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 5, 5, 1, 3}; }
    @Override public int getOffset() { return 5; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineElectrolyser electrolyser ? electrolyser : null;
    }

    /** Public so the layout test can ask whether a footprint would fit before it builds one. */
    @Override
    public boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] volume : EXTRA_VOLUMES) {
            if (!MultiblockHandlerXR.checkSpace(level, core, volume, clicked, facing)) return false;
        }
        for (int along : RING_OFFSETS) {
            if (!MultiblockHandlerXR.checkSpace(level, ring(core, facing, along), RING_VOLUME, clicked, facing)) {
                return false;
            }
        }
        return true;
    }

    /** Three ports across each end of the hall. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] volume : EXTRA_VOLUMES) MultiblockHandlerXR.visitBox(core, volume, facing, visitor);
        for (int along : RING_OFFSETS) {
            MultiblockHandlerXR.visitBox(ring(core, facing, along), RING_VOLUME, facing, visitor);
        }

        Direction rot = facing.getClockWise();
        for (int side = -1; side <= 1; side++) {
            visitor.cell(core.relative(facing, 5).relative(rot, side), MASK_SOUTH);
            visitor.cell(core.relative(facing, -5).relative(rot, side), MASK_NORTH);
        }
        if (sidePorts) {
            // hbm-space: the two side proxies and the four getConPos points beside them (local frame: facing SOUTH,
            // rot = WEST): core - rot * 3 opens east, core + rot and the two end cells at rot + 1 open west
            visitor.cell(core.relative(rot, -3), MASK_EAST);
            visitor.cell(core.relative(rot, 1), MASK_WEST);
            visitor.cell(core.relative(facing, 5).relative(rot, 1), MASK_WEST);
            visitor.cell(core.relative(facing, -5).relative(rot, 1), MASK_WEST);
        }
    }

    /**
     * hbm-space's {@code MachineElectrolyser}/{@code TileEntityElectrolyser}: two more proxies on the long sides
     * ({@code makeExtra} at core - rot * 3 and core + rot) and four more connection points (ADDON-API-INVENTORY V-178).
     * Off in plain HBM (1.7: the six end ports); NTM: Space turns it on before any electrolyser exists.
     */
    public static boolean sidePorts = false;

    /** 1.7 stamps {@code inventory().power().fluid()} on the six. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int side = -1; side <= 1; side++) {
            visitor.passiveCell(core.relative(facing, 5).relative(rot, side), MASK_ALL, domains);
            visitor.passiveCell(core.relative(facing, -5).relative(rot, side), MASK_ALL, domains);
        }
        if (sidePorts) {
            visitor.passiveCell(core.relative(rot, -3), MASK_ALL, domains);
            visitor.passiveCell(core.relative(rot, 1), MASK_ALL, domains);
        }
    }

    private static BlockPos ring(BlockPos core, Direction facing, int along) {
        return core.relative(facing, along).above(3);
    }

    /** 1.7 {@code MachineElectrolyser.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {0, 0, 5, 5, 1, 3},
            new int[] {2, -1, 5, 5, 1, 1},
            new int[] {3, -3, 5, 5, 0, 0},
            new int[] {3, -1, 4, -4, -3, 3},
            new int[] {3, -1, 2, -2, -3, 3},
            new int[] {3, -1, 0, 0, -3, 3},
            new int[] {3, -1, -2, 2, -3, 3},
            new int[] {3, -1, -4, 4, -3, 3},
            new int[] {0, 0, 0, 0, -1, 2, 4, 3, 0},
            new int[] {0, 0, 0, 0, -1, 2, 2, 3, 0},
            new int[] {0, 0, 0, 0, -1, 2, 0, 3, 0},
            new int[] {0, 0, 0, 0, -1, 2, -2, 3, 0},
            new int[] {0, 0, 0, 0, -1, 2, -4, 3, 0},
        };
    }
}
