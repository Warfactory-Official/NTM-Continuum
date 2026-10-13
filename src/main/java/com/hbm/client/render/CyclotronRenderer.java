package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * The cyclotron body, its four plug sockets, and the Standard Galactic inscription that turns above a
 * fully plugged machine.
 *
 * <p>A socket is one mesh with two textures (empty and filled) so it names the mesh once and picks
 * its skin from the plug it reads. The inscription is not declared at all: it is glyph quads the font
 * atlas builds per frame, with no mesh for either backend to hold.
 */
public final class CyclotronRenderer extends GemBER<TileEntityMachineCyclotron, CyclotronRenderState> {

    private static final String INSCRIPTION = "plures necat crapula quam gladius";

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/cyclotron.obj"));

    @Override
    protected void setup(Gem.Model<CyclotronRenderState> declaration) {
        Gem.Part<CyclotronRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F);

        machine.child(model.part("Body"), surface("cyclotron"));
        socket(machine, "B1", "cyclotron_ashes", state -> state.ashes);
        socket(machine, "B2", "cyclotron_book", state -> state.book);
        socket(machine, "B3", "cyclotron_gavel", state -> state.gavel);
        socket(machine, "B4", "cyclotron_coin", state -> state.coin);
    }

    private void socket(Gem.Part<CyclotronRenderState> machine, String part, String texture,
                        Predicate<CyclotronRenderState> plugged) {
        Surface empty = surface(texture);
        Surface filled = surface(texture + "_filled");
        machine.child(model.part(part), empty)
                .skin(state -> plugged.test(state) ? filled : empty);
    }

    private static Surface surface(String texture) {
        return Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/" + texture + ".png");
    }

    @Override
    public CyclotronRenderState createRenderState() {
        return new CyclotronRenderState();
    }

    @Override
    public void extract(TileEntityMachineCyclotron be, CyclotronRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        state.ashes = be.getPlug(0);
        state.book = be.getPlug(1);
        state.gavel = be.getPlug(2);
        state.coin = be.getPlug(3);
        state.rotation = (float) (GameTime.now() * 0.025D % 360D);
    }

    @Override
    protected void submitUndeclared(CyclotronRenderState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (!(state.ashes && state.book && state.gavel && state.coin)) return;

        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate(0.5D, 2D, 0.5D);
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
}
