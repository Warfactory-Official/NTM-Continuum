package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The break-action revolver, a direct port of 1.12.2 {@code ItemRenderAtlas}.
 *
 * <p>The model is authored at 8×, hence the 0.125 scale, and several joints read a bus's Z component
 * as an <em>angle</em> rather than a translation — upstream's shorthand, not a transcription slip.
 */
public class AtlasWeaponRenderer extends WeaponRenderer {

    private static final Identifier MODEL = dev.golem.core.Ids.of("hbm", "models/weapons/bio_revolver.obj");
    private final Identifier texture;

    private WavefrontObj model;

    public AtlasWeaponRenderer() {
        this("bio_revolver");
    }

    public AtlasWeaponRenderer(String texture) {
        this.texture = dev.golem.core.Ids.of("hbm", "textures/models/weapons/" + texture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL);
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] mn = obj.boundsMin();
        float[] mx = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            corners[i++] = new org.joml.Vector3f(xi == 0 ? mn[0] : mx[0],
                    yi == 0 ? mn[1] : mx[1], zi == 0 ? mn[2] : mx[2]);
        }
        return corners;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose,
                -1.0F * offset, -0.75F * offset, offset,
                0, -3.125 / 8D, 0.25);
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    /**
     * Two details are upstream's and read like slips: the draw sweep pivots seven units <em>behind</em>
     * the weapon so it swings up into view rather than growing out of nothing, and the reload rotation
     * is applied X, Z, Y — not X, Y, Z.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Surface body = Surface.cutout(texture.toString());

        Gem.Part<WeaponPose> grip = declaration.part(obj.part("Grip"), body)
                .stretch(state -> state.animated ? 0.125F : 1F)
                .slide(Gem.Axes.X, state -> (float) state.bus("RECOIL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .angle(Gem.Axes.X, state -> (float) (state.bus("RECOIL")[2] * 10))
                .spin(Gem.Axes.X, 0F, 0F, -7F, state -> (float) -state.bus("EQUIP")[0])
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_MOVE")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RELOAD_MOVE")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RELOAD_MOVE")[2])
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD_ROT")[0])
                .angle(Gem.Axes.Z, state -> (float) state.bus("RELOAD_ROT")[2])
                .angle(Gem.Axes.Y, state -> (float) state.bus("RELOAD_ROT")[1]);

        Gem.Part<WeaponPose> barrel = grip.child(obj.part("Barrel"), body)
                .angle(Gem.Axes.X, state -> (float) state.bus("FRONT")[2]);

        barrel.child(obj.part("Latch"), body)
                .spin(Gem.Axes.X, 0F, 2.3125F, -0.875F, state -> (float) state.bus("LATCH")[2]);

        barrel.child(obj.part("Drum"), body)
                .spin(Gem.Axes.Z, 0F, 1F, 0F, state -> (float) (state.bus("DRUM")[2] * 60))
                .slide(Gem.Axes.Z, state -> (float) state.bus("DRUM_PUSH")[2]);

        grip.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.X, 0F, 0F, -4.5F,
                        state -> (float) (-45 + 45 * state.bus("HAMMER")[2]));
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.125F, 1.125F, 1.125F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 1.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0F, 1F, 3F);
    }
}
