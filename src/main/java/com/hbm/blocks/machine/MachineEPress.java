package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.api.block.IToolable;
import com.hbm.tileentity.machine.PlushiePress;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Electric Press: a 1×1×3 dummyable column (core at the bottom, two dummies stacked above).
 */
public class MachineEPress extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder, IToolable {

    public MachineEPress(Properties properties) {
        super(properties);
    }

    /**
     * Any face of the column routes hoppers/pipes to the core's sided inventory.
     */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineEPress te ? te : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineEPress(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        return PlushiePress.drill(level, pos, level.getBlockState(pos).getValue(CELL), tool);
    }

    /**
     * 1.7's six-direction loop minus UP, buried under the machine's own cell.
     */
    @Override
    public int coreMask() {
        return MASK_HORIZONTAL | MASK_DOWN;
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][]{
                {0, 0, 0, ForgeDirection.EAST.ordinal()},
                {0, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, 0, ForgeDirection.NORTH.ordinal()},
                {0, 0, 0, ForgeDirection.DOWN.ordinal()},
        }, dir);
    }
}
