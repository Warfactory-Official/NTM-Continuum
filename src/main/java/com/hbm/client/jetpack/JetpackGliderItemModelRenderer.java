package com.hbm.client.jetpack;

//? if >=26.1 {

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.render.VanillaSubmitSink;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Modern special-item renderer for the glider's animated Collada body. */
public final class JetpackGliderItemModelRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier ID = Ids.of("hbm", "jetpack_glider");
    private final JetpackGliderItemModel model;
    private final float scale;
    private final List<Float> translation;

    private JetpackGliderItemModelRenderer(JetpackGliderItemModel model, float scale, List<Float> translation) {
        this.model = model;
        this.scale = scale;
        this.translation = translation;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
            boolean foil, int outlineColor) {
        pose.pushPose();
        pose.translate(translation.get(0), translation.get(1), translation.get(2));
        pose.scale(scale, scale, scale);
        model.submitRaw(pose, new VanillaSubmitSink(collector), light, overlay);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.075F));
        output.accept(new Vector3f(1.075F));
    }

    public record Unbaked(Identifier obj, Identifier texture, Identifier animation, float scale,
            List<Float> translate) implements NoDataSpecialModelRenderer.Unbaked, ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("obj", JetpackColladaModel.OBJ).forGetter(Unbaked::obj),
                Identifier.CODEC.optionalFieldOf("texture", JetpackColladaModel.TEXTURE).forGetter(Unbaked::texture),
                Identifier.CODEC.optionalFieldOf("animation", JetpackColladaModel.ANIMATION).forGetter(Unbaked::animation),
                Codec.FLOAT.optionalFieldOf("scale", 1.5F).forGetter(Unbaked::scale),
                Codec.FLOAT.listOf().optionalFieldOf("translate", List.of(-5F, -6F, 0F)).forGetter(Unbaked::translate)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new JetpackGliderItemModelRenderer(new JetpackGliderItemModel(), scale,
                    translate.size() == 3 ? translate : List.of(-5F, -6F, 0F));
        }

        @Override
        public void resolveDependencies(net.minecraft.client.resources.model.ResolvableModel.Resolver resolver) {
            resolver.markDependency(Ids.of("hbm", "item/jetpack_glider_base"));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var base = baker.getModel(Ids.of("hbm", "item/machine_obj_context_base"));
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(
                    baker, base, base.getTopTextureSlots());
            return new ContextualItemModel(new JetpackGliderItemModel(), properties);
        }
    }

    private static final class ContextualItemModel implements ItemModel {
        private final JetpackGliderItemModel model;
        private final ModelRenderProperties properties;
        private final Body body = new Body();

        private ContextualItemModel(JetpackGliderItemModel model, ModelRenderProperties properties) {
            this.model = model;
            this.properties = properties;
        }

        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            output.appendModelIdentityElement(this);
            ItemStackRenderState.LayerRenderState layer = output.newLayer();
            layer.setExtents(() -> extents());
            layer.setupSpecialModel(body, displayContext);
            properties.applyToLayer(layer, displayContext);
        }

        private Vector3f[] extents() {
            return new Vector3f[] {new Vector3f(-8F, -8F, -8F), new Vector3f(8F, 8F, 8F)};
        }

        private final class Body implements SpecialModelRenderer<ItemDisplayContext> {
            @Override
            public void submit(@Nullable ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                    int light, int overlay, boolean foil, int outlineColor) {
                if (context != null) model.submit(pose, new VanillaSubmitSink(collector), light, overlay, context);
            }

            @Override
            public void getExtents(Consumer<Vector3fc> output) {
                output.accept(new Vector3f(-8F, -8F, -8F));
                output.accept(new Vector3f(8F, 8F, 8F));
            }

            @Override
            public ItemDisplayContext extractArgument(ItemStack stack) {
                return null;
            }
        }
    }
}
//?}
