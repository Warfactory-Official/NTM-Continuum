package com.hbm.client.jetpack;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * The three worn jetpack bodies, declared rather than drawn. They hang off the wearer's torso bone,
 * which arrives through the render state.
 *
 * <p>The scale out of model units is its own op rather than part of the bone matrix: a pose stack
 * handed a matrix that scales recomputes its normal matrix as the inverse transpose, leaving normals
 * sixteen times too long on the versions that do not normalise afterwards.
 */
public final class JetpackWornModels {

    private static final float PIXEL = 1F / 16F;

    private static final int PIVOT_SIDE = 1;
    private static final int PIVOT_FRONT = 5;
    private static final int PIVOT_Z = 3;
    private static final int TIP_SIDE = 16;
    private static final int TIP_Z = 2;
    private static final float INWARD_ANGLE = 10F;

    private static final Surface BJ = Surface.cutoutCull("hbm:textures/armor/bj_jetpack.png");
    private static final Surface WINGS = Surface.cutoutCull("hbm:textures/armor/wings_murk.png");
    /** 1.7 {@code ModelArmorWings} types 2 and 3: Bob's and the_NCR's own wings ({@code SpecialPlayerRenderLayer}). */
    public static final Surface WINGS_BOB = Surface.cutoutCull("hbm:textures/armor/wings_bob.png");
    public static final Surface WINGS_BLACK = Surface.cutoutCull("hbm:textures/armor/wings_black.png");
    // 1.7 drew these two inside the living renderer's disabled face culling and never re-enabled it
    private static final Surface AXEPACK = Surface.cutoutNoCull("hbm:textures/armor/axepack.png");
    private static final Surface TAIL = Surface.cutoutNoCull("hbm:textures/armor/tail_peep.png");
    static final Identifier AXEPACK_MODEL = Ids.of("hbm", "models/armor/wings_pheo.obj");
    static final Identifier TAIL_MODEL = Ids.of("hbm", "models/armor/tail_peep.obj");

    private static final Map<String, Surface> PACKS = new ConcurrentHashMap<>();

    /** What a worn body reads: the torso bone it rides, the flap angles, and which pack this is. */
    public static final class Pose {
        public ModelPart bone;
        public Surface skin = BJ;
        public float sweep;
        public float trail;
    }

    private final Gem.Model<Pose> model;
    private final Pose pose = new Pose();

    private JetpackWornModels(Gem.Model<Pose> model) {
        this.model = model;
    }

    private static @Nullable JetpackWornModels standard;
    private static @Nullable JetpackWornModels bj;
    private static @Nullable JetpackWornModels wings;
    private static @Nullable JetpackWornModels axePack;
    private static @Nullable JetpackWornModels tail;

    public static JetpackWornModels standard() {
        if (standard == null) standard = new JetpackWornModels(declareStandard());
        return standard;
    }

    public static JetpackWornModels bj() {
        if (bj == null) bj = new JetpackWornModels(declareBj());
        return bj;
    }

    public static JetpackWornModels wings() {
        if (wings == null) wings = new JetpackWornModels(declareWings());
        return wings;
    }

    /** 1.7 {@code ModelArmorWingsPheo}'s pack (its flames are drawn by the caller). */
    public static JetpackWornModels axePack() {
        if (axePack == null) axePack = new JetpackWornModels(declareRooted(AXEPACK_MODEL, AXEPACK));
        return axePack;
    }

    /** 1.7 {@code ModelArmorTailPeep}. */
    public static JetpackWornModels tail() {
        if (tail == null) tail = new JetpackWornModels(declareRooted(TAIL_MODEL, TAIL));
        return tail;
    }

    /** A declaration holds the meshes it names, so a reload has to rebuild the tree to pick them up. */
    public static void clear() {
        standard = null;
        bj = null;
        wings = null;
        axePack = null;
        tail = null;
    }

    /** The colour a standard pack wears, by the name the item's path resolves to. */
    public static Surface pack(String kind) {
        return PACKS.computeIfAbsent(kind, name -> Surface.cutoutCull(switch (name) {
            case "standard_blue" -> "hbm:textures/armor/jetpackblue.png";
            case "standard_green" -> "hbm:textures/armor/jetpackgreen.png";
            case "standard_boost" -> "hbm:textures/armor/jetpack.png";
            default -> "hbm:textures/armor/jetpackred.png";
        }));
    }

    public void submit(PoseStack poseStack, SubmitSink sink, int light, ModelPart bone) {
        submit(poseStack, sink, light, bone, BJ, 0F, 0F);
    }

    public void submit(PoseStack poseStack, SubmitSink sink, int light, ModelPart bone, Surface skin) {
        submit(poseStack, sink, light, bone, skin, 0F, 0F);
    }

    public void submit(PoseStack poseStack, SubmitSink sink, int light, ModelPart bone, Surface skin,
            float sweep, float trail) {
        pose.bone = bone;
        pose.skin = skin;
        pose.sweep = sweep;
        pose.trail = trail;
        Gem.replay(model, pose, poseStack, sink, light);
    }

    /**
     * {@code ModelRenderer}'s rooted form: the geometry is local to the bone, so the pivot is not
     * translated back after the rotation the way an absolute armour part's is.
     */
    private static Matrix4f rooted(ModelPart bone) {
        return new Matrix4f()
                .translate(bone.x * PIXEL, bone.y * PIXEL, bone.z * PIXEL)
                .rotateZ(bone.zRot)
                .rotateY(bone.yRot)
                .rotateX(bone.xRot);
    }

    private static Gem.Model<Pose> declareStandard() {
        WavefrontObj obj = WavefrontObj.load(JetpackClientVisuals.JETPACK_MODEL);
        Gem.Model<Pose> declaration = Gem.model();
        declaration.part(obj.all(), pack("standard"))
                .skin(state -> state.skin)
                .visibleIf(state -> state.bone.visible)
                .pose(state -> rooted(state.bone))
                .scale(PIXEL);
        return declaration.seal();
    }

    private static Gem.Model<Pose> declareBj() {
        WavefrontObj obj = WavefrontObj.load(JetpackClientVisuals.BJ_MODEL);
        Gem.Model<Pose> declaration = Gem.model();
        declaration.part(obj.part("Jetpack"), BJ)
                .pose(state -> rooted(state.bone))
                .translate(0F, 0F, -0.1F)
                .scale(PIXEL);
        return declaration.seal();
    }

    /** {@code body.copyTo(part); part.render(scale)}: the whole mesh riding the torso bone. */
    private static Gem.Model<Pose> declareRooted(Identifier model, Surface surface) {
        WavefrontObj obj = WavefrontObj.load(model);
        Gem.Model<Pose> declaration = Gem.model();
        declaration.part(obj.all(), surface)
                .pose(state -> rooted(state.bone))
                .scale(PIXEL);
        return declaration.seal();
    }

    private static Gem.Model<Pose> declareWings() {
        WavefrontObj obj = WavefrontObj.load(JetpackClientVisuals.WINGS_MODEL);
        Gem.Model<Pose> declaration = Gem.model();
        Gem.Part<Pose> back = declaration.joint()
                .pose(state -> rooted(state.bone))
                .translate(0F, -2F * PIXEL, 0F);
        wing(back, obj, 1, "Left");
        wing(back, obj, -1, "Right");
        return declaration.seal();
    }

    /** One wing: the base hinge, then the tip hinge riding on it. {@code side} mirrors the whole thing. */
    private static void wing(Gem.Part<Pose> back, WavefrontObj obj, int side, String name) {
        float pivotX = PIVOT_SIDE * PIXEL * side;
        float pivotY = PIVOT_FRONT * PIXEL;
        float pivotZ = PIVOT_Z * PIXEL;

        Gem.Part<Pose> base = back.joint()
                .rotate(Gem.Axes.Y, -INWARD_ANGLE * side)
                .translate(pivotX, pivotY, pivotZ)
                .angle(Gem.Axes.Y, state -> state.sweep * 0.5F * side)
                .angle(Gem.Axes.Z, state -> (state.sweep + 5F) * side)
                .rotate(Gem.Axes.X, 45F)
                .translate(-pivotX, -pivotY, -pivotZ)
                .translate(pivotX, pivotY, pivotZ)
                .angle(Gem.Axes.Z, state -> state.sweep * side)
                .translate(-pivotX, -pivotY, -pivotZ);
        base.child(obj.part(name + "Base"), WINGS).skin(JetpackWornModels::wingSkin).scale(PIXEL);

        Gem.Part<Pose> tip = base.joint()
                .translate(TIP_SIDE * PIXEL * side, pivotY, TIP_Z * PIXEL)
                .angle(Gem.Axes.Y, state -> state.trail * side)
                .angle(Gem.Axes.Z, state -> (state.trail * 0.25F + 5F) * side)
                .translate(-TIP_SIDE * PIXEL * side, -pivotY, -TIP_Z * PIXEL);
        tip.child(obj.part(name + "Tip"), WINGS).skin(JetpackWornModels::wingSkin).scale(PIXEL);
    }

    /** Murk and limp wings pass no skin (1.7's default {@code wings_murk}); the special players pass theirs. */
    private static Surface wingSkin(Pose state) {
        return state.skin == null ? WINGS : state.skin;
    }
}
