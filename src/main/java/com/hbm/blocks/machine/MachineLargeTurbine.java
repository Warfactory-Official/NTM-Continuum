package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The four-long legacy steam turbine multiblock. */
public class MachineLargeTurbine extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineLargeTurbine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineLargeTurbine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 3, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineLargeTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getPowerPos(), pos, dir);
    }

    /**
     * The rear anchor takes power and the other three carry steam. 1.7 states those two surfaces at
     * separate call sites, so folding them into one mask would open a cable at a steam port and a duct at
     * the power one; the roles keep them apart.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing), MASK_SOUTH, ROLE_FLUID);
        visitor.cell(core.relative(facing, -3), MASK_NORTH, ROLE_POWER);
        visitor.cell(core.relative(rot), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(rot, -1), MASK_EAST, ROLE_FLUID);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -1), MASK_ALL, domains);
    }
}
