package com.hbm.client.render.item;

import com.hbm.client.gun.GluonGunClient;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.world.TimeOfDay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Set;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.12.2 {@code ItemRenderGunEgon} (a {@code TEISRBase}), in the {@code teisr_bare} frame. In the right hand the hose
 * kicks back and shakes while the beam is on ({@link GluonGunClient#firstPersonFade}) and the muzzle's smoke and glow
 * are drawn in its space; the left hand draws nothing.
 */
final class ItemRenderGunEgon implements CustomItemRenderer {

    static final Identifier HOSE = tex("textures/models/weapons/egon_hose.png");
    static final Identifier DISPLAY = tex("textures/models/weapons/egon_display.png");

    private final WavefrontObj hose = obj("models/weapons/egon_hose.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON;
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
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return;
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        pose.pushPose();
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                pose.scale(0.5F, 0.4F, 0.5F);
                pose.translate(-2D, -0.5D, 3D);
                pose.mulPose(Axis.XP.rotationDegrees(90F));
                pose.mulPose(Axis.ZP.rotationDegrees(-90F));
                float partial = Frames.partialTick();
                var level = Minecraft.getInstance().level;
                float time = level == null ? 0F : TimeOfDay.get(level) + partial;
                float fade = owner instanceof Player player ? GluonGunClient.firstPersonFade(player, partial) : 0F;
                float[] offset = GluonGunClient.getOffset(time);
                float[] jitter = GluonGunClient.getJitter(time);
                pose.translate(offset[0] * fade - jitter[1] * fade * 0.1F, offset[1] * fade * fade - jitter[0] * fade * 0.05F, 0F);
                pose.mulPose(Axis.XP.rotationDegrees(jitter[0] * fade));
                pose.mulPose(Axis.YP.rotationDegrees(jitter[1] * fade));
                float rec = -Mth.sin(Math.min(fade * 1.5F, 1F));
                pose.translate(0F, 0F, rec * 1.5F);
                pose.mulPose(Axis.XP.rotationDegrees(7 * rec));
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD, FIXED, GROUND -> {
                pose.translate(0.5D, 0.55D, 0.7D);
                pose.mulPose(Axis.YP.rotationDegrees(180F));
                pose.scale(0.125F, 0.125F, 0.125F);
            }
            case GUI -> {
                pose.translate(0.4D, 0D, 0.5D);
                pose.scale(0.15F, 0.15F, 0.15F);
                pose.mulPose(Axis.YP.rotationDegrees(45F));
                pose.mulPose(Axis.XP.rotationDegrees(45F));
            }
            default -> { }
        }
        sink.submitCustomGeometry(pose, culled(HOSE),
                (p, buffer) -> hose.renderExcept(p, buffer, light, overlay, Set.of("Screen")));
        part(pose, sink, hose, culled(DISPLAY), "Screen", light, overlay);
        if (firstPerson) muzzle(pose, sink);
        pose.popPose();
    }

    /** The smoke, then the glow, of {@link GluonGunClient#MUZZLE}: additive, unculled, depth tested without writing. */
    private static void muzzle(PoseStack pose, SubmitSink sink) {
        if (GluonGunClient.MUZZLE.isEmpty()) return;
        float partial = Frames.partialTick();
        for (var texture : new Identifier[] {GluonGunClient.MUZZLE_SMOKE, GluonGunClient.MUZZLE_GLOW}) {
            sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(texture), (p, buffer) -> {
                for (GluonGunClient.MuzzleSmoke smoke : GluonGunClient.MUZZLE) {
                    if (smoke.texture != texture) continue;
                    smoke.emit(partial, (x, y, z, u, v, color) -> Vertexes.vertex(buffer, p, x, y, z, color, u, v,
                            OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F));
                }
            });
        }
    }
}
