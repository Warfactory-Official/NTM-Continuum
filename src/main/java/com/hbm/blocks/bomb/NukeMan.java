package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The Fat Man ({@code nuke_man}) bomb block. Ported from 1.12.2. Renders {@code INVISIBLE} (its OBJ
 * model is drawn by a {@code BlockEntityRenderer}); right-click opens the assembly GUI, and a redstone
 * signal detonates it when {@link TileEntityNukeMan#isReady()}. Detonation spawns an
 * {@link EntityNukeExplosionMK5} of radius {@link BombConfig#manRadius}.
 *
 * <p>{@code FACING} (2–5, the old ForgeDirection horizontal ordinals N/S/W/E) drives the model yaw.
 */
public class NukeMan extends dev.golem.core.world.CompatBlock implements EntityBlock, IBomb {

    public static final IntegerProperty FACING = IntegerProperty.create("facing", 2, 5);

    public NukeMan(Properties properties) {
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
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeMan(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockHorizontal used the placer-facing opposite in 1.12.2; retain its numeric direction values.
        int facing = switch (context.getHorizontalDirection().getOpposite()) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2; // NORTH
        };
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityNukeMan nuke) {
            nuke.placerID = player.getUUID();
        }
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
            }
        }
        return player.isShiftKeyDown() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos,
                                     net.minecraft.world.level.block.Block block, boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        // Drop the bomb's components when it's broken (the old breakBlock behaviour).
        if (level.getBlockEntity(pos) instanceof TileEntityNukeMan nuke) {
            net.minecraft.world.Containers.dropContents(level, pos, nuke);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeMan entity)) return BombReturnCode.UNDEFINED;

        if (entity.isReady()) {
            UUID placer = entity.placerID;
            entity.clearSlots();
            level.removeBlock(pos, false);
            ignite(level, detonator, pos, placer);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    private void ignite(Level level, @Nullable Entity detonator, BlockPos pos, @Nullable UUID placer) {
        level.playSound(null, pos,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);

        EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFac(level, BombConfig.manRadius,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (detonator != null) mk5.setDetonator(detonator);
        else mk5.detonator = placer;
        level.addFreshEntity(mk5);

        // The mushroom cloud (which carries the shockwave ring, condensation collar, flare, flash + the
        // client sky flash / detonation sound / HUD white-out + shake). Gated like the original, and it
        // only affects visuals — the blast/crater above runs regardless.
        if (BombConfig.enableNukeClouds) {
            EntityNukeTorex torex = EntityNukeTorex.statFac(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BombConfig.manRadius);
            level.addFreshEntity(torex);
        }
    }
}
