package com.hbm.blocks.generic;

import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.client.Links;
import dev.golem.core.world.CompatBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code Guide}, "Hbm's Nuclear Tech Mod Manual [LEGACY]" ({@code book_guide_block}; the item id
 * {@code book_guide} belongs to the guide books, and 1.7 kept the two apart as {@code tile.} and
 * {@code item.}). A block with six faces that turns to its placer; right-clicking it on the client opens the
 * wiki in the browser, and looking at it shows "Click to open wiki".
 *
 * <p>{@link #FACING} is the placer's look direction. 1.7's metadata table puts the front texture on the
 * placer's right-hand side (facing south: front on west), which the declared model reproduces.
 */
public class Guide extends CompatBlock implements ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String WIKI = "https://nucleartech.wiki/wiki/Main_Page";

    public Guide(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide() && !player.isShiftKeyDown()) {
            Links.open(WIKI);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public Data getLookOverlay(Level level, BlockPos pos, Player player) {
        // 1.7 printHook: green over dark green, no further lines.
        return new Data("Click to open wiki", 0x00FF00, 0x006000, List.of());
    }
}
