package com.hbm.client.jetpack;

import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The moving jetpack loop: volume is log(thrust*30+1)*.25, exactly as old MovingSoundJetpack. */
public final class JetpackSound {

    private static final Map<UUID, Loop> LOOPS = new HashMap<>();

    private JetpackSound() {}

    public static void clear(UUID playerId) {
        if (playerId == null) return;
        Loop loop = LOOPS.remove(playerId);
        if (loop != null) loop.finish();
    }

    public static void clearAll() {
        for (Loop loop : LOOPS.values()) loop.finish();
        LOOPS.clear();
    }

    public static void tick(Player player, JetpackClientBridge.State state) {
        if (state == null || state.thrust() <= 0.001F || Minecraft.getInstance().player == null) {
            end(player);
            return;
        }
        Loop loop = LOOPS.get(player.getUUID());
        if (loop == null || loop.isStopped()) {
            loop = new Loop(player);
            LOOPS.put(player.getUUID(), loop);
            Minecraft.getInstance().getSoundManager().play(loop);
        }
    }

    private static void end(Player player) {
        if (player != null) clear(player.getUUID());
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Player player;

        private Loop(Player player) {
            super(ModSounds.JETPACK.get(), SoundSource.PLAYERS,
                    SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
        }

        @Override
        public void tick() {
            JetpackClientBridge.State state = JetpackClientBridge.state(player);
            if (player.isRemoved() || state == null || state.thrust() <= 0.001F) {
                stop();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.volume = (float) Math.log(state.thrust() * 30F + 1F) * 0.25F;
        }

        private void finish() {
            stop();
        }
    }
}
