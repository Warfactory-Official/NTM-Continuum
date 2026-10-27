package com.hbm.client.render;

import com.hbm.client.model.TextureStrip;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.DoorDecl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A machine's OBJ model posed as an inventory/hand item: a declared part tree, plus the framing
 * transform that puts it in an item slot.
 *
 * <p>The two eras disagree only about <em>who calls this</em>. 26.1 has a special-model system, so
 * {@link MachineItemModelRenderer} wraps this in a {@code NoDataSpecialModelRenderer} and its config
 * arrives through a codec. Older versions have no such system: {@code MachineItemModels} parses the same
 * item-definition JSON itself and a per-loader BEWLR draws it. Both paths end here.
 *
 * <p>The body is a {@link Gem.Model}, the same vocabulary the placed machine declares in. The frame,
 * the item display transform, stays on the pose stack where the caller left it, as a block entity's
 * placement does. {@link #extents} is the same walk as the draw, not a second traversal that could
 * drift from the first.
 */
public final class MachineItemModel {

    /** The arm pitch the old autosaw item renderer posed its icon at: {@code 80 - rotationPitch}, at rest. */
    private static final float AUTOSAW_PITCH = 80F;

    /** Phases sampled over one turn when framing a model that moves. See {@link #sampleExtents}. */
    private static final int ANIMATION_SAMPLES = 16;
    /** 1.7 {@code ItemRenderLibraryDoors}: these door items show each skin for a second, in turn. */
    private static final Map<String, DoorDecl> CYCLING_DOORS = Map.of(
            "door_skins/secure_access_door", DoorDecl.SECURE_ACCESS_DOOR, "door_skins/fire_door", DoorDecl.FIRE_DOOR,
            "door_skins/sliding_blast_door", DoorDecl.SLIDE_DOOR, "door_skins/large_vehicle_door", DoorDecl.LARGE_VEHICLE_DOOR,
            "door_skins/water_door", DoorDecl.WATER_DOOR, "door_skins/qe_containment", DoorDecl.QE_CONTAINMENT,
            "door_skins/qe_sliding_door", DoorDecl.QE_SLIDING, "door_skins/round_airlock_door", DoorDecl.ROUND_AIRLOCK_DOOR,
            "door_skins/sliding_seal_door", DoorDecl.SLIDING_SEAL_DOOR);

    /** The untextured lamp of the lantern item, tinted per frame. */
    private static final Surface LANTERN_LIGHT = Surface.solid("hbm:textures/models/misc/white.png");

    private final WavefrontObj model;
    private final Surface surface;
    private final TextureStrip strip;
    private final float scale;
    /** Uses the literal legacy item transform instead of the block-space framing convention. */
    private final boolean rawLegacyTransform;
    /** Nested {@code minecraft:special} models retain the block-item framing convention. */
    private final boolean blockFramed;
    private final float yaw;
    /**
     * Rotation about X, applied before {@link #yaw}. Zero for machines; the armour suits need 180
     * because their OBJs are authored in the biped's flipped space (the helmet sits at negative Y),
     * which is the same {@code rotate(180, 1, 0, 0)} 1.12's {@code setupRenderInv} opened with.
     */
    private final float pitch;
    private final float ty;
    /** Legacy item translation applied in item space, before the model's facing rotation. */
    private final Vector3f itemTranslate;
    /** Legacy pre-translation, rotated by yaw but not scaled. */
    private final Vector3f itemPreTranslate;
    /** Extra transform applied only by the legacy inventory/GUI path. */
    private final Vector3f guiTranslate;
    private final float guiScale;
    private final float guiYaw;
    private final Set<String> exclude;
    /** The caller's exclusions only: what the icon is framed on. See the constructor. */
    private final Set<String> userExclude;
    /** Optional second texture + the parts drawn with it (e.g. a tank body vs its frame); may be null. */
    private final @Nullable Surface overlaySurface;
    private final List<String> overlayParts;
    /** An opaque pass for parts whose legacy item renderer drew with texture sampling disabled. */
    private final @Nullable Surface blackSurface;
    private final List<String> blackParts;
    /** Parts drawn with an extra X-axis rotation about {@link #rotatePivot} (e.g. the mining laser barrel,
     * so the item icon poses it like the placed machine); rendered separately from the static passes. */
    private final List<String> rotateParts;
    private final float rotateX;
    private final Vector3f rotatePivot;
    /** Parts that use the legacy inventory spin around {@link #spinPivot}. */
    private final List<String> spinParts;
    private final Vector3f spinPivot;
    /** Multi-texture mode ({@code "mtl": true}): material name maps to its surface, from the OBJ's .mtl. */
    private final Map<String, Surface> materialSurfaces;
    /** Explicit OBJ-part texture overrides for models whose sockets do not use an .mtl file. */
    private final Map<String, Surface> partSurfaces;
    /** Whether this item has the old continuously rotating inventory pose. */
    private final boolean spin;
    private final Map<String, ContextTransform> contextTransforms;
    /** Optional named item animation. The pump animation keeps all moving parts on one rotor angle. */
    private final String animation;
    /** Parts selected from the primary OBJ when ordered model passes are declared. */
    private final List<String> primaryParts;
    /** Local translation of the primary ordered pass. */
    private final Vector3f primaryTranslation;
    /** Additional ordered OBJ/texture passes. Empty for the existing single-OBJ contract. */
    private final List<ModelPass> passes;
    /**
     * Degrees per millisecond for the old GUI-only continuous spin, zero when unused. The wrap is taken at a
     * whole turn. 1.7 wrote each of these as its own millisecond expression, and the two rates in the tree
     * ({@code % 3600 / 10} and {@code / 15 % 360}) differ in period as well as in speed.
     */
    private final float guiSpinSpeed;

    private final ItemGem body;
    /** Framing boxes, one per display context; see {@link #sampleExtents}. */
    private final HashMap<ItemDisplayContext, Vector3fc[]> extentsByContext = new HashMap<>();

    /** A legacy renderer adjustment applied after the context display matrix and before the model pose. */
    public record ContextTransform(Vector3f rotation, Vector3f translation, float scale, boolean rawMesh) {
        public ContextTransform(Vector3f rotation, Vector3f translation, float scale) {
            this(rotation, translation, scale, false);
        }

        public ContextTransform {
            if (rawMesh && (rotation.lengthSquared() != 0F || translation.lengthSquared() != 0F || scale != 1F)) {
                throw new IllegalArgumentException("raw_mesh is a body policy, not a numeric context transform");
            }
        }

        public void apply(PoseStack poseStack) {
            if (rawMesh) throw new IllegalArgumentException("raw_mesh is supported only for NONE");
            if (rotation.x() != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
            if (rotation.y() != 0F) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
            if (rotation.z() != 0F) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            poseStack.translate(translation.x(), translation.y(), translation.z());
            if (scale != 1F) poseStack.scale(scale, scale, scale);
        }
    }

    /** One ordered OBJ pass, optionally restricted to named parts and locally translated. */
    public record ModelPass(WavefrontObj model, Surface surface, List<String> parts, Vector3f translation,
                           List<PassStep> steps, int tint, float scale) {

        /** No tint: the pass is drawn in its texture's own colours. */
        public static final int UNTINTED = -1;

        public ModelPass(WavefrontObj model, Surface surface, List<String> parts, Vector3f translation,
                         List<PassStep> steps) {
            this(model, surface, parts, translation, steps, UNTINTED, 1F);
        }

        public ModelPass(WavefrontObj model, Surface surface, List<String> parts, Vector3f translation) {
            this(model, surface, parts, translation, List.of());
        }
    }

    /**
     * One step of a pass's own transform, in declaration order.
     *
     * <p>1.7's item renderers never pop between the pieces they draw, so a later piece inherits every
     * turn the ones before it made. A declared pass is a sibling instead, which is why a pass that is not
     * the first sometimes has to state a whole chain rather than one offset.
     */
    public record PassStep(boolean rotate, Vector3f value) {}

    /**
     * One step of the GUI-only tail of a 1.7 {@code renderInventory}, applied in order after {@code guiTranslate},
     * {@code guiScale} and {@code guiYaw}. {@code kind} is {@code rotate} (Euler degrees, X then Y then Z),
     * {@code translate}, {@code scale} (per axis, so 1.7's {@code glScaled(s, s, -s)} survives) or {@code spin}:
     * the GUI spin angle about {@code value} as an axis. A model with a spin step does not also take the default
     * trailing Y spin. Exists for renderers whose {@code renderInventory} turned the model after scaling it
     * (the gear/sawblade wheel: {@code Ry(-45) Rx(30)} then a Z spin about the hub).
     */
    public record GuiStep(String kind, Vector3f value) {}

    /** The GUI-only tail; see {@link GuiStep}. Set once, right after construction, by the item-definition readers. */
    private List<GuiStep> guiSteps = List.of();

    /** Sets the GUI-only renderInventory tail. Called by the item-definition readers before the model is drawn. */
    public MachineItemModel withGuiSteps(List<GuiStep> steps) {
        this.guiSteps = List.copyOf(steps);
        extentsByContext.clear();
        return this;
    }

    public MachineItemModel(WavefrontObj model, Surface surface, float scale, boolean rawLegacyTransform,
                            boolean blockFramed,
                            float yaw, float pitch, float ty,
                            Vector3f itemTranslate, Vector3f itemPreTranslate, Vector3f guiTranslate, float guiScale,
                            float guiYaw, Set<String> exclude,
                            @Nullable Surface overlaySurface, List<String> overlayParts,
                            @Nullable Surface blackSurface, List<String> blackParts,
                              List<String> rotateParts, float rotateX, Vector3f rotatePivot,
                              List<String> spinParts, Vector3f spinPivot, Map<String, Surface> materialSurfaces,
                              Map<String, Surface> partSurfaces,
                              boolean spin, Map<String, ContextTransform> contextTransforms, String animation) {
        this(model, surface, scale, rawLegacyTransform, blockFramed, yaw, pitch, ty,
                itemTranslate, itemPreTranslate, guiTranslate, guiScale, guiYaw, exclude,
                overlaySurface, overlayParts, blackSurface, blackParts, rotateParts, rotateX, rotatePivot,
                spinParts, spinPivot, materialSurfaces, partSurfaces, spin, contextTransforms, animation,
                List.of(), new Vector3f(), List.of(), 0F);
    }

    public MachineItemModel(WavefrontObj model, Surface surface, float scale, boolean rawLegacyTransform,
                             boolean blockFramed,
                             float yaw, float pitch, float ty,
                             Vector3f itemTranslate, Vector3f itemPreTranslate, Vector3f guiTranslate, float guiScale,
                             float guiYaw, Set<String> exclude,
                             @Nullable Surface overlaySurface, List<String> overlayParts,
                             @Nullable Surface blackSurface, List<String> blackParts,
                             List<String> rotateParts, float rotateX, Vector3f rotatePivot,
                             List<String> spinParts, Vector3f spinPivot, Map<String, Surface> materialSurfaces,
                             Map<String, Surface> partSurfaces,
                             boolean spin, Map<String, ContextTransform> contextTransforms, String animation,
                             List<String> primaryParts, Vector3f primaryTranslation, List<ModelPass> passes,
                             float guiSpinSpeed) {
        this.model = model;
        this.surface = surface;
        this.strip = TextureStrip.of(Ids.parse(surface.texture()));
        this.scale = scale;
        this.rawLegacyTransform = rawLegacyTransform;
        this.blockFramed = blockFramed;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ty = ty;
        this.itemTranslate = itemTranslate;
        this.itemPreTranslate = itemPreTranslate;
        this.guiTranslate = guiTranslate;
        this.guiScale = guiScale;
        this.guiYaw = guiYaw;
        this.overlaySurface = overlaySurface;
        this.overlayParts = overlayParts;
        this.blackSurface = blackSurface;
        this.blackParts = blackParts;
        this.rotateParts = rotateParts;
        this.rotateX = rotateX;
        this.rotatePivot = rotatePivot;
        this.spinParts = spinParts;
        this.spinPivot = spinPivot;
        this.materialSurfaces = materialSurfaces;
        this.partSurfaces = Map.copyOf(partSurfaces);
        this.spin = spin;
        this.contextTransforms = Map.copyOf(contextTransforms);
        this.animation = animation;
        this.primaryParts = List.copyOf(primaryParts);
        this.primaryTranslation = primaryTranslation;
        this.passes = List.copyOf(passes);
        this.guiSpinSpeed = guiSpinSpeed;
        // Framing is centred on what is actually drawn, which is everything except the caller's own
        // exclusions, not the merged set below, since overlay and rotated parts are still on screen.
        this.userExclude = Set.copyOf(exclude);
        // The overlay parts are textured separately, so the base pass must skip them. Rotated parts are
        // also drawn in their own pass, so exclude them from both static passes.
        Set<String> merged = new HashSet<>(exclude);
        if (overlaySurface != null && !overlayParts.isEmpty()) merged.addAll(overlayParts);
        if (blackSurface != null && !blackParts.isEmpty()) merged.addAll(blackParts);
        merged.addAll(rotateParts);
        merged.addAll(spinParts);
        if (spinParts.contains("Stirling")) merged.addAll(List.of("Cog", "CogSmall", "Piston"));
        this.exclude = merged;
        this.body = declare();
    }

    public Gem.Model<ItemPose> gem() {
        return body.model();
    }

    /** Draws the model, framed for an item slot. The pose is left as it was found. */
    public void submit(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords) {
        submit(poseStack, sink, lightCoords, overlayCoords, null);
    }

    /** Draws the model with the optional display context used by the context-aware item-model path. */
    public void submit(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords,
                       @Nullable ItemDisplayContext context) {
        poseStack.pushPose();
        applyTransform(poseStack, context);
        body.submit(poseStack, sink, lightCoords, overlayCoords);
        poseStack.popPose();
    }

    /** The eight transformed corners of the model's bounding box (26.1 uses these to size the icon). */
    public void extents(Consumer<Vector3fc> output) {
        extents(output, null);
    }

    public void extents(Consumer<Vector3fc> output, @Nullable ItemDisplayContext context) {
        for (Vector3fc corner : extents(context)) output.accept(corner);
    }

    public Vector3fc[] extents(@Nullable ItemDisplayContext context) {
        return extentsByContext.computeIfAbsent(context, this::sampleExtents);
    }

    /**
     * The box the icon is framed on, held once it is known.
     *
     * <p>26.1 turns this box into the oversized-item rectangle in whole GUI pixels (floored corner, ceiled
     * size). A box taken at the current instant would quantise differently from frame to frame, and that
     * rectangle is both the picture-in-picture texture and the offset the model is centred at, so the drawing
     * would jump a pixel at a time. So the box is the one the whole animation stays inside: the union over a
     * turn of the GUI spin and a cycle of the body, reduced to one axis-aligned box.
     */
    private Vector3fc[] sampleExtents(@Nullable ItemDisplayContext context) {
        int spins = context == ItemDisplayContext.GUI && guiSpinSpeed != 0F ? ANIMATION_SAMPLES : 1;
        int phases = animatedBody() ? ANIMATION_SAMPLES : 1;
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Consumer<Vector3fc> include = point -> {
            min[0] = Math.min(min[0], point.x());
            min[1] = Math.min(min[1], point.y());
            min[2] = Math.min(min[2], point.z());
            max[0] = Math.max(max[0], point.x());
            max[1] = Math.max(max[1], point.y());
            max[2] = Math.max(max[2], point.z());
        };
        for (int spin = 0; spin < spins; spin++) {
            PoseStack framed = new PoseStack();
            applyTransform(framed, context, 360F / spins * spin);
            for (int phase = 0; phase < phases; phase++) {
                body.extents(framed, 360F / phases * phase, include);
            }
        }
        if (min[0] > max[0]) return new Vector3fc[0];
        List<Vector3fc> corners = new ArrayList<>(8);
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            corners.add(new Vector3f(xi == 0 ? min[0] : max[0], yi == 0 ? min[1] : max[1],
                    zi == 0 ? min[2] : max[2]));
        }
        return corners.toArray(Vector3fc[]::new);
    }

    /**
     * Whether the drawing changes with time in this context. 26.1 has to be told: an icon it believes is
     * still is drawn once and blitted from a cache afterwards.
     */
    public boolean animated(@Nullable ItemDisplayContext context) {
        return animatedBody() || (context == ItemDisplayContext.GUI && guiSpinSpeed != 0F);
    }

    /** Whether the declared body itself moves: the branches of {@link #declare()} that read the clock. */
    private boolean animatedBody() {
        return "pump".equals(animation) || "autosaw".equals(animation) || "radar_large".equals(animation)
                || "lantern".equals(animation) || "thresher".equals(animation) || CYCLING_DOORS.containsKey(animation)
                || (spin && !spinParts.isEmpty()) || strip.frames() > 1;
    }

    private ItemGem declare() {
        ItemGem.Builder builder = ItemGem.builder();
        if ("pump".equals(animation)) {
            pump(builder);
        } else if ("autosaw".equals(animation)) {
            autosaw(builder);
        } else if ("radar_large".equals(animation)) {
            radarLarge(builder);
        } else if ("lantern".equals(animation)) {
            lantern(builder);
        } else if ("thresher".equals(animation)) {
            thresher(builder);
        } else if ("rbmk_gauge".equals(animation)) {
            RBMKGaugeIcon.declare(builder, model, surface, partSurfaces);
        } else if (RBMKPanelIcon.handles(animation)) {
            RBMKPanelIcon.declare(builder, animation, model, surface, partSurfaces);
        } else if ("armor_bj_jetpack_chest".equals(animation) && !partSurfaces.isEmpty()) {
            jetpackChest(builder);
        } else if (!partSurfaces.isEmpty()) {
            sockets(builder);
        } else if (!materialSurfaces.isEmpty()) {
            materials(builder);
        } else {
            body(builder);
            black(builder);
            overlay(builder);
            rotated(builder);
            spinning(builder);
        }
        return builder.build();
    }

    /** The old pump item renderer moves every pump part from one shared millisecond-derived rotor angle. */
    private void pump(ItemGem.Builder builder) {
        named(builder, "Base", surface);
        named(builder, "Rotor", surface).spin(Gem.Axes.Z, 0F, 2.25F, 0F, state -> state.animation - 90F);
        named(builder, "Arms", surface)
                .slide(Gem.Axes.Y, MachineItemModel::pumpLift)
                .spin(Gem.Axes.Z, 0F, 4.75F, 0F, state -> -pumpArmAngle(state));
        named(builder, "Piston", surface).slide(Gem.Axes.Y, MachineItemModel::pumpLift);
    }

    /**
     * 1.7 {@code RenderRadarLarge}'s item: the {@code Radar} body, then the {@code Dish} turned
     * {@code currentTimeMillis % 3600 * 0.1} degrees about -Y (one turn in 3.6 s) - the item clock exactly (G-C).
     */
    private void radarLarge(ItemGem.Builder builder) {
        named(builder, "Radar", surface);
        named(builder, "Dish", surface).angle(Gem.Axes.Y, state -> -state.animation);
    }

    /**
     * 1.7 {@code RenderLantern}'s item: the {@code Lantern} body in its skin, then the {@code Light} untextured in
     * {@code (1, 1, 0.7)} times the wall-clock pulse {@code sin(millis / 200) / 2 + 0.5} scaled into 0.9..1.0. The
     * item renderer (unlike the placed one) leaves lighting on, so the lamp is shaded like the body.
     */
    private void lantern(ItemGem.Builder builder) {
        named(builder, "Lantern", surface);
        Set<String> light = Set.of("Light");
        builder.part(model.parts(light, false), LANTERN_LIGHT, model.boundsIncluding(light))
                .tint(state -> {
                    float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2D + 0.5D) * 0.1F + 0.9F;
                    int r = (int) (255F * mult);
                    int b = (int) (255F * 0.7F * mult);
                    return 0xFF000000 | (r << 16) | (r << 8) | b;
                });
    }

    private static float pumpArmAngle(ItemPose state) {
        double cos = Math.cos(state.animation * Math.PI / 180D) * 0.5D;
        return (float) (Math.acos(cos / 2D) * 180D / Math.PI - 90D);
    }

    /**
     * The autosaw icon is the placed machine's own arm chain, parked at the pitch the old item renderer
     * asked for and with the blade turning. Drawing the OBJ flat leaves the three segments lying in a
     * straight line, which is a pose the machine never holds.
     */
    private void autosaw(ItemGem.Builder builder) {
        Gem.Part<ItemPose> ground = builder.joint();
        segment(builder, ground, "Base");
        segment(builder, ground, "Main");
        segment(builder, ground, "Engine");

        Gem.Part<ItemPose> upper = ground.joint()
                .translate(0F, 1.75F, 0F)
                .rotate(Gem.Axes.X, AUTOSAW_PITCH)
                .translate(0F, -1.75F, 0F);
        segment(builder, upper, "ArmUpper");

        Gem.Part<ItemPose> lower = upper.joint()
                .translate(0F, 1.75F, -4F)
                .rotate(Gem.Axes.X, -AUTOSAW_PITCH * 2F)
                .translate(0F, -1.75F, 4F)
                .translate(-0.01F, 0F, 0F);
        segment(builder, lower, "ArmLower");

        Gem.Part<ItemPose> tip = lower.joint()
                .translate(0.01F, 0F, 0F)
                .translate(0F, 1.75F, -8F)
                .rotate(Gem.Axes.X, AUTOSAW_PITCH)
                .translate(0F, -1.75F, 8F);
        segment(builder, tip, "ArmTip");

        Gem.Part<ItemPose> blade = tip.joint()
                .translate(0F, 1.75F, -10F)
                .angle(Gem.Axes.Y, state -> -state.animation)
                .translate(0F, -1.75F, 10F);
        segment(builder, blade, "Sawblade");
    }

    /**
     * 1.7 {@code RenderThresher}'s item: {@code renderCommon(80, millis % 3600 * 0.25, 0)}, the placed machine's arm
     * chain parked at 80 degrees with the wheel turning and the engine at rest. Drawing the OBJ flat instead lays
     * the arm out straight, which is a pose the machine never holds and spills the icon into the next slot.
     */
    private void thresher(ItemGem.Builder builder) {
        float angle = 80F;
        Gem.Part<ItemPose> ground = builder.joint();
        segment(builder, ground, "Base");
        segment(builder, ground, "Engine");

        Gem.Part<ItemPose> upper = ground.joint()
                .translate(0F, 0.5F, -1F)
                .rotate(Gem.Axes.X, angle)
                .translate(0F, -0.5F, 1F);
        segment(builder, upper, "ArmUpper");

        Gem.Part<ItemPose> lower = upper.joint()
                .translate(0F, 0.5F, -5F)
                .rotate(Gem.Axes.X, -angle * 2F)
                .translate(0F, -0.5F, 5F)
                .translate(-0.01F, 0F, 0F);
        segment(builder, lower, "ArmLower");

        Gem.Part<ItemPose> front = lower.joint()
                .translate(0.01F, 0F, 0F)
                .translate(0F, 0.5F, -9F)
                .rotate(Gem.Axes.X, angle)
                .translate(0F, -0.5F, 9F)
                .translate(0.01F, 0F, 0F);
        segment(builder, front, "Front");

        // the item clock is millis % 3600 * 0.1; the thresher's wheel turned at * 0.25
        Gem.Part<ItemPose> wheel = front.joint()
                .translate(0F, 0.5F, -11F)
                .angle(Gem.Axes.X, state -> -state.animation * 2.5F)
                .translate(0F, -0.5F, 11F);
        segment(builder, wheel, "Wheel");
    }

    private void segment(ItemGem.Builder builder, Gem.Part<ItemPose> parent, String part) {
        builder.child(parent, model.part(part), surface, model.boundsIncluding(Set.of(part)));
    }

    private static float pumpLift(ItemPose state) {
        double sin = Math.sin(state.animation * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(state.animation * Math.PI / 180D) * 0.5D;
        return (float) (1D - Math.sqrt(1D + (cos * cos) / 2D) + sin);
    }

    /**
     * The BJ jetpack chestplate: the suit's own three sockets, then the pack a tenth of a unit behind
     * them. That offset is the whole reason this is not the ordinary socket branch.
     */
    private void jetpackChest(ItemGem.Builder builder) {
        Set<String> framed = new LinkedHashSet<>();
        Gem.Part<ItemPose> first = null;
        for (String part : List.of("Body", "LeftArm", "RightArm")) {
            Gem.Part<ItemPose> declared = socket(builder, part);
            if (declared == null) continue;
            if (first == null) first = declared;
            framed.add(part);
        }
        if (first != null) builder.frame(first, model.boundsIncluding(framed));

        Gem.Part<ItemPose> pack = socket(builder, "Jetpack");
        if (pack != null) {
            pack.translate(0F, 0F, -0.1F);
            builder.frame(pack, model.boundsIncluding(Set.of("Jetpack")));
        }
    }

    private void sockets(ItemGem.Builder builder) {
        Set<String> framed = new LinkedHashSet<>();
        Gem.Part<ItemPose> first = null;
        for (String part : partSurfaces.keySet()) {
            Gem.Part<ItemPose> declared = socket(builder, part);
            if (declared == null) continue;
            if (first == null) first = declared;
            framed.add(part);
        }
        if (first != null) builder.frame(first, model.boundsIncluding(framed));
    }

    private Gem.@Nullable Part<ItemPose> socket(ItemGem.Builder builder, String part) {
        Surface skin = partSurfaces.get(part);
        if (skin == null || userExclude.contains(part)) return null;
        // The partTextures route always seeds a "Body" skin from the main texture; an OBJ without a
        // Body part (the crane partitioner) must not fail to bake over that implicit entry.
        if (!model.partNames().contains(part)) return null;
        return builder.part(model.part(part), skin, null);
    }

    private void materials(ItemGem.Builder builder) {
        Gem.Part<ItemPose> first = null;
        for (Map.Entry<String, Surface> entry : materialSurfaces.entrySet()) {
            Gem.Part<ItemPose> declared = builder.part(model.material(entry.getKey()), entry.getValue(), null);
            if (first == null) first = declared;
        }
        if (first != null) builder.frame(first, model.boundsIncludingMaterials(materialSurfaces.keySet()));
    }

    private void body(ItemGem.Builder builder) {
        // The declared "parts"/"translate" hold without extra passes too (charger, red pylons, spotlights...):
        // so this pass takes them; List.of() and a zero vector would draw every OBJ group, unmoved.
        Gem.Part<ItemPose> primary = pass(builder, model, surface, primaryParts, exclude, primaryTranslation, List.of());
        if (primary != null && strip.frames() > 1) {
            Geometry[] frames = new Geometry[strip.frames()];
            for (int i = 0; i < frames.length; i++) {
                frames[i] = geometry(model.stripFrame(i, frames.length), primaryParts, exclude);
            }
            primary.mesh(state -> frames[strip.frameAt(System.currentTimeMillis() / 50L)]);
        }
        DoorDecl door = CYCLING_DOORS.get(animation);
        if (primary != null && door != null) {
            Map<Identifier, Surface> skins = new HashMap<>();
            boolean noCull = surface.blend() == Surface.Blend.CUTOUT_NO_CULL;
            for (Identifier skin : door.getSEDNASkins()) {
                skins.put(skin, noCull ? Surface.cutoutNoCull(skin.toString()) : Surface.cutout(skin.toString()));
            }
            primary.skin(state -> skins.get(door.getCyclingSkins(System.currentTimeMillis())));
        }
        if (passes.isEmpty()) return;
        for (ModelPass extra : passes) {
            Gem.Part<ItemPose> declared = pass(builder, extra.model(), extra.surface(), extra.parts(), userExclude,
                    extra.translation(), extra.steps());
            if (declared == null) continue;
            if (extra.scale() != 1F) declared.scale(extra.scale());
            // 1.7 ObjUtil.setColor: the RGB multiplies the pass, opaque.
            if (extra.tint() != ModelPass.UNTINTED) {
                int argb = 0xFF000000 | (extra.tint() & 0xFFFFFF);
                declared.tint(state -> argb);
            }
        }
    }

    private void pass(ItemGem.Builder builder, WavefrontObj source, Surface skin, List<String> parts,
                      Set<String> excluded, Vector3f translation) {
        pass(builder, source, skin, parts, excluded, translation, List.of());
    }

    private Gem.@Nullable Part<ItemPose> pass(ItemGem.Builder builder, WavefrontObj source, Surface skin,
                                              List<String> parts, Set<String> excluded, Vector3f translation,
                                              List<PassStep> steps) {
        Geometry geometry = geometry(source, parts, excluded);
        if (geometry == null) return null;
        float[][] box = parts.isEmpty() ? source.boundsExcludingExact(excluded)
                : source.boundsIncluding(chosen(source, parts, excluded));
        Gem.Part<ItemPose> declared = builder.part(geometry, skin, box);
        if (translation.x != 0F || translation.y != 0F || translation.z != 0F) {
            declared.translate(translation.x, translation.y, translation.z);
        }
        for (PassStep step : steps) {
            Vector3f value = step.value();
            if (!step.rotate()) {
                declared.translate(value.x, value.y, value.z);
                continue;
            }
            if (value.x != 0F) declared.rotate(Gem.Axes.X, value.x);
            if (value.y != 0F) declared.rotate(Gem.Axes.Y, value.y);
            if (value.z != 0F) declared.rotate(Gem.Axes.Z, value.z);
        }
        return declared;
    }

    private static @Nullable Geometry geometry(WavefrontObj source, List<String> parts, Set<String> excluded) {
        if (parts.isEmpty()) return source.allExcept(excluded);
        Set<String> chosen = chosen(source, parts, excluded);
        return chosen.isEmpty() ? null : source.parts(chosen, false);
    }

    /** 1.7 renderPart drew nothing for a name the OBJ lacks; a pass may name none of its parts. */
    private static Set<String> chosen(WavefrontObj source, List<String> parts, Set<String> excluded) {
        Set<String> chosen = remaining(parts, excluded);
        chosen.retainAll(source.partNames());
        return chosen;
    }

    private void black(ItemGem.Builder builder) {
        if (blackSurface == null) return;
        Set<String> chosen = remaining(blackParts, Set.copyOf(rotateParts));
        if (chosen.isEmpty()) return;
        builder.part(model.parts(chosen, false), blackSurface, model.boundsIncluding(chosen))
                .tint(state -> 0xFF000000);
    }

    private void overlay(ItemGem.Builder builder) {
        if (overlaySurface == null) return;
        Set<String> chosen = remaining(overlayParts, Set.copyOf(rotateParts));
        if (chosen.isEmpty()) return;
        builder.part(model.parts(chosen, false), overlaySurface, model.boundsIncluding(chosen));
    }

    /** The same X-rotation the placed machine applies, about the connection pivot. */
    private void rotated(ItemGem.Builder builder) {
        if (rotateParts.isEmpty()) return;
        Set<String> chosen = new LinkedHashSet<>(rotateParts);
        builder.part(model.parts(chosen, false), overlaySurface != null ? overlaySurface : surface,
                        model.boundsIncluding(chosen))
                .translate(rotatePivot.x, rotatePivot.y, rotatePivot.z)
                .rotate(Gem.Axes.X, rotateX)
                .translate(-rotatePivot.x, -rotatePivot.y, -rotatePivot.z);
    }

    private void spinning(ItemGem.Builder builder) {
        if (!spin || spinParts.isEmpty()) return;
        if (spinParts.contains("Stirling")) {
            stirling(builder);
            return;
        }
        Set<String> chosen = remaining(spinParts, userExclude);
        if (chosen.isEmpty()) return;
        builder.part(model.parts(chosen, false), surface, model.boundsIncluding(chosen))
                .spin(Gem.Axes.Z, spinPivot.x, spinPivot.y, spinPivot.z, state -> state.animation);
    }

    /** The legacy Stirling inventory icon animates its three moving parts independently. */
    private void stirling(ItemGem.Builder builder) {
        if (!userExclude.contains("Cog")) {
            named(builder, "Cog", surface).spin(Gem.Axes.Z, 0F, 1.375F, 0F, state -> -state.animation);
        }
        named(builder, "CogSmall", surface)
                .spin(Gem.Axes.X, 0F, 1.375F, 0.25F, state -> state.animation * 2F + 3F);
        named(builder, "Piston", surface)
                .slide(Gem.Axes.X, state -> (float) (Math.sin(state.animation * Math.PI / 90D) * 0.25D + 0.125D));
    }

    private Gem.Part<ItemPose> named(ItemGem.Builder builder, String part, Surface skin) {
        return builder.part(model.part(part), skin, model.boundsIncluding(Set.of(part)));
    }

    private static Set<String> remaining(List<String> parts, Set<String> excluded) {
        Set<String> chosen = new LinkedHashSet<>(parts);
        chosen.removeAll(excluded);
        return chosen;
    }

    private void applyTransform(PoseStack poseStack, @Nullable ItemDisplayContext context) {
        applyTransform(poseStack, context, guiSpinAngle());
    }

    private void applyTransform(PoseStack poseStack, @Nullable ItemDisplayContext context, float spinDegrees) {
        var none = context == ItemDisplayContext.NONE ? contextTransforms.get("none") : null;
        if (none != null && none.rawMesh()) {
            var min = model.boundsMin();
            var max = model.boundsMax();
            // The item layer subtracts its block center. NONE keeps the primary OBJ unscaled and centered.
            poseStack.translate(0.5F - (min[0] + max[0]) * 0.5F,
                    0.5F - (min[1] + max[1]) * 0.5F, 0.5F - (min[2] + max[2]) * 0.5F);
            return;
        }
        if (!blockFramed && animation.startsWith("armor_")) {
            applyArmorItemTransform(poseStack, context);
            return;
        }
        // The old item renderer inserted its inventory adjustment after the perspective matrix but before
        // renderCommon. The context-aware path has already applied that perspective matrix to the pose.
        ContextTransform contextTransform = context == null || context == ItemDisplayContext.NONE ? null
                : context == ItemDisplayContext.GUI ? contextTransforms.get("gui")
                : context == ItemDisplayContext.GROUND ? contextTransforms.get("ground")
                : slotTransform(context);
        if (contextTransform != null) {
            contextTransform.apply(poseStack);
        } else if (context == ItemDisplayContext.GUI) {
            poseStack.translate(guiTranslate.x, guiTranslate.y, guiTranslate.z);
            poseStack.scale(guiScale, guiScale, guiScale);
            poseStack.mulPose(Axis.YP.rotationDegrees(guiYaw));
        }
        boolean steppedSpin = false;
        if (context == ItemDisplayContext.GUI) {
            for (GuiStep step : guiSteps) {
                Vector3f v = step.value();
                switch (step.kind()) {
                    case "translate" -> poseStack.translate(v.x, v.y, v.z);
                    case "scale" -> poseStack.scale(v.x, v.y, v.z);
                    case "spin" -> {
                        poseStack.mulPose(Axis.of(new Vector3f(v).normalize()).rotationDegrees(spinDegrees));
                        steppedSpin = true;
                    }
                    default -> {
                        if (v.x != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(v.x));
                        if (v.y != 0F) poseStack.mulPose(Axis.YP.rotationDegrees(v.y));
                        if (v.z != 0F) poseStack.mulPose(Axis.ZP.rotationDegrees(v.z));
                    }
                }
            }
        }
        if (context == ItemDisplayContext.GUI && guiSpinSpeed != 0F && !steppedSpin) {
            poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        }

        if (rawLegacyTransform || !blockFramed) {
            // Direct context-aware models and explicit raw callbacks both reproduce the old OBJ item
            // geometry. Do not add the block centre or recenter on OBJ bounds: legacy models use
            // their authored coordinates, with the display matrix supplying the outer item frame.
            poseStack.translate(itemTranslate.x, ty + itemTranslate.y, itemTranslate.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(itemPreTranslate.x, itemPreTranslate.y, itemPreTranslate.z);
            if (pitch != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.scale(scale, scale, scale);
            return;
        }

        // The nested base model (block/block) frames content authored in 0..1 block space, centred at 0.5. The
        // OBJ has an arbitrary origin (often a corner), so: move its bounding-box centre to the block
        // centre (0.5,0.5,0.5), then rotate/scale, then re-centre the model on its own bounds. Without
        // the final translate the model's corner, not its middle, sits at the slot centre.
        float[][] bounds = model.boundsExcluding(userExclude);
        float[] mn = bounds[0];
        float[] mx = bounds[1];
        float cx = (mn[0] + mx[0]) * 0.5F;
        float cy = (mn[1] + mx[1]) * 0.5F;
        float cz = (mn[2] + mx[2]) * 0.5F;
        poseStack.translate(0.5F + itemTranslate.x, 0.5F + ty + itemTranslate.y, 0.5F + itemTranslate.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // The legacy bake rotates itemPreTranslate by yaw, but adds it after scaling the OBJ.
        poseStack.translate(itemPreTranslate.x, itemPreTranslate.y, itemPreTranslate.z);
        if (pitch != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-cx, -cy, -cz);
    }

    /**
     * The non-GUI adjustment for one display slot. {@code first_person}, {@code third_person} (hands and head)
     * and {@code fixed} are optional refinements of {@code non_gui}, for a 1.7 renderer whose equipped preamble
     * differs from its frame's per slot (the custom missile parts' {@code ItemRenderMissilePart}).
     */
    private @Nullable ContextTransform slotTransform(ItemDisplayContext context) {
        String key = switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> "first_person";
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> "third_person";
            case FIXED -> "fixed";
            default -> null;
        };
        ContextTransform specific = key == null ? null : contextTransforms.get(key);
        return specific != null ? specific : contextTransforms.get("non_gui");
    }

    /** The GUI spin's angle now, wrapped at a whole turn rather than at the millisecond count 1.7 wrote. */
    private float guiSpinAngle() {
        return guiSpinSpeed == 0F ? 0F : (float) (System.currentTimeMillis() * (double) guiSpinSpeed % 360D);
    }

    /**
     * Reproduces a 1.7 armor item renderer: the class's own renderInventory adjustment, ArmorFSB.setupRenderInv
     * (or setupRenderNonInv), the class's own renderCommon adjustment, then renderStandard's per-slot frame.
     */
    private void applyArmorItemTransform(PoseStack poseStack, @Nullable ItemDisplayContext context) {
        boolean inventory = context == ItemDisplayContext.GUI;
        if (inventory) {
            if (animation.endsWith("_head") && animation.startsWith("armor_dnt_"))
                poseStack.translate(0F, -1F, 0F);
            // ArmorDigamma / ArmorTrenchmaster / ArmorTaurun renderInventory: their own adjustment before setupRenderInv.
            if (animation.equals("armor_fau_head")) {
                poseStack.scale(0.875F, 0.875F, 0.875F);
                poseStack.translate(0F, -2F, 0F);
            }
            if (animation.equals("armor_trenchmaster_head") || animation.equals("armor_taurun_head"))
                poseStack.translate(0F, 1F, 0F);
            if (animation.equals("armor_trenchmaster_chest") || animation.equals("armor_taurun_chest"))
                poseStack.translate(0F, 1.5F, 0F);
            poseStack.translate(0F, -1.5F, 0F);
            poseStack.scale(3.25F, 3.25F, 3.25F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-135F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        }

        // ArmorBismuth renderCommon: its own framing in front of renderStandard.
        if (animation.equals("armor_bismuth_head")) {
            poseStack.translate(0F, -0.5F, 0F);
            poseStack.scale(0.625F, 0.625F, 0.625F);
        } else if (animation.equals("armor_bismuth_chest")) {
            poseStack.scale(0.875F, 0.875F, 0.875F);
        } else if (animation.equals("armor_dieselsuit_head")) {
            // ArmorDiesel renderCommon, before renderStandard
            poseStack.translate(0F, 0.5F, 0F);
            poseStack.scale(0.875F, 0.875F, 0.875F);
        } else if (animation.equals("armor_rpa_chest")) {
            // ArmorRPA renderCommon: if(armorType == 1) glTranslated(0, 0.25, 0)
            poseStack.translate(0F, 0.25F, 0F);
        } else if (animation.equals("armor_ncrpa_head")) {
            // ArmorNCRPA renderCommon: if(armorType == 0) glTranslated(0, 0.5, 0)
            poseStack.translate(0F, 0.5F, 0F);
        }

        if (animation.endsWith("_head")) {
            poseStack.scale(0.3125F, 0.3125F, 0.3125F);
            poseStack.translate(0F, 1F, 0F);
        } else if (animation.endsWith("_chest")) {
            if (animation.equals("armor_bj_jetpack_chest"))
                poseStack.scale(0.6875F, 0.6875F, 0.6875F);
            else if (animation.startsWith("armor_bj_"))
                poseStack.scale(0.875F, 0.875F, 0.875F);
            poseStack.scale(0.225F, 0.225F, 0.225F);
            poseStack.translate(0F, -10F, 0F);
        } else if (animation.endsWith("_legs")) {
            poseStack.scale(0.25F, 0.25F, 0.25F);
            poseStack.translate(0F, -20F, 0F);
        } else if (animation.endsWith("_feet")) {
            poseStack.scale(0.25F, 0.25F, 0.25F);
            poseStack.translate(0F, -22F, 0F);
        }
    }
}
