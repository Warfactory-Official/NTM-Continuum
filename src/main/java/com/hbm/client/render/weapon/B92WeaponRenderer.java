package com.hbm.client.render.weapon;

import com.hbm.items.weapon.GunB92;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** GEM renderer for the original box-model B92, including its pump slide during charge reload. */
public final class B92WeaponRenderer extends WeaponRenderer {

    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/b92.png");
    private static final String[] STATIC_PARTS = {
            "Muzzle1", "Barrel1", "Barrel2", "Grip", "Front1", "Front2", "Body", "Top",
            "GripBottom", "Handle", "HandleBack", "Frame1", "Frame2", "Frame3", "Trigger",
            "BackPlate1", "Back", "BackPlate2", "BodyPlate"};

    @Override
    public org.joml.Vector3fc[] extents() {
        return new Vector3f[] {
                new Vector3f(-1.5F, -1F, -3.5F), new Vector3f(20F, 22F, 7F),
                new Vector3f(-1.5F, -1F, 3.5F), new Vector3f(20F, 22F, -3.5F),
                new Vector3f(-1.5F, 22F, -3.5F), new Vector3f(20F, 22F, 3.5F),
                new Vector3f(-1.5F, -1F, 3.5F), new Vector3f(20F, -1F, -3.5F)};
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0F, 0F, 1.2F);
        standardAimingTransform(stack, pose, -0.7F, -0.7F, 0.8F, -0.1F, -0.25F, 0.65F);
    }

    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        Gem.Part<WeaponPose> root = declaration.joint()
                .scale(0.0625F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Z, state -> state.animated
                        ? -GunB92.getRotationFromAnim(state.stack) * 90F : 0F)
                .slide(Gem.Axes.X, state -> state.animated
                        ? -GunB92.getRotationFromAnim(state.stack) : 0F)
                .slide(Gem.Axes.Y, state -> state.animated
                        ? -GunB92.getRotationFromAnim(state.stack) : 0F);

        for (String name : STATIC_PARTS) addPart(root, name);

        addPart(root.joint()
                        .translate(10F, 1F, -1F)
                        .slide(Gem.Axes.X, state -> GunB92.getTransFromAnim(state.stack)), "Pump1");
        addPart(root.joint()
                        .translate(17F, 0.5F, -3.5F)
                        .slide(Gem.Axes.X, state -> GunB92.getTransFromAnim(state.stack)), "Pump2");
    }

    private static void addPart(Gem.Part<WeaponPose> parent, String name) {
        switch (name) {
            case "Handle", "HandleBack" -> parent.child(B92BoxGeometry.part(name), BODY)
                    .translate(6F, 7F, name.equals("Handle") ? -2F : -1.5F)
                    .rotate(Gem.Axes.Z, -13F)
                    .translate(-6F, -7F, name.equals("Handle") ? 2F : 1.5F);
            case "Frame3" -> parent.child(B92BoxGeometry.part(name), BODY)
                    .translate(-2F, 10.5F, -1F).rotate(Gem.Axes.Z, 30F)
                    .translate(2F, -10.5F, 1F);
            case "Trigger" -> parent.child(B92BoxGeometry.part(name), BODY)
                    .translate(4F, 7F, -0.5F).rotate(Gem.Axes.Z, 11F)
                    .translate(-4F, -7F, 0.5F);
            case "BackPlate1" -> parent.child(B92BoxGeometry.part(name), BODY)
                    .translate(15F, 0F, -1.5F).rotate(Gem.Axes.Z, -30F)
                    .translate(-15F, 0F, 1.5F);
            case "BackPlate2" -> parent.child(B92BoxGeometry.part(name), BODY)
                    .translate(15F, 0.5F, -2F).rotate(Gem.Axes.Z, -28F)
                    .translate(-15F, -0.5F, 2F);
            default -> parent.child(B92BoxGeometry.part(name), BODY).translate(pivotX(name), pivotY(name), pivotZ(name));
        }
    }

    private static float pivotX(String name) {
        return switch (name) {
            case "Muzzle1" -> -24F; case "Barrel1" -> -24F; case "Barrel2" -> -22F;
            case "Grip" -> -20F; case "Front1", "Front2" -> -22F; case "Body", "Top" -> 0F;
            case "GripBottom" -> -18F; case "Frame1" -> 0.5F; case "Frame2", "Frame3" -> -2F;
            case "Back", "BackPlate1", "BackPlate2" -> 15F; case "BodyPlate" -> 1.5F; default -> 0F;
        };
    }

    private static float pivotY(String name) {
        return switch (name) {
            case "Muzzle1" -> 0.5F; case "Barrel1" -> 1F; case "Barrel2" -> 0.5F;
            case "Grip" -> 3F; case "Front1" -> 0.5F; case "Front2", "Body" -> 0F;
            case "Top" -> 0F; case "GripBottom" -> 5.5F; case "Frame1" -> 11F;
            case "Frame2", "Frame3" -> 6.5F; case "Back" -> 3.5F; case "BodyPlate" -> 2F; default -> 0F;
        };
    }

    private static float pivotZ(String name) {
        return switch (name) {
            case "Muzzle1" -> -1F; case "Barrel1" -> -1.5F; case "Barrel2" -> -1F;
            case "Grip" -> -2F; case "Front1" -> -2F; case "Front2" -> -1.5F;
            case "Body" -> -2F; case "Top" -> -1.5F; case "GripBottom" -> -1F;
            case "Frame1", "Frame2", "Frame3" -> -1F; case "Back" -> -2F;
            case "BodyPlate" -> -2.5F; default -> 0F;
        };
    }
}
