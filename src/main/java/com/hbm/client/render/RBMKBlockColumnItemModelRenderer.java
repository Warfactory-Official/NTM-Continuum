package com.hbm.client.render;

//? if >=26.1 {

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.render.VanillaSubmitSink;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import java.util.function.Consumer;

/** 26.1 codec for the plain/piped/lidded RBMK column inventory preview. See {@link RBMKBlockColumnItemModel}. */
public final class RBMKBlockColumnItemModelRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = dev.golem.core.Ids.of("hbm", "rbmk_block_column");
    private final RBMKBlockColumnItemModel model;
    private RBMKBlockColumnItemModelRenderer(RBMKBlockColumnItemModel model) { this.model = model; }
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) { model.submit(pose, new VanillaSubmitSink(collector), light, overlay); }
    @Override public void getExtents(Consumer<Vector3fc> output) { model.extents(output); }
    public record Unbaked(Identifier side, Identifier top, java.util.Optional<Identifier> bottom,
                          java.util.Optional<Identifier> pipeSide, java.util.Optional<Identifier> pipeTop,
                          java.util.Optional<Identifier> lidTexture, float scale, float yaw, float lift) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("side").forGetter(Unbaked::side),
                Identifier.CODEC.fieldOf("top").forGetter(Unbaked::top),
                Identifier.CODEC.optionalFieldOf("bottom").forGetter(Unbaked::bottom),
                Identifier.CODEC.optionalFieldOf("pipe_side").forGetter(Unbaked::pipeSide),
                Identifier.CODEC.optionalFieldOf("pipe_top").forGetter(Unbaked::pipeTop),
                Identifier.CODEC.optionalFieldOf("lid_texture").forGetter(Unbaked::lidTexture),
                Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(Unbaked::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(Unbaked::yaw),
                Codec.FLOAT.optionalFieldOf("lift", -0.5F).forGetter(Unbaked::lift)
        ).apply(i, Unbaked::new));
        @Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        @Override public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new RBMKBlockColumnItemModelRenderer(new RBMKBlockColumnItemModel(side, top,
                    bottom.orElse(null), pipeSide.orElse(null), pipeTop.orElse(null), lidTexture.orElse(null),
                    scale, yaw, lift));
        }
    }
}
//?}
