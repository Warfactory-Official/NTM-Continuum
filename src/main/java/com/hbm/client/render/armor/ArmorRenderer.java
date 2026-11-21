package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * The worn body of one modelled armour set: an OBJ declared over the wearer's skeleton.
 *
 * <p>An armour OBJ is authored in absolute biped space, so a bone's pivot is a centre of rotation and
 * nothing more. <b>The geometry is never moved to the bone</b>, or every limb lands at twice its
 * offset. Crouching moves the live pivot but not the authored geometry, so the translate back uses the
 * rest pivot; using the live one in both directions cancels the crouch and detaches the suit.
 */
public abstract class ArmorRenderer {

    private static final float UNIT = 1F / 16F;

    /** A limb that bends in the middle: an arm at the elbow, a leg at the knee. */
    public enum Limb { RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG,
        /** The torso at the waist: here the bend moves the half above the joint (the chest), the hips stay on the body bone. */
        TORSO }

    /**
     * A wearer's bent limbs, for an addon that knows them (a body animation that bends elbows and knees): the lower half's transform in the limb bone's
     * own frame, in blocks (a rotation about the joint), or null for a straight limb. With one, every suit's arm and leg pieces are cut at the joint and
     * what is below it (forearm, shin, boot) follows.
     */
    @FunctionalInterface
    public interface LimbBends {
        org.joml.@Nullable Matrix4fc lower(HumanoidPose wearer, Limb limb);
    }

    public static volatile LimbBends LIMB_BENDS = (wearer, limb) -> null;

    /**
     * Across a first-person sleeve at most, px: the vanilla arm is 4, and a first-person arm (its frames map that 4 onto a thinner arm) would be
     * swallowed by a power armour sleeve kept at its width.
     */
    private static final float FIRST_PERSON_ACROSS = 5.5F;
    /** Across a first-person sleeve at least, px: just over the 4 px arm, so no skin shows through. */
    private static final float FIRST_PERSON_MIN = 4.6F;

    /**
     * Bone-local (blocks) fit of an arm piece (one half: its shoulder plate must not set the forearm's size) for first person: centred on the vanilla
     * arm's axis (1 px out from the pivot, towards the body's side) and sized across, each axis on its own, to between {@link #FIRST_PERSON_MIN} and
     * {@link #FIRST_PERSON_ACROSS}.
     */
    private static Matrix4f firstPersonFit(dev.golem.core.render.gem.@org.jspecify.annotations.Nullable Geometry piece, Pivot pivot, int side) {
        float[][] b = piece == null ? null : piece.bounds();
        if (b == null) return new Matrix4f();
        float cx = (b[0][0] + b[1][0]) / 2F - pivot.x, cz = (b[0][2] + b[1][2]) / 2F - pivot.z;
        float axis = side == 0 ? -1F : 1F;
        return new Matrix4f().translate(axis * UNIT, 0, 0).scale(fit(b[1][0] - b[0][0]), 1, fit(b[1][2] - b[0][2]))
                .translate(-cx * UNIT, 0, -cz * UNIT);
    }

    /** The factor taking a sleeve {@code extent} px across to between just over the arm and {@link #FIRST_PERSON_ACROSS}. */
    private static float fit(float extent) {
        return extent <= 0F ? 1F : Math.max(FIRST_PERSON_MIN, Math.min(FIRST_PERSON_ACROSS, extent)) / extent;
    }

    /** Where a limb piece is cut, below its bone's pivot, model units: halfway down vanilla's 12-long arm and leg cubes. */
    private static final float JOINT = 6F;

    /** A bone, the rest pivot the OBJ was authored around, and the crouched height that replaces it. */
    protected enum Pivot {
        HEAD(HumanoidPose::head, 0F, 0F, 0F, 1F),
        BODY(HumanoidPose::body, 0F, 0F, 0F, 0F),
        LEFT_ARM(HumanoidPose::leftArm, 5F, 2F, 0F, 2F),
        RIGHT_ARM(HumanoidPose::rightArm, -5F, 2F, 0F, 2F),
        LEFT_LEG(HumanoidPose::leftLeg, 1.9F, 12F, 0F, 9F),
        RIGHT_LEG(HumanoidPose::rightLeg, -1.9F, 12F, 0F, 9F);

        private final Function<HumanoidPose, ModelPart> bone;
        private final float x;
        private final float y;
        private final float z;
        private final float crouchY;

        Pivot(Function<HumanoidPose, ModelPart> bone, float x, float y, float z, float crouchY) {
            this.bone = bone;
            this.x = x;
            this.y = y;
            this.z = z;
            this.crouchY = crouchY;
        }

        ModelPart bone(HumanoidPose wearer) {
            return bone.apply(wearer);
        }

        /**
         * The scale out of model units is not folded in here: a pose stack handed a
         * matrix that scales recomputes its normal matrix as the inverse transpose, leaving normals
         * sixteen times too long. 1.20.1 does not normalise afterwards. Stated as an op it is
         * recognised as uniform and the normals are left alone.
         */
        Matrix4f pose(HumanoidPose wearer) {
            return pose(wearer, null);
        }

        /** The authored geometry on a frame that takes this bone's local space (blocks) somewhere: a first-person arm. */
        Matrix4f onto(org.joml.Matrix4fc frame) {
            return new Matrix4f(frame).translate(-x * UNIT, -y * UNIT, -z * UNIT);
        }

        /** As {@link #onto(org.joml.Matrix4fc)}, {@code fit} (bone-local, blocks) applied first. */
        Matrix4f onto(org.joml.Matrix4fc frame, org.joml.Matrix4fc fit) {
            return new Matrix4f(frame).mul(fit).translate(-x * UNIT, -y * UNIT, -z * UNIT);
        }

        /** As {@link #pose(HumanoidPose)}, {@code bend} (in the bone's frame, blocks) applied after the bone: a forearm below a bent elbow. */
        Matrix4f pose(HumanoidPose wearer, org.joml.@Nullable Matrix4fc bend) {
            ModelPart part = bone(wearer);
            Matrix4f m = new Matrix4f()
                    .translate(part.x * UNIT, (wearer.sneaking ? crouchY : part.y) * UNIT, part.z * UNIT)
                    .rotateZ(part.zRot)
                    .rotateY(part.yRot)
                    .rotateX(part.xRot);
            if (bend != null) m.mul(bend);
            return m.translate(-x * UNIT, -y * UNIT, -z * UNIT);
        }
    }

    private final ArmorPose pose = new ArmorPose();
    private Gem.@Nullable Model<ArmorPose> model;

    protected abstract void setup(Gem.Model<ArmorPose> declaration);

    /** Built on first use, which is after resources load and so after {@link #setup} can name groups. */
    public final Gem.Model<ArmorPose> gem() {
        Gem.Model<ArmorPose> built = model;
        if (built == null) {
            built = Gem.model();
            setup(built);
            built.seal();
            model = built;
        }
        return built;
    }

    /**
     * The arm pieces of this suit's chestplate alone, on arm frames of the caller's (a first-person view that draws its own arms): {@code arms} as
     * {@link ArmorPose#arms}. Everything else of the suit is left out.
     */
    public final void submitArms(PoseStack poseStack, SubmitSink sink, int light, org.joml.Matrix4fc[] arms) {
        pose.slot = ArmorSlot.CHESTPLATE;
        pose.wearer = null;
        pose.arms = arms;
        try {
            Gem.replay(gem(), pose, poseStack, sink, light);
        } finally {
            pose.arms = null;
        }
    }

    /** Draws the piece worn in {@code slot}. Slots this suit does not model draw nothing. */
    public final void submit(ArmorSlot slot, PoseStack poseStack, SubmitSink sink, int light,
            HumanoidPose wearer) {
        pose.slot = slot;
        pose.wearer = wearer;
        Gem.replay(gem(), pose, poseStack, sink, light);
    }

    protected final Gem.Part<ArmorPose> piece(Gem.Model<ArmorPose> declaration, ArmorSlot slot,
            WavefrontObj model, String group, Surface surface, Pivot pivot) {
        // Only the slot gates a piece, never the wearer bone's `visible` flag. 1.7's ModelRendererObj copied
        // the biped's pose but not its isHidden, so a suit that hides the wearer's own limbs (FAU leggings,
        // full set: LEFT_LEG/RIGHT_LEG via ArmorPartHiding) still drew its own leg and boot geometry over them.
        // Reading `visible` here made the FAU legs and boots vanish as soon as the boots completed the set.
        Limb limb = switch (pivot) {
            case RIGHT_ARM -> Limb.RIGHT_ARM;
            case LEFT_ARM -> Limb.LEFT_ARM;
            case RIGHT_LEG -> Limb.RIGHT_LEG;
            case LEFT_LEG -> Limb.LEFT_LEG;
            default -> null;
        };
        if (pivot == Pivot.BODY) {
            // cut at the waist (6 px down the torso): the hips on the bone, the chest on the bone and the torso bend
            var whole = model.part(group);
            SplitGeometry chest = SplitGeometry.of(whole, JOINT, true), hips = SplitGeometry.of(whole, JOINT, false);
            if (chest != null && hips != null) {
                declaration.part(hips, surface)
                        .visibleIf(state -> state.slot == slot && state.arms == null)
                        .pose(state -> pivot.pose(state.wearer))
                        .scale(UNIT);
                return declaration.part(chest, surface)
                        .visibleIf(state -> state.slot == slot && state.arms == null)
                        .pose(state -> pivot.pose(state.wearer, LIMB_BENDS.lower(state.wearer, Limb.TORSO)))
                        .scale(UNIT);
            }
        }
        if (limb == Limb.RIGHT_ARM || limb == Limb.LEFT_ARM) {
            // first person: the arm on the caller's frames, cut at the elbow as below
            int side = limb == Limb.RIGHT_ARM ? 0 : 1;
            var whole = model.part(group);
            float cut = pivot.y - 2F + JOINT;
            SplitGeometry upper = SplitGeometry.of(whole, cut, true), lower = SplitGeometry.of(whole, cut, false);
            Matrix4f fitUpper = firstPersonFit(upper != null ? upper : whole, pivot, side), fitLower = firstPersonFit(lower, pivot, side);
            Gem.Part<ArmorPose> top = null;
            if (lower != null) {
                top = declaration.part(lower, surface)
                        .visibleIf(state -> state.slot == slot)
                        .pose(state -> state.arms != null ? pivot.onto(state.arms[side * 2 + 1], fitLower)
                                : pivot.pose(state.wearer, LIMB_BENDS.lower(state.wearer, limb)))
                        .scale(UNIT);
            }
            if (upper != null) {
                top = declaration.part(upper, surface)
                        .visibleIf(state -> state.slot == slot)
                        .pose(state -> state.arms != null ? pivot.onto(state.arms[side * 2], fitUpper) : pivot.pose(state.wearer))
                        .scale(UNIT);
            }
            if (top != null) return top;
            return declaration.part(whole, surface)
                    .visibleIf(state -> state.slot == slot)
                    .pose(state -> state.arms != null ? pivot.onto(state.arms[side * 2], fitUpper) : pivot.pose(state.wearer))
                    .scale(UNIT);
        }
        if (limb != null) {
            // cut at the joint: above it on the bone, below it on the bone and the bend (LIMB_BENDS), straight when there is none. A piece wholly
            // below the joint (a boot) follows the bend whole.
            var whole = model.part(group);
            float cut = (limb.ordinal() < 2 ? pivot.y - 2F : pivot.y) + JOINT;   // the arm bone's pivot sits 2 below the cube's top
            SplitGeometry upper = SplitGeometry.of(whole, cut, true), lower = SplitGeometry.of(whole, cut, false);
            Gem.Part<ArmorPose> top = null;
            if (lower != null) {
                top = declaration.part(lower, surface)
                        .visibleIf(state -> state.slot == slot && state.arms == null)
                        .pose(state -> pivot.pose(state.wearer, LIMB_BENDS.lower(state.wearer, limb)))
                        .scale(UNIT);
            }
            if (upper != null) {
                top = declaration.part(upper, surface)
                        .visibleIf(state -> state.slot == slot && state.arms == null)
                        .pose(state -> pivot.pose(state.wearer))
                        .scale(UNIT);
            }
            if (top != null) return top;
        }
        return declaration.part(model.part(group), surface)
                .visibleIf(state -> state.slot == slot && state.arms == null)
                .pose(state -> pivot.pose(state.wearer))
                .scale(UNIT);
    }
}
