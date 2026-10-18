package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Oil Derrick block, a 1:1 port of the 1.12.2 {@code MachineOilWell}: a 10-tall structure with a 1-high
 * 3×3 base at ground level, a 9-high 1×1 tower above the core, and the 4 base corners one block up
 * (the old custom checkRequirement/fillSpace, copied unchanged). Core at the clicked position (offset 0).
 */
public class MachineOilWell extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

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

    private static final int[] PLINTH = {1, -1, 0, 0, 0, 0};
    private static final int[] TOWER = {8, 0, 1, 1, 1, 1};
    private static final int[] LEG = {-1, 1, 0, 0, 0, 0};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return MultiblockHandlerXR.checkSpace(level, core, PLINTH, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.above(), TOWER, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(1, 1, 1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(1, 1, -1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-1, 1, 1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-1, 1, -1), LEG, clicked, facing);
    }

    /**
     * No super: getDimensions is the placement envelope and is wider than what 1.7 actually fills; the
     * four edge-centre cells at ground level stay empty, which is what the derrick stands on.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, PLINTH, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(), TOWER, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(1, 1, 1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(1, 1, -1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-1, 1, 1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-1, 1, -1), LEG, facing, visitor);
    }

    /** The whole port surface is the core's own four horizontals. */
    @Override public int coreMask() { return MASK_HORIZONTAL | MASK_DOWN; }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, 0, ForgeDirection.EAST.ordinal()},
                {0, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, 0, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
}
