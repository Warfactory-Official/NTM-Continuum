package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockCoreIndex;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
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
 * custom-highlight hook is not ported — the outline follows the platform for free.
 *
 * <p>Right-clicking with another elevator in hand adds a course; with anything else, it toggles.
 * Breaking any part drops the whole stack, since the structure is only ever placed as whole courses.
 */
public class BlockCargoElevator extends BlockMultiblockMachine {

    public BlockCargoElevator(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCargoElevator(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * The lift grows a course at a time, so its real extent is live configuration rather than a function
     * of the core's state. Teardown is allowed a superset for exactly this case, and the demolition guards
     * every cell on the index, so overshooting the column costs a lookup and claims nothing that is not
     * ours.
     */
    private static final int MAX_COURSES = 384;

    @Override
    protected void visitTeardownCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int dy = 0; dy <= MAX_COURSES; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    visitor.cell(core.offset(dx, dy, dz), MASK_NONE);
                }
            }
        }
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

        double dx = core.getX() - pos.getX();
        double dy = core.getY() - pos.getY();
        double dz = core.getZ() - pos.getZ();
        double top = lift.height + 1;
        double platform = lift.extension;

        VoxelShape shape = Shapes.empty();
        for (AABB box : new AABB[]{
                new AABB(-1D, 0D, -1D, -0.75D, top, -0.75D),
                new AABB(-1D, 0D, 1.75D, -0.75D, top, 2D),
                new AABB(1.75D, 0D, -1D, 2D, top, -0.75D),
                new AABB(1.75D, 0D, 1.75D, 2D, top, 2D),
                new AABB(-1D, 0.75D + platform, -1D, 2D, 1D + platform, 2D)}) {
            shape = Shapes.or(shape, Shapes.create(box.move(dx, dy, dz)));
        }
        return shape;
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
        lift.setChanged();
        if (!player.isCreative()) stack.shrink(1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
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
