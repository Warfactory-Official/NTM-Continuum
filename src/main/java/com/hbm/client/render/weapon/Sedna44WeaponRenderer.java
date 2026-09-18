package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** OBJ renderers for the Gen-3 Henry, heavy revolver, and Hangman .44 guns. */
public final class Sedna44WeaponRenderer extends WeaponRenderer {

    public enum Variant {
        HENRY("henry", "henry", false),
        HENRY_LINCOLN("henry", "henry_lincoln", false),
        HEAVY_REVOLVER("lilmac", "heavy_revolver", false),
        LILMAC("lilmac", "lilmac", true),
        PROTEGE("lilmac", "protege", false),
        HANGMAN("hangman", "hangman", false);

        private final String model;
        private final String texture;
        private final boolean scoped;

        Variant(String model, String texture, boolean scoped) {
            this.model = model;
            this.texture = texture;
            this.scoped = scoped;
        }
    }

    private final Variant variant;
    private final Identifier modelId;
    private static final Surface SCOPE = Surface.cutout("hbm:textures/misc/scope_44.png");

    private final Surface body;
    private WavefrontObj model;

    public Sedna44WeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.body = Surface.cutout("hbm:textures/models/weapons/" + variant.texture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(modelId);
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin();
        float[] max = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0],
                    y == 0 ? min[1] : max[1], z == 0 ? min[2] : max[2]);
        }
        return corners;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        if (variant == Variant.HENRY || variant == Variant.HENRY_LINCOLN || variant == Variant.HANGMAN) {
            return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
        }
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        float offset = 0.8F;
        if (variant == Variant.HENRY || variant == Variant.HENRY_LINCOLN) {
            pose.translate(0, 0, 0.875);
            standardAimingTransform(stack, pose, -1.25F * offset, -1F * offset, 1.75F * offset,
                    0, -5 / 8D, 1);
            float aiming = ItemGunBaseNT.prevAimingProgress
                    + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
            pose.mulPose(Axis.XP.rotationDegrees(-2.5F * aiming));
        } else if (variant == Variant.HANGMAN) {
            pose.translate(0, 0, 0.875);
            standardAimingTransform(stack, pose, -1.5F * offset, -0.875F * offset, 1.75F * offset,
                    0, -1.5 / 8D, 1.25);
        } else {
            pose.translate(0, 0, 1);
            standardAimingTransform(stack, pose, -1.0F * offset, -0.75F * offset, 1F * offset,
                    0, variant.scoped ? -4.75 / 8D : -3.875 / 8D, variant.scoped ? -0.25 : 0);
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, variant.scoped ? 0.66F : 0.33F);
    }

    /**
     * The heavy revolver's recoil runs backwards from the hip and sideways from the sights, so it is
     * the aiming lerp applied to two different axes of the same value.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        switch (variant) {
            case HENRY, HENRY_LINCOLN -> {
                henry(declaration);
                rest(declaration, "Gun", "Sight", "Hammer", "Lever", "Front", "Bullet");
            }
            case HEAVY_REVOLVER, LILMAC, PROTEGE -> {
                heavyRevolver(declaration);
                rest(declaration, "Gun", "Pivot", "Cylinder", "Bullets", "Casings", "Hammer");
                declaration.part(model().part("Scope"), SCOPE)
                        .visibleIf(state -> !state.animated && isScoped(state.stack));
            }
            case HANGMAN -> {
                hangman(declaration);
                rest(declaration, "Rifle", "Internals", "Lid", "Magazine", "Bullets");
            }
        }
    }

    private static Gem.Part<WeaponPose> held(Gem.Model<WeaponPose> declaration) {
        return declaration.joint().visibleIf(state -> state.animated);
    }

    private void rest(Gem.Model<WeaponPose> declaration, String... parts) {
        declaration.part(model().parts(parts), body).visibleIf(state -> !state.animated);
    }

    private void henry(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = held(declaration)
                .scale(0.375F)
                .slide(Gem.Axes.X, state -> (float) (state.bus("RECOIL")[0] * 2))
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .angle(Gem.Axes.X, state -> (float) (state.bus("RECOIL")[2] * 5))
                .angle(Gem.Axes.Z, state -> (float) state.bus("TURN")[2])
                .slide(Gem.Axes.X, state -> (float) state.bus("YEET")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("YEET")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("YEET")[2])
                .spin(Gem.Axes.Z, 0F, 1F, 0F, state -> (float) state.bus("ROLL")[2])
                .spin(Gem.Axes.X, 0F, -4F, 4F, state -> (float) state.bus("LIFT")[0])
                .spin(Gem.Axes.X, 0F, 2F, -4F, state -> (float) -state.bus("EQUIP")[0]);

        gun.child(obj.part("Gun"), body);
        gun.child(obj.part("Sight"), body)
                .spin(Gem.Axes.X, 0F, 1.25F, -0.1875F, state -> (float) state.bus("SIGHT")[0]);
        gun.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.X, 0F, 0.625F, -3F, state -> (float) (-30 + state.bus("HAMMER")[0]));
        gun.child(obj.part("Lever"), body)
                .spin(Gem.Axes.X, 0F, 0.25F, -2.3125F, state -> (float) state.bus("LEVER")[0]);
        gun.child(obj.part("Front"), body)
                .spin(Gem.Axes.Z, 0F, 1F, 0F, state -> (float) state.bus("TWIST")[2]);
        gun.child(obj.part("Bullet"), body)
                .translate(0F, 0F, -1F)
                .slide(Gem.Axes.X, state -> (float) state.bus("BULLET")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BULLET")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BULLET")[2]);
    }

    private void heavyRevolver(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = held(declaration)
                .scale(0.125F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Z, state -> (float) state.bus("SPIN")[0])
                .spin(Gem.Axes.Z, 6F, -3F, 0F, state -> (float) state.bus("ROTATE")[0])
                .slide(Gem.Axes.X, state -> aimLerp(0, -state.bus("RECOIL")[2]))
                .slide(Gem.Axes.Z, state -> aimLerp(state.bus("RECOIL")[2], 0))
                .angle(Gem.Axes.Z, state -> (float) (state.bus("RECOIL")[2] * 10))
                .angle(Gem.Axes.Z, state -> (float) state.bus("RELOAD_LIFT")[0])
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_JOLT")[0])
                .angle(Gem.Axes.X, state -> (float) state.bus("RELAOD_TILT")[0]);

        gun.child(obj.part("Gun"), body);

        Gem.Part<WeaponPose> pivot = gun.child(obj.part("Pivot"), body)
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD_CYLINDER")[0]);

        Gem.Part<WeaponPose> cylinder = pivot.child(obj.part("Cylinder"), body)
                .spin(Gem.Axes.X, 0F, 1.75F, 0F, state -> (float) (state.bus("DRUM")[2] * -60));

        Gem.Part<WeaponPose> loaded = cylinder.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_BULLETS")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RELOAD_BULLETS")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RELOAD_BULLETS")[2]);
        loaded.child(obj.part("Bullets"), body)
                .visibleIf(state -> state.bus("RELOAD_BULLETS_CON")[0] != 1);
        loaded.child(obj.part("Casings"), body);

        gun.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.Z, 4F, 1.25F, 0F,
                        state -> (float) (-30 + 30 * state.bus("HAMMER")[2]));

        gun.child(obj.part("Scope"), SCOPE).visibleIf(state -> isScoped(state.stack));
    }

    private void hangman(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        float offset = 0.8F;
        Gem.Part<WeaponPose> gun = held(declaration)
                .spin(Gem.Axes.Y, 1.5F * offset, 0F, -1F, state -> (float) state.bus("TURN")[1])
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROLL")[2])
                .slide(Gem.Axes.X, state -> (float) state.bus("SMACK")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("SMACK")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("SMACK")[2])
                .scale(0.125F)
                .spin(Gem.Axes.X, 0F, -4F, -10F, state -> (float) state.bus("EQUIP")[0])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);

        gun.child(obj.parts("Rifle", "Internals"), body);
        gun.child(obj.part("Lid"), body)
                .spin(Gem.Axes.Z, -2.1875F, -1.75F, 0F, state -> (float) state.bus("LID")[2]);

        Gem.Part<WeaponPose> magazine = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG")[2]);
        magazine.child(obj.part("Magazine"), body);
        magazine.child(obj.part("Bullets"), body).visibleIf(state -> state.bus("BULLETS")[0] == 0);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        switch (variant) {
            case HENRY, HENRY_LINCOLN -> {
                pose.scale(1.5F, 1.5F, 1.5F);
                pose.mulPose(Axis.XP.rotationDegrees(25F));
                pose.mulPose(Axis.YP.rotationDegrees(45F));
                pose.translate(-0.5F, 0.5F, 0F);
            }
            case HANGMAN -> {
                pose.scale(0.375F, 0.375F, 0.375F);
                pose.mulPose(Axis.XP.rotationDegrees(25F));
                pose.mulPose(Axis.YP.rotationDegrees(45F));
                pose.translate(-0.5F, 2.5F, 0F);
            }
            default -> {
                if (isScoped(stack)) {
                    pose.scale(1.125F, 1.125F, 1.125F);
                    pose.mulPose(Axis.XP.rotationDegrees(25F));
                    pose.mulPose(Axis.YP.rotationDegrees(45F));
                    pose.translate(0F, -0.5F, 0F);
                } else {
                    pose.scale(1.25F, 1.25F, 1.25F);
                    pose.mulPose(Axis.XP.rotationDegrees(25F));
                    pose.mulPose(Axis.YP.rotationDegrees(45F));
                }
            }
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        switch (variant) {
            case HENRY, HENRY_LINCOLN -> {
                pose.scale(1.75F, 1.75F, 1.75F);
                pose.translate(0F, 0.25F, 3F);
            }
            case HANGMAN -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(0F, 4.25F, 11F);
            }
            default -> {
                pose.scale(0.75F, 0.75F, 0.75F);
                pose.translate(0F, 1F, 3F);
            }
        }
    }

    @Override
    protected void setupEntity(PoseStack pose, ItemStack stack) {
        if (variant == Variant.HANGMAN) {
            pose.scale(0.0625F, 0.0625F, 0.0625F);
            return;
        }
        super.setupEntity(pose, stack);
    }

    @Override
    protected boolean scopedIn(ItemStack stack) {
        return isScoped(stack) && fullyAimed();
    }

    private boolean isScoped(ItemStack stack) {
        return variant.scoped || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }
}
