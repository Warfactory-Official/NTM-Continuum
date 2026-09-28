package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntitySnowglobe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderSnowglobe}: turned {@code -(22.5 * rotation + 90)} about Y, scaled to 1/16, the socket, the
 * glass and the type's scene - smooth, unculled - and the type's label written flat along the socket's front.
 */
public final class RenderSnowglobe extends GemBER<TileEntitySnowglobe, RenderSnowglobe.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public SnowglobeType type = SnowglobeType.NONE;
    }

    static final Identifier SOCKET = Ids.of(Constants.MOD_ID, "textures/models/trinkets/snowglobe.png");
    static final Identifier GLASS = Ids.of(Constants.MOD_ID, "textures/models/trinkets/snowglobe_glass.png");
    static final Identifier FEATURES = Ids.of(Constants.MOD_ID, "textures/models/trinkets/snowglobe_features.png");

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/trinkets/snowglobe.obj"));
    private final Font font = Minecraft.getInstance().font;

    @Override
    protected void setup(Gem.Model<State> declaration) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntitySnowglobe globe, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = -(22.5F * globe.getBlockState().getValue(BlockTrinket.ROTATION) + 90F);
        state.type = globe.type();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int light = state.lightCoords;

        float scale = 0.0625F;
        pose.scale(scale, scale, scale);
        part(pose, sink, SOCKET, "Socket", light);
        part(pose, sink, GLASS, "Glass", light);
        String scene = switch (state.type) {
            case RIVETCITY -> "RivetCity";
            case TENPENNYTOWER -> "TenpennyTower";
            case LUCKY38 -> "Lucky38";
            case SIERRAMADRE -> "SierraMadre";
            case PRYDWEN -> "Prydwen";
            default -> null;
        };
        if (scene != null) part(pose, sink, FEATURES, scene, light);

        float f3 = 0.05F;
        pose.translate(4.025D, 0.5D, 0D);
        pose.scale(f3, -f3, f3);
        pose.translate(0D, -font.lineHeight / 2F, font.width(state.type.label) * 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.translate(0F, 1F, 0F);
        sink.submitText(pose, font, 0F, 0F, Component.literal(state.type.label).getVisualOrderText(), false,
                Font.DisplayMode.NORMAL, light, 0xFFFFFFFF, 0);
        pose.popPose();
    }

    private void part(PoseStack pose, SubmitSink sink, Identifier texture, String name, int light) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(texture),
                (p, buffer) -> model.renderPart(name, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
