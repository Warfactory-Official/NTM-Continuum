package com.hbm.client;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.packet.CasingEjectPayload;
import com.hbm.packet.GunCasingEjectPayload;
import com.hbm.packet.MuzzleFlashPayload;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.GameTime;
import dev.golem.core.client.ClientTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Client-owned muzzle, casing and gun-loop state shared by packet handlers and weapon renderers. */
public final class GunEffectState {
    /** Observer flashes use the source map shape so renderers can read the timestamp without a new adapter. */
    public static final Map<LivingEntity, Long> flashMap = new HashMap<>();
    private static final Map<LivingEntity, GunLoopSound> LOOPS = new HashMap<>();
    private static Consumer<CasingEjectPayload> casingHandler;
    private static Consumer<GunCasingEjectPayload> gunCasingHandler;
    private static boolean installed;

    private GunEffectState() {}

    public static void install() {
        if (installed) return;
        installed = true;
        ClientTicks.onEndTick(GunEffectState::tick);
    }

    public static void setCasingHandler(Consumer<CasingEjectPayload> handler) {
        casingHandler = handler;
    }

    public static void setGunCasingHandler(Consumer<GunCasingEjectPayload> handler) {
        gunCasingHandler = handler;
    }

    public static void handleCasing(CasingEjectPayload payload) {
        Consumer<CasingEjectPayload> handler = casingHandler;
        if (handler != null) handler.accept(payload);
    }

    public static void handleGunCasing(GunCasingEjectPayload payload) {
        Consumer<GunCasingEjectPayload> handler = gunCasingHandler;
        if (handler != null) handler.accept(payload);
    }

    public static void markMuzzleFlash(MuzzleFlashPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        LivingEntity entity = entity(level, payload.entityId());
        if (entity == null || entity == minecraft.player || !(entity.getMainHandItem().getItem() instanceof ItemGunBaseNT)) return;
        flashMap.put(entity, GameTime.millis(level));
    }

    public static long flashAt(LivingEntity entity) {
        return flashMap.getOrDefault(entity, -1L);
    }

    public static void clear() {
        flashMap.clear();
        LOOPS.values().forEach(GunLoopSound::stopLoop);
        LOOPS.clear();
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clear();
            return;
        }
        long now = GameTime.millis(level);
        if (level.getGameTime() % 30L == 0L) flashMap.values().removeIf(stamp -> now - stamp >= 150L);
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.getItem() instanceof ItemGunStinger
                && ItemGunStinger.getLockonProgress(stack) > 0 && !ItemGunBaseNT.getIsLockedOn(stack)) {
            updateLoop(minecraft.player, ModGunSounds.LOCKON.get(), 1F, 1F);
        } else if ((stack.getItem() instanceof ItemGunChemthrower || isFlamer(stack))
                // a put-away gun reads CYCLE, with a clock of 0 when new, until the server draws it
                && ItemGunBaseNT.getIsEquipped(stack)
                && ItemGunBaseNT.getLastAnim(stack, 0) == GunAnimation.CYCLE
                && ItemGunBaseNT.getAnimTimer(stack, 0) < 5) {
            updateLoop(minecraft.player, ModGunSounds.FIRE_FLAMER_LOOP.get(), 1F, 1F);
        } else if (isTau(stack) && ItemGunBaseNT.getLastAnim(stack, 0) == GunAnimation.SPINUP
                && ItemGunBaseNT.getAnimTimer(stack, 0) < 300) {
            int timer = ItemGunBaseNT.getAnimTimer(stack, 0);
            updateLoop(minecraft.player, ModGunSounds.TAU_LOOP.get(), 1F, 0.75F + timer * 0.01F);
        } else if (stack.getItem() instanceof ItemGunDrill) {
            double speed = HbmAnimations.getRelevantTransformation("SPEED")[0];
            if (speed > 0D) updateLoop(minecraft.player, ModSounds.ELECTRIC_HUM.get(),
                    (float) speed, (float) speed);
            else stopLoop(minecraft.player);
        } else {
            stopLoop(minecraft.player);
        }
    }

    private static void updateLoop(LivingEntity entity, SoundEvent event, float volume, float pitch) {
        GunLoopSound loop = LOOPS.get(entity);
        if (loop == null || loop.isStopped() || loop.event != event) {
            if (loop != null) loop.stopLoop();
            loop = new GunLoopSound(event, entity, volume, pitch);
            LOOPS.put(entity, loop);
            Minecraft.getInstance().getSoundManager().play(loop);
        } else {
            loop.update(volume, pitch);
        }
    }

    private static void stopLoop(LivingEntity entity) {
        GunLoopSound loop = LOOPS.remove(entity);
        if (loop != null) loop.stopLoop();
    }

    private static boolean isFlamer(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("gun_flamer") || path.equals("gun_flamer_topaz")
                || path.equals("gun_flamer_daybreaker");
    }

    private static boolean isTau(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("gun_tau");
    }

    private static LivingEntity entity(ClientLevel level, int id) {
        return level.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static final class GunLoopSound extends AbstractTickableSoundInstance {
        private final SoundEvent event;
        private final LivingEntity entity;

        private GunLoopSound(SoundEvent event, LivingEntity entity, float volume, float pitch) {
            super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.event = event;
            this.entity = entity;
            this.volume = volume;
            this.pitch = pitch;
            this.looping = true;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        private void update(float volume, float pitch) {
            this.volume = volume;
            this.pitch = pitch;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        private void stopLoop() {
            stop();
        }

        @Override
        public void tick() {
            if (entity.isRemoved()) stop();
            else {
                x = entity.getX();
                y = entity.getY();
                z = entity.getZ();
            }
        }
    }
}
