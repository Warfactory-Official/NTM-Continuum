package com.hbm.client.render.item;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderGrenade}: the universal grenade assembled from its stack, in every slot (the inventory
 * included), and through {@link #renderGrenade} the thrown {@code RenderGrenadeUniversal} too, which passed a
 * {@code null} render type. The port had drawn the item as a flat sprite and the projectile as that sprite.
 *
 * <p>Each shell is one {@code grenades.obj} part drawn several times: the shell's own skin, then with blending on
 * the filling's body colour, the filling's label colour and the fuze's band colour, each over its own mask texture
 * (Tech has no label pass; its lights take the label colour, unlit at lightmap 240/240 with culling off). The mask
 * passes lie exactly on the skin, so they are submitted at increasing orders: 1.7 drew them in that sequence with
 * {@code GL_LEQUAL}, and only submission order reproduces that on every node. The spoon, ring and cap are drawn
 * only when there is a render type, so the thrown grenade has none (the pin was pulled). First person plays the
 * equip animation's buses ({@code BODYMOVE}/{@code BODYTURN}/{@code RINGMOVE}/{@code RINGTURN}/{@code RENDERRING});
 * the ring only shows while {@code RENDERRING} is set, so once the pull finished 1.7 showed no ring either.
 */
public final class ItemRenderGrenade implements CustomItemRenderer {

    private static final Identifier FRAG = tex("textures/models/grenades/frag.png");
    private static final Identifier FRAG_BODY = tex("textures/models/grenades/frag_body.png");
    private static final Identifier FRAG_LABEL = tex("textures/models/grenades/frag_label.png");
    private static final Identifier FRAG_FUZE = tex("textures/models/grenades/frag_fuze.png");
    private static final Identifier STICK = tex("textures/models/grenades/stick.png");
    private static final Identifier STICK_BODY = tex("textures/models/grenades/stick_body.png");
    private static final Identifier STICK_LABEL = tex("textures/models/grenades/stick_label.png");
    private static final Identifier STICK_FUZE = tex("textures/models/grenades/stick_fuze.png");
    private static final Identifier TECH = tex("textures/models/grenades/tech.png");
    private static final Identifier TECH_BODY = tex("textures/models/grenades/tech_body.png");
    private static final Identifier TECH_LIGHTS = tex("textures/models/grenades/tech_lights.png");
    private static final Identifier TECH_FUZE = tex("textures/models/grenades/tech_fuze.png");
    private static final Identifier NUKA = tex("textures/models/grenades/nuka.png");
    private static final Identifier NUKA_BODY = tex("textures/models/grenades/nuka_body.png");
    private static final Identifier NUKA_LABEL = tex("textures/models/grenades/nuka_label.png");
    private static final Identifier NUKA_FUZE = tex("textures/models/grenades/nuka_fuze.png");

    /** 1.7 {@code ResourceManager.grenades}; loaded on the first draw, on the render thread. */
    private static @Nullable WavefrontObj grenades;

    private static WavefrontObj model() {
        if (grenades == null) grenades = obj("models/weapons/grenades.obj");
        return grenades;
    }

    /** 1.7 {@code handleRenderType}: EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY and INVENTORY, i.e. all four. */
    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    /** The first-person draw follows the equip animation; the other slots are static. */
    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        GrenadeData data = ItemGrenadeUniversal.getData(stack);
        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> renderFirstPerson(data, pose, sink, light);
            case INVENTORY -> {
                pose.translate(8D, 8D, 0D);
                pose.scale(-1F, -1F, -1F);
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.mulPose(Axis.YP.rotationDegrees(150F));
                pose.mulPose(Axis.XP.rotationDegrees(15F));
                renderGrenade(data, type, pose, sink, light);
            }
            case EQUIPPED -> {
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.translate(3D, 1D, -0.5D);
                renderGrenade(data, type, pose, sink, light);
            }
            case ENTITY -> {
                pose.scale(0.125F, 0.125F, 0.125F);
                renderGrenade(data, type, pose, sink, light);
            }
        }
        pose.popPose();
    }

    private static void renderFirstPerson(GrenadeData data, PoseStack pose, SubmitSink sink, int light) {
        pose.scale(0.125F, 0.125F, 0.125F);
        pose.translate(3D, 1D, -3D);
        pose.mulPose(Axis.YN.rotationDegrees(180F));

        double[] bodyMove = HbmAnimations.getRelevantTransformation("BODYMOVE");
        double[] bodyTurn = HbmAnimations.getRelevantTransformation("BODYTURN");
        double[] ringMove = HbmAnimations.getRelevantTransformation("RINGMOVE");
        double[] ringTurn = HbmAnimations.getRelevantTransformation("RINGTURN");
        double[] renderRing = HbmAnimations.getRelevantTransformation("RENDERRING");
        pose.translate(bodyMove[0], bodyMove[1], bodyMove[2]);
        WavefrontObj model = model();

        switch (data.shell()) {
            case FRAG -> {
                pose.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                renderBodyStandard(data, "Frag", FRAG, FRAG_BODY, FRAG_LABEL, FRAG_FUZE, pose, sink, light);
                part(pose, sink, model, culled(FRAG), "FragSpoon", light, NO_OVERLAY);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    part(pose, sink, model, culled(FRAG), "FragRing", light, NO_OVERLAY);
                }
            }
            case STICK -> {
                pose.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                renderBodyStandard(data, "Stick", STICK, STICK_BODY, STICK_LABEL, STICK_FUZE, pose, sink, light);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.YP.rotationDegrees((float) ringTurn[1]));
                    // blending on: the cap's skin, then its body colour over the same faces
                    part(pose, sink, model, culled(STICK), "StickCap", light, NO_OVERLAY);
                    tinted(pose, sink.order(1), STICK_BODY, "StickCap", light, data.filling().bodyColor());
                }
            }
            case TECH -> {
                pose.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                renderTechBody(data, pose, sink, light);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    part(pose, sink, model, culled(TECH), "TechRing", light, NO_OVERLAY);
                }
            }
            case NUKE -> {
                pose.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                renderBodyStandard(data, "Nuka", NUKA, NUKA_BODY, NUKA_LABEL, NUKA_FUZE, pose, sink, light);
                part(pose, sink, model, culled(NUKA), "NukaSpoon", light, NO_OVERLAY);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.translate(-1D, 5D, 0D);
                    pose.mulPose(Axis.ZN.rotationDegrees((float) ringTurn[2]));
                    pose.translate(1D, -5D, 0D);
                    part(pose, sink, model, culled(NUKA), "NukaRing", light, NO_OVERLAY);
                }
            }
        }
    }

    /**
     * 1.7 {@code renderGrenade(stack, renderType)}. {@code type} {@code null} is the thrown grenade
     * ({@code RenderGrenadeUniversal}), which skips the spoon, ring and cap.
     */
    public static void renderGrenade(GrenadeData data, @Nullable Type type, PoseStack pose, SubmitSink sink, int light) {
        WavefrontObj model = model();
        switch (data.shell()) {
            case FRAG -> {
                if (type == Type.INVENTORY) {
                    pose.scale(3F, 3F, 3F);
                    pose.translate(0D, -2D, 0D);
                }
                if (type == null) pose.translate(0D, -2D, 0D);
                renderBodyStandard(data, "Frag", FRAG, FRAG_BODY, FRAG_LABEL, FRAG_FUZE, pose, sink, light);
                if (type != null) {
                    part(pose, sink, model, culled(FRAG), "FragSpoon", light, NO_OVERLAY);
                    part(pose, sink, model, culled(FRAG), "FragRing", light, NO_OVERLAY);
                }
            }
            case STICK -> {
                if (type == Type.INVENTORY) {
                    pose.scale(2F, 2F, 2F);
                    pose.translate(0D, -4.5D, 0D);
                }
                if (type == Type.EQUIPPED) pose.translate(0D, -2D, 0D);
                if (type == null) pose.translate(0D, -2D, 0D);
                renderBodyStandard(data, "Stick", STICK, STICK_BODY, STICK_LABEL, STICK_FUZE, pose, sink, light);
                if (type != null) {
                    // blending is off again here: only the body-coloured mask, alpha-tested, and no skin under it
                    int color = 0xFF000000 | data.filling().bodyColor();
                    sink.submitCustomGeometry(pose, culled(STICK_BODY),
                            (p, buffer) -> model.renderPart("StickCap", p, buffer, light, NO_OVERLAY, color));
                }
            }
            case TECH -> {
                if (type == Type.INVENTORY) {
                    pose.scale(3.5F, 3.5F, 3.5F);
                    pose.translate(0D, -1.75D, 0D);
                }
                if (type == Type.EQUIPPED) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0.5D, -1D, 0.5D);
                }
                if (type == null) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0D, -1D, 0D);
                }
                renderTechBody(data, pose, sink, light);
                if (type != null) part(pose, sink, model, culled(TECH), "TechRing", light, NO_OVERLAY);
            }
            case NUKE -> {
                if (type == Type.INVENTORY) {
                    pose.scale(2.5F, 2.5F, 2.5F);
                    pose.translate(0D, -2.75D, 0D);
                }
                if (type == Type.EQUIPPED) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0.5D, -3D, 0.5D);
                }
                if (type == null) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0D, -3D, 0D);
                }
                renderBodyStandard(data, "Nuka", NUKA, NUKA_BODY, NUKA_LABEL, NUKA_FUZE, pose, sink, light);
                if (type != null) {
                    part(pose, sink, model, culled(NUKA), "NukaSpoon", light, NO_OVERLAY);
                    part(pose, sink, model, culled(NUKA), "NukaRing", light, NO_OVERLAY);
                }
            }
        }
    }

    /** 1.7 {@code renderBodyStandard}: skin, then blended body, label and fuze colours, in that order. */
    private static void renderBodyStandard(GrenadeData data, String part, Identifier base, Identifier body,
                                           Identifier label, Identifier fuze, PoseStack pose, SubmitSink sink,
                                           int light) {
        part(pose, sink, model(), culled(base), part, light, NO_OVERLAY);
        tinted(pose, sink.order(1), body, part, light, data.filling().bodyColor());
        tinted(pose, sink.order(2), label, part, light, data.filling().labelColor());
        tinted(pose, sink.order(3), fuze, part, light, data.fuze().bandColor());
    }

    /** 1.7 {@code renderTechBody}: skin, blended body and fuze colours, then the lights unlit in the label colour. */
    private static void renderTechBody(GrenadeData data, PoseStack pose, SubmitSink sink, int light) {
        WavefrontObj model = model();
        part(pose, sink, model, culled(TECH), "Tech", light, NO_OVERLAY);
        tinted(pose, sink.order(1), TECH_BODY, "Tech", light, data.filling().bodyColor());
        tinted(pose, sink.order(2), TECH_FUZE, "Tech", light, data.fuze().bandColor());
        // RenderArcFurnace.fullbright(true): lighting and culling off, lightmap 240/240, blending still on. Culled here:
        // the back faces lie behind the opaque skin, which writes depth first
        int color = 0xFF000000 | data.filling().labelColor();
        sink.order(3).submitCustomGeometry(pose, CompatRenderTypes.flatTranslucentCull(TECH_LIGHTS),
                (p, buffer) -> model.renderPart("Tech", p, buffer, FULLBRIGHT, NO_OVERLAY, color));
    }

    /** A {@code glColor3f(rgb)} pass with {@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA} blending, lit. */
    private static void tinted(PoseStack pose, SubmitSink sink, Identifier texture, String part, int light, int rgb) {
        WavefrontObj model = model();
        int color = 0xFF000000 | rgb;
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(texture),
                (p, buffer) -> model.renderPart(part, p, buffer, light, NO_OVERLAY, color));
    }

    private static final int NO_OVERLAY = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
}
