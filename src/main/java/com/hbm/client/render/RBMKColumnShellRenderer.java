package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Runtime top geometry for normal/control/boiler columns; bodies stay in JSON to retain culling. */
public final class RBMKColumnShellRenderer<T extends BlockEntityRBMKBase> implements CompatBER<T, RBMKColumnShellRenderer.State> {
    public enum Kind { PLAIN, BOILER }
    private final String sheet; private final Kind kind;
    public RBMKColumnShellRenderer(String sheet, Kind kind) { this.sheet=sheet; this.kind=kind; }
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int viewDistance() { return 256; }
    @Override public void extract(T be, State state, float partial, Vec3 camera) { state.height=RBMKDials.getColumnHeight(be.getLevel()); state.lid=RBMKBase.lidOf(be.getBlockState()); state.sheet=sheet; state.kind=kind;
        // Caps and pipes sit a full column above the core; light them where they render.
        state.topLight = be.getLevel() == null ? 0 : Renderers.lightAt(be.getLevel(), be.getBlockPos().above(state.height)); }
    @Override public void submit(State s, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (s.lid.present()) {
            String prefix=s.lid==RBMKBase.Lid.GLASS ? "_glass" : "_cover";
            pose.pushPose(); pose.translate(0,s.height,0);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(tex(s.sheet + prefix + "_side")),
                    (p, b) -> RBMKColumnGeometry.box(p, b, 0, 0, 0, 1, .25F, 1, s.topLight, 0, false));
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(tex(s.sheet + prefix + "_top")),
                    (p, b) -> RBMKColumnGeometry.box(p, b, 0, 0, 0, 1, .25F, 1, 0, s.topLight, true));
            pose.popPose();
        } else if (s.kind==Kind.BOILER) pipes(s,pose,sink);
    }
    private static void pipes(State s, PoseStack pose, SubmitSink sink) {
        pose.pushPose(); pose.translate(0,s.height,0);
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(tex(s.sheet + "_pipe_side")),
                (p, b) -> { for (float x : new float[]{.0625F, .5625F}) for (float z : new float[]{.0625F, .5625F})
                    RBMKColumnGeometry.box(p, b, x, 0, z, x + .375F, .125F, z + .375F, s.topLight, 0, false); });
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(tex(s.sheet + "_pipe_top")),
                (p, b) -> { for (float x : new float[]{.0625F, .5625F}) for (float z : new float[]{.0625F, .5625F})
                    RBMKColumnGeometry.box(p, b, x, 0, z, x + .375F, .125F, z + .375F, 0, s.topLight, true); });
        pose.popPose();
    }
    private static Identifier tex(String name) { return Ids.of(Constants.MOD_ID,"textures/blocks/rbmk/"+name+".png"); }
    public static final class State extends CompatRenderState { int height; int topLight; RBMKBase.Lid lid; String sheet; Kind kind; }
}
