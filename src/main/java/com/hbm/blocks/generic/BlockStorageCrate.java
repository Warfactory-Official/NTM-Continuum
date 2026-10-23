package com.hbm.blocks.generic;

import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.tileentity.machine.storage.TileEntityCrateTungsten;
import com.hbm.tileentity.machine.storage.TileEntitySafe;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;

/**
 * 1.7 {@code BlockStorageCrate} for its two lockable crates, the tungsten crate and the safe (the iron, steel
 * and desh crates are the port's {@link com.hbm.blocks.machine.storage.BlockCrate}).
 *
 * <p>Right-click without sneaking opens the crate if {@link TileEntityLockableBase#canAccess} lets the player
 * in (a matching key, the red key card, or a successful pick), and lets any spiders out. A padlock or the key
 * kit in hand is handed to the item instead. The contents, name and lock ride on the dropped item.
 *
 * <p>{@link #HOT} is 1.7's metadata 1 on the tungsten crate: set while a laser keeps it heated. Placement faced
 * the crate towards the player (1.7 metadata 2..5); the safe shows its door on that face.
 */
public class BlockStorageCrate extends CarriedBlock implements TickingBlock, IRadResistantBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HOT = BooleanProperty.create("hot");

    public enum Kind { TUNGSTEN, SAFE }

    private final Kind kind;

    public BlockStorageCrate(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HOT, false));
    }

    public Kind kind() {
        return kind;
    }

    /** 1.12.2 {@code BlockStorageCrateRadResistant}: the tungsten crate seals radiation, the safe does not. */
    @Override
    public boolean isRadResistant(BlockGetter level, BlockPos pos) {
        return isRadShielding();
    }

    @Override
    public boolean isRadShielding() {
        return kind == Kind.TUNGSTEN;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (kind == Kind.TUNGSTEN && level instanceof ServerLevel server && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(server, pos);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        super.onBlockRemoved(state, level, pos, movedByPiston);
        if (kind == Kind.TUNGSTEN && level instanceof ServerLevel server) RadiationSystemNT.markSectionForRebuild(server, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HOT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.TUNGSTEN ? new TileEntityCrateTungsten(pos, state) : new TileEntitySafe(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityCrateBase crate
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(crate) : 0;
    }

    /** 1.7 {@code removedByPlayer} with {@code /ntmserver CRATE_KEEP_CONTENTS} off: see {@link com.hbm.blocks.generic.CrateKeepContents}. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos,
                                       BlockState state, Player player) {
        CrateKeepContents.breakLoose(level, pos, state, player);
        super.onPlayerWillDestroy(level, pos, state, player);
    }
}
