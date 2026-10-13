package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.tileentity.deco.TileEntityEmitter;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Dyes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockEmitter} (deco_emitter): an iron cube with the {@code emitter} texture that shines a coloured
 * beam out of the face it was placed towards (1.7 {@code BlockPistonBase.determineOrientation}, so up and down
 * too). A dye recolours it and is used up; the screwdriver widens the beam by 1/8, the defuser narrows it (not
 * below 1/8), the hand drill cycles the five special effects.
 */
public class BlockEmitter extends CompatBlock implements TickingBlock, IToolable {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public BlockEmitter(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityEmitter(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        DyeColor dye = Dyes.colorOf(held);
        if (dye == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TileEntityEmitter te) {
            // 1.7 ItemDye.field_150922_c, the firework colour table
            te.color = dye.getFireworkColor();
            te.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand,
                           ToolType tool) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityEmitter te)) return false;

        if (tool == ToolType.SCREWDRIVER) {
            te.girth += 0.125F;
            te.setChanged();
            return true;
        }
        if (tool == ToolType.DEFUSER) {
            te.girth -= 0.125F;
            if (te.girth < 0.125F) te.girth = 0.125F;
            te.setChanged();
            return true;
        }
        if (tool == ToolType.HAND_DRILL) {
            te.effect = (te.effect + 1) % TileEntityEmitter.effectCount;
            te.setChanged();
            return true;
        }
        return false;
    }

    public static void addInformation(List<Component> list) {
        list.add(Component.literal("Use screwdriver to widen beam").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use defuser to narrow beam").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use hand drill to cycle special effects").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use dye to change color").withStyle(ChatFormatting.GOLD));
    }

    /** Structure rotation and mirroring turn the placed facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
