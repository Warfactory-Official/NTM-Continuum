package com.hbm.tileentity.machine;

import com.hbm.lib.ModDamageSources;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * 1.7 {@code TileEntityBroadcaster}: every tick, every living thing within 25 blocks gets nausea for 15 s
 * (topped up once under 5 s left), and within 15 blocks takes {@code (15 - d) / 15 * 10} broadcast damage -
 * absolute and armour-piercing. Clients hear one of three broadcast loops, picked by a {@code Random} seeded
 * with {@code x + y + z}, audible to 25 blocks.
 */
public class TileEntityBroadcaster extends TileEntityLoadedBase {

    private @Nullable AudioWrapper audio;

    public TileEntityBroadcaster(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.BROADCASTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBroadcaster te) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cx - 25, cy - 25, cz - 25, cx + 25, cy + 25, cz + 25));

        for (LivingEntity e : list) {
            double d = Math.sqrt(Math.pow(e.getX() - cx, 2) + Math.pow(e.getY() - cy, 2) + Math.pow(e.getZ() - cz, 2));

            if (d <= 25) {
                MobEffectInstance confusion = e.getEffect(MobEffects.NAUSEA);
                if (confusion == null || confusion.getDuration() < 100)
                    e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0));
            }

            if (d <= 15) {
                double t = (15 - d) / 15 * 10;
                e.hurt(ModDamageSources.source(level, ModDamageSources.BROADCAST), (float) t);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityBroadcaster te) {
        if (te.audio == null) {
            te.audio = te.createAudioLoop();
            if (te.audio != null) te.audio.startSound();
        } else if (!te.audio.isPlaying()) {
            te.audio.stopSound();
            te.audio = te.createAudioLoop();
            if (te.audio != null) te.audio.startSound();
        }
        if (te.audio != null) te.audio.keepAlive();
    }

    private @Nullable AudioWrapper createAudioLoop() {
        Random rand = new Random(worldPosition.getX() + worldPosition.getY() + worldPosition.getZ());
        SoundEvent sound = switch (rand.nextInt(3) + 1) {
            case 1 -> ModSounds.BROADCAST_1.get();
            case 2 -> ModSounds.BROADCAST_2.get();
            default -> ModSounds.BROADCAST_3.get();
        };
        return AudioSystem.getLoopedSound(sound, SoundSource.BLOCKS, worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), 25F, 25F, 1.0F, 20);
    }

    /** 1.7 {@code invalidate} / {@code onChunkUnload}: the loop dies with the tile. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }
}
