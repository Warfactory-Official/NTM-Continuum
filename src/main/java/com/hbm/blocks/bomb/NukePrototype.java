package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import com.hbm.tileentity.bomb.TileEntityNukePrototype;

/**
 * The Prototype ({@code nuke_prototype}): the F.L.E.I.J.A. blast at {@link BombConfig#prototypeRadius}. Unlike
 * the other bombs it also fires when right-clicked with a held {@code igniter}, provided it is assembled.
 */
public class NukePrototype extends NukeBomb2Block {

    public NukePrototype(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukePrototype(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && stack.getItem() == ModItems.igniter.get()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof TileEntityNukeBomb bomb && bomb.isReady()) {
                bomb.clearSlots();
                level.removeBlock(pos, false);
                NukeFleija.igniteTestBomb(level, pos, BombConfig.prototypeRadius);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        return explode(level, pos, detonator, detonator == null ? null : detonator.getUUID());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator, @Nullable UUID detonatorId) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeBomb bomb)) return BombReturnCode.UNDEFINED;
        if (!bomb.isReady()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        bomb.clearSlots();
        level.removeBlock(pos, false);
        NukeFleija.igniteTestBomb(level, pos, BombConfig.prototypeRadius);
        return BombReturnCode.DETONATED;
    }
}
