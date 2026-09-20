package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityCraneSplitter;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderSplitter}: the {@code splitter.obj} parts, each wearing its own block sprite. The core (left lane)
 * draws itself and the right lane one block over, which 1.7 drew as the dummy block with the mirrored sheets.
 * The animated belt sprite is drawn from a still first frame: a Gem surface is a whole texture, not a sprite.
 */
public class SplitterRenderer extends GemBER<TileEntityCraneSplitter, SplitterRenderer.State> {

    public static class State extends CompatRenderState {
        public float yaw;
    }

    private static final String T = Constants.MOD_ID + ":textures/blocks/";

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/blocks/splitter.obj"));

    private final Surface topLeft = Surface.cutout(T + "crane_splitter_top_left.png");
    private final Surface topRight = Surface.cutout(T + "crane_splitter_top_right.png");
    private final Surface frontLeft = Surface.cutout(T + "crane_splitter_front_left.png");
    private final Surface frontRight = Surface.cutout(T + "crane_splitter_front_right.png");
    private final Surface backLeft = Surface.cutout(T + "crane_splitter_back_left.png");
    private final Surface backRight = Surface.cutout(T + "crane_splitter_back_right.png");
    private final Surface left = Surface.cutout(T + "crane_splitter_left.png");
    private final Surface right = Surface.cutout(T + "crane_splitter_right.png");
    private final Surface belt = Surface.cutout(T + "crane_splitter_belt_still.png");
    private final Surface inner = Surface.cutout(T + "crane_splitter_inner.png");
    private final Surface innerSide = Surface.cutout(T + "crane_splitter_inner_side.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        Gem.Part<State> root = declaration.joint().translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, state -> state.yaw);
        lane(root.joint(), true);
        // 1.7: the right lane is the dummy at facing.getRotation(DOWN), which is -Z in the unturned model frame
        lane(root.joint().translate(0F, 0F, -1F), false);
    }

    /** 1.7 {@code drawSplitter(isLeft)}. */
    private void lane(Gem.Part<State> part, boolean isLeft) {
        part.child(model.part("Top"), isLeft ? topLeft : topRight);
        part.child(model.part("Bottom"), isLeft ? topRight : topLeft);
        if (isLeft) part.child(model.part("Left"), left);
        if (!isLeft) part.child(model.part("Right"), right);
        part.child(model.part("Back"), isLeft ? backLeft : backRight);
        part.child(model.part("Front"), isLeft ? frontLeft : frontRight);
        part.child(model.part("Inner"), inner);
        part.child(model.part("InnerLeft"), innerSide);
        part.child(model.part("InnerRight"), innerSide);
        part.child(model.part("InnerTop"), innerSide);
        part.child(model.part("InnerBottom"), belt);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCraneSplitter be, State state, float partialTicks, Vec3 cameraPosition) {
        // 1.7: meta 12/4 -> 90, 13/5 -> 270, 14/3 -> 180, 15/2 -> 0 (core meta minus the dummyable offset)
        state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
            case 2 -> 90F;
            case 3 -> 270F;
            case 4 -> 180F;
            default -> 0F;
        };
    }
}
