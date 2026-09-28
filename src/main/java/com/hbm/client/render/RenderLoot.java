package com.hbm.client.render;

import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.generic.BlockLoot.TileEntityLoot;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModGunItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code RenderLoot}: every stack of the pile at its own offset. Mini nukes are drawn as the mini-nuke
 * projectile model at half scale, the lever-action shotgun as its gun model laid on its side, the NCR power armor
 * as the suit's own pieces standing at 1/16 scale; everything else lies flat as its icon at half scale in the
 * quarter-block corner - a Sedna gun as the gun_darter icon every one of them carried. The trenchmaster suit stands the same way, its helmet light drawn full-bright.
 */
public class RenderLoot implements CompatBER<TileEntityLoot, RenderLoot.State> {

    enum Kind { NUKE, SHOTGUN, TRENCHMASTER, NCR, GUN, STANDARD }

    record Entry(Kind kind, Item item, CompatItemRenderState stack, double x, double y, double z) {}

    public static final class State extends CompatRenderState {
        final List<Entry> entries = new ArrayList<>();
    }

    private static final Identifier NCR_HELMET = Ids.of("hbm", "textures/armor/ncrpa_helmet.png");
    private static final Identifier NCR_CHEST = Ids.of("hbm", "textures/armor/ncrpa_chest.png");
    private static final Identifier NCR_ARM = Ids.of("hbm", "textures/armor/ncrpa_arm.png");
    private static final Identifier NCR_LEG = Ids.of("hbm", "textures/armor/ncrpa_leg.png");
    private static final Identifier MINI_NUKE = Ids.of("hbm", "textures/models/projectiles/mini_nuke.png");
    private static final Identifier MARESLEG = Ids.of("hbm", "textures/models/weapons/maresleg.png");
    // 1.7 ItemGunBaseNT's constructor: every Sedna gun's IIcon is gun_darter; only gun_pa_ranged renames it
    private static final Identifier GUN_ICON = Ids.of("hbm", "textures/items/gun_darter.png");
    private static final Identifier PA_RANGED_ICON = Ids.of("hbm", "textures/items/gun_pa_ranged.png");
    private static final Geometry ICON_2D = itemIn2D("hbm:loot_icon_2d", 16, 16, 0.0625F);

    private WavefrontObj ncr;
    private WavefrontObj trenchmaster;
    private static final Identifier TRENCH_HELMET = Ids.of("hbm", "textures/armor/trenchmaster_helmet.png");
    private static final Identifier TRENCH_CHEST = Ids.of("hbm", "textures/armor/trenchmaster_chest.png");
    private static final Identifier TRENCH_ARM = Ids.of("hbm", "textures/armor/trenchmaster_arm.png");
    private static final Identifier TRENCH_LEG = Ids.of("hbm", "textures/armor/trenchmaster_leg.png");
    private WavefrontObj projectiles;
    private WavefrontObj maresleg;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // 1.7 widened the render box to x-1..x+2, y..y+3 for the standing armor
        return true;
    }

    private static Kind kind(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.startsWith("ammo_standard_nuke_")) return Kind.NUKE;
        if (stack.is(ModGunItems.gun_maresleg.get())) return Kind.SHOTGUN;
        if (stack.getItem() instanceof com.hbm.items.armor.ArmorTrenchmaster) return Kind.TRENCHMASTER;
        if (stack.getItem() instanceof com.hbm.items.weapon.sedna.ItemGunBaseNT) return Kind.GUN;
        if (stack.is(ModArmorItems.ncrpa_helmet.get()) || stack.is(ModArmorItems.ncrpa_plate.get())
                || stack.is(ModArmorItems.ncrpa_legs.get()) || stack.is(ModArmorItems.ncrpa_boots.get())) return Kind.NCR;
        return Kind.STANDARD;
    }

    @Override
    public void extract(TileEntityLoot loot, State state, float partialTicks, Vec3 cameraPosition) {
        state.entries.clear();
        for (BlockLoot.LootItem item : loot.items) {
            Kind kind = kind(item.stack());
            CompatItemRenderState render = new CompatItemRenderState();
            if (kind == Kind.STANDARD) render.extract(loot.getLevel(), item.stack(), ItemDisplayContext.NONE);
            state.entries.add(new Entry(kind, item.stack().getItem(), render, item.x(), item.y(), item.z()));
        }
    }

    private static void draw(SubmitSink sink, PoseStack pose, Identifier texture, Geometry geometry, int light) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(texture),
                (p, buffer) -> geometry.draw(p, buffer, light, OverlayTexture.NO_OVERLAY, -1));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        for (Entry entry : state.entries) {
            pose.pushPose();
            pose.translate(entry.x(), entry.y(), entry.z());
            switch (entry.kind()) {
                case NUKE -> {
                    if (projectiles == null) projectiles = WavefrontObj.load(Ids.of("hbm", "models/projectiles/projectiles.obj"));
                    pose.scale(0.5F, 0.5F, 0.5F);
                    pose.translate(1, 0.5, 1);
                    draw(sink, pose, MINI_NUKE, projectiles.part("MiniNuke"), light);
                }
                case SHOTGUN -> {
                    if (maresleg == null) maresleg = WavefrontObj.load(Ids.of("hbm", "models/weapons/maresleg.obj"));
                    pose.scale(0.125F, 0.125F, 0.125F);
                    pose.translate(3, 0, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(25));
                    pose.mulPose(Axis.XP.rotationDegrees(90));
                    pose.mulPose(Axis.YP.rotationDegrees(90));
                    draw(sink, pose, MARESLEG, maresleg.all(), light);
                }
                case TRENCHMASTER -> {
                    if (trenchmaster == null) trenchmaster = WavefrontObj.load(Ids.of("hbm", "models/armor/trenchmaster.obj"));
                    renderTrenchmaster(sink, pose, entry.item(), light);
                }
                case NCR -> {
                    if (ncr == null) ncr = WavefrontObj.load(Ids.of("hbm", "models/armor/ncrpa.obj"));
                    renderNCR(sink, pose, entry.item(), light);
                }
                case GUN -> {
                    // 1.7 renderStandardItem: a gun has no IIcon of its own - its IItemRenderer never sees this
                    // path - so it lies here as the shared gun_darter sprite, the same half-block flat icon
                    pose.translate(0.25, 0, 0.25);
                    pose.scale(0.5F, 0.5F, 0.5F);
                    pose.mulPose(Axis.XP.rotationDegrees(90));
                    boolean paRanged = BuiltInRegistries.ITEM.getKey(entry.item()).getPath().equals("gun_pa_ranged");
                    draw(sink, pose, paRanged ? PA_RANGED_ICON : GUN_ICON, ICON_2D, light);
                }
                case STANDARD -> {
                    // 1.7 renderItemIn2D: the icon lying in the quarter-block corner, 1/16 thick
                    pose.translate(0.25, 0, 0.25);
                    pose.scale(0.5F, 0.5F, 0.5F);
                    pose.mulPose(Axis.XP.rotationDegrees(90));
                    pose.translate(0.5, 0.5, -0.03125);
                    sink.submitItem(pose, entry.stack(), light, OverlayTexture.NO_OVERLAY, 0);
                }
            }
            pose.popPose();
        }
    }

    /**
     * 1.7 {@code ItemRenderer.renderItemIn2D} over a whole {@code width x height} texture: the front at z = 0, the
     * back {@code thickness} behind it, and one edge strip per texel column and row sampling that texel's centre.
     */
    static Geometry itemIn2D(String name, int width, int height, float thickness) {
        float minU = 0F, maxU = 1F, minV = 0F, maxV = 1F;
        dev.golem.core.render.gem.Quads.Builder q = dev.golem.core.render.gem.Quads.named(name);
        q.normal(0, 0, 1)
                .vertex(0, 0, 0, maxU, maxV).vertex(1, 0, 0, minU, maxV)
                .vertex(1, 1, 0, minU, minV).vertex(0, 1, 0, maxU, minV);
        q.normal(0, 0, -1)
                .vertex(0, 1, -thickness, maxU, minV).vertex(1, 1, -thickness, minU, minV)
                .vertex(1, 0, -thickness, minU, maxV).vertex(0, 0, -thickness, maxU, maxV);
        float halfU = 0.5F * (maxU - minU) / width;
        float halfV = 0.5F * (maxV - minV) / height;
        q.normal(-1, 0, 0);
        for (int k = 0; k < width; k++) {
            float f = (float) k / width;
            float u = maxU + (minU - maxU) * f - halfU;
            q.vertex(f, 0, -thickness, u, maxV).vertex(f, 0, 0, u, maxV)
                    .vertex(f, 1, 0, u, minV).vertex(f, 1, -thickness, u, minV);
        }
        q.normal(1, 0, 0);
        for (int k = 0; k < width; k++) {
            float f = (float) k / width;
            float u = maxU + (minU - maxU) * f - halfU;
            float x = f + 1F / width;
            q.vertex(x, 1, -thickness, u, minV).vertex(x, 1, 0, u, minV)
                    .vertex(x, 0, 0, u, maxV).vertex(x, 0, -thickness, u, maxV);
        }
        q.normal(0, 1, 0);
        for (int k = 0; k < height; k++) {
            float f = (float) k / height;
            float v = maxV + (minV - maxV) * f - halfV;
            float y = f + 1F / height;
            q.vertex(0, y, 0, maxU, v).vertex(1, y, 0, minU, v)
                    .vertex(1, y, -thickness, minU, v).vertex(0, y, -thickness, maxU, v);
        }
        q.normal(0, -1, 0);
        for (int k = 0; k < height; k++) {
            float f = (float) k / height;
            float v = maxV + (minV - maxV) * f - halfV;
            q.vertex(1, f, 0, minU, v).vertex(0, f, 0, maxU, v)
                    .vertex(0, f, -thickness, maxU, v).vertex(1, f, -thickness, minU, v);
        }
        return q.build();
    }

    /** 1.7 {@code renderTrenchmaster}: the same standing 1/16-scale suit as the NCR one. */
    private void renderTrenchmaster(SubmitSink sink, PoseStack pose, Item item, int light) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (item == com.hbm.items.ModArmorSetItems.trenchmaster_helmet.get()) {
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(TRENCH_HELMET),
                    (p, buffer) -> trenchmaster.part("Helmet").draw(p, buffer, light, OverlayTexture.NO_OVERLAY, -1));
            // the lamp: lightmap 240/240, lighting off
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(TRENCH_HELMET),
                    (p, buffer) -> trenchmaster.part("Light").draw(p, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1));
        }
        if (item == com.hbm.items.ModArmorSetItems.trenchmaster_plate.get()) {
            draw(sink, pose, TRENCH_CHEST, trenchmaster.part("Chest"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-3));
            draw(sink, pose, TRENCH_ARM, trenchmaster.part("LeftArm"), light);
            draw(sink, pose, TRENCH_ARM, trenchmaster.part("RightArm"), light);
            pose.popPose();
        }
        if (item == com.hbm.items.ModArmorSetItems.trenchmaster_legs.get()) {
            draw(sink, pose, TRENCH_LEG, trenchmaster.part("LeftLeg"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            draw(sink, pose, TRENCH_LEG, trenchmaster.part("RightLeg"), light);
            pose.popPose();
        }
        if (item == com.hbm.items.ModArmorSetItems.trenchmaster_boots.get()) {
            draw(sink, pose, TRENCH_LEG, trenchmaster.part("LeftBoot"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            draw(sink, pose, TRENCH_LEG, trenchmaster.part("RightBoot"), light);
            pose.popPose();
        }
        pose.popPose();
    }

    private void renderNCR(SubmitSink sink, PoseStack pose, Item item, int light) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (item == ModArmorItems.ncrpa_helmet.get()) {
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(NCR_HELMET),
                    (p, buffer) -> ncr.part("Helmet").draw(p, buffer, light, OverlayTexture.NO_OVERLAY, -1));
            // the visor glows: fullbright, unlit
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(NCR_HELMET),
                    (p, buffer) -> ncr.part("Eyes").draw(p, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1));
        }
        if (item == ModArmorItems.ncrpa_plate.get()) {
            draw(sink, pose, NCR_CHEST, ncr.part("Chest"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-3));
            draw(sink, pose, NCR_ARM, ncr.part("LeftArm"), light);
            draw(sink, pose, NCR_ARM, ncr.part("RightArm"), light);
            pose.popPose();
        }
        if (item == ModArmorItems.ncrpa_legs.get()) {
            draw(sink, pose, NCR_LEG, ncr.part("LeftLeg"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            draw(sink, pose, NCR_LEG, ncr.part("RightLeg"), light);
            pose.popPose();
        }
        if (item == ModArmorItems.ncrpa_boots.get()) {
            draw(sink, pose, NCR_LEG, ncr.part("LeftBoot"), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            draw(sink, pose, NCR_LEG, ncr.part("RightBoot"), light);
            pose.popPose();
        }
        pose.popPose();
    }
}
