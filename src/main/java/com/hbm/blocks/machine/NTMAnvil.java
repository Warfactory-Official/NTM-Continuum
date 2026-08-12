package com.hbm.blocks.machine;

import com.hbm.inventory.container.ContainerAnvil;
import com.hbm.platform.Services;
import com.hbm.tileentity.TileEntityAnvil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The NTM anvil — HBM's tiered crafting station, ported from the 1.12.2 {@code NTMAnvil}. A stateless
 * GUI block: right-click opens {@link ContainerAnvil} (two smithing slots + the construction-recipe
 * browser); the {@link #tier} is baked into the block instance and gates which recipes it can run.
 *
 * <p>Rendering: the old anvils were {@code forge_marker} OBJ blockstates, so like the machines the block
 * is {@link RenderShape#INVISIBLE} and carries a no-op {@link TileEntityAnvil} for
 * {@link com.hbm.client.render.AnvilObjRenderer}. The thin per-axis collision box is faithful to the old
 * {@code getBoundingBox}. <b>Deferred:</b> the old {@code BlockFalling} gravity (a falling-block entity
 * would render nothing for an INVISIBLE block) and the tier tooltip's JEI hooks.
 */
public class NTMAnvil extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final int TIER_IRON = 1;
    public static final int TIER_STEEL = 2;
    public static final int TIER_OIL = 3;
    public static final int TIER_NUCLEAR = 4;
    public static final int TIER_RBMK = 5;
    public static final int TIER_FUSION = 6;
    public static final int TIER_PARTICLE = 7;
    public static final int TIER_GERALD = 8;
    /** The murky easter-egg anvil's magic tier (as upstream). */
    public static final int TIER_MURKY = 1916169;

    public final int tier;

    // The old thin anvil AABB (0.75 tall, 0.5 wide across the facing axis).
    private static final VoxelShape SHAPE_X = Block.box(4, 0, 0, 12, 12, 16);
    private static final VoxelShape SHAPE_Z = Block.box(0, 0, 4, 16, 12, 12);

    public NTMAnvil(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAnvil(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Services.PLATFORM.openMenu(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, p) -> new ContainerAnvil(id, inventory, pos),
                    Component.translatable("container.anvil", this.tier)), pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Faithful to the old anvil: it faces AWAY from the placer (getHorizontalFacing().getOpposite()).
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
