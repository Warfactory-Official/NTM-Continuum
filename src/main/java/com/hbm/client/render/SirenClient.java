package com.hbm.client.render;

import com.hbm.items.machine.ItemCassette.SoundType;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.sound.ModPowerGenSounds;
import com.hbm.packet.SirenPayload;
import com.hbm.tileentity.machine.TileEntityMachineSiren;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import com.hbm.sound.SoundLoopSiren;

/** 1.7 {@code TESirenPacket.Handler}, with the {@code SoundLoopSiren.list} it walks. */
public final class SirenClient {

    private SirenClient() {}

    public static final List<SoundLoopSiren> LIST = new ArrayList<>();

    public static void handle(SirenPayload payload, GolemNetwork.ClientContext context) {
        Level level = context.level();
        if (level == null) return;
        BlockEntity te = level.getBlockEntity(payload.pos());
        if (!(te instanceof TileEntityMachineSiren)) return;

        SoundLoopSiren sound = null;
        for (SoundLoopSiren candidate : LIST) {
            if (candidate.getTE() == te) sound = candidate;
        }

        TrackType track = TrackType.getEnum(payload.track());
        Minecraft minecraft = Minecraft.getInstance();

        if (payload.active()) {
            if (sound == null) {
                // Start sound
                if (payload.track() > 0) {
                    SoundEvent event = ModPowerGenSounds.byKey(track.getSoundKey());
                    if (event == null) return;
                    boolean loop = track.getType() == SoundType.LOOP;
                    SoundLoopSiren s = new SoundLoopSiren(event, te, track.getType());
                    s.setRepeat(loop);
                    s.intendedVolume = track.getVolume();
                    LIST.add(s);
                    minecraft.getSoundManager().play(s);
                }
            } else {
                SoundEvent event = ModPowerGenSounds.byKey(track.getSoundKey());
                if (event != null && sound.getEvent() != event) {
                    // Track switched, stop and restart
                    sound.endSound();
                    if (payload.track() > 0) {
                        SoundLoopSiren s = new SoundLoopSiren(event, te, track.getType());
                        LIST.add(s);
                        minecraft.getSoundManager().play(s);
                    }
                }
                sound.intendedVolume = track.getVolume();
            }
        } else {
            if (sound != null) {
                // Stop sound
                sound.endSound();
                LIST.remove(sound);
            }
        }
    }
}
