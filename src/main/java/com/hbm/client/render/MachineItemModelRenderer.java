package com.hbm.client.render;

//? if >=26.1 {

import dev.golem.core.render.VanillaSubmitSink;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CompatBlockEntityRenderState;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatMovingBlock;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Renders a HBM machine's OBJ model as its inventory/hand item, via 26.1's special-model system (the
 * modern replacement for {@code ItemStackTileEntityRenderer}/BEWLR). Data-driven: the obj/texture and
 * a scale/yaw/vertical offset come from the item-model JSON, so the icon's framing can be tuned without
 * recompiling.
 *
 * <p>This class is only the 26.1 plumbing — the codec, and the bridge from vanilla's collector to
 * {@link dev.golem.core.render.SubmitSink}. The model itself lives in {@link MachineItemModel}, which
 * older versions drive from a BEWLR instead.
 */
public class MachineItemModelRenderer implements NoDataSpecialModelRenderer {

    /** Registration id of this special-model codec (referenced by {@code "type"} in the item JSON). */
    public static final Identifier ID = dev.golem.core.Ids.of("hbm", "machine_obj");

    private final MachineItemModel model;

    private MachineItemModelRenderer(MachineItemModel model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        model.submit(poseStack, new VanillaSubmitSink(collector), lightCoords, overlayCoords);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        model.extents(output);
    }

    /** One additional ordered OBJ/texture pass; an omitted OBJ reuses the primary OBJ. */
    public record Pass(Optional<Identifier> obj, Identifier texture, List<String> parts, List<Float> translate,
                       List<Step> steps, int tint, float scale) {}

    /** One step of a pass's own transform; exactly one of the two is present. */
    public record Step(List<Float> rotate, List<Float> translate) {}

    /** One {@link MachineItemModel.GuiStep} as the item JSON spells it; exactly one of the four is present. */
    public record GuiStepJson(List<Float> rotate, List<Float> translate, List<Float> scale, List<Float> spin) {

        MachineItemModel.GuiStep step() {
            if (spin.size() == 3) return new MachineItemModel.GuiStep("spin", vector(spin));
            if (scale.size() == 3) return new MachineItemModel.GuiStep("scale", vector(scale));
            if (translate.size() == 3) return new MachineItemModel.GuiStep("translate", vector(translate));
            return new MachineItemModel.GuiStep("rotate", vector(rotate));
        }
    }

    /** The data-driven, codec-backed factory for this renderer (one entry per machine in the item JSON). */
    public record Unbaked(Identifier obj, Identifier texture, boolean noCull, float scale, boolean rawLegacyTransform,
                           float yaw, float pitch, float ty,
                           List<Float> itemTranslate, List<Float> itemPreTranslate, List<Float> guiTranslate,
                           float guiScale, float guiYaw, List<String> exclude, List<String> blackParts,
                            Optional<Identifier> overlayTexture, List<String> overlayParts, boolean mtl,
                            Map<String, Identifier> partTextures,
                            List<String> rotateParts, float rotateX, List<Float> rotatePivot,
                             List<String> spinParts, List<Float> spinPivot,
                             Map<String, MachineItemModel.ContextTransform> contextTransforms, String animation,
                             List<String> parts, List<Float> translate, List<Pass> passes, float guiSpinSpeed,
                             String frame, List<GuiStepJson> guiSteps)
                 implements NoDataSpecialModelRenderer.Unbaked, ItemModel.Unbaked {

        private static final MapCodec<Unbaked> BASE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("obj").forGetter(Unbaked::obj),
                Identifier.CODEC.optionalFieldOf("texture", dev.golem.core.Ids.of("hbm", "textures/models/machines/lava_gray.png")).forGetter(Unbaked::texture),
                Codec.BOOL.optionalFieldOf("noCull", false).forGetter(Unbaked::noCull),
                Codec.FLOAT.optionalFieldOf("scale", 0.28F).forGetter(Unbaked::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 90.0F).forGetter(Unbaked::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(Unbaked::pitch),
                Codec.FLOAT.optionalFieldOf("ty", 0.0F).forGetter(Unbaked::ty),
                 Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(Unbaked::exclude),
                 Identifier.CODEC.optionalFieldOf("overlayTexture").forGetter(Unbaked::overlayTexture),
                 Codec.STRING.listOf().optionalFieldOf("overlayParts", List.of()).forGetter(Unbaked::overlayParts),
                Codec.BOOL.optionalFieldOf("mtl", false).forGetter(Unbaked::mtl),
                Codec.STRING.listOf().optionalFieldOf("rotateParts", List.of()).forGetter(Unbaked::rotateParts),
                Codec.FLOAT.optionalFieldOf("rotateX", 0.0F).forGetter(Unbaked::rotateX),
                 Codec.FLOAT.listOf().optionalFieldOf("rotatePivot", List.of()).forGetter(Unbaked::rotatePivot),
                  Codec.STRING.listOf().optionalFieldOf("spinParts", List.of()).forGetter(Unbaked::spinParts),
                  Codec.FLOAT.listOf().optionalFieldOf("spinPivot", List.of()).forGetter(Unbaked::spinPivot)
          ).apply(i, (obj, texture, noCull, scale, yaw, pitch, ty, exclude, overlayTexture, overlayParts, mtl,
                      rotateParts, rotateX, rotatePivot, spinParts, spinPivot) ->
                       new Unbaked(obj, texture, noCull, scale, false, yaw, pitch, ty, List.of(), List.of(), List.of(), 1.0F, 0.0F, exclude, List.of(),
                              overlayTexture, overlayParts, mtl, Map.of(), rotateParts, rotateX, rotatePivot, spinParts, spinPivot,
                              Map.of(), "", List.of(), List.of(), List.of(), 0F, MACHINE_FRAME, List.of())));

        private static final MapCodec<Map<String, Identifier>> PART_TEXTURES =
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("partTextures", Map.of());

        private static final MapCodec<List<Float>> ITEM_TRANSLATE =
                Codec.FLOAT.listOf().optionalFieldOf("itemTranslate", List.of());
        private static final MapCodec<List<Float>> ITEM_PRE_TRANSLATE =
                 Codec.FLOAT.listOf().optionalFieldOf("itemPreTranslate", List.of());
        private static final MapCodec<List<Float>> GUI_TRANSLATE =
                Codec.FLOAT.listOf().optionalFieldOf("guiTranslate", List.of());
         private static final MapCodec<Float> GUI_SCALE = Codec.FLOAT.optionalFieldOf("guiScale", 1.0F);
         private static final MapCodec<Boolean> RAW_LEGACY_TRANSFORM =
                 Codec.BOOL.optionalFieldOf("rawLegacyTransform", false);
         private static final MapCodec<Float> GUI_YAW = Codec.FLOAT.optionalFieldOf("guiYaw", 0.0F);
         private static final MapCodec<List<String>> BLACK_PARTS =
                 Codec.STRING.listOf().optionalFieldOf("blackParts", List.of());
         private static final Codec<MachineItemModel.ContextTransform> CONTEXT_TRANSFORM_CODEC =
                 RecordCodecBuilder.create(i -> i.group(
                         Codec.FLOAT.listOf().optionalFieldOf("rotation", List.of(0F, 0F, 0F))
                                 .forGetter(transform -> List.of(transform.rotation().x(), transform.rotation().y(), transform.rotation().z())),
                         Codec.FLOAT.listOf().optionalFieldOf("translation", List.of(0F, 0F, 0F))
                                 .forGetter(transform -> List.of(transform.translation().x(), transform.translation().y(), transform.translation().z())),
                         Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(MachineItemModel.ContextTransform::scale)
                 ).apply(i, (rotation, translation, scale) -> new MachineItemModel.ContextTransform(
                         vector(rotation), vector(translation), scale)));
          private static final MapCodec<Map<String, MachineItemModel.ContextTransform>> CONTEXT_TRANSFORMS =
                  Codec.unboundedMap(Codec.STRING, CONTEXT_TRANSFORM_CODEC).optionalFieldOf("contextTransforms", Map.of());
          private static final MapCodec<String> ANIMATION = Codec.STRING.optionalFieldOf("animation", "");
           private static final MapCodec<List<String>> PARTS =
                   Codec.STRING.listOf().optionalFieldOf("parts", List.of());
           private static final MapCodec<List<Float>> TRANSLATE =
                   Codec.FLOAT.listOf().optionalFieldOf("translate", List.of());
          private static final Codec<Step> STEP_CODEC = RecordCodecBuilder.create(i -> i.group(
                  Codec.FLOAT.listOf().optionalFieldOf("rotate", List.of()).forGetter(Step::rotate),
                  Codec.FLOAT.listOf().optionalFieldOf("translate", List.of()).forGetter(Step::translate)
          ).apply(i, Step::new));
          private static final Codec<Pass> PASS_CODEC = RecordCodecBuilder.create(i -> i.group(
                  Identifier.CODEC.optionalFieldOf("obj").forGetter(Pass::obj),
                  Identifier.CODEC.fieldOf("texture").forGetter(Pass::texture),
                  Codec.STRING.listOf().optionalFieldOf("parts", List.of()).forGetter(Pass::parts),
                  Codec.FLOAT.listOf().optionalFieldOf("translate", List.of()).forGetter(Pass::translate),
                  STEP_CODEC.listOf().optionalFieldOf("steps", List.of()).forGetter(Pass::steps),
                  Codec.INT.optionalFieldOf("tint", MachineItemModel.ModelPass.UNTINTED).forGetter(Pass::tint),
                  Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(Pass::scale)
          ).apply(i, Pass::new));
          private static final MapCodec<List<Pass>> PASSES =
                  PASS_CODEC.listOf().optionalFieldOf("passes", List.of());
          private static final MapCodec<Float> GUI_SPIN_SPEED = Codec.FLOAT.optionalFieldOf("guiSpinSpeed", 0F);
          private static final MapCodec<String> FRAME = Codec.STRING.optionalFieldOf("frame", MACHINE_FRAME);
          private static final Codec<GuiStepJson> GUI_STEP_CODEC = RecordCodecBuilder.create(i -> i.group(
                  Codec.FLOAT.listOf().optionalFieldOf("rotate", List.of()).forGetter(GuiStepJson::rotate),
                  Codec.FLOAT.listOf().optionalFieldOf("translate", List.of()).forGetter(GuiStepJson::translate),
                  Codec.FLOAT.listOf().optionalFieldOf("scale", List.of()).forGetter(GuiStepJson::scale),
                  Codec.FLOAT.listOf().optionalFieldOf("spin", List.of()).forGetter(GuiStepJson::spin)
          ).apply(i, GuiStepJson::new));
          private static final MapCodec<List<GuiStepJson>> GUI_STEPS =
                  GUI_STEP_CODEC.listOf().optionalFieldOf("guiSteps", List.of());
          private record Extensions(Map<String, Identifier> partTextures, List<Float> itemTranslate,
                                    List<Float> itemPreTranslate, List<Float> guiTranslate, float guiScale,
                                    boolean rawLegacyTransform, float guiYaw, List<String> blackParts,
                                    Map<String, MachineItemModel.ContextTransform> contextTransforms,
                                    String animation, List<String> parts, List<Float> translate,
                                    List<Pass> passes, float guiSpinSpeed, String frame,
                                    List<GuiStepJson> guiSteps) {}
          private static final MapCodec<Extensions> EXTENSIONS_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                  PART_TEXTURES.forGetter(Extensions::partTextures),
                  ITEM_TRANSLATE.forGetter(Extensions::itemTranslate),
                  ITEM_PRE_TRANSLATE.forGetter(Extensions::itemPreTranslate),
                  GUI_TRANSLATE.forGetter(Extensions::guiTranslate),
                  GUI_SCALE.forGetter(Extensions::guiScale),
                  RAW_LEGACY_TRANSFORM.forGetter(Extensions::rawLegacyTransform),
                  GUI_YAW.forGetter(Extensions::guiYaw),
                  BLACK_PARTS.forGetter(Extensions::blackParts),
                  CONTEXT_TRANSFORMS.forGetter(Extensions::contextTransforms),
                  ANIMATION.forGetter(Extensions::animation),
                  PARTS.forGetter(Extensions::parts),
                  TRANSLATE.forGetter(Extensions::translate),
                  PASSES.forGetter(Extensions::passes),
                  GUI_SPIN_SPEED.forGetter(Extensions::guiSpinSpeed),
                  FRAME.forGetter(Extensions::frame),
                  GUI_STEPS.forGetter(Extensions::guiSteps)
          ).apply(i, Extensions::new));

         /** Adds the legacy transform extensions without exceeding RecordCodecBuilder's 16-field limit. */
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.of(
                new com.mojang.serialization.MapEncoder<>() {
                    @Override
                    public <T> com.mojang.serialization.RecordBuilder<T> encode(Unbaked value,
                                                                                  com.mojang.serialization.DynamicOps<T> ops,
                                                                                  com.mojang.serialization.RecordBuilder<T> prefix) {
                         prefix = BASE_CODEC.encode(value, ops, prefix);
                         return EXTENSIONS_CODEC.encode(new Extensions(value.partTextures(), value.itemTranslate(),
                                 value.itemPreTranslate(), value.guiTranslate(), value.guiScale(),
                                 value.rawLegacyTransform(), value.guiYaw(), value.blackParts(),
                                 value.contextTransforms(), value.animation(), value.parts(), value.translate(),
                                 value.passes(), value.guiSpinSpeed(), value.frame(), value.guiSteps()), ops, prefix);
                    }

                    @Override
                    public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                         return java.util.stream.Stream.concat(BASE_CODEC.keys(ops), EXTENSIONS_CODEC.keys(ops));
                    }

                    @Override
                    public <T> com.mojang.serialization.KeyCompressor<T> compressor(com.mojang.serialization.DynamicOps<T> ops) {
                        return BASE_CODEC.compressor(ops);
                    }
                },
                new com.mojang.serialization.MapDecoder<>() {
                    @Override
                    public <T> com.mojang.serialization.DataResult<Unbaked> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                                                      com.mojang.serialization.MapLike<T> input) {
                         return BASE_CODEC.decode(ops, input).flatMap(base ->
                                 EXTENSIONS_CODEC.decode(ops, input).map(extra ->
                                         withItemTranslations(base, extra.rawLegacyTransform(), extra.partTextures(),
                                                 extra.itemTranslate(), extra.itemPreTranslate(), extra.guiTranslate(),
                                                 extra.guiScale(), extra.guiYaw(), extra.blackParts(),
                                                 extra.contextTransforms(), extra.animation(), extra.parts(),
                                                 extra.translate(), extra.passes(), extra.guiSpinSpeed(),
                                                 extra.frame(), extra.guiSteps())));
                    }

                    @Override
                    public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                         return java.util.stream.Stream.concat(BASE_CODEC.keys(ops), EXTENSIONS_CODEC.keys(ops));
                    }

                    @Override
                    public <T> com.mojang.serialization.KeyCompressor<T> compressor(com.mojang.serialization.DynamicOps<T> ops) {
                        return BASE_CODEC.compressor(ops);
                    }
                });

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new MachineItemModelRenderer(createModel(true));
        }

        @Override
        public void resolveDependencies(net.minecraft.client.resources.model.ResolvableModel.Resolver resolver) {
            resolver.markDependency(frameModel());
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var base = baker.getModel(frameModel());
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(
                    baker, base, base.getTopTextureSlots());
            return new ContextualItemModel(createModel(false), properties);
        }

        private MachineItemModel createModel(boolean blockFramed) {
            Surface overlay = overlayTexture.map(texture -> cutout(texture, noCull)).orElse(null);
            Surface black = blackParts.isEmpty() ? null : Surface.solid("hbm:textures/models/misc/white.png");
            WavefrontObj loaded = WavefrontObj.load(obj);
            Vector3f pivot = rotatePivot.size() == 3
                    ? new Vector3f(rotatePivot.get(0), rotatePivot.get(1), rotatePivot.get(2))
                    : new Vector3f();
            Vector3f spinningPivot = spinPivot.size() == 3
                    ? new Vector3f(spinPivot.get(0), spinPivot.get(1), spinPivot.get(2))
                    : new Vector3f();
            Vector3f itemTranslation = itemTranslate.size() == 3
                    ? new Vector3f(itemTranslate.get(0), itemTranslate.get(1), itemTranslate.get(2))
                    : new Vector3f();
            Vector3f itemPreTranslation = itemPreTranslate.size() == 3
                    ? new Vector3f(itemPreTranslate.get(0), itemPreTranslate.get(1), itemPreTranslate.get(2))
                    : new Vector3f();
            Vector3f guiTranslation = guiTranslate.size() == 3
                    ? new Vector3f(guiTranslate.get(0), guiTranslate.get(1), guiTranslate.get(2))
                    : new Vector3f();
            java.util.Map<String, Surface> types = java.util.Map.of();
             if (mtl) {
                // Multi-texture OBJ: resolve per-material textures from the .mtl next to the model.
                var lib = WavefrontObj.loadMtl(dev.golem.core.Ids.of(obj.getNamespace(), obj.getPath().replace(".obj", ".mtl")));
                java.util.Map<String, Surface> resolved = new java.util.LinkedHashMap<>();
                for (String mat : loaded.materialNames()) {
                    Identifier tex = lib.get(mat);
                    if (tex != null) resolved.put(mat, cutout(tex, noCull));
                }
                 types = resolved;
             }
             Map<String, Surface> partTypes = Map.of();
             if (!partTextures.isEmpty()) {
                  java.util.Map<String, Surface> resolved = new java.util.LinkedHashMap<>();
                 resolved.put("Body", cutout(texture, noCull));
                 for (Map.Entry<String, Identifier> entry : partTextures.entrySet()) {
                     resolved.put(entry.getKey(), cutout(entry.getValue(), noCull));
                 }
                  partTypes = resolved;
              }
              List<MachineItemModel.ModelPass> modelPasses = new ArrayList<>();
              for (Pass pass : passes) {
                  Identifier passObj = pass.obj().orElse(obj);
                  Vector3f translation = vector(pass.translate());
                  WavefrontObj passModel = obj.equals(passObj) ? loaded : WavefrontObj.load(passObj);
                  List<MachineItemModel.PassStep> steps = new java.util.ArrayList<>();
                  for (Step step : pass.steps()) {
                      boolean rotate = step.rotate().size() == 3;
                      steps.add(new MachineItemModel.PassStep(rotate,
                              vector(rotate ? step.rotate() : step.translate())));
                  }
                  modelPasses.add(new MachineItemModel.ModelPass(passModel,
                          cutout(pass.texture(), noCull), List.copyOf(pass.parts()), translation,
                          List.copyOf(steps), pass.tint(), pass.scale()));
              }
              Vector3f primaryTranslation = vector(translate);
              return new MachineItemModel(loaded, cutout(texture, noCull),
                    scale, rawLegacyTransform, blockFramed, yaw, pitch, ty,
                    itemTranslation, itemPreTranslation, guiTranslation, guiScale, guiYaw,
                    Set.copyOf(exclude), overlay, List.copyOf(overlayParts), black, List.copyOf(blackParts),
                     List.copyOf(rotateParts), rotateX, pivot, List.copyOf(spinParts), spinningPivot, types, partTypes,
                      !spinParts.isEmpty(), contextTransforms, animation, List.copyOf(parts), primaryTranslation,
                      modelPasses, guiSpinSpeed)
                      .withGuiSteps(guiSteps.stream().map(GuiStepJson::step).toList());
         }

        /**
         * The shared {@code display{}} this body is framed by. One per 1.7 item-render class, named by the
         * {@code frame} field: {@code machine} is {@code ItemRenderBase}, {@code missile} is
         * {@code ItemRenderMissileGeneric}, which frames its body from the slot origin rather than centring
         * it.
         */
        private Identifier frameModel() {
            return dev.golem.core.Ids.of("hbm", "item/" + frame + "_obj_context_base");
        }
     }

    /** The default {@code frame}: 1.7's {@code ItemRenderBase}, which every OBJ machine wore. */
    public static final String MACHINE_FRAME = "machine";

    private static Unbaked withItemTranslations(Unbaked base, boolean rawLegacyTransform,
                                                 Map<String, Identifier> partTextures,
                                                 List<Float> itemTranslate, List<Float> itemPreTranslate,
                                                  List<Float> guiTranslate, float guiScale, float guiYaw,
                                                  List<String> blackParts,
                                                  Map<String, MachineItemModel.ContextTransform> contextTransforms,
                                                   String animation, List<String> parts, List<Float> translate,
                                                   List<Pass> passes,
                                                   float guiSpinSpeed, String frame, List<GuiStepJson> guiSteps) {
         return new Unbaked(base.obj(), base.texture(), base.noCull(), base.scale(), rawLegacyTransform,
                 base.yaw(), base.pitch(), base.ty(),
                  itemTranslate, itemPreTranslate, guiTranslate, guiScale, guiYaw, base.exclude(), blackParts,
                  base.overlayTexture(), base.overlayParts(), base.mtl(), partTextures,
                 base.rotateParts(), base.rotateX(), base.rotatePivot(), base.spinParts(), base.spinPivot(),
                  contextTransforms, animation, parts, translate, passes, guiSpinSpeed, frame, guiSteps);
     }

    private static Vector3f vector(List<Float> values) {
        return values.size() == 3
                ? new Vector3f(values.get(0), values.get(1), values.get(2))
                : new Vector3f();
    }

    private static final class ContextualItemModel implements ItemModel {
        private final MachineItemModel model;
        private final ModelRenderProperties properties;
        private final Body body = new Body();

        private ContextualItemModel(MachineItemModel model, ModelRenderProperties properties) {
            this.model = model;
            this.properties = properties;
        }

        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                           ItemDisplayContext displayContext, @Nullable ClientLevel level,
                           @Nullable ItemOwner owner, int seed) {
            output.appendModelIdentityElement(this);
            if (model.animated(displayContext)) output.setAnimated();
            ItemStackRenderState.LayerRenderState layer = output.newLayer();
            layer.setExtents(() -> model.extents(displayContext));
            layer.setupSpecialModel(body, displayContext);
            properties.applyToLayer(layer, displayContext);
        }

        private final class Body implements SpecialModelRenderer<ItemDisplayContext> {
            @Override
            public void submit(@Nullable ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                               int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
                if (context != null) {
                    model.submit(pose, new VanillaSubmitSink(collector), lightCoords, overlayCoords, context);
                }
            }

            @Override
            public void getExtents(java.util.function.Consumer<Vector3fc> output) {
                model.extents(output);
            }

            @Override
            public @Nullable ItemDisplayContext extractArgument(ItemStack stack) {
                return null;
            }
        }
    }

    private static Surface cutout(Identifier texture, boolean noCull) {
        return noCull ? Surface.cutoutNoCull(texture.toString()) : Surface.cutout(texture.toString());
    }

    /**
     * {@code {"type": "hbm:custom_item", "renderer": ..., "frame": ..., "flat": ...}}: a ported 1.7 item renderer.
     *
     * <p>An item model rather than a special model, for the reason {@code GunItemModel} is one: only
     * {@link #update} is handed the display context, and 1.7's renderers branched on it. The frame (the engine's
     * pose for the slot) is the {@code display{}} of {@code <frame>_obj_context_base}; the body is the renderer.
     * A slot the renderer declines resolves the {@code flat} model instead, which is how a weapon is its sprite in
     * the inventory and its OBJ in the hand.
     *
     * <p>Below 26.1 {@code MachineItemModels} reads the same definition and draws the same renderer from the
     * loader's item-renderer hook.
     */
    public static final class CustomItem implements ItemModel {

        public static final Identifier ID = dev.golem.core.Ids.of("hbm", "custom_item");

        private final com.hbm.client.render.item.CustomItemRenderer renderer;
        private final ModelRenderProperties properties;
        private final @Nullable ItemModel flat;
        private final Body body = new Body();

        private CustomItem(com.hbm.client.render.item.CustomItemRenderer renderer, ModelRenderProperties properties, @Nullable ItemModel flat) {
            this.renderer = renderer;
            this.properties = properties;
            this.flat = flat;
        }

        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                           ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner,
                           int seed) {
            com.hbm.client.render.item.CustomItemRenderer.Type type = com.hbm.client.render.item.CustomItemRenderer.Type.of(displayContext);
            if (!renderer.handles(item, type)) {
                if (flat != null) flat.update(output, item, resolver, displayContext, level, owner, seed);
                return;
            }
            output.appendModelIdentityElement(this);
            // Charge, fuel, variant and missile parts all change the drawing; the picture-in-picture cache keys on this.
            output.appendModelIdentityElement(item.getComponentsPatch());
            if (renderer.animated(item, type)) output.setAnimated();

            LivingEntity living = owner == null ? null : owner.asLivingEntity();
            ItemStackRenderState.LayerRenderState layer = output.newLayer();
            layer.setExtents(() -> extents(item, type, living));
            layer.setupSpecialModel(body, new Argument(item, type, displayContext, living));
            properties.applyToLayer(layer, displayContext);
        }

        /**
         * The drawn geometry's corners in body space. Measured by running the renderer into a sink that only records
         * positions: a renderer's body is code, so there is no declared box to read.
         */
        private Vector3fc[] extents(ItemStack item, com.hbm.client.render.item.CustomItemRenderer.Type type, @Nullable LivingEntity owner) {
            Bounds bounds = new Bounds();
            renderer.render(item, type, new PoseStack(), bounds, 0xF000F0, 0, owner);
            return bounds.corners();
        }

        public record Argument(ItemStack stack, com.hbm.client.render.item.CustomItemRenderer.Type type,
                               ItemDisplayContext context, @Nullable LivingEntity owner) {}

        private final class Body implements SpecialModelRenderer<Argument> {
            @Override
            public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector,
                               int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
                if (argument == null) return;
                renderer.render(argument.stack(), argument.context(), pose, new VanillaSubmitSink(collector),
                        lightCoords, overlayCoords, argument.owner());
            }

            @Override
            public void getExtents(Consumer<Vector3fc> output) {}

            @Override
            public @Nullable Argument extractArgument(ItemStack stack) {
                return null;
            }
        }

        /** A sink that keeps the axis-aligned box of every vertex written to it and draws nothing. */
        private static final class Bounds implements SubmitSink, VertexConsumer {
            private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            private float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            Vector3fc[] corners() {
                if (minX > maxX) return new Vector3fc[0];
                List<Vector3fc> out = new ArrayList<>(8);
                for (int i = 0; i < 8; i++) {
                    out.add(new Vector3f((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY,
                            (i & 4) == 0 ? minZ : maxZ));
                }
                return out.toArray(Vector3fc[]::new);
            }

            @Override
            public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometry geometry) {
                geometry.render(poseStack.last(), this);
            }

            @Override
            public SubmitSink order(int order) {
                return this;
            }

            @Override
            public void submitItem(PoseStack poseStack, CompatItemRenderState item, int light, int overlay, int seed) {}

            @Override
            public void submitBlock(PoseStack poseStack, CompatBlockRenderState block, int light, int overlay) {}

            @Override
            public void submitMovingBlock(PoseStack poseStack, CompatMovingBlock block) {}

            @Override
            public void submitBlockEntity(PoseStack poseStack, CompatBlockEntityRenderState blockEntity) {}

            @Override
            public void submitText(PoseStack poseStack, Font font, float x, float y, FormattedCharSequence text,
                                   boolean shadow, Font.DisplayMode mode, int light, int color, int backgroundColor) {}

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
                return this;
            }

            @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer setColor(int color) { return this; }
            @Override public VertexConsumer setUv(float u, float v) { return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }
        }

        /**
         * {@code renderer} names the ported 1.7 renderer; {@code frame} the shared engine pose it sits in
         * ({@code weapon}: the bare slot an {@code IItemRenderer} got; {@code machine}: {@code ItemRenderBase}'s);
         * {@code flat}, optional, the model shown in a slot the renderer declines. {@code generated} and
         * {@code handheld} are sprite frames: the renderer is posed in {@code flat}'s own display, and draws the sprite
         * itself before what it adds (see {@code ItemSprites} for why it cannot be vanilla's). {@code sprite} is read only by the pre-26.1 path, which has no
         * baked flat model to fall back on.
         */
        public record Unbaked(String renderer, String frame, Optional<Identifier> flat) implements ItemModel.Unbaked {

            public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.fieldOf("renderer").forGetter(Unbaked::renderer),
                    Codec.STRING.optionalFieldOf("frame", "weapon").forGetter(Unbaked::frame),
                    Identifier.CODEC.optionalFieldOf("flat").forGetter(Unbaked::flat)
            ).apply(i, Unbaked::new));

            @Override
            public MapCodec<? extends ItemModel.Unbaked> type() {
                return MAP_CODEC;
            }

            private boolean spriteFrame() {
                return flat.isPresent() && (frame.equals("generated") || frame.equals("handheld"));
            }

            private Identifier frameModel() {
                return spriteFrame() ? flat.get() : dev.golem.core.Ids.of("hbm", "item/" + frame + "_obj_context_base");
            }

            @Override
            public void resolveDependencies(ResolvableModel.Resolver resolver) {
                resolver.markDependency(frameModel());
                flat.ifPresent(resolver::markDependency);
            }

            @Override
            public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
                var baker = context.blockModelBaker();
                var base = baker.getModel(frameModel());
                ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(
                        baker, base, base.getTopTextureSlots());
                ItemModel flatModel = flat.map(id -> new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of())
                        .bake(context, transformation)).orElse(null);
                return new CustomItem(com.hbm.client.render.item.CustomItemRenderers.byName(renderer), properties, flatModel);
            }
        }
    }
}
//?}
