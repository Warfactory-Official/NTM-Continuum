package com.hbm.client.render.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import dev.golem.core.render.display.Pose17;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The first-person body of a Sedna weapon.
 *
 * <p>A held weapon is not posed by vanilla's item display transforms at all: it is drawn in camera
 * space, as 1.12's cancelled {@code RenderHandEvent} did — {@link PoseStack#setIdentity()} then the
 * camera's rotation. Expressed as a {@code display{}} slot instead it is the wrong size, in the wrong
 * place, rotating about the wrong pivot. On that frame come upstream's three layers in order: turn
 * lag, walk sway, then the weapon's own offset with its aim-down-sights lerp.
 */
public abstract class WeaponRenderer {

    protected static float interp;

    /** Phases sampled over one turn when framing an icon that spins; see {@link #staticExtents}. */
    private static final int SPIN_SAMPLES = 16;

    private final WeaponPose pose = new WeaponPose();
    private Gem.@Nullable Model<WeaponPose> model;

    protected abstract void setup(Gem.Model<WeaponPose> declaration);

    /** Built on first use, which is after resources load and so after {@link #setup} can name groups. */
    public final Gem.Model<WeaponPose> gem() {
        Gem.Model<WeaponPose> built = model;
        if (built == null) {
            built = Gem.model();
            built.overlay(state -> state.overlay);
            setup(built);
            built.seal();
            model = built;
        }
        return built;
    }

    /** The pose arrives carrying vanilla's item transform, which is discarded: this is camera space. */
    public void submitFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector, int light,
            int overlay, boolean otherArm) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        interp = dev.golem.core.client.Frames.partialTick();

        pose.pushPose();
        dev.golem.core.client.HandSpace.enterCameraSpace(pose);
        if (otherArm) pose.scale(-1F, 1F, 1F);
        setupTransformsAndSubmit(stack, player, pose, collector, light, overlay, otherArm);
        pose.popPose();
    }

    /** Turn lag, walk sway, then the weapon's own offset — upstream's {@code setupTransformsAndRender}. */
    protected void setupTransformsAndSubmit(ItemStack stack, LocalPlayer player, PoseStack pose,
            SubmitSink collector, int light, int overlay, boolean otherArm) {

        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

        float armPitch = Mth.lerp(interp, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(interp, player.yBobO, player.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(interp) - armPitch) * 0.1F * turnMagnitude));
        pose.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(interp) - armYaw) * 0.1F * turnMagnitude));

        pose.mulPose(Axis.YP.rotationDegrees(180));

        float distanceInterp = dev.golem.core.client.ViewBob.walkDistance(player, interp);
        float bob = dev.golem.core.client.ViewBob.bob(player, interp);
        pose.translate(
                Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * bob * 0.5F * swayMagnitude,
                -Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod) * bob) * swayMagnitude,
                0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * bob * 3.0F));
        pose.mulPose(Axis.XP.rotationDegrees(
                Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod - 0.2F) * bob) * 5.0F));

        setupFirstPerson(stack, pose);
        renderFirstPerson(stack, pose, collector, light, overlay,
                otherArm ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** Where the weapon sits relative to the camera. Overridden per weapon. */
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);
        if (Minecraft.getInstance().player.isShiftKeyDown()) {
            pose.translate(0, -3.875 / 8D, 0);
        } else {
            float offset = 0.8F;
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(offset, -0.75F * offset, -0.5F * offset);
            pose.mulPose(Axis.YP.rotationDegrees(180));
        }
    }

    /** The upstream FOV adjustment for this renderer; most renderers deliberately leave it unchanged. */
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return fov;
    }

    protected static float aimingFov(ItemStack stack, float fov, float partialTick, float zoom) {
        return ItemGunBaseNT.applyAimingFov(fov, partialTick, zoom);
    }

    /** Lerps between the hip position and the sighted one by how far the aim transition has run. */
    public static void standardAimingTransform(ItemStack stack, PoseStack pose,
            double sX, double sY, double sZ, double aX, double aY, double aZ) {
        pose.translate(aimLerp(sX, aX), aimLerp(sY, aY), aimLerp(sZ, aZ));
    }

    protected static float aimLerp(double hip, double sighted) {
        float aimingProgress = ItemGunBaseNT.prevAimingProgress
                + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
        return (float) (hip + (sighted - hip) * aimingProgress);
    }

    protected float getSwayMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F;
    }

    protected float getSwayPeriod(ItemStack stack) {
        return 0.75F;
    }

    protected float getTurnMagnitude(ItemStack stack) {
        return 2.75F;
    }

    public final void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay, InteractionHand hand) {
        replay(stack, true, pose, collector, light, overlay,
                hand == InteractionHand.OFF_HAND ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                Minecraft.getInstance().player, hand);
    }

    public final void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        renderStatic(stack, pose, collector, light, overlay, null);
    }

    public final void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay, net.minecraft.world.entity.@Nullable LivingEntity owner) {
        replay(stack, false, pose, collector, light, overlay, staticContext, owner, InteractionHand.MAIN_HAND);
    }

    private void replay(ItemStack stack, boolean animated, PoseStack poseStack, SubmitSink collector,
            int light, int overlay, ItemDisplayContext context,
            net.minecraft.world.entity.@Nullable LivingEntity owner, InteractionHand hand) {
        pose.begin(stack, animated, overlay, context, owner, hand);
        Gem.replay(gem(), pose, poseStack, collector, light);
        WeaponEffects.render(pose, poseStack, collector, light, overlay);
    }

    /**
     * The pose upstream puts a weapon in for every context that is not the first-person hand.
     *
     * <p>These used to be dropped, on the reading that the chains are GUI-space calls that would throw
     * the weapon off screen. They do — on their own. What they are missing is the matrix 1.7 had already
     * applied before it called the renderer, which is exactly what {@link Pose17.Klass#ITEM_RENDER_WEAPON}
     * supplies through the base model's {@code display{}}: with the engine slot in front of them the
     * chains land where they did in 1.7, and the fifty-two per-weapon poses they carry come back with
     * them. Dropping them instead cost every gun its own inventory and third-person framing and left one
     * hand-fitted {@code display{}} standing in for all of them.
     */
    public final void setupStatic(PoseStack pose, ItemStack stack, ItemDisplayContext context) {
        staticContext = context;
        applyStatic(pose, stack, context, guiSpinAngle());
    }

    /** The context-to-chain dispatch, without the side effect, so framing can run it too. */
    private void applyStatic(PoseStack pose, ItemStack stack, ItemDisplayContext context, float spinDegrees) {
        switch (context) {
            case GUI -> setupInv(pose, stack, spinDegrees);
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, HEAD -> setupThirdPerson(pose, stack);
            case GROUND, FIXED -> setupEntity(pose, stack);
            default -> { }
        }
    }

    /**
     * {@code ItemRenderWeaponBase.setupInv}. Written in 1.7's pixel slot — sixteen units wide, y down —
     * because that is the space {@link Pose17.Klass#ITEM_RENDER_WEAPON}'s GUI frame ends in.
     */
    protected void setupInv(PoseStack pose, ItemStack stack, float spinDegrees) {
        setupInv(pose, stack);
    }

    /**
     * Degrees per millisecond of the GUI-only spin some icons have, zero when the icon is still. The
     * angle is wrapped at a whole turn rather than at the millisecond count 1.7 wrote.
     */
    protected float guiSpinSpeed() {
        return 0F;
    }

    private float guiSpinAngle() {
        float speed = guiSpinSpeed();
        return speed == 0F ? 0F : (float) (System.currentTimeMillis() * (double) speed % 360D);
    }

    /**
     * Whether the drawing changes with time in this context. 26.1 has to be told: an icon it believes is
     * still is drawn once and blitted from a cache afterwards.
     */
    public boolean animated(ItemDisplayContext context) {
        return context == ItemDisplayContext.GUI && guiSpinSpeed() != 0F;
    }

    protected void setupInv(PoseStack pose, ItemStack stack) {
        pose.scale(1F, 1F, -1F);
        pose.translate(8F, 8F, 0F);
        pose.mulPose(Axis.ZP.rotationDegrees(225F));
        pose.mulPose(Axis.YP.rotationDegrees(90F));
    }

    /** {@code ItemRenderWeaponBase.setupThirdPerson}, which the head slot inherits. */
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        pose.scale(0.125F, 0.125F, 0.125F);
        pose.mulPose(Axis.ZP.rotationDegrees(15F));
        pose.mulPose(Axis.YP.rotationDegrees(12.5F));
        pose.mulPose(Axis.XP.rotationDegrees(15F));
        pose.translate(3.5F, 0F, 0F);
    }

    /** {@code ItemRenderWeaponBase.setupEntity} — the dropped item, and with it the frame and shelf. */
    protected void setupEntity(PoseStack pose, ItemStack stack) {
        pose.scale(0.125F, 0.125F, 0.125F);
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
    }

    /** Recorded because {@code renderStatic} is not handed it and Dani's rest pose depends on it. */
    private ItemDisplayContext staticContext = ItemDisplayContext.NONE;

    /**
     * The box the icon is framed on in one context: the model's own corners carried through the same
     * chain the draw uses.
     *
     * <p>{@link #extents} is the mesh, in mesh space. The static pose now scales it — by anything from
     * 0.375 to 7.5 — so reporting the mesh box unchanged would size the picture-in-picture rectangle for
     * a weapon nobody draws. Nothing on this path reads a clock, and the result is the union box, so the
     * rectangle 26.1 floors to whole pixels is fixed for a given stack and context.
     */
    public final Vector3fc[] staticExtents(Vector3fc[] mesh, ItemStack stack, ItemDisplayContext context) {
        int phases = animated(context) ? SPIN_SAMPLES : 1;
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Vector3f point = new Vector3f();
        for (int phase = 0; phase < phases; phase++) {
            PoseStack pose = new PoseStack();
            applyStatic(pose, stack, context, 360F / phases * phase);
            Matrix4f matrix = pose.last().pose();
            for (Vector3fc corner : mesh) {
                matrix.transformPosition(point.set(corner));
                min[0] = Math.min(min[0], point.x); max[0] = Math.max(max[0], point.x);
                min[1] = Math.min(min[1], point.y); max[1] = Math.max(max[1], point.y);
                min[2] = Math.min(min[2], point.z); max[2] = Math.max(max[2], point.z);
            }
        }
        if (min[0] > max[0]) return new Vector3fc[0];
        Vector3fc[] corners = new Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[i++] = new Vector3f(x == 0 ? min[0] : max[0], y == 0 ? min[1] : max[1],
                    z == 0 ? min[2] : max[2]);
        }
        return corners;
    }

    /** 26.1 sizes a GUI item from these; no extents means an empty box and no icon at all. */
    public abstract Vector3fc[] extents();
}
