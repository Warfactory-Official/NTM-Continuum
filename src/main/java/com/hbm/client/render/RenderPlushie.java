package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.generic.BlockPlushie.Plushie;
import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityPlushie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderPlushie}: turned {@code -(22.5 * rotation + 90)} about Y, squashed while the squish timer runs
 * ({@code 1 + -sin(s) * s * 0.025} tall), scaled per type, then Yomi, Hundun ("goober_posed"), the dragon
 * (blepping while squished) or Number Nine - {@code HorsePronter}'s horse sat down, with the No. 9 helmet and a
 * cigarette in its mouth.
 */
public final class RenderPlushie extends GemBER<TileEntityPlushie, RenderPlushie.State> {

    public static final class State extends CompatRenderState {
        public int rotation;
        public PlushieType type = PlushieType.NONE;
        public float squish;
        public boolean miserable;
        public double misery;
        public final CompatItemRenderState cigarette = new CompatItemRenderState();
    }

    static final Identifier YOMI = Ids.of(Constants.MOD_ID, "textures/models/trinkets/yomi.png");
    static final Identifier NUMBERNINE = Ids.of(Constants.MOD_ID, "textures/models/horse/numbernine.png");
    static final Identifier HUNDUN = Ids.of(Constants.MOD_ID, "textures/models/trinkets/hundun.png");
    static final Identifier DERG = Ids.of(Constants.MOD_ID, "textures/models/trinkets/derg.png");
    static final Identifier NO9 = Ids.of(Constants.MOD_ID, "textures/armor/no9.png");
    static final Identifier NO9_INSIGNIA = Ids.of(Constants.MOD_ID, "textures/armor/no9_insignia.png");

    private final WavefrontObj yomi = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/trinkets/yomi.obj"));
    private final WavefrontObj hundun = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/trinkets/hundun.obj"));
    private final WavefrontObj derg = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/trinkets/derg.obj"));
    private final WavefrontObj horse = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/mobs/horse.obj"));
    private final WavefrontObj no9 = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/armor/no9.obj"));

    @Override
    protected void setup(Gem.Model<State> declaration) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityPlushie plushie, State state, float interp, Vec3 cameraPosition) {
        state.rotation = plushie.getBlockState().getValue(BlockTrinket.ROTATION);
        state.type = plushie.type();
        state.squish = plushie.squishTimer > 0 ? plushie.squishTimer - interp : -1F;
        state.miserable = plushie.miseryFactor > 0.01F;
        state.misery = plushie.lastMiseryFactor + (plushie.miseryFactor - plushie.lastMiseryFactor) * interp;
        if (state.type == PlushieType.NUMBERNINE) {
            state.cigarette.extract(plushie.getLevel(), new ItemStack(BuiltInRegistries.ITEM.getValue(
                    Ids.of(Constants.MOD_ID, "cigarette"))), ItemDisplayContext.NONE);
        } else {
            state.cigarette.clear();
        }
    }

    public static void rootPose(PoseStack pose, Plushie type, int rotation, boolean miserable, double misery,
                                boolean squishing, double squish) {
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-(22.5F * rotation + 90F)));
        if (miserable) pose.scale(1F, (float) (1D - misery), 1F);
        else if (squishing) pose.scale(1F, (float) (1D + (-Math.sin(squish) * squish) * 0.025D), 1F);
        pose.scale(type.scaleX(), type.scaleY(), type.scaleZ());
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        rootPose(pose, state.type, state.rotation, state.miserable, state.misery, state.squish >= 0F, state.squish);
        renderPlushie(pose, sink, state, state.lightCoords);
        pose.popPose();
    }

    private static @org.jspecify.annotations.Nullable RenderPlushie itemRenderer;
    private final State itemState = new State();

    /** 1.7 {@code RenderPlushie}'s item: {@code renderPlushie(type, false)} at the item's pose, cigarette included. */
    public static void renderItem(PoseStack pose, SubmitSink sink, PlushieType type, int light) {
        if (itemRenderer == null) itemRenderer = new RenderPlushie();
        State state = itemRenderer.itemState;
        state.type = type;
        state.squish = -1F;
        if (type == PlushieType.NUMBERNINE) {
            state.cigarette.extract(Minecraft.getInstance().level, new ItemStack(
                    BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "cigarette"))), ItemDisplayContext.NONE);
        } else {
            state.cigarette.clear();
        }
        itemRenderer.renderPlushie(pose, sink, state, light);
    }

    private void renderPlushie(PoseStack pose, SubmitSink sink, State state, int light) {
        switch (state.type) {
            case YOMI -> sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(YOMI),
                    (p, b) -> yomi.renderAll(p, b, light, OverlayTexture.NO_OVERLAY));
            case HUNDUN -> part(pose, sink, hundun, HUNDUN, "goober_posed", light, true);
            case DERG -> {
                part(pose, sink, derg, DERG, "Derg", light, true);
                part(pose, sink, derg, DERG, state.squish >= 0F ? "Blep" : "ColonThree", light, true);
            }
            case NUMBERNINE -> numberNine(pose, sink, state, light);
            default -> { }
        }
    }

    private void numberNine(PoseStack pose, SubmitSink sink, State state, int light) {
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.mulPose(Axis.XP.rotationDegrees(-15F));
        pose.translate(0D, -0.25D, 0.75D);

        double r = 45;
        // HorsePronter: yaw about Y, pitch about X, roll about Z, about each part's offset; unculled
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((float) -r));
        part(pose, sink, horse, NUMBERNINE, "Body", light, false);
        horsePart(pose, sink, light, new double[]{0, 1.125, 0.375}, new double[]{0, r + 15, 0}, "Head", "Mane", "NoseFemale");
        horsePart(pose, sink, light, new double[]{0.125, 0.75, 0.3125}, new double[]{0, r - 25, 5}, "LeftFrontLeg");
        horsePart(pose, sink, light, new double[]{-0.125, 0.75, 0.3125}, new double[]{0, r - 25, -5}, "RightFrontLeg");
        horsePart(pose, sink, light, new double[]{0.125, 0.75, -0.25}, new double[]{0, -75 + r, 35}, "LeftBackLeg");
        horsePart(pose, sink, light, new double[]{-0.125, 0.75, -0.25}, new double[]{0, -75 + r, -35}, "RightBackLeg");
        horsePart(pose, sink, light, new double[]{0, 1.125, -0.4375}, new double[]{0, 60, 90}, "Tail");
        pose.popPose();

        pose.mulPose(Axis.XP.rotationDegrees(15F));
        pose.pushPose();
        pose.translate(0D, 1D, -0.6875D);
        float s = 0.0625F * 1.125F;
        pose.scale(s, s, s);
        pose.mulPose(Axis.XP.rotationDegrees(180F));
        part(pose, sink, no9, NO9, "Helmet", light, false);
        part(pose, sink, no9, NO9_INSIGNIA, "Insignia", light, false);
        pose.popPose();

        // renderItemIn2D spans (0,0)..(1,1) with its back at z = -1/16; the item model is centred on the origin
        float scale = 0.25F;
        pose.translate(-0.06D, 1.13D, -0.42D);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
        pose.mulPose(Axis.ZP.rotationDegrees(-60F));
        pose.translate(0.5D, 0.5D, -0.03125D);
        if (!state.cigarette.isEmpty()) sink.submitItem(pose, state.cigarette, light, OverlayTexture.NO_OVERLAY, 0);
    }

    private void horsePart(PoseStack pose, SubmitSink sink, int light, double[] offset, double[] rot, String... parts) {
        pose.pushPose();
        pose.translate(offset[0], offset[1], offset[2]);
        pose.mulPose(Axis.YP.rotationDegrees((float) rot[0]));
        pose.mulPose(Axis.XP.rotationDegrees((float) rot[1]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot[2]));
        pose.translate(-offset[0], -offset[1], -offset[2]);
        for (String part : parts) part(pose, sink, horse, NUMBERNINE, part, light, false);
        pose.popPose();
    }

    static void part(PoseStack pose, SubmitSink sink, WavefrontObj model, Identifier texture, String name, int light,
                     boolean cull) {
        sink.submitCustomGeometry(pose, cull ? CompatRenderTypes.entityCutoutCull(texture)
                        : CompatRenderTypes.entityCutoutNoCull(texture),
                (p, buffer) -> model.renderPart(name, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
