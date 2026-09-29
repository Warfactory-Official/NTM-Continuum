package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The legacy 9x9x5 Big Ass Tank multiblock. */
public class MachineBigAssTank extends MachineGiantTank {

    public MachineBigAssTank(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBigAssTank(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 6;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(stack.getItem() instanceof IItemFluidId identifier)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineBigAssTank tank) {
            var type = identifier.getType(level, tank.getBlockPos().getX(), tank.getBlockPos().getY(),
                    tank.getBlockPos().getZ(), stack);
            tank.setTankType(type);
            player.sendSystemMessage(Component.literal("Changed type to ")
                    .withStyle(ChatFormatting.YELLOW).append(type.getDisplayName()).append("!"));
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7: the four cross arms and the two long spurs the intake caps sit on. */
    private static final int[][] ARMS = {
            {4, 0, 5, -4, 2, 2}, {4, 0, -4, 5, 2, 2},
            {4, 0, 2, 2, 5, -4}, {4, 0, 2, 2, -4, 5},
            {3, 0, 6, -5, 0, 0}, {3, 0, -5, 6, 0, 0}};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] arm : ARMS) {
            if (!MultiblockHandlerXR.checkSpace(level, core, arm, clicked, facing)) return false;
        }
        return true;
    }

    /** One intake cap at each end of the long axis. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] arm : ARMS) MultiblockHandlerXR.visitBox(core, arm, facing, visitor);
        visitor.cell(core.relative(facing, 6), MASK_SOUTH);
        visitor.cell(core.relative(facing, -6), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.relative(facing, 6), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing, -6), MASK_ALL, PASSIVE_FLUID_IN);
    }

    /** 1.7 {@code MachineBigAssTank.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {5, 0, 4, 4, 4, 4},
            new int[] {4, 0, 5, -4, 2, 2},
            new int[] {4, 0, -4, 5, 2, 2},
            new int[] {4, 0, 2, 2, 5, -4},
            new int[] {4, 0, 2, 2, -4, 5},
            new int[] {3, 0, 6, -5, 0, 0},
            new int[] {3, 0, -5, 6, 0, 0}
        };
    }
}
