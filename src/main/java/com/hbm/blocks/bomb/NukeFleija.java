package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
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
import com.hbm.tileentity.bomb.TileEntityNukeFleija;

/** The F.L.E.I.J.A. ({@code nuke_fleija}): an MK3 antimatter blast of {@link BombConfig#fleijaRadius}. */
public class NukeFleija extends NukeBomb2Block {

    public NukeFleija(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeFleija(pos, state);
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
        igniteTestBomb(level, pos, BombConfig.fleijaRadius);
        return BombReturnCode.DETONATED;
    }

    /**
     * The 1.7 {@code igniteTestBomb}: the MK3 factory may hand back an already-dead entity (the anti-lag
     * cooldown), in which case neither the blast nor the cloud is spawned. The cloud sits on the raw
     * block corner rather than the centre because the original placed it there.
     */
    public static void igniteTestBomb(Level level, BlockPos pos, int radius) {
        if (level.isClientSide()) return;
        EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
        if (!ex.isRemoved()) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.addFreshEntity(ex);
            level.addFreshEntity(EntityCloudFleija.statFac(level, radius, pos.getX(), pos.getY(), pos.getZ()));
        }
    }
}
