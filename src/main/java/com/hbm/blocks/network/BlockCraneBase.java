package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import com.hbm.items.tool.ItemTooling;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.tileentity.network.TileEntityCraneBase;

/**
 * 1.7 {@code blocks/network/BlockCraneBase}.
 *
 * <p>1.7 keeps the input side in the metadata and an output override on the tile, {@code UNKNOWN} meaning
 * "opposite the input". Both sides are blockstate here (which is also what selects the per-face textures the
 * old {@code getIcon} computed), and "overridden" is the surviving test {@code OUTPUT != INPUT.getOpposite()}.
 */
public abstract class BlockCraneBase extends CompatBlock implements TickingBlock, IToolable {

    public static final EnumProperty<Direction> INPUT = EnumProperty.create("input", Direction.class);
    public static final EnumProperty<Direction> OUTPUT = EnumProperty.create("output", Direction.class);

    protected BlockCraneBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INPUT, Direction.NORTH).setValue(OUTPUT, Direction.SOUTH));
    }

    public static Direction inputSide(BlockState state) {
        return state.getValue(INPUT);
    }

    public static Direction outputSide(BlockState state) {
        return state.getValue(OUTPUT);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INPUT, OUTPUT);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(INPUT, rotation.rotate(state.getValue(INPUT)))
                .setValue(OUTPUT, rotation.rotate(state.getValue(OUTPUT)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(INPUT, mirror.mirror(state.getValue(INPUT)))
                .setValue(OUTPUT, mirror.mirror(state.getValue(OUTPUT)));
    }

    /** 1.7 {@code BlockPistonBase.determineOrientation}: the input side faces the placer. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction input = context.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(INPUT, input).setValue(OUTPUT, input.getOpposite());
    }

    public Direction getInputSide(Level level, BlockPos pos) {
        return inputSide(level.getBlockState(pos));
    }

    public Direction getOutputSide(Level level, BlockPos pos) {
        return outputSide(level.getBlockState(pos));
    }

    /** 1.7 {@code onBlockActivated}: a tool or the wand acts instead, sneaking does nothing, anything else opens the GUI. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemTooling || stack.getItem() instanceof ItemConveyorWand) {
            return InteractionResult.PASS;
        }
        return onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider)) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 1.7 {@code onScrew} + {@code TileEntityCraneBase.setInput/setOutputOverride}: the clicked face becomes the
     * input, or the output while sneaking. Aiming a side at the one it already holds takes the opposite instead,
     * and aiming it at the other end swaps the pair.
     */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (side == null) return false;

        BlockState state = level.getBlockState(pos);
        Direction input = inputSide(state);
        Direction output = outputSide(state);
        Direction aimed = side;

        if (player.isShiftKeyDown()) {
            if (aimed == output) aimed = aimed.getOpposite();
            level.setBlockAndUpdate(pos, aimed == input
                    ? state.setValue(INPUT, output).setValue(OUTPUT, input)
                    : state.setValue(OUTPUT, aimed));
        } else {
            if (aimed == input) aimed = aimed.getOpposite();
            level.setBlockAndUpdate(pos, aimed == output
                    ? state.setValue(INPUT, output).setValue(OUTPUT, input)
                    : state.setValue(INPUT, aimed));
        }

        return true;
    }
}
