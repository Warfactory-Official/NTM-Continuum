package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Day And Night's two independently animated break-action revolvers. */
public final class DaniWeaponRenderer extends WeaponRenderer {

    private static final Identifier MODEL = Ids.of("hbm", "models/weapons/bio_revolver.obj");
    private static final Surface CELESTIAL = Surface.cutout("hbm:textures/models/weapons/dani_celestial.png");
    private static final Surface LUNAR = Surface.cutout("hbm:textures/models/weapons/dani_lunar.png");
    private static final String[] PARTS = {"Grip", "Barrel", "Latch", "Drum", "Hammer"};

    private WavefrontObj model;

    /*
     * The item base has already applied the current hand's display transform before this renderer is
     * called. The old offhand copy was a second ordinary third-person item draw, so move that draw from
     * the current hand frame into the other hand frame. These are the two hand transforms represented by
     * gun_obj_dani_base.json, expressed in model space.
     */
    private static final RelativeHand RIGHT_TO_LEFT = relativeHandTransform(
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    private static final RelativeHand LEFT_TO_RIGHT = relativeHandTransform(
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL);
        return model;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    /**
     * Which rest draws appear is {@link DaniStaticLayout}'s answer for the context, and it is not a
     * subset of one list — a slot shows both revolvers, a hand shows this hand's and the other hand's,
     * the head shows only the lunar one. Each is therefore its own gated subtree rather than a loop.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        float offset = 0.8F;
        held(declaration, 0, -1, CELESTIAL, offset);
        held(declaration, 1, 1, LUNAR, offset);

        rest(declaration, DaniStaticLayout.Draw.GUI_CELESTIAL, CELESTIAL)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, 45F)
                .translate(2F, 0F, 0F);

        rest(declaration, DaniStaticLayout.Draw.GUI_LUNAR, LUNAR)
                .rotate(Gem.Axes.Z, -225F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, 45F)
                .rotate(Gem.Axes.Y, 180F)
                .translate(2F, 0F, 0F);

        rest(declaration, DaniStaticLayout.Draw.CELESTIAL, CELESTIAL);
        rest(declaration, DaniStaticLayout.Draw.LUNAR, LUNAR);

        otherHand(rest(declaration, DaniStaticLayout.Draw.THIRD_PERSON_RIGHT_LUNAR, LUNAR),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, LEFT_TO_RIGHT);
        otherHand(rest(declaration, DaniStaticLayout.Draw.THIRD_PERSON_LEFT_LUNAR, LUNAR),
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, RIGHT_TO_LEFT);
    }

    private Gem.Part<WeaponPose> rest(Gem.Model<WeaponPose> declaration, DaniStaticLayout.Draw draw,
            Surface body) {
        return declaration.part(model().parts(PARTS), body)
                .visibleIf(state -> !state.animated
                        && (state.modTable ? DaniStaticLayout.forModTable(state.tableIndex)
                                : DaniStaticLayout.forContext(state.context)).contains(draw));
    }

    /** Scaled down out of the 8× the model is authored at. */
    private void held(Gem.Model<WeaponPose> declaration, int rail, int side, Surface body, float offset) {
        Gem.Part<WeaponPose> frame = declaration.joint()
                .visibleIf(state -> state.animated)
                .slide(Gem.Axes.X, state -> aimLerp(-1.5F * offset * side, 0))
                .slide(Gem.Axes.Y, state -> aimLerp(-0.75F * offset, -3.125 / 8D))
                .slide(Gem.Axes.Z, state -> aimLerp(offset, 0.25))
                .scale(0.125F);
        revolver(frame, rail, side, body);
    }

    /** The reload's Z and Y rotations are mirrored for the left revolver, which is what {@code side} is for. */
    private void revolver(Gem.Part<WeaponPose> parent, int rail, int side, Surface body) {
        WavefrontObj obj = model();

        Gem.Part<WeaponPose> grip = parent.child(obj.part("Grip"), body)
                .slide(Gem.Axes.X, state -> (float) state.bus("RECOIL", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL", rail)[2])
                .angle(Gem.Axes.X, state -> (float) (state.bus("RECOIL", rail)[2] * 10))
                .spin(Gem.Axes.X, 0F, -2F, -2F, state -> (float) -state.bus("EQUIP", rail)[0])
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_MOVE", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RELOAD_MOVE", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RELOAD_MOVE", rail)[2])
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD_ROT", rail)[0])
                .angle(Gem.Axes.Z, state -> (float) (state.bus("RELOAD_ROT", rail)[2] * side))
                .angle(Gem.Axes.Y, state -> (float) (state.bus("RELOAD_ROT", rail)[1] * side));

        Gem.Part<WeaponPose> barrel = grip.child(obj.part("Barrel"), body)
                .angle(Gem.Axes.X, state -> (float) state.bus("FRONT", rail)[2]);

        barrel.child(obj.part("Latch"), body)
                .spin(Gem.Axes.X, 0F, 2.3125F, -0.875F, state -> (float) state.bus("LATCH", rail)[2]);

        barrel.child(obj.part("Drum"), body)
                .spin(Gem.Axes.Z, 0F, 1F, 0F, state -> (float) (state.bus("DRUM", rail)[2] * 60))
                .slide(Gem.Axes.Z, state -> (float) state.bus("DRUM_PUSH", rail)[2]);

        grip.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.X, 0F, 0F, -4.5F,
                        state -> (float) (-45 + 45 * state.bus("HAMMER", rail)[2]));
    }

    /**
     * Moves one rest draw into the other hand's frame. Stated as ops rather than the composed matrix:
     * a pose stack handed a matrix recomputes its normal matrix as the inverse transpose of everything
     * accumulated, including the display transform's scale, and the versions that do not normalise
     * afterwards would light the revolver with normals fourteen times too long.
     */
    private static void otherHand(Gem.Part<WeaponPose> draw, ItemDisplayContext own, RelativeHand other) {
        draw.slide(Gem.Axes.X, state -> state.context == own ? 0F : other.translation().x())
                .slide(Gem.Axes.Y, state -> state.context == own ? 0F : other.translation().y())
                .slide(Gem.Axes.Z, state -> state.context == own ? 0F : other.translation().z())
                .angle(Gem.Axes.Z, state -> state.context == own ? 0F : other.rotation().z())
                .angle(Gem.Axes.Y, state -> state.context == own ? 0F : other.rotation().y())
                .angle(Gem.Axes.X, state -> state.context == own ? 0F : other.rotation().x());
    }

    /** A hand-to-hand move, decomposed into the Z-Y-X rotation order the ops apply in. */
    private record RelativeHand(Vector3f translation, Vector3f rotation) {}

    private static RelativeHand relativeHandTransform(ItemDisplayContext from, ItemDisplayContext to) {
        Matrix4f relative = displayTransform(from).invert().mul(displayTransform(to));
        Vector3f radians = relative.getUnnormalizedRotation(new Quaternionf()).normalize()
                .getEulerAnglesZYX(new Vector3f());
        return new RelativeHand(relative.getTranslation(new Vector3f()),
                new Vector3f((float) Math.toDegrees(radians.x), (float) Math.toDegrees(radians.y),
                        (float) Math.toDegrees(radians.z)));
    }

    private static Matrix4f displayTransform(ItemDisplayContext context) {
        boolean right = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        return new Matrix4f()
                .translate((right ? -0.757072F : 8.662276F) / 16F,
                        (right ? 1.14817F : 3.115906F) / 16F,
                        (right ? -2.356844F : 0.150338F) / 16F)
                .rotateXYZ((float) Math.toRadians(right ? -179.493995F : -172.402674F),
                        (float) Math.toRadians(right ? -1.125273F : 29.330267F),
                        (float) Math.toRadians(right ? 179.74399F : 154.481445F))
                .scale(0.0703125F);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        pose.scale(1F, 1F, -1F);
        pose.translate(8F, 6F, 0F);
        pose.scale(1.125F, 1.125F, 1.125F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, 1F, 3F);
    }
}
