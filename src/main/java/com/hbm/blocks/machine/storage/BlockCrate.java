package com.hbm.blocks.machine.storage;

import com.hbm.blocks.generic.CrateKeepContents;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * A horizontal-facing HBM storage crate block backed by a {@link TileEntityCrate}. One class serves all
 * three crates; the {@link CrateType} fixes the slot count / GUI, and the {@link BlockEntityType} supplier
 * is resolved lazily (the block is registered before the block-entity type). Right-click opens the menu.
 *
 * <p>Contents are kept on the dropped item via the crate's loot table (a {@code copy_components} of the
 * {@code minecraft:container} + {@code custom_name} components from the block entity), so no in-code drop
 * logic is needed, see {@link TileEntityCrate}.
 */
public class BlockCrate extends CarriedBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final CrateType crateType;

    public BlockCrate(Properties properties, CrateType crateType) {
        super(properties);
        this.crateType = crateType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCrate(crateType, pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                Menus.openWithPos(serverPlayer, provider, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

    /** 1.7 {@code removedByPlayer} with {@code /ntmserver CRATE_KEEP_CONTENTS} off: see {@link com.hbm.blocks.generic.CrateKeepContents}. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos,
                                       BlockState state, Player player) {
        CrateKeepContents.breakLoose(level, pos, state, player);
        super.onPlayerWillDestroy(level, pos, state, player);
    }
}
