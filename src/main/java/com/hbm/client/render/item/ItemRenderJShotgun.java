package com.hbm.client.render.item;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.client.model.ColladaModel;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.sound.ModGunSounds;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.Frames;
import dev.golem.core.client.PlayerSkins;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.EntityShaderRenderType;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.12.2 {@code ItemRenderJShotgun} (a {@code TEISRBase}), in the {@code teisr_bare} frame: the Collada jade shotgun
 * ({@code jshotgun_anim1.dae}). In first person it kicks back on the {@code JS_RECOIL} bus, plays a Collada reload
 * with the player's arms, and for the first 37.5 ms of a shot shows the barrel's muzzle flash and draws the whole
 * model through {@code flash_lmap}, which adds the gun's glow map. Elsewhere it is drawn at rest without arms, with
 * the flash while the shot's tick lasts.
 */
public final class ItemRenderJShotgun implements CustomItemRenderer {

    static final Identifier TEXTURE = tex("textures/models/jade_shotgun.png");
    static final Identifier GLOW_MAP = tex("textures/models/jade_shotgun_lmap.png");
    static final Identifier MUZZLE_FLASH = tex("textures/misc/mflash_4.png");
    static final String FLASH_SHADER = "hbm:flash_lmap";

    /** 1.12's {@code timeMax}: how long, in seconds after a shot, the flash shows. */
    private static final float FLASH_TIME = 0.0375F;

    /** Harness only: the muzzle flash held on (the {@code hbm_reloaded_guns} query's {@code pin_flash}). */
    public static boolean pinnedFlash;

    private static @Nullable ColladaModel model;
    private static ColladaModel.@Nullable Animation reloadOne;
    private static ColladaModel.@Nullable Animation reloadTwo;
    private static boolean reloadRegistered;

    /** The reload playing and the keyframe it last drew, 1.12's {@code AnimationWrapper.prevFrame}. */
    private long reloadStart = Long.MIN_VALUE;
    private int prevFrame;

    private static ColladaModel model() {
        if (!reloadRegistered) {
            reloadRegistered = true;
            ClientResourceReloads.register("hbm", "jshotgun", () -> {
                model = null;
                reloadOne = null;
                reloadTwo = null;
            });
        }
        if (model == null) {
            model = ColladaModel.load(tex("models/anim/jshotgun_anim1.dae"), true);
            reloadOne = ColladaModel.loadAnimation(1500, tex("models/anim/jshotgun_anim0.dae"));
            reloadTwo = ColladaModel.loadAnimation(3000, tex("models/anim/jshotgun_anim1.dae"));
        }
        return model;
    }

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON || type == Type.EQUIPPED;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        render(stack, switch (type) {
            case INVENTORY -> ItemDisplayContext.GUI;
            case EQUIPPED -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case EQUIPPED_FIRST_PERSON -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case ENTITY -> ItemDisplayContext.GROUND;
        }, pose, sink, light, overlay, owner);
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitSink sink, int light,
                       int overlay, @Nullable LivingEntity owner) {
        ColladaModel model = model();
        int mag = JadeShotgunClient.loaded(stack);
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                firstPerson(model, context, mag, pose, sink, light, overlay, owner);
                pose.popPose();
                return;
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD, FIXED, GROUND -> {
                pose.scale(0.75F, 0.75F, 0.75F);
                pose.translate(-0.5D, -1.5D, 0.8D);
                pose.mulPose(Axis.YP.rotationDegrees(180F));
            }
            case GUI -> {
                pose.translate(-0.55D, 0D, 0D);
                pose.scale(0.4F, 0.4F, 0.4F);
                pose.mulPose(Axis.YP.rotationDegrees(180F));
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.mulPose(Axis.XP.rotationDegrees(-30F));
            }
            default -> { }
        }
        // the shot's tick: 1.12 read getDelay(stack) == rateOfFire, the cooldown set when the gun fired
        boolean flash = pinnedFlash || ItemGunBaseNT.getState(stack, 0) == GunState.COOLDOWN
                && ItemGunBaseNT.getTimer(stack, 0) == 8 && Frames.partialTick() < 0.75F;
        RenderType gun = CompatRenderTypes.entityCutoutCull(TEXTURE);
        model.render(pose, sink, -1, light, overlay, (name, p, prev, frame) -> {
            if (name.startsWith("rightArm") || name.startsWith("leftArm") || name.startsWith("Boolet")) return null;
            if (name.startsWith("Flash")) {
                if (flash && ((mag == 1 && name.equals("Flash1")) || (mag == 0 && name.equals("Flash2")))) {
                    muzzleFlash(model, name, p, sink, overlay);
                }
                return null;
            }
            return gun;
        });
        pose.popPose();
    }

    private void firstPerson(ColladaModel model, ItemDisplayContext context, int mag, PoseStack pose, SubmitSink sink,
                             int light, int overlay, @Nullable LivingEntity owner) {
        InteractionHand hand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        HbmAnimations.Animation playing = HbmAnimations.getRelevantAnim(hand, 0);
        long now = GameTime.now();
        boolean recoilPlaying = playing != null && playing.animation.getBus(JadeShotgunClient.RECOIL) != null;
        // 1.12's getTimeDifference: seconds since the slot's bus animation began, 0 while a Collada reload holds it
        float time = recoilPlaying ? (now - playing.startMillis) / 1000F : 0F;
        boolean flash = pinnedFlash || time > 0 && time < FLASH_TIME;
        double recoil = HbmAnimations.getRelevantTransformation(JadeShotgunClient.RECOIL, hand, 0)[0];
        pose.scale(2.5F, 2.5F, 2.5F);
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.ZP.rotationDegrees((float) (270 + recoil * 10)));
            pose.mulPose(Axis.YP.rotationDegrees(95F));
            pose.translate(0.2D, -1.9D, -1D);
        }
        pose.translate(0D, 0D, -recoil * 1.25F);

        ColladaModel.Animation reload = null;
        if (playing != null && playing.animation.getBus(JadeShotgunClient.RELOAD_ONE) != null) reload = reloadOne;
        else if (playing != null && playing.animation.getBus(JadeShotgunClient.RELOAD_TWO) != null) reload = reloadTwo;
        long elapsed = reload == null ? 0L : now - playing.startMillis;
        boolean done = reload == null || elapsed > reload.length();
        if (reload != null && playing.startMillis != reloadStart) {
            reloadStart = playing.startMillis;
            prevFrame = 0;
        }

        Identifier skin = owner instanceof AbstractClientPlayer player ? PlayerSkins.texture(player) : TEXTURE;
        RenderType gun = flash ? EntityShaderRenderType.cutoutCull(FLASH_SHADER, TEXTURE, GLOW_MAP)
                : CompatRenderTypes.entityCutoutCull(TEXTURE);
        RenderType arms = flash ? EntityShaderRenderType.cutoutCull(FLASH_SHADER, skin, GLOW_MAP)
                : CompatRenderTypes.entityCutoutCull(skin);
        ColladaModel.NodeDraw draw = (name, p, prev, frame) -> {
            // 1.12 ran these checks for every node, so a reload sound played once per node of the model
            if (prev == 9 && frame == 10) playReloadSound(ModGunSounds.JSG_RELOAD_0.get());
            if (prev == 45 && frame == 46) playReloadSound(ModGunSounds.JSG_RELOAD_1.get());
            if (name.startsWith("Flash")) {
                if (flash) {
                    boolean left = mag == 1 || mag == 2;
                    if ((left && name.equals("Flash1")) || (!left && name.equals("Flash2"))) {
                        muzzleFlash(model, name, p, sink, overlay);
                    }
                }
                return null;
            }
            boolean arm = name.startsWith("rightArm") || name.startsWith("leftArm");
            if (done && (arm || name.startsWith("Boolet"))) return null;
            return arm ? arms : gun;
        };
        if (reload == null) model.render(pose, sink, -1, light, overlay, draw);
        else prevFrame = model.render(reload, elapsed, prevFrame, pose, sink, -1, light, overlay, draw);
    }

    /** The flash plane in {@code mflash_4}: additive, full-bright, unlit, unculled, not writing depth. */
    private static void muzzleFlash(ColladaModel model, String name, PoseStack pose, SubmitSink sink, int overlay) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(MUZZLE_FLASH),
                (p, buffer) -> model.draw(name, p, buffer, -1, FULLBRIGHT, overlay));
    }

    /** {@code PositionedSoundRecord.getRecord(sound, 1, 0.15)}: not positioned, master volume, pitch 1, volume 0.15. */
    private static void playReloadSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1F, 0.15F));
    }
}
