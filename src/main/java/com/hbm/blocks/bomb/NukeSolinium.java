package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import com.hbm.tileentity.bomb.TileEntityNukeSolinium;

/** The Blue Rinse ({@code nuke_solinium}): a solinium-flavoured MK3 blast of {@link BombConfig#soliniumRadius}. */
public class NukeSolinium extends NukeBomb2Block {

    public NukeSolinium(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeSolinium(pos, state);
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
        igniteTestBomb(level, pos, BombConfig.soliniumRadius);
        return BombReturnCode.DETONATED;
    }

    public static void igniteTestBomb(Level level, BlockPos pos, int radius) {
        if (level.isClientSide()) return;
        EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius).makeSol();
        if (!ex.isRemoved()) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.addFreshEntity(ex);
            level.addFreshEntity(EntityCloudSolinium.statFac(level, radius, pos.getX(), pos.getY(), pos.getZ()));
        }
    }
}
