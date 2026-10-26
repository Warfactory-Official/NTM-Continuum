package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Crane desk with its tall body and ground desk folded into the core. */
public class RBMKCraneConsole extends BlockMultiblockMachine implements IToolable {

    /** The console has no inventory or tanks, and its desk removes block entities found at its cells. */
    @Override public boolean proxiesCells() { return false; }

    private static final int[] DIMENSIONS = {1, 0, 0, 0, 1, 1};
    private static final int[] DESK = {0, 0, 0, 1, 1, 1};

    public RBMKCraneConsole(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked) && checkDeskSpace(level, core, facing, clicked);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitBox(core, DESK, facing, visitor);
    }

    private static void visitBox(BlockPos core, int[] dimensions, Direction facing, CellVisitor visitor) {
        for (int y = -dimensions[1]; y <= dimensions[0]; y++) {
            for (int z = -dimensions[2]; z <= dimensions[3]; z++) {
                for (int x = -dimensions[4]; x <= dimensions[5]; x++) {
                    if (x != 0 || y != 0 || z != 0) {
                        visitor.cell(core.offset(MultiblockMaskTable.worldX(x, y, z, facing), y,
                                MultiblockMaskTable.worldZ(x, y, z, facing)), MASK_NONE);
                    }
                }
            }
        }
    }

    private static boolean checkDeskSpace(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        int minX = core.getX(), maxX = core.getX(), minY = core.getY(), maxY = core.getY();
        int minZ = core.getZ(), maxZ = core.getZ();
        for (int y = -DESK[1]; y <= DESK[0]; y++) {
            for (int z = -DESK[2]; z <= DESK[3]; z++) {
                for (int x = -DESK[4]; x <= DESK[5]; x++) {
                    BlockPos pos = core.offset(MultiblockMaskTable.worldX(x, y, z, facing), y,
                            MultiblockMaskTable.worldZ(x, y, z, facing));
                    minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
                    minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
                    if (!pos.equals(clicked) && !level.getBlockState(pos).canBeReplaced()) return false;
                }
            }
        }
        return level.getEntitiesOfClass(Player.class, new AABB(minX, minY, minZ, maxX + 1D, maxY + 1D,
                maxZ + 1D)).isEmpty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isCellState(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof BlockEntityCraneConsole crane) crane.cycleCraneRotation();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        // 1.7 createNewTileEntity: only the core (meta >= offset) has a tile; BlockMultiblockMachine gives cells none
        return new BlockEntityCraneConsole(pos, state);
    }

    /** 1.7 {@code onScrew}: the screwdriver on any part finds the core. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        if (core != null && level.getBlockEntity(core) instanceof BlockEntityCraneConsole console) {
            console.cycleCraneRotation();
            console.setChanged();
        }
        return true;
    }

    /** 1.7 {@code RBMKCraneConsole.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {1, 0, 0, 0, 1, 1},
            new int[] {0, 0, 0, 1, 1, 1},
        };
    }
}
