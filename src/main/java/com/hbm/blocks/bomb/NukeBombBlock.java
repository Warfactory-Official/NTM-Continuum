package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import dev.golem.core.registry.TickingBlock;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Common block-side behavior shared by the four old MK5/Torex bomb blocks. */
public abstract class NukeBombBlock extends dev.golem.core.world.CompatBlock implements TickingBlock, IBomb {

    /** Legacy horizontal facing ordinals: north=2, south=3, west=4, east=5. */
    public static final IntegerProperty FACING = IntegerProperty.create("facing", 2, 5);

    protected NukeBombBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public abstract TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state);

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockHorizontal used the placer-facing opposite in 1.12.2; retain its numeric direction values.
        return this.defaultBlockState().setValue(FACING, legacyFacing(context.getHorizontalDirection().getOpposite()));
    }

    private static int legacyFacing(Direction direction) {
        return switch (direction) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityNukeBomb bomb) {
            bomb.placerID = player.getUUID();
        }
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        return explode(level, pos, detonator, detonator == null ? null : detonator.getUUID());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator,
                                  @Nullable UUID detonatorId) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeBomb bomb) || !bomb.isReady()) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        int radius = radiusFor(bomb);
        UUID placer = bomb.placerID;
        bomb.clearSlots();
        level.removeBlock(pos, false);
        ignite(level, pos, radius, detonatorId, placer);
        return BombReturnCode.DETONATED;
    }

    /** Selects the old block's radius before its component inventory is consumed. */
    protected abstract int radiusFor(TileEntityNukeBomb bomb);

    /** N2 intentionally uses the no-radiation MK5 factory; the other three are nuclear blasts. */
    protected boolean noRadiation() {
        return false;
    }

    private void ignite(Level level, BlockPos pos, int radius, @Nullable UUID detonatorId,
                        @Nullable UUID placer) {
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        EntityNukeExplosionMK5 mk5 = noRadiation()
                ? EntityNukeExplosionMK5.statFacNoRad(level, radius, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                : EntityNukeExplosionMK5.statFac(level, radius, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        mk5.detonator = detonatorId != null ? detonatorId : placer;
        if (level.addFreshEntity(mk5)) mk5.startChunkLoading();

        if (BombConfig.enableNukeClouds) {
            EntityNukeTorex torex = EntityNukeTorex.statFac(level,
                    pos.getX() + cloudOffset(), pos.getY() + cloudOffset(), pos.getZ() + cloudOffset(), radius);
            if (level.addFreshEntity(torex)) torex.startChunkLoading();
        }
    }

    /** Little Boy alone used the integer block origin for its old Torex call; all other bombs use the centre. */
    protected double cloudOffset() {
        return 0.5D;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING,
                rotation.rotate(Direction.from3DDataValue(state.getValue(FACING))).get3DDataValue());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING,
                mirror.mirror(Direction.from3DDataValue(state.getValue(FACING))).get3DDataValue());
    }
}
