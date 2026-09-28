package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.Charger;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityCharger;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.7 {@code RenderCharger}: the base turned by the block's facing (a quarter turn, then 0/90/180/270 for
 * metadata 2/4/3/5); the clamp carrier tilted 10 degrees about (-0.34375, 0.25) and lowered by a quarter block as
 * it extends over the first half of the animation; the two clamps swivelling 30 degrees apart about y 0.28 over
 * the second half. The amber lamp is untextured and fullbright, and so is the slide, which 1.7 drew while the
 * lighting was still off.
 */
public class RenderCharger extends GemBER<TileEntityCharger, RenderCharger.State> {

    public static class State extends CompatRenderState {
        public int meta = 2;
        public float extend;
        public float swivel;
    }

    private WavefrontObj model;

    @Override
    protected void setup(Gem.Model<State> declaration) {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/charger.obj"));
        Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/charger.png");
        Surface white = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/misc/white.png");

        Gem.Part<State> body = declaration.joint().pose(state -> {
            Matrix4f m = new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY((float) Math.toRadians(90));
            switch (state.meta) {
                case 4 -> m.rotateY((float) Math.toRadians(90));
                case 3 -> m.rotateY((float) Math.toRadians(180));
                case 5 -> m.rotateY((float) Math.toRadians(270));
                default -> { }
            }
            return m;
        });
        body.child(model.part("Base"), surface);

        Gem.Part<State> carrier = body.joint().pose(RenderCharger::carrier);
        carrier.joint().pose(state -> swivel(30F * state.swivel)).child(model.part("Left"), surface);
        carrier.joint().pose(state -> swivel(-30F * state.swivel)).child(model.part("Right"), surface);

        body.child(model.part("Light"), white).tint(state -> 0xFFFFBF00).light(state -> 0xF000F0);
        body.joint().pose(RenderCharger::carrier).child(model.part("Slide"), surface).light(state -> 0xF000F0);
    }

    /** The clamp carrier: tilted 10 degrees about (-0.34375, 0.25), then lowered by a quarter of the extension. */
    private static Matrix4f carrier(State state) {
        return new Matrix4f()
                .translate(-0.34375F, 0.25F, 0F).rotateZ((float) Math.toRadians(10)).translate(0.34375F, -0.25F, 0F)
                .translate(0F, -0.25F * state.extend, 0F);
    }

    private static Matrix4f swivel(float degrees) {
        return new Matrix4f().translate(0F, 0.28F, 0F).rotateX((float) Math.toRadians(degrees)).translate(0F, -0.28F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCharger charger, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = charger.getBlockState();
        Direction facing = bs.hasProperty(Charger.FACING) ? bs.getValue(Charger.FACING) : Direction.NORTH;
        state.meta = facing.get3DDataValue(); // north 2, south 3, west 4, east 5, the 1.7 metadata
        double time = (charger.lastUsingTicks + (charger.usingTicks - charger.lastUsingTicks) * partialTicks)
                / (double) TileEntityCharger.DELAY;
        state.extend = (float) Math.min(1, time * 2);
        state.swivel = (float) Math.max(0, (time - 0.5) * 2);
    }
}
