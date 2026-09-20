package com.hbm.blocks.bomb;

import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import com.hbm.tileentity.bomb.TileEntityNukeBalefire;

/**
 * The Balefire Bomb ({@code nuke_fstbmb}). A 1.7 {@code BlockMachineBase} with a GUI: the egg and battery
 * live in the tile, which also runs the countdown; redstone or a detonator fires it immediately when loaded.
 */
public class NukeBalefire extends NukeBomb2Block {

    public NukeBalefire(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeBalefire(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        return explode(level, pos, detonator, detonator == null ? null : detonator.getUUID());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator, @Nullable UUID detonatorId) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeBalefire bomb)) return BombReturnCode.UNDEFINED;
        if (!bomb.isLoaded()) return BombReturnCode.ERROR_MISSING_COMPONENT;
        bomb.explode();
        return BombReturnCode.DETONATED;
    }
}
