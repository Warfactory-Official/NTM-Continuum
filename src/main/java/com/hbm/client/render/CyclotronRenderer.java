package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer for the cyclotron body, plug sockets, and rotating Standard Galactic inscription. */
public final class CyclotronRenderer implements CompatBER<TileEntityMachineCyclotron, CyclotronRenderState> {
    private static final WavefrontObj MODEL = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/cyclotron.obj"));
    private static final Identifier BODY = texture("cyclotron.png");
    private static final Identifier ASHES = texture("cyclotron_ashes.png");
    private static final Identifier ASHES_FILLED = texture("cyclotron_ashes_filled.png");
    private static final Identifier BOOK = texture("cyclotron_book.png");
    private static final Identifier BOOK_FILLED = texture("cyclotron_book_filled.png");
    private static final Identifier GAVEL = texture("cyclotron_gavel.png");
    private static final Identifier GAVEL_FILLED = texture("cyclotron_gavel_filled.png");
    private static final Identifier COIN = texture("cyclotron_coin.png");
    private static final Identifier COIN_FILLED = texture("cyclotron_coin_filled.png");
    private static final String INSCRIPTION = "plures necat crapula quam gladius";

    @Override public CyclotronRenderState createRenderState() { return new CyclotronRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineCyclotron be, Vec3 cameraPosition) { return true; }
    @Override public int viewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineCyclotron be, CyclotronRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        state.ashes = be.getPlug(0);
        state.book = be.getPlug(1);
        state.gavel = be.getPlug(2);
        state.coin = be.getPlug(3);
        state.rotation = (float) (System.currentTimeMillis() * 0.025D % 360D);
    }

    @Override
    public void submit(CyclotronRenderState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        part(sink, pose, "Body", BODY, state.lightCoords);
        part(sink, pose, "B1", state.ashes ? ASHES_FILLED : ASHES, state.lightCoords);
        part(sink, pose, "B2", state.book ? BOOK_FILLED : BOOK, state.lightCoords);
        part(sink, pose, "B3", state.gavel ? GAVEL_FILLED : GAVEL, state.lightCoords);
        part(sink, pose, "B4", state.coin ? COIN_FILLED : COIN, state.lightCoords);

        if (state.ashes && state.book && state.gavel && state.coin) {
            Font font = Minecraft.getInstance().font;
            pose.pushPose();
            pose.translate(0D, 2D, 0D);
            pose.mulPose(Axis.XP.rotationDegrees(180F));
            float rotation = state.rotation;
            for (char character : INSCRIPTION.toCharArray()) {
                Component text = Component.literal(String.valueOf(character));
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(rotation));
                rotation -= font.width(text.getVisualOrderText()) * 2F;
                pose.translate(2.75D, 0D, 0D);
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.scale(0.1F, 0.1F, 0.1F);
                sink.submitText(pose, font, 0F, 0F, text.getVisualOrderText(), false,
                        Font.DisplayMode.POLYGON_OFFSET, 0xF000F0, 0xFF600060, 0);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static void part(SubmitSink sink, PoseStack pose, String name, Identifier texture, int light) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(texture),
                (last, buffer) -> MODEL.renderPart(name, last, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private static Identifier texture(String name) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/" + name);
    }
}
