package com.hbm.blocks.machine.storage;

import com.hbm.blocks.generic.CrateKeepContents;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import org.jspecify.annotations.Nullable;

/**
 * The iron, steel and desh crates (1.7 {@code BlockStorageCrate}) over a {@link TileEntityCrate}; the
 * {@link CrateType} fixes the slot count and GUI. Right-click opens the crate if the player may access it (a
 * padlock or the key kit in hand acts instead) and lets any spiders out. The contents, name and lock ride on
 * the dropped item through the loot table.
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (TileEntityLockableBase.LOCK_TOOLS.test(player.getMainHandItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof TileEntityCrateBase crate && crate.canAccess(player)
                && player instanceof ServerPlayer serverPlayer) {
            Menus.openWithPos(serverPlayer, crate, pos);
            TileEntityCrateBase.spawnSpiders(player, level, crate);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityCrate crate
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(crate) : 0;
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

    /** 1.7 {@code removedByPlayer} with {@code /ntmserver CRATE_KEEP_CONTENTS} off: see {@link CrateKeepContents}. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos,
                                       BlockState state, Player player) {
        CrateKeepContents.breakLoose(level, pos, state, player);
        super.onPlayerWillDestroy(level, pos, state, player);
    }
}
