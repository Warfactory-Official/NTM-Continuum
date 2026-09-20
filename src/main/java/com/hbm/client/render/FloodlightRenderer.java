package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.machine.Floodlight;
import com.hbm.tileentity.machine.TileEntityFloodlight;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.7 {@code RenderFloodlight}: the base turns with the block's facing (the old six-way metadata table,
 * plus the quarter turn every non-vertical fixture got), the lamp head pitches by the tile's
 * {@code rotation} about the hinge half a block up, and the lamps draw fullbright while powered or at
 * a quarter brightness while dark.
 */
public class FloodlightRenderer extends GemBER<TileEntityFloodlight, FloodlightRenderer.State> {

    public static class State extends CompatRenderState {
        public int meta;
        public float rotation;
        public boolean isOn;
    }

    private WavefrontObj model;
    private Surface surface;

    @Override
    protected void setup(Gem.Model<State> declaration) {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/floodlight.obj"));
        surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/floodlight.png");
        // 1.7 getRenderBoundingBox spanned 3x3x3 around the fixture; offScreen keeps the tilted head from being culled.
        declaration.offScreen().viewDistance(128);

        Gem.Part<State> body = declaration.joint().pose(state -> {
            Matrix4f m = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
            switch (state.meta) {
                case 0, 6 -> m.rotateX((float) Math.PI);
                case 1, 7 -> { }
                case 2 -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI);
                case 3 -> m.rotateX((float) Math.PI / 2);
                case 4 -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI / 2);
                case 5 -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI * 1.5F);
                default -> { }
            }
            m.translate(0F, -0.5F, 0F);
            if (state.meta != 0 && state.meta != 1) m.rotateY((float) Math.PI / 2);
            return m;
        });
        body.child(model.part("Base"), surface);
        Gem.Part<State> head = body.joint().pose(state -> {
            float rotation = state.rotation;
            if (state.meta == 0 || state.meta == 6) rotation -= 90;
            if (state.meta == 1 || state.meta == 7) rotation += 90;
            return new Matrix4f().translate(0F, 0.5F, 0F).rotateZ((float) Math.toRadians(rotation)).translate(0F, -0.5F, 0F);
        });
        head.child(model.part("Lights"), surface);
        head.child(model.part("Lamps"), surface)
                .tint(state -> state.isOn ? 0xFFFFFFFF : 0xFF404040)
                .light(state -> state.isOn ? 0xF000F0 : state.lightCoords);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityFloodlight blockEntity, State state, float partialTicks, Vec3 cameraPosition) {
        state.meta = Floodlight.meta(blockEntity.getBlockState());
        state.rotation = blockEntity.rotation;
        state.isOn = blockEntity.isOn;
    }
}
