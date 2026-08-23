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
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.function.Consumer;

/** 26.1 codec for a complete four-segment RBMK fuel-channel item, not a hollow block OBJ. */
public final class RBMKColumnItemModelRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = dev.golem.core.Ids.of("hbm", "rbmk_fuel_column");
    private final RBMKColumnItemModel model;
    private RBMKColumnItemModelRenderer(RBMKColumnItemModel model) { this.model = model; }
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) { model.submit(pose, new VanillaSubmitSink(collector), light, overlay); }
    @Override public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f(0F, -.675F, 0F)); output.accept(new Vector3f(1F, .725F, 1F)); }
    public record Unbaked(Identifier side, Identifier top, Identifier inner, float scale, float yaw, float lift) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("side").forGetter(Unbaked::side), Identifier.CODEC.fieldOf("top").forGetter(Unbaked::top),
                Identifier.CODEC.fieldOf("inner").forGetter(Unbaked::inner), Codec.FLOAT.optionalFieldOf("scale", .35F).forGetter(Unbaked::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(Unbaked::yaw), Codec.FLOAT.optionalFieldOf("lift", -.675F).forGetter(Unbaked::lift)
        ).apply(i, Unbaked::new));
        @Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
        @Override public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) { return new RBMKColumnItemModelRenderer(new RBMKColumnItemModel(side, top, inner, scale, yaw, lift)); }
    }
}
//?}
