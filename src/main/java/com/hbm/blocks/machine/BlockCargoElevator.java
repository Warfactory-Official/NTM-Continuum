package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.world.ItemStacks;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import dev.golem.core.data.Tags;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The freight lift's block half, ported from 1.12.2's {@code BlockCargoElevator}.
 *
 * <p>Its shape is not fixed: four corner guide rails run the full height of the structure and one slab
 * moves with the platform, so both are derived from the core's block entity every time they are asked
 * for. Because 26.1 drives the block outline from the same shape the collider uses, the fork's separate
 * custom-highlight hook is not ported. The outline follows the platform for free.
 *
 * <p>Right-clicking with another elevator in hand adds a course; with anything else, it toggles.
 * Breaking any part drops the whole stack, since the structure is only ever placed as whole courses.
 */
public class BlockCargoElevator extends BlockMultiblockMachine {

    /** The elevator splices its cells as it moves. */
    @Override public boolean proxiesCells() { return false; }

    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public BlockCargoElevator(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCargoElevator(pos, state);
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
    public void visitPlacedCells(BlockGetter level, BlockPos core, Direction facing, CellVisitor visitor) {
        int height = level.getBlockEntity(core) instanceof TileEntityCargoElevator lift ? lift.height : 0;
        visitCourses(core, height, visitor);
    }

    @Override
    public void visitPlacementCells(BlockPos core, Direction facing, ItemStack stack, CellVisitor visitor) {
        var data = ItemStacks.blockEntityData(stack, ModBlockEntities.CARGO_ELEVATOR.get());
        visitCourses(core, data == null ? 0 : Tags.getIntOr(data, "height", 0), visitor);
    }

    @Override
    protected boolean checksPlacementCells() {
        return true;
    }

    @Override
    protected void visitTeardownCells(ServerLevel level, BlockPos core, Direction facing, CellVisitor visitor) {
        // The removed core's entity may already be gone. Demolition checks the index before removing any cell.
        visitCourses(core, level.getMinY() + level.getHeight() - 1 - core.getY(), visitor);
    }

    private static void visitCourses(BlockPos core, int height, CellVisitor visitor) {
        var cell = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= height; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    visitor.cell(cell.setWithOffset(core, dx, dy, dz), MASK_NONE);
                }
            }
        }
    }

    @Override
    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
        int dy = cell.getY() - core.getY();
        return dy >= 0 && Math.abs(cell.getX() - core.getX()) <= 1 && Math.abs(cell.getZ() - core.getZ()) <= 1
                && level.getBlockState(cell).is(this)
                && level.getBlockEntity(core) instanceof TileEntityCargoElevator lift && dy <= lift.height;
    }

    @Override
    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        int floor = level.getMinY();
        if (!(level instanceof Level)) floor = Math.max(floor, cell.getY() - MultiblockSurface.REGION_REACH);
        var probe = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                probe.setWithOffset(cell, dx, 0, dz);
                var state = level.getBlockState(probe);
                while (state.is(this) && state.getValue(CELL) && probe.getY() > floor) {
                    probe.move(Direction.DOWN);
                    state = level.getBlockState(probe);
                }
                if (state.is(this) && !state.getValue(CELL)
                        && claimsCell(level, probe, cell, state.getValue(FACING))) return probe.immutable();
            }
        }
        return null;
    }

    @Override
    protected boolean carriesData() {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return structureShape(world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
                                        CollisionContext context) {
        return structureShape(world, pos);
    }

    private VoxelShape structureShape(BlockGetter world, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof TileEntityCargoElevator lift)) {
            return Shapes.block();
        }

        return lift.shape().move(core.getX() - pos.getX(), core.getY() - pos.getY(), core.getZ() - pos.getZ());
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityCargoElevator lift)) {
            return InteractionResult.SUCCESS;
        }

        if (stack.is(asItem())) {
            addCourse(level, core, lift, player, stack);
        } else {
            lift.toggleElevator();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityCargoElevator lift) {
            lift.toggleElevator();
        }
        return InteractionResult.SUCCESS;
    }

    private void addCourse(Level level, BlockPos core, TileEntityCargoElevator lift, Player player,
                           ItemStack stack) {
        int top = core.getY() + lift.height + 1;
        if (level.isOutsideBuildHeight(top)) return;
        for (int x = core.getX() - 1; x <= core.getX() + 1; x++) {
            for (int z = core.getZ() - 1; z <= core.getZ() + 1; z++) {
                BlockPos target = new BlockPos(x, top, z);
                if (!level.getBlockState(target).canBeReplaced()) return;
            }
        }

        for (int x = core.getX() - 1; x <= core.getX() + 1; x++) {
            for (int z = core.getZ() - 1; z <= core.getZ() + 1; z++) {
                BlockPos target = new BlockPos(x, top, z);
                level.setBlock(target, defaultBlockState().setValue(CELL, Boolean.TRUE), 3);
                if (level instanceof ServerLevel server) MultiblockCoreIndex.put(server, target, core);
            }
        }
        lift.height++;
        lift.markChanged();
        if (!player.isCreative()) stack.shrink(1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(CELL)) return List.of();
        BlockEntity entity = params.getOptionalParameter(
                LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof TileEntityCargoElevator lift)) return super.getDrops(state, params);

        List<ItemStack> drops = new ArrayList<>();
        int remaining = lift.height + 1;
        while (remaining > 0) {
            int size = Math.min(remaining, 64);
            remaining -= size;
            drops.add(new ItemStack(this, size));
        }
        return drops;
    }

}
