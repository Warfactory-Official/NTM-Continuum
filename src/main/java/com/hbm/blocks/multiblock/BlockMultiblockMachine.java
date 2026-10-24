package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import com.hbm.tileentity.TileEntityProxyCell;

/**
 * A folded core whose cells are its own block, told apart by {@link #CELL}: what a 1.7
 * {@code BlockDummyable} machine becomes.
 *
 * <p>Deviation from ntm-next, which registers a bucketed cell block per owner triple: blast resistance,
 * hardness and map colour are each read off a per-block field, so a cell shared between machines answers
 * one set for all of them, and the bucket table makes the position-blind vanilla getters correct there.
 * A cell that <i>is</i> the machine's own block answers all of them, and the sound, the tool and the drop,
 * by construction, with no registry entries and no table.
 *
 * <p>A cell carries no pointer home; {@link MultiblockCoreIndex} is the sole cell-to-core authority.
 * {@link #CELL} says only whether a position is a cell, never whose.
 */
public abstract class BlockMultiblockMachine extends BlockMultiblockCore {

    public static final BooleanProperty CELL = BooleanProperty.create("cell");

    protected BlockMultiblockMachine(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CELL, Boolean.FALSE));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CELL);
    }

    @Override protected BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId) {
        return defaultBlockState().setValue(FACING, facing).setValue(CELL, Boolean.TRUE);
    }

    @Override public boolean isCellState(BlockState state) {
        return state.getValue(CELL);
    }

    /** The block entity renderer on the core draws the whole machine; every cell of it draws nothing. */
    @Override public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CELL) ? TileEntityProxyCell.at(pos, state) : createCoreBlockEntity(pos, state);
    }

    protected abstract BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state);

    /**
     * A block entity a cell still carries from a save made before its machine gave cells none (the RBMK
     * consoles): removes it and answers true, so the caller's tick stops there. Old worlds only.
     */
    public static boolean removeIfAtCell(BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Level level = blockEntity.getLevel();
        if (level == null || !(state.getBlock() instanceof BlockMultiblockCore block) || !block.isCellState(state)) {
            return false;
        }
        level.removeBlockEntity(blockEntity.getBlockPos());
        return true;
    }

    /**
     * The core of the multiblock {@code pos} belongs to, or null where the index has no answer, which is
     * every cell of a half-torn structure, and every cell on a client that has not yet meshed its core.
     */
    protected static @Nullable BlockPos coreOf(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getValue(CELL) ? MultiblockSurface.coreOfCell(level, pos) : pos;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAt(state, level, pos, false);
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                  CollisionContext context) {
        return shapeAt(state, level, pos, true);
    }

    /**
     * {@link #bounding} states one silhouette in the core's frame, so a cell reports it translated by its
     * own offset from the core and the union over the footprint is the structure. A cell that cannot find
     * its core reports a full cube rather than nothing, so a broken index never opens a hole to fall into.
     */
    private VoxelShape shapeAt(BlockState state, BlockGetter level, BlockPos pos, boolean forCollision) {
        if (bounding.isEmpty()) return Shapes.block();
        if (!state.getValue(CELL)) return cachedCellShape(0, 0, 0, state.getValue(FACING), forCollision);
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        if (core == null) return Shapes.block();
        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(core));
        if (block != this) return Shapes.block();
        Direction facing = level.getBlockState(core).getValue(FACING);
        int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
        return cachedCellShape(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing), facing, forCollision);
    }

    /** Whether the core's item carries state ({@code CarriedDrops}): a cell a player breaks then rolls the core's loot. */
    protected boolean carriesData() {
        return false;
    }

    /**
     * A cell has no block entity and its loot table is limited to the core, so a cell broken by a player drops the
     * core's loot, rolled with the core's block entity: the machine's contents come from whichever part was broken.
     */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.onPlayerWillDestroy(level, pos, state, player);
        if (!carriesData() || !(level instanceof ServerLevel server) || !state.getValue(CELL)
                || playerPreventsBlockDrops(player) || !player.hasCorrectToolForDrops(state)) return;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return;
        BlockState coreState = level.getBlockState(core);
        if (coreState.getBlock() != this) return;
        Block.dropResources(coreState, server, pos, server.getBlockEntity(core), player, player.getMainHandItem());
    }

    @Override protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(CELL)) BlockMultiblockCore.foldedCellRemoved(level, pos, movedByPiston);
        else super.removedAt(state, level, pos, movedByPiston);
    }

    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                           BlockHitResult hit) {
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return useAtCore(level.getBlockState(core), level, core, player, hit);
    }

    @Override protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                            BiConsumer<ItemStack, BlockPos> onHit) {
        if (!state.getValue(CELL)) return false;
        FoldedOwner owner = MultiblockSurface.ownerOf(level, pos);
        if (owner == null) return false;
        owner.block().onExplosionHitAtCore(owner.state(), level, owner.pos(), explosion, onHit);
        return true;
    }

    @Override protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos, state);
        return core == null ? 0 : super.onAnalogOutputSignal(level.getBlockState(core), level, core);
    }

    @Override protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                               boolean movedByPiston) {
        if (!wantsNeighborUpdates() || !(level instanceof ServerLevel server)) return;
        BlockPos core = coreOf(level, pos, state);
        if (core != null) cellNeighborChanged(server, core, pos);
    }

    @Override public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        return core == null ? null : super.getSettings(level, core);
    }

    @Override public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        if (core != null) super.pasteSettings(nbt, index, level, player, core);
    }

    @Override public String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        return core == null ? null : super.infoForDisplay(level, core);
    }
}
