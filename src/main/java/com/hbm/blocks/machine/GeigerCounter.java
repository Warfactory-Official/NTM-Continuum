package com.hbm.blocks.machine;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.util.ContaminationUtil;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code GeigerCounter}: the placed Geiger counter, facing the placer's look direction.
 *
 * <p>Right-clicking without sneaking plays {@code hbm:item.techBoop} at the player (1.7 {@code playSoundAtEntity}, as
 * the handheld counter does) and prints the same GEIGER COUNTER readout ({@link ContaminationUtil#printGeigerData});
 * sneaking passes the click on. The comparator reads the chunk radiation live, +1 per 5 RAD/s rounded up, capped at 15
 * ({@link TileEntityGeiger#comparatorLevel}); the block entity's ten-tick sample only triggers the comparator update.
 *
 * <p>Like 1.7 (render type -1, a TESR), the block has no model of its own: the OBJ is drawn by
 * {@code HbmBlockEntityRenderers}' {@code BlockObjRenderer}. The shapes are 1.7's metadata bounds.
 */
public class GeigerCounter extends dev.golem.core.world.CompatBlock implements dev.golem.core.registry.TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH = Block.box(1.5D, 0D, 0D, 16D, 9D, 14D);
    private static final VoxelShape SOUTH = Block.box(0D, 0D, 2D, 14.5D, 9D, 16D);
    private static final VoxelShape WEST = Block.box(0D, 0D, 0D, 14D, 9D, 14.5D);
    private static final VoxelShape EAST = Block.box(2D, 0D, 1.5D, 16D, 9D, 16D);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.NORTH, NORTH);
        SHAPES.put(Direction.SOUTH, SOUTH);
        SHAPES.put(Direction.WEST, WEST);
        SHAPES.put(Direction.EAST, EAST);
    }

    public GeigerCounter(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityGeiger(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), NORTH);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide()) {
            // 1.7: world.playSoundAtEntity(player, "hbm:item.techBoop", 1.0F, 1.0F), at the player, not the block
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printGeigerData(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityGeiger geiger)) return 0;
        return TileEntityGeiger.comparatorLevel(geiger.check());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
