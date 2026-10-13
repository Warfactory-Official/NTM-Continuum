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
            ModelPart part = bone(wearer);
            return new Matrix4f()
                    .translate(part.x * UNIT, (wearer.sneaking ? crouchY : part.y) * UNIT, part.z * UNIT)
                    .rotateZ(part.zRot)
                    .rotateY(part.yRot)
                    .rotateX(part.xRot)
                    .translate(-x * UNIT, -y * UNIT, -z * UNIT);
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
        return declaration.part(model.part(group), surface)
                .visibleIf(state -> state.slot == slot)
                .pose(state -> pivot.pose(state.wearer))
                .scale(UNIT);
    }
}
