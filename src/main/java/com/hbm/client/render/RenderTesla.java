package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityTesla;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code RenderTesla}: {@code tesla.obj} turned 180, both-sided, and a random solid arc to every target.
 */
public class RenderTesla extends GemBER<TileEntityTesla, RenderTesla.State> {

    public static class State extends CompatRenderState {
        public final List<Vec3> arcs = new ArrayList<>();
        public int start;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/tesla.obj"));
    private final Surface surface = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/tesla.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        // 1.7: INFINITE_EXTENT_AABB and a 256-block draw distance, so the arcs reach.
        declaration.offScreen().viewDistance(256);
        declaration.part(model.all(), surface).translate(0.5F, 0F, 0.5F).rotate(Gem.Axes.Y, 180F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityTesla be, State state, float partialTicks, Vec3 cameraPosition) {
        state.arcs.clear();
        double sx = be.getBlockPos().getX() + 0.5D;
        double sy = be.getBlockPos().getY() + TileEntityTesla.offset;
        double sz = be.getBlockPos().getZ() + 0.5D;
        // 1.7 negated x/z but drew under the model's 180-degree Y turn, which cancels; the arcs here are
        // submitted in the unrotated block frame, so the vector is the plain source-to-target delta.
        for (Vec3 target : be.targets) state.arcs.add(new Vec3(target.x - sx, target.y - sy, target.z - sz));
        state.start = be.getLevel() == null ? 1 : (int) (be.getLevel().getGameTime() % 1000) + 1;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        if (state.arcs.isEmpty()) return;
        ps.pushPose();
        ps.translate(0.5D, TileEntityTesla.offset, 0.5D);
        for (Vec3 skeleton : state.arcs) {
            double length = skeleton.length();
            BeamPronter.prontMachineBeam(ps, sink, skeleton, BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID,
                    state.start, (int) (length * 5), 0.125F, 0x404040, 0x404040, 2, 0.03125F);
        }
        ps.popPose();
    }
}
