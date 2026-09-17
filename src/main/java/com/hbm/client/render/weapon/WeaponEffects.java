package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** The source Sedna muzzle and attached-node geometry, kept outside the GEM body declaration. */
public final class WeaponEffects {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final String FLASH = "hbm:textures/models/weapons/lilmac_plume.png";
    private static final String LASER = "hbm:textures/models/weapons/laser_flash.png";
    private static @org.jspecify.annotations.Nullable String renderedItem;

    private WeaponEffects() {}

    static void render(WeaponPose state, PoseStack base, SubmitSink sink,
            int light, int overlay) {
        if (!(state.stack.getItem() instanceof ItemGunBaseNT)) return;
        String id = itemPath(state.stack);
        renderedItem = id;
        try {
            if (!state.animated) {
                renderStatic(state, base, sink, light, overlay);
                return;
            }

            switch (id) {
            case "gun_light_revolver", "gun_light_revolver_atlas" -> atlas(state, base, sink, light, overlay);
            case "gun_am180" -> am180(state, base, sink, light, overlay);
            case "gun_star_f" -> star(state, base, sink, light, overlay);
            case "gun_star_f_akimbo" -> starAkimbo(state, base, sink, light, overlay);
            case "gun_maresleg", "gun_maresleg_broken" -> maresleg(state, base, sink, light, overlay);
            case "gun_maresleg_akimbo" -> mareslegAkimbo(state, base, sink, light, overlay);
            case "gun_greasegun" -> greasegun(state, base, sink, light, overlay);
            case "gun_lag" -> lag(state, base, sink, light, overlay);
            case "gun_uzi" -> uzi(state, base, sink, light, overlay, 0, 0, 1);
            case "gun_uzi_akimbo" -> uziAkimbo(state, base, sink, light, overlay);
            case "gun_henry", "gun_henry_lincoln" -> henry(state, base, sink, light, overlay);
            case "gun_heavy_revolver", "gun_heavy_revolver_lilmac", "gun_heavy_revolver_protege" ->
                    heavyRevolver(state, base, sink, light, overlay);
            case "gun_hangman" -> hangman(state, base, sink, light, overlay);
            case "gun_light_revolver_dani" -> dani(state, base, sink, light, overlay);
            case "gun_pepperbox" -> pepperbox(state, base, sink, light, overlay);
            case "gun_g3", "gun_g3_zebra" -> g3(state, base, sink, light, overlay);
            case "gun_carbine" -> carbine(state, base, sink, light, overlay);
            case "gun_liberator" -> liberator(state, base, sink, light, overlay);
            case "gun_congolake" -> congoLake(state, base, sink, light, overlay);
            case "gun_spas12" -> spas12(state, base, sink, light, overlay);
            case "gun_stg77" -> stg77(state, base, sink, light, overlay);
            case "gun_minigun", "gun_minigun_lacunae" -> minigun(state, base, sink, light, overlay);
            case "gun_minigun_dual" -> minigunDual(state, base, sink, light, overlay);
            case "gun_mas36" -> mas36(state, base, sink, light, overlay);
            case "gun_m2" -> m2(state, base, sink, light, overlay);
            case "gun_autoshotgun", "gun_autoshotgun_shredder" -> shredder(state, base, sink, light, overlay);
            case "gun_autoshotgun_sexy", "gun_autoshotgun_heretic" -> sexy(state, base, sink, light, overlay);
            case "gun_amat", "gun_amat_subtlety", "gun_amat_penance" -> amat(state, base, sink, light, overlay);
            case "gun_aberrator" -> aberrator(state, base, sink, light, overlay);
            case "gun_aberrator_eott" -> eott(state, base, sink, light, overlay);
            case "gun_lasrifle" -> lasrifle(state, base, sink, light, overlay);
            case "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_laser_pistol_morning_glory" ->
                    laserPistol(state, base, sink, light, overlay);
            case "gun_n_i_4_n_i" -> ni4ni(state, base, sink, light, overlay);
            case "gun_mk108" -> mk108(state, base, sink, light, overlay);
            case "gun_missile_launcher" -> missileLauncher(state, base, sink, light, overlay);
            case "gun_panzerschreck" -> panzerschreck(state, base, sink, light, overlay);
            case "gun_quadro" -> quadro(state, base, sink, light, overlay);
            case "gun_stinger" -> stinger(state, base, sink, light, overlay);
            case "gun_flaregun" -> flaregun(state, base, sink, light, overlay);
            case "gun_double_barrel", "gun_double_barrel_sacred_dragon" ->
                    doubleBarrel(state, base, sink, light, overlay);
            // These source renderFirstPerson methods submit model/UI geometry only. They deliberately
            // have no transient smoke or flash call; keep them explicit rather than hiding them in default.
            case "gun_charge_thrower", "gun_chemthrower", "gun_coilgun", "gun_drill",
                    "gun_fatman", "gun_folly", "gun_tau", "gun_tesla_cannon",
                    "gun_bolter", "gun_flamer", "gun_flamer_topaz", "gun_flamer_daybreaker",
                    "gun_pa_melee", "gun_fireext", "gun_pa_ranged" -> { }
            case "gun_debug" -> debug(state, base, sink, light, overlay);
                default -> { }
            }
        } finally {
            renderedItem = null;
        }
    }

    private static void renderStatic(WeaponPose state, PoseStack pose, SubmitSink sink, int light, int overlay) {
        if (state.context != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && state.context != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return;
        String id = itemPath(state.stack);
        switch (id) {
            case "gun_light_revolver", "gun_light_revolver_atlas" -> staticNormal(state, pose, sink, 0, 1.5, 9.25, 1, 75, 7.5);
            case "gun_am180" -> {
                boolean silenced = SpecialWeaponRenderers.hasUpgrade(state.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER);
                staticNormal(state, pose, sink, 0, 1.875, silenced ? 16.75 : 12, silenced ? .5F : .75F,
                        silenced ? 75 : 50, silenced ? 5 : 7.5);
            }
            case "gun_star_f", "gun_star_f_akimbo" -> staticNormal(state, pose, sink, 0, 3, 6.25, .75F, 75, 7.5);
            case "gun_greasegun" -> staticNormal(state, pose, sink, 0, 0, 8, .5F, 75, 7.5);
            case "gun_lag" -> { pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-10.25, 1, 0); staticNormal(state, pose, sink, 0, 0, 0, .5F, 75, 7.5); }
            case "gun_uzi", "gun_uzi_akimbo" -> {
                if (!SpecialWeaponRenderers.hasUpgrade(state.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER))
                    staticNormal(state, pose, sink, 0, .75, 8.5, 1, 75, 7.5);
            }
            case "gun_henry", "gun_henry_lincoln" -> staticNormal(state, pose, sink, 0, 1, 8, 1, 75, 5);
            case "gun_maresleg", "gun_maresleg_broken" -> staticNormal(state, pose, sink, 0, 1,
                    id.endsWith("broken") ? 3.75 : 8, 1, 75, 5);
            case "gun_heavy_revolver", "gun_heavy_revolver_lilmac", "gun_heavy_revolver_protege" -> {
                pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(.125, 2.5, 0); gap(sink, pose, shot(state, 0));
            }
            case "gun_hangman" -> staticNormal(state, pose, sink, 0, 0, 29, 2, 75, 7.5);
            case "gun_pepperbox" -> { staticNormal(state, pose, sink, 0, .5, 7, .5F, 75, 15); pose.mulPose(Axis.XP.rotationDegrees(45)); staticNormal(state, pose, sink, 0, 0, 0, 1, 75, 15); }
            case "gun_g3", "gun_g3_zebra" -> {
                if (!id.equals("gun_g3_zebra") && !SpecialWeaponRenderers.hasUpgrade(state.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) {
                    pose.translate(0, 0, 12); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.mulPose(Axis.XP.rotationDegrees(-25)); pose.scale(.75F, .75F, .75F); muzzle(sink, pose, shot(state, 0), 75, 10, 1);
                }
            }
            case "gun_carbine" -> staticNormal(state, pose, sink, 0, 1, 8, .5F, 75, 7.5);
            case "gun_liberator" -> staticNormal(state, pose, sink, 0, .5, 8, 1.5F, 75, 5);
            case "gun_congolake" -> staticNormal(state, pose, sink, 0, 1.75, 4.25, .5F, 150, 7.5);
            case "gun_spas12" -> { pose.mulPose(Axis.YP.rotationDegrees(180)); pose.translate(0, 1.5, -11); pose.mulPose(Axis.YP.rotationDegrees(-90)); muzzle(sink, pose, shot(state, 0), 75, 7.5, 1); }
            case "gun_stg77" -> { pose.translate(0, 0, 7.5); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.scale(.25F, .25F, .25F); pose.mulPose(Axis.XP.rotationDegrees(-5)); gap(sink, pose, shot(state, 0)); }
            case "gun_minigun", "gun_minigun_dual" -> { pose.translate(0, 0, 12.25); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(0, .5, 0); pose.scale(1.5F, 1.5F, 1.5F); muzzle(sink, pose, shot(state, 0), 50, 7.5, 1); }
            case "gun_minigun_lacunae" -> { pose.translate(0, 0, 12.25); pose.mulPose(Axis.YP.rotationDegrees(90)); laser(sink, pose, shot(state, 0), 50, 1, 0xFF00FF); pose.translate(0, 0, -.25); laser(sink, pose, shot(state, 0), 50, .5, 0xFF0080); }
            case "gun_mas36" -> staticNormal(state, pose, sink, 0, 1, 8, .5F, 75, 10);
            case "gun_m2" -> staticNormal(state, pose, sink, 0, 1.625, 5, .5F, 75, 7.5);
            case "gun_autoshotgun", "gun_autoshotgun_shredder" -> staticNormal(state, pose, sink, 0, 1, 7.5, .75F, 75, 7.5);
            case "gun_autoshotgun_sexy", "gun_autoshotgun_heretic" -> staticNormal(state, pose, sink, 0, 0, 8, 1, 150, 7.5);
            case "gun_amat", "gun_amat_subtlety", "gun_amat_penance" -> {
                if (!id.equals("gun_amat_penance") && !SpecialWeaponRenderers.hasUpgrade(state.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) {
                    pose.translate(0, .5, 11); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.scale(.75F, .75F, .75F); gap(sink, pose, shot(state, 0));
                }
            }
            case "gun_aberrator", "gun_aberrator_eott" -> {
                pose.pushPose();
                staticNormal(state, pose, sink, 0, 2, 4, .75F, 75, 7.5);
                pose.popPose();
                pose.pushPose();
                pose.translate(0, 2, -1.5);
                pose.scale(.5F, .5F, .5F);
                fireball(sink, pose, shot(state, 0));
                pose.popPose();
            }
            case "gun_lasrifle" -> { pose.translate(0, 1.5, 12); pose.mulPose(Axis.YP.rotationDegrees(90)); laser(sink, pose, shot(state, 0), 150, 1.5, 0xFF0000); pose.translate(0, 0, -.25); laser(sink, pose, shot(state, 0), 150, .75, 0xFF8000); }
            case "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_laser_pistol_morning_glory" -> { boolean green = id.endsWith("morning_glory"); pose.translate(0, 2, 4.75); pose.mulPose(Axis.YP.rotationDegrees(90)); laser(sink, pose, shot(state, 0), 150, 1.5, green ? 0x008000 : 0xFF0000); pose.translate(0, 0, -.25); laser(sink, pose, shot(state, 0), 150, .75, green ? 0x80FF00 : 0xFF8000); }
            case "gun_n_i_4_n_i" -> { pose.translate(0, .75, 4); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.mulPose(Axis.XP.rotationDegrees(90F * state.shotRand)); pose.scale(.125F, .125F, .125F); laser(sink, pose, shot(state, 0), 75, 7.5, 0xFFFFFF); }
            case "gun_mk108" -> staticNormal(state, pose, sink, 0, 0, 8.125, 1, 50, 5);
            case "gun_missile_launcher" -> staticNormal(state, pose, sink, 0, 1, 6.75, .75F, 75, 7.5);
            case "gun_panzerschreck" -> staticNormal(state, pose, sink, 0, 0, 6.5, .75F, 150, 7.5);
            case "gun_quadro" -> staticNormal(state, pose, sink, 0, .75, 2, .75F, 150, 7.5);
            case "gun_stinger" -> staticNormal(state, pose, sink, 0, 3.5, -10.3795, .75F, 150, 10);
            case "gun_double_barrel", "gun_double_barrel_sacred_dragon" -> {
                pose.translate(0, 0, 8); pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale(2F, 2F, 2F); muzzle(sink, pose, shot(state, 0), 75, 5, 1);
            }
            // Proven no-effect source renderers: their active method has no transient effect call.
            case "gun_charge_thrower", "gun_chemthrower", "gun_coilgun", "gun_drill",
                    "gun_fatman", "gun_folly", "gun_tau", "gun_tesla_cannon",
                    "gun_bolter", "gun_flamer", "gun_flamer_topaz", "gun_flamer_daybreaker",
                    "gun_pa_melee", "gun_fireext", "gun_pa_ranged" -> { }
            case "gun_debug" -> { pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(.125, 2.5, 0); gap(sink, pose, shot(state, 0)); }
            default -> { }
        }
    }

    private static void staticNormal(WeaponPose state, PoseStack pose, SubmitSink sink, int index,
            double y, double z, float scale, int duration, double length) {
        pose.translate(0, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        if (scale != 1F) pose.scale(scale, scale, scale);
        muzzle(sink, pose, shot(state, index), duration, length, 1);
    }

    private static String itemPath(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static long shot(WeaponPose state, int index) {
        LivingEntity living = state.owner;
        if (living != null && living != Minecraft.getInstance().player) return com.hbm.client.GunEffectState.flashAt(living);
        return index >= 0 && index < state.lastShot.length ? state.lastShot[index] : -1L;
    }

    private static void bus(PoseStack pose, WeaponPose state, String name) {
        applyBus(pose, state.bus(name));
    }

    private static void bus(PoseStack pose, WeaponPose state, String name, int rail) {
        applyBus(pose, state.bus(name, rail));
    }

    private static void applyBus(PoseStack pose, double[] t) {
        pose.translate(t[0], t[1], t[2]);
        for (int i = 0; i < 3; i++) {
            switch ((int) t[12 + i]) {
                case 0 -> pose.mulPose(Axis.XP.rotationDegrees((float) t[3]));
                case 1 -> pose.mulPose(Axis.YP.rotationDegrees((float) t[4]));
                case 2 -> pose.mulPose(Axis.ZP.rotationDegrees((float) t[5]));
                default -> throw new IllegalStateException("Invalid weapon animation axis");
            }
        }
        pose.translate(-t[9], -t[10], -t[11]);
        pose.scale((float) t[6], (float) t[7], (float) t[8]);
    }

    private static GunConfig config(WeaponPose state, int index) {
        return ((ItemGunBaseNT) state.stack.getItem()).getConfig(state.stack, index);
    }

    private static void smoke(WeaponPose state, PoseStack pose, SubmitSink sink, int index, double scale) {
        List<ItemGunBaseNT.SmokeNode> nodes = config(state, index).getSmokeNodes(state.hand);
        if (nodes.size() < 2) return;
        ItemGunBaseNT.SmokeNode[] snapshot = nodes.toArray(new ItemGunBaseNT.SmokeNode[0]);
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.weaponSmoke(), (p, buffer) -> {
            for (int i = 0; i < snapshot.length - 1; i++) {
                ItemGunBaseNT.SmokeNode node = snapshot[i];
                ItemGunBaseNT.SmokeNode past = snapshot[i + 1];
                int nodeColor = argb((float) node.alpha);
                int pastColor = argb((float) past.alpha);
                Vertexes.color(buffer, p, (float) node.forward, (float) node.lift, (float) node.side, nodeColor);
                Vertexes.color(buffer, p, (float) node.forward, (float) node.lift,
                        (float) (node.side + node.width * scale), 0x00FFFFFF);
                Vertexes.color(buffer, p, (float) past.forward, (float) past.lift,
                        (float) (past.side + past.width * scale), 0x00FFFFFF);
                Vertexes.color(buffer, p, (float) past.forward, (float) past.lift, (float) past.side, pastColor);
                Vertexes.color(buffer, p, (float) node.forward, (float) node.lift, (float) node.side, nodeColor);
                Vertexes.color(buffer, p, (float) node.forward, (float) node.lift,
                        (float) (node.side - node.width * scale), 0x00FFFFFF);
                Vertexes.color(buffer, p, (float) past.forward, (float) past.lift,
                        (float) (past.side - past.width * scale), 0x00FFFFFF);
                Vertexes.color(buffer, p, (float) past.forward, (float) past.lift, (float) past.side, pastColor);
            }
        });
    }

    private static int argb(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255F)));
        return (a << 24) | 0xFFFFFF;
    }

    private static boolean active(long stamp, int duration) {
        long age = com.hbm.util.GameTime.now() - stamp;
        return age >= 0L && age < duration;
    }

    private static void muzzle(SubmitSink sink, PoseStack pose, long stamp, int duration, double length, float scale) {
        if (!active(stamp, duration)) return;
        long age = com.hbm.util.GameTime.now() - stamp;
        if (renderedItem != null) {
            com.hbm.client.gun.GunSmokeClient.noteMuzzleSubmission(renderedItem, stamp, age, duration);
        }
        float fire = age / (float) duration;
        float width = 6F * fire * (scale == 0F ? 1F : scale);
        float reach = (float) (length * fire * (scale == 0F ? 1F : scale));
        float inset = 2F * (scale == 0F ? 1F : scale);
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(Ids.parse(FLASH)), (p, b) -> {
            flashQuad(b, p, 0, -width, -inset, 1, 1, 0, width, -inset, 0, 1,
                    0.1F, width, reach - inset, 0, 0, 0.1F, -width, reach - inset, 1, 0);
            flashQuad(b, p, 0, width, inset, 0, 1, 0, -width, inset, 1, 1,
                    0.1F, -width, -reach + inset, 1, 0, 0.1F, width, -reach + inset, 0, 0);
            flashQuad(b, p, 0, -inset, width, 0, 1, 0, -inset, -width, 1, 1,
                    0.1F, reach - inset, -width, 1, 0, 0.1F, reach - inset, width, 0, 0);
            flashQuad(b, p, 0, inset, -width, 1, 1, 0, inset, width, 0, 1,
                    0.1F, -reach + inset, width, 0, 0, 0.1F, -reach + inset, -width, 1, 0);
        });
    }

    private static void gap(SubmitSink sink, PoseStack pose, long stamp) {
        if (!active(stamp, 75)) return;
        float fire = (com.hbm.util.GameTime.now() - stamp) / 75F;
        float height = 4F * fire, length = 15F * fire, lift = 3F * fire, offset = fire;
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(Ids.parse(FLASH)), (p, b) -> {
            flashQuad(b, p, 0, -height, -offset, 1, 1, 0, height, -offset, 0, 1,
                    0, height + lift, length - offset, 0, 0, 0, -height + lift, length - offset, 1, 0);
            flashQuad(b, p, 0, height, offset, 0, 1, 0, -height, offset, 1, 1,
                    0, -height + lift, -length + offset, 1, 0, 0, height + lift, -length + offset, 0, 0);
            flashQuad(b, p, 0, -height, -offset, 1, 1, 0, height, -offset, 0, 1,
                    0.125F, height, length - offset, 0, 0, 0.125F, -height, length - offset, 1, 0);
            flashQuad(b, p, 0, height, offset, 0, 1, 0, -height, offset, 1, 1,
                    0.125F, -height, -length + offset, 1, 0, 0.125F, height, -length + offset, 0, 0);
        });
    }

    private static void laser(SubmitSink sink, PoseStack pose, long stamp, int duration, double scale, int color) {
        if (!active(stamp, duration)) return;
        float size = (float) (4D * (com.hbm.util.GameTime.now() - stamp) / duration * scale);
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(Ids.parse(LASER)), (p, b) -> {
            Vertexes.vertex(b, p, 0, -size, -size, argb, 1, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, 0, size, -size, argb, 0, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, 0, size, size, argb, 0, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, 0, -size, size, argb, 1, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
        });
    }

    private static void fireball(SubmitSink sink, PoseStack pose, long stamp) {
        if (!active(stamp, 150)) return;
        float fire = (com.hbm.util.GameTime.now() - stamp) / 150F;
        float height = 5F * fire;
        float length = 10F * fire;
        float offset = fire;
        float lengthOffset = -1.125F;
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(Ids.parse(FLASH)), (p, b) -> {
            Vertexes.vertex(b, p, height, -offset, 0, -1, 0, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, -height, -offset, 0, -1, 1, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, -height, -offset + length, -lengthOffset, -1, 1, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, height, -offset + length, -lengthOffset, -1, 0, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, height, -offset, 0, -1, 0, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, -height, -offset, 0, -1, 1, 1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, -height, -offset + length, lengthOffset, -1, 1, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
            Vertexes.vertex(b, p, height, -offset + length, lengthOffset, -1, 0, 0, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
        });
    }

    private static void flashQuad(com.mojang.blaze3d.vertex.VertexConsumer b, PoseStack.Pose p,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float x4, float y4, float z4, float u4, float v4) {
        Vertexes.vertex(b, p, x1, y1, z1, -1, u1, v1, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
        Vertexes.vertex(b, p, x2, y2, z2, -1, u2, v2, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
        Vertexes.vertex(b, p, x3, y3, z3, -1, u3, v3, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
        Vertexes.vertex(b, p, x4, y4, z4, -1, u4, v4, OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0, 1, 0);
    }

    private static void atlas(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.125F, .125F, .125F); recoil(p, s, 10); equipNeg(p, s, 0, 0, -7, 7);
        p.pushPose(); p.translate(0, 1.5, 9.25); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("RECOIL")[2] * 10));
        p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .5); p.popPose();
        p.translate(0, 0, 0); bus(p, s, "RELOAD_MOVE"); rotateReload(p, s);
        p.pushPose(); p.translate(0, 1.5, 9.25); p.mulPose(Axis.YP.rotationDegrees(90)); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void am180(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.1875F, .1875F, .1875F); equip(p, s, 0, -2, -6, 6); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN")[2])); p.translate(0, 0, s.bus("RECOIL")[2]);
        boolean silenced = SpecialWeaponRenderers.hasUpgrade(s.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER);
        p.pushPose(); p.translate(0, 1.875, silenced ? 17 : 13); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .25); p.popPose();
        p.pushPose(); p.translate(0, 1.875, silenced ? 16.75 : 12); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(silenced ? .5F : .75F, silenced ? .5F : .75F, silenced ? .5F : .75F); muzzle(c, p, shot(s, 0), silenced ? 75 : 50, silenced ? 5 : 7.5, 1); p.popPose();
    }

    private static void star(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        if (SpecialWeaponRenderers.hasUpgrade(s.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) return;
        p.scale(.25F, .25F, .25F); equip(p, s, 0, -2, -8, 8); p.translate(0, 1, -3); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("TILT")[0])); p.translate(0, -1, 3); p.translate(0, 0, s.bus("RECOIL")[2]);
        p.pushPose(); p.translate(0, 3, 6.125); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, .75); p.popPose();
        p.pushPose(); p.translate(0, 3, 6.125); p.scale(.75F, .75F, .75F); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void starAkimbo(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        for (int side = -1; side <= 1; side += 2) {
            int rail = side < 0 ? 0 : 1;
            if (SpecialWeaponRenderers.hasUpgrade(s.stack, rail, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) continue;
            p.pushPose(); p.translate(-2F * .8F * side, -1.75F * .8F, 2.5F * .8F); p.scale(.25F, .25F, .25F); equipRail(p, s, rail, 0, -2, -8); p.translate(0, 1, -3); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN", rail)[2] * side)); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("TILT", rail)[0])); p.translate(0, -1, 3); p.translate(0, 0, s.bus("RECOIL", rail)[2]);
            p.pushPose(); p.translate(0, 3, 6.125); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, rail, .75); p.popPose();
            p.pushPose(); p.translate(0, 3, 6.125); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, rail), 75, 7.5, 1); p.popPose(); p.popPose();
        }
    }

    private static void maresleg(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F);
        p.translate(s.bus("RECOIL")[0] * 2, s.bus("RECOIL")[1], s.bus("RECOIL")[2]);
        p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[2] * 5));
        p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN")[2]));
        p.translate(0, 0, -4); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("LIFT")[0])); p.translate(0, 0, 4);
        p.translate(0, 0, -4); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("EQUIP")[0])); p.translate(0, 0, 4);
        p.translate(0, 0, -2); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("FLIP")[0])); p.translate(0, 0, 2);
        boolean shortened = itemPath(s.stack).equals("gun_maresleg_broken");
        double z = shortened ? 3.75D : 8D;
        p.pushPose(); p.translate(0, 1, z); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("FLIP")[0])); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .25); p.popPose();
        p.pushPose(); p.translate(0, 1, z); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 5, 1); p.popPose();
    }

    private static void mareslegAkimbo(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        for (int side = -1; side <= 1; side += 2) {
            int rail = side < 0 ? 0 : 1;
            p.pushPose(); p.translate(-1.5F * .8F * side, -.8F, 2F * .8F); p.scale(.375F, .375F, .375F);
            p.translate(s.bus("RECOIL", rail)[0] * 2, s.bus("RECOIL", rail)[1], s.bus("RECOIL", rail)[2]);
            p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL", rail)[2] * 5));
            p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN", rail)[2]));
            p.translate(0, 0, -4); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("LIFT", rail)[0])); p.translate(0, 0, 4);
            p.translate(0, 0, -4); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("EQUIP", rail)[0])); p.translate(0, 0, 4);
            p.translate(0, 0, -2); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("FLIP", rail)[0])); p.translate(0, 0, 2);
            p.pushPose(); p.translate(0, 1, 3.75); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN", rail)[2])); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("FLIP", rail)[0])); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, rail, .25); p.popPose();
            p.pushPose(); p.translate(0, 1, 3.75); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, rail), 75, 5, 1); p.popPose(); p.popPose();
        }
    }

    private static void greasegun(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -3, -3, 3); lift(p, s, 0, -3, -3, 3); if (ItemGunBaseNT.aimingProgress < 1F) p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN")[2])); p.translate(0, 0, s.bus("RECOIL")[2]);
        p.pushPose(); p.translate(-.25, 0, 1.5); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.25F, .25F, .25F); smoke(s, p, c, 0, 1); p.popPose();
        p.pushPose(); p.translate(0, 0, 8); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.25F, .25F, .25F); smoke(s, p, c, 0, 1); p.popPose();
        p.pushPose(); p.translate(0, 0, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.5F, .5F, .5F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void lag(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.25F, .25F, .25F); p.mulPose(Axis.YP.rotationDegrees(90)); p.translate(4, -4, 0); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("EQUIP")[0])); p.translate(-4, 4, 0); bus(p, s, "ADD_TRANS"); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ADD_ROT")[2])); p.mulPose(Axis.YP.rotationDegrees((float) s.bus("ADD_ROT")[1]));
        p.pushPose(); p.translate(-10.25, 1, 0); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(-10.25, 1, 0); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void uzi(WeaponPose s, PoseStack p, SubmitSink c, int l, int o, int index, int rail, int side) {
        if (SpecialWeaponRenderers.hasUpgrade(s.stack, index, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) return;
        p.scale(.25F, .25F, .25F); bus(p, s, "YEET", rail); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("SPEEN", rail)[0] * side)); equipRail(p, s, rail, 0, -2, -4); liftRail(p, s, rail, 0, 0, -6, 6); p.translate(0, 0, s.bus("RECOIL", rail)[2]);
        p.pushPose(); p.translate(0, .75, 8.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, index, .75); p.popPose(); p.pushPose(); p.translate(0, .75, 8.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, index), 75, 7.5, 1); p.popPose();
    }

    private static void uziAkimbo(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        uziRail(s, p, c, 0, -1); uziRail(s, p, c, 1, 1);
    }

    private static void uziRail(WeaponPose s, PoseStack p, SubmitSink c, int rail, int side) {
        p.pushPose(); p.translate(aimLerp(s, -2.25F * .8F * side, 0), aimLerp(s, -1.5F * .8F, -4.375 / 8D), aimLerp(s, 2.5F * .8F, 1)); uzi(s, p, c, 0, 0, rail, rail, side); p.popPose();
    }

    private static void henry(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); p.translate(s.bus("RECOIL")[0] * 2, s.bus("RECOIL")[1], s.bus("RECOIL")[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[2] * 5)); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("TURN")[2])); bus(p, s, "YEET"); p.translate(0, 1, 0); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROLL")[2])); p.translate(0, -1, 0); lift(p, s, 0, -4, 4); equipNeg(p, s, 0, -2, 4, -4);
        p.pushPose(); p.translate(0, 1, 8); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("TURN")[2])); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .25); p.popPose(); p.pushPose(); p.translate(0, 1, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 5, 1); p.popPose();
    }

    private static void heavyRevolver(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.125F, .125F, .125F); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("SPIN")[0])); p.translate(6, -3, 0); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROTATE")[0])); p.translate(-6, 3, 0); p.translate(aimLerp(s, 0, -s.bus("RECOIL")[2]), 0, aimLerp(s, s.bus("RECOIL")[2], 0)); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("RECOIL")[2] * 10));
        p.pushPose(); p.translate(-9, 2.5, 0); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("RECOIL")[2] * -10)); smoke(s, p, c, 0, .5); p.popPose(); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("RELOAD_LIFT")[0])); p.translate(s.bus("RELOAD_JOLT")[0], 0, 0); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RELAOD_TILT")[0])); p.pushPose(); p.translate(.125, 2.5, 0); gap(c, p, shot(s, 0)); p.popPose();
    }

    private static void hangman(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.translate(1.2F, 0, -1); p.mulPose(Axis.YP.rotationDegrees((float) s.bus("TURN")[1])); p.translate(-1.2F, 0, 1); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROLL")[2])); p.translate(s.bus("SMACK")[0], s.bus("SMACK")[1], s.bus("SMACK")[2]); p.scale(.125F, .125F, .125F); equip(p, s, 0, -4, -10, 10); p.translate(0, 0, s.bus("RECOIL")[2]);
        p.pushPose(); p.translate(0, 0, 29); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(1.5F, 1.5F, 1.5F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 0, 29); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(2F, 2F, 2F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void dani(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        for (int side = -1; side <= 1; side += 2) { int rail = side < 0 ? 0 : 1; p.pushPose(); p.translate(-1.5F * .8F * side, -.75F * .8F, .8F); p.scale(.125F, .125F, .125F); p.translate(s.bus("RECOIL", rail)[0], s.bus("RECOIL", rail)[1], s.bus("RECOIL", rail)[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL", rail)[2] * 10)); p.translate(0, -2, -2); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("EQUIP", rail)[0])); p.translate(0, 2, 2); p.pushPose(); p.translate(0, 1.5, 9.25); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("RECOIL", rail)[2] * 10)); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, rail, .5); p.popPose(); p.translate(s.bus("RELOAD_MOVE", rail)[0], s.bus("RELOAD_MOVE", rail)[1], s.bus("RELOAD_MOVE", rail)[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RELOAD_ROT", rail)[0])); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("RELOAD_ROT", rail)[2] * side)); p.mulPose(Axis.YP.rotationDegrees((float) s.bus("RELOAD_ROT", rail)[1] * side)); p.pushPose(); p.translate(0, 1.5, 9.25); p.mulPose(Axis.YP.rotationDegrees(90)); muzzle(c, p, shot(s, rail), 75, 7.5, 1); p.popPose(); p.popPose(); }
    }

    private static void pepperbox(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.25F, .25F, .25F); bus(p, s, "TRANSLATE"); p.translate(0, 0, -5); p.mulPose(Axis.XN.rotationDegrees((float) s.bus("RECOIL")[0])); p.translate(0, 0, 5); p.pushPose(); p.translate(0, .5, 7); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, .5, 7); p.scale(.5F, .5F, .5F); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 15, 1); p.mulPose(Axis.XP.rotationDegrees(45)); muzzle(c, p, shot(s, 0), 75, 15, 1); p.popPose();
    }

    private static void g3(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        boolean zebra = itemPath(s.stack).equals("gun_g3_zebra");
        if (zebra || SpecialWeaponRenderers.hasUpgrade(s.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) return;
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -2, -6, 6); lift(p, s, 0, 0, -4, 4); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 0, 13); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.75F, .75F, .75F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 0, 12); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(-25 + s.shotRand * 10)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 75, 10, 1); p.popPose();
    }

    private static void carbine(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.5F, .5F, .5F); equip(p, s, 0, -1, -2, 2); lift(p, s, 0, 0, -2, 2); bus(p, s, "STAB"); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 1, 8); p.mulPose(Axis.YP.rotationDegrees(90)); smoke(s, p, c, 0, .25); p.popPose(); p.pushPose(); p.translate(0, 1, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.5F, .5F, .5F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void liberator(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -1, -3, 3); lift(p, s, 0, -3, -3, 3); p.translate(s.bus("RECOIL")[0] * 2, s.bus("RECOIL")[1], s.bus("RECOIL")[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[2] * 10)); p.pushPose(); p.translate(0, .25, 7.25); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.375F, .375F, .375F); p.translate(0, 0, .25 / .375); smoke(s, p, c, 0, 1); p.translate(0, 0, -.5 / .375); smoke(s, p, c, 0, 1); p.translate(0, .5 / .375, 0); smoke(s, p, c, 0, 1); p.translate(0, 0, .5 / .375); smoke(s, p, c, 0, 1); p.popPose(); p.pushPose(); p.translate(0, .5, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(1.5F, 1.5F, 1.5F); muzzle(c, p, shot(s, 0), 75, 5, 1); p.popPose();
    }

    private static void congoLake(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.5F, .5F, .5F); bus(p, s, "Gun"); p.pushPose(); p.translate(0, 1.75, 4.25); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("Gun")[5])); p.mulPose(Axis.YN.rotationDegrees((float) s.bus("Gun")[4])); p.mulPose(Axis.XN.rotationDegrees((float) s.bus("Gun")[3])); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.25F, .25F, .25F); smoke(s, p, c, 0, 1); p.popPose(); p.pushPose(); p.translate(0, 1.75, 4.25); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.5F, .5F, .5F); muzzle(c, p, shot(s, 0), 150, 7.5, 1); p.popPose();
    }

    private static void spas12(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.5F, .5F, .5F); p.mulPose(Axis.YP.rotationDegrees(180)); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("EQUIP")[0])); p.pushPose(); p.translate(0, 1.5, -11); p.mulPose(Axis.YP.rotationDegrees(-90)); p.scale(.25F, .25F, .25F); smoke(s, p, c, 0, .75); p.popPose(); p.pushPose(); p.translate(0, 1.5, -11); p.mulPose(Axis.YP.rotationDegrees(-90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void stg77(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.5F, .5F, .5F); equip(p, s, 0, -1, -4, 4); lift(p, s, 0, 0, -4, 4); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 0, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.75F, .75F, .75F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 0, 7.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.25F, .25F, .25F); p.mulPose(Axis.XP.rotationDegrees(-5 + s.shotRand * 10)); gap(c, p, shot(s, 0)); p.popPose();
    }

    private static void minigun(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, 3, -6, 6); p.translate(0, 0, s.bus("RECOIL")[2]); if (itemPath(s.stack).equals("gun_minigun_lacunae")) { p.pushPose(); p.translate(0, 0, 12); p.mulPose(Axis.YP.rotationDegrees(90)); laser(c, p, shot(s, 0), 50, 1, 0xFF00FF); p.translate(0, 0, -.25F); laser(c, p, shot(s, 0), 50, .5, 0xFF0080); p.popPose(); } else { p.pushPose(); p.translate(-2, 1.25, -3.5); p.mulPose(Axis.YP.rotationDegrees(45)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 0, 12); p.mulPose(Axis.YP.rotationDegrees(90)); p.translate(0, .5, 0); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(1.5F, 1.5F, 1.5F); muzzle(c, p, shot(s, 0), 50, 7.5, 1); p.popPose(); }
    }

    private static void minigunDual(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        for (int side = -1; side <= 1; side += 2) { int rail = side < 0 ? 0 : 1; p.pushPose(); p.translate(-2.75F * .8F * side, -1.75F * .8F, 2.5F * .8F); p.scale(.375F, .375F, .375F); equipRail(p, s, rail, 0, 3, -6); p.translate(0, 0, s.bus("RECOIL", rail)[2]); p.translate(0, 0, 12); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(1.5F, 1.5F, 1.5F); muzzle(c, p, shot(s, rail), 50, 7.5, 1); p.popPose(); }
    }

    private static void mas36(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -3, -3, 3); lift(p, s, 0, 0, 0, 0); bus(p, s, "STAB"); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 1.125, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.25F, .25F, .25F); smoke(s, p, c, 0, 1); p.popPose(); p.pushPose(); p.translate(0, 1, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.5F, .5F, .5F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void m2(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.75F, .75F, .75F); equip(p, s, 0, 1, -2.25F, 2.25F); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 1.625, 5); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, .375); p.popPose(); p.pushPose(); p.translate(0, 1.625, 5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.5F, .5F, .5F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void shredder(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.25F, .25F, .25F); equip(p, s, 0, -2, -6, 6); lift(p, s, 0, 0, -4, 4); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 1, 7.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.75F, .75F, .75F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 1, 7.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void sexy(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -1, -8, 8); p.translate(0, 0, -6); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("LOWER")[0])); p.translate(0, 0, 6); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 0, 8); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 150, 7.5, 1); p.popPose();
    }

    private static void amat(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        if (itemPath(s.stack).equals("gun_amat_penance") || SpecialWeaponRenderers.hasUpgrade(s.stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)) return;
        p.scale(.375F, .375F, .375F); p.translate(0, 0, s.bus("RECOIL")[2]); lift(p, s, 0, -3, -8, 8); equip(p, s, 0, -3, -8, 8); p.pushPose(); p.translate(0, .625, 12); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, 1); p.popPose(); p.pushPose(); p.translate(0, .5, 11); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.75F, .75F, .75F); gap(c, p, shot(s, 0)); p.popPose();
    }

    private static void aberrator(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.25F, .25F, .25F); p.translate(0, s.bus("RISE")[1], 0); equip(p, s, 0, 1, -2.25, 2.25); p.translate(0, -1, -4); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[0])); p.translate(0, 1, 4); p.translate(0, 1, 0); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROLL")[2])); p.translate(0, -1, 0); p.pushPose(); p.translate(0, 2, 4); p.mulPose(Axis.XN.rotationDegrees((float) s.bus("RECOIL")[0])); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("ROLL")[2])); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(0, 2, 4); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose(); p.pushPose(); p.translate(0, 2, -1.5); p.scale(.5F, .5F, .5F); fireball(c, p, shot(s, 0)); p.popPose();
    }

    private static void eott(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        for (int side = -1; side <= 1; side += 2) { int rail = side < 0 ? 0 : 1; p.pushPose(); p.translate(-.8F * side, -1F, 1F); p.scale(.25F, .25F, .25F); p.translate(0, s.bus("RISE", rail)[1], 0); equipRail(p, s, rail, 0, 1, -2.25F); p.translate(0, -1, -4); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL", rail)[0])); p.translate(0, 1, 4); p.translate(0, 1, 0); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROLL", rail)[2] * side)); p.translate(0, -1, 0); p.pushPose(); p.translate(0, 2, 4); p.mulPose(Axis.XN.rotationDegrees((float) s.bus("RECOIL", rail)[0])); p.mulPose(Axis.ZN.rotationDegrees((float) s.bus("ROLL", rail)[2] * side)); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, rail, .5); p.popPose(); p.pushPose(); p.translate(0, 2, 4); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, rail), 75, 7.5, 1); p.popPose(); p.pushPose(); p.translate(0, 2, -1.5); p.scale(.5F, .5F, .5F); fireball(c, p, shot(s, rail)); p.popPose(); p.popPose(); }
    }

    private static void lasrifle(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.3125F, .3125F, .3125F); equip(p, s, 0, -1, -6, 6); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 1.5, 12); p.mulPose(Axis.YP.rotationDegrees(90)); laser(c, p, shot(s, 0), 150, 1.5, 0xFF0000); p.translate(0, 0, -.25F); laser(c, p, shot(s, 0), 150, .75, 0xFF8000); p.popPose();
    }

    private static void laserPistol(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -1, -6, 6); lift(p, s, 0, 2, -2, 2); p.translate(0, 0, s.bus("RECOIL")[2]); p.translate(s.bus("JOLT")[0], s.bus("JOLT")[1], s.bus("JOLT")[2]); p.pushPose(); p.translate(0, 2, 4.75); p.mulPose(Axis.YP.rotationDegrees(90)); boolean green = itemPath(s.stack).equals("gun_laser_pistol_morning_glory"); laser(c, p, shot(s, 0), 150, 1.5, green ? 0x008000 : 0xFF0000); p.translate(0, 0, -.25F); laser(c, p, shot(s, 0), 150, .75, green ? 0x80FF00 : 0xFF8000); p.popPose();
    }

    private static void ni4ni(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.3125F, .3125F, .3125F); equip(p, s, 0, 0, -2.25, 2.25); p.translate(0, -1, -6); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[0])); p.translate(0, 1, 6); p.pushPose(); p.translate(0, .75, 4); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.125F, .125F, .125F); laser(c, p, shot(s, 0), 75, 7.5, 0xFFFFFF); p.popPose();
    }

    private static void mk108(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.375F, .375F, .375F); equip(p, s, 0, -1, -8, 8); lift(p, s, 0, 1, -4, 4); p.translate(0, 0, s.bus("RECOIL")[2]); p.pushPose(); p.translate(0, 0, 8.125); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); muzzle(c, p, shot(s, 0), 50, 5, 1); p.popPose();
    }

    private static void missileLauncher(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.5F, .5F, .5F); equip(p, s, 0, -2, -2, 2); p.pushPose(); p.translate(0, 1, 6.75); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 75, 7.5, 1); p.popPose();
    }

    private static void panzerschreck(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(1.25F, 1.25F, 1.25F); equip(p, s, 0, -1, -1, 1); p.pushPose(); p.translate(0, 0, 6.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 150, 7.5, 1); p.popPose();
    }

    private static void quadro(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(1.75F, 1.75F, 1.75F); equip(p, s, 0, -1, -1, 1); p.translate(0, 0, s.bus("RECOIL")[2]); p.translate(0, -1, -1); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("RELOAD_ROTATE")[2])); p.translate(0, 1, 1); p.pushPose(); p.translate(-1, .75, 6.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 150, 7.5, 1); p.popPose();
    }

    private static void stinger(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(1.5F, 1.5F, 1.5F); equip(p, s, 0, -1, -1, 1); p.pushPose(); p.translate(0, 0, 6.5); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.XP.rotationDegrees(s.shotRand * 90)); p.scale(.75F, .75F, .75F); muzzle(c, p, shot(s, 0), 150, 10, 1); p.popPose();
    }

    private static void flaregun(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.125F, .125F, .125F); p.translate(s.bus("RECOIL")[0], s.bus("RECOIL")[1], s.bus("RECOIL")[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[2] * 10)); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("FLIP")[0])); equipNeg(p, s, 0, 0, -8, 8); p.pushPose(); p.translate(0, 4, 9); p.mulPose(Axis.YP.rotationDegrees(90)); p.scale(.5F, .5F, .5F); smoke(s, p, c, 0, 2.5); p.translate(0, 0, .1); smoke(s, p, c, 0, 2); p.popPose();
    }

    private static void doubleBarrel(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.pushPose();
        p.translate(0, 0, 8); p.mulPose(Axis.YP.rotationDegrees(90));
        p.mulPose(Axis.XP.rotationDegrees(90F * s.shotRand)); p.scale(2F, 2F, 2F);
        muzzle(c, p, shot(s, 0), 75, 5, 1);
        p.popPose();
    }

    private static void debug(WeaponPose s, PoseStack p, SubmitSink c, int l, int o) {
        p.scale(.125F, .125F, .125F); p.mulPose(Axis.YP.rotationDegrees(90)); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("ROTATE")[0])); p.pushPose(); p.translate(-9, 2.5, 0); p.mulPose(Axis.ZP.rotationDegrees((float) -s.bus("RECOIL")[2] * 10)); smoke(s, p, c, 0, .5); p.popPose(); p.pushPose(); p.translate(.125, 2.5, 0); gap(c, p, shot(s, 0)); p.popPose();
    }

    private static void recoil(PoseStack p, WeaponPose s, double angle) { p.translate(s.bus("RECOIL")[0], s.bus("RECOIL")[1], s.bus("RECOIL")[2]); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RECOIL")[2] * (float) angle)); }
    private static void equip(PoseStack p, WeaponPose s, double x, double y, double z, double ignored) { p.translate(x, y, z); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("EQUIP")[0])); p.translate(-x, -y, -z); }
    private static void equipNeg(PoseStack p, WeaponPose s, double x, double y, double z, double ignored) { p.translate(x, y, z); p.mulPose(Axis.XP.rotationDegrees((float) -s.bus("EQUIP")[0])); p.translate(-x, -y, -z); }
    private static void lift(PoseStack p, WeaponPose s, double x, double y, double z, double ignored) { p.translate(x, y, z); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("LIFT")[0])); p.translate(-x, -y, -z); }
    private static void lift(PoseStack p, WeaponPose s, double x, double y, double z) { lift(p, s, x, y, z, 0D); }
    private static void equipRail(PoseStack p, WeaponPose s, int rail, double x, double y, double z) { p.translate(x, y, z); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("EQUIP", rail)[0])); p.translate(-x, -y, -z); }
    private static void liftRail(PoseStack p, WeaponPose s, int rail, double x, double y, double z, double ignored) { p.translate(x, y, z); p.mulPose(Axis.XP.rotationDegrees((float) s.bus("LIFT", rail)[0])); p.translate(-x, -y, -z); }
    private static double aimLerp(WeaponPose s, double hip, double sighted) {
        float progress = ItemGunBaseNT.aimingProgress(dev.golem.core.client.Frames.partialTick());
        return hip + progress * (sighted - hip);
    }
    private static void rotateReload(PoseStack p, WeaponPose s) { p.mulPose(Axis.XP.rotationDegrees((float) s.bus("RELOAD_ROT")[0])); p.mulPose(Axis.ZP.rotationDegrees((float) s.bus("RELOAD_ROT")[2])); p.mulPose(Axis.YP.rotationDegrees((float) s.bus("RELOAD_ROT")[1])); }
}
