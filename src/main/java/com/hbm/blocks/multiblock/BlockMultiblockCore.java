package com.hbm.blocks.multiblock;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Explosion;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Folded multiblock core. Cells carry no core pointer; ownership is persisted before their placement is
 * considered complete, and removal consults that record so neighbouring structures cannot be consumed.
 */
public abstract class BlockMultiblockCore extends CompatBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MASK_NONE = 0;
    public static final int MASK_DOWN = 1, MASK_UP = 2, MASK_NORTH = 4, MASK_SOUTH = 8, MASK_WEST = 16, MASK_EAST = 32;
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);
    private @Nullable MultiblockMaskTable table;

    protected BlockMultiblockCore(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Suppresses removal-triggered teardown while a batch replaces folded cells. */
    public static void withoutTeardown(Runnable action) {
        boolean previous = BUSY.get();
        BUSY.set(true);
        try {
            action.run();
        } finally {
            BUSY.set(previous);
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    public abstract int[] getDimensions();
    public abstract int getOffset();
    public int getHeightOffset() { return 0; }
    public Direction getDirModified(Direction direction) { return direction; }
    public int coreMask() { return MASK_NONE; }

    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        int[] dimensions = getDimensions();
        for (int y = -dimensions[1]; y <= dimensions[0]; y++) for (int z = -dimensions[2]; z <= dimensions[3]; z++)
            for (int x = -dimensions[4]; x <= dimensions[5]; x++) if (x != 0 || y != 0 || z != 0)
                visitor.cell(core.offset(MultiblockMaskTable.worldX(x, y, z, facing), y,
                        MultiblockMaskTable.worldZ(x, y, z, facing)), MASK_NONE);
    }

    protected void visitTeardownCells(BlockPos core, Direction facing, CellVisitor visitor) { visitCells(core, facing, visitor); }
    /**
     * The state written for one cell. The shape id is retained from ntm-next's folded contract: this
     * first slice has no geometry-cell interning yet, so every caller receives zero.
     */
    protected abstract BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId);
    public boolean isCellState(BlockState state) { return false; }

    public final boolean isCellOf(BlockState state) {
        return state.getBlock() instanceof BlockMultiblockCell || state.getBlock() == this && isCellState(state);
    }

    public final MultiblockMaskTable maskTable() {
        if (table == null) table = MultiblockMaskTable.bake(this);
        return table;
    }

    public final boolean claimsCell(BlockPos core, BlockPos cell, Direction facing) {
        return maskTable().maskAtLocal(MultiblockMaskTable.localX(cell.getX() - core.getX(), cell.getY() - core.getY(), cell.getZ() - core.getZ(), facing),
                cell.getY() - core.getY(), MultiblockMaskTable.localZ(cell.getX() - core.getX(), cell.getY() - core.getY(), cell.getZ() - core.getZ(), facing)) != MultiblockMaskTable.NOT_A_CELL;
    }

    public boolean isOpenAt(Direction facing, int dx, int dy, int dz, Direction side) {
        int mask = maskTable().maskAtLocal(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing));
        Direction local = side.getAxis().isVertical() ? side : switch (facing) {
            case WEST -> side.getCounterClockWise(); case NORTH -> side.getOpposite(); case EAST -> side.getClockWise(); default -> side;
        };
        return (mask & (1 << local.ordinal())) != 0;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(placer instanceof Player player)) return;
        boolean previous = BUSY.get();
        BUSY.set(true);
        try {
            level.removeBlock(pos, false);
        } finally {
            BUSY.set(previous);
        }
        Direction facing = getDirModified(switch (Mth.floor(player.getYRot() * 4.0F / 360.0F + .5D) & 3) {
            case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH;
        });
        BlockPos core = pos.above(getHeightOffset()).relative(facing, -getOffset());
        if (!checkRequirement(level, core, facing, pos)) {
            if (!player.getAbilities().instabuild && !player.getInventory().add(new ItemStack(this))) {
                player.drop(new ItemStack(this), false);
            }
            return;
        }
        if (level.isClientSide()) return;
        withoutTeardown(() -> {
            level.setBlock(core, defaultBlockState().setValue(FACING, facing), 3);
            fillSpace(level, core, facing);
        });
    }

    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        int[] dimensions = getDimensions();
        if (dimensions == null || dimensions.length != 6) return false;
        int[] rotated = rotate(dimensions, facing);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int x = core.getX() - rotated[4]; x <= core.getX() + rotated[5]; x++)
            for (int y = core.getY() - rotated[1]; y <= core.getY() + rotated[0]; y++)
                for (int z = core.getZ() - rotated[2]; z <= core.getZ() + rotated[3]; z++) {
                    if (x == clicked.getX() && y == clicked.getY() && z == clicked.getZ()) continue;
                    if (!level.getBlockState(probe.set(x, y, z)).canBeReplaced()) return false;
                }
        AABB footprint = new AABB(core.getX() - rotated[4], core.getY() - rotated[1], core.getZ() - rotated[2],
                core.getX() + rotated[5] + 1, core.getY() + rotated[0] + 1, core.getZ() + rotated[3] + 1);
        return level.getEntitiesOfClass(Player.class, footprint).isEmpty();
    }

    private static int[] rotate(int[] dimensions, Direction facing) {
        return switch (facing) {
            case NORTH -> new int[]{dimensions[0], dimensions[1], dimensions[3], dimensions[2], dimensions[5], dimensions[4]};
            case EAST -> new int[]{dimensions[0], dimensions[1], dimensions[5], dimensions[4], dimensions[2], dimensions[3]};
            case WEST -> new int[]{dimensions[0], dimensions[1], dimensions[4], dimensions[5], dimensions[3], dimensions[2]};
            default -> dimensions;
        };
    }

    public final void fillSpace(Level level, BlockPos core, Direction facing) {
        CoreIndexScratch scratch = CoreIndexScratch.borrow();
        try {
            visitCells(core, facing, (pos, mask) -> {
                int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
                int lx = MultiblockMaskTable.localX(dx, dy, dz, facing), lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
                level.setBlock(pos, cellStateFor(lx, dy, lz, facing, 0), 3);
                // An event or a failed state write may replace the target; an index entry is a cell claim.
                if (isCellOf(level.getBlockState(pos))) scratch.add(pos);
            });
            if (level instanceof ServerLevel server) for (int i = 0; i < scratch.size(); i++) MultiblockCoreIndex.put(server, BlockPos.of(scratch.get(i)), core);
        } finally { scratch.release(); }
    }

    @Override protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) removedAt(state, server, pos, movedByPiston);
    }

    /** Split out so own-block cells such as RBMK columns can route removal through the recorded owner. */
    protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && !BUSY.get()) demolish(level, pos, this, state.getValue(FACING), false);
    }

    public static void foldedCellRemoved(ServerLevel level, BlockPos cell, boolean movedByPiston) {
        long packed = MultiblockCoreIndex.lookup(level, cell);
        MultiblockCoreIndex.remove(level, cell);
        if (movedByPiston || BUSY.get() || packed == Long.MIN_VALUE) return;
        BlockPos core = BlockPos.of(packed);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(core));
        if (block != null) demolish(level, core, block, level.getBlockState(core).getValue(FACING), true);
    }

    private static void demolish(ServerLevel level, BlockPos core, BlockMultiblockCore block, Direction facing, boolean removeCore) {
        withoutTeardown(() -> {
            block.visitTeardownCells(core, facing, (cell, mask) -> {
                if (!block.isCellOf(level.getBlockState(cell)) || MultiblockCoreIndex.lookup(level, cell) != core.asLong()) return;
                level.removeBlock(cell, false);
                MultiblockCoreIndex.remove(level, cell);
            });
            if (removeCore) level.removeBlock(core, false);
        });
    }

    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) { return InteractionResult.TRY_WITH_EMPTY_HAND; }
    public void onExplosionHitAtCore(BlockState state, ServerLevel level, BlockPos core, Explosion explosion,
                                     BiConsumer<ItemStack, BlockPos> onHit) {
        forwardExplosionHit(state, level, core, explosion, onHit);
    }
    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return useAtCore(state, level, pos, player, hit); }

    @FunctionalInterface public interface CellVisitor { void cell(BlockPos pos, int mask); }
    public record FoldedOwner(BlockPos pos, BlockState state, BlockMultiblockCore block, Direction facing) {}
}
