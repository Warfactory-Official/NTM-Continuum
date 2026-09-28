package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderRBMKAutoloader}: the fixed tower and a piston lowered four blocks at full extension. */
public final class RBMKAutoloaderRenderer extends GemBER<BlockEntityRBMKAutoloader, RBMKAutoloaderRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/autoloader.obj"));
    private static final String TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_autoloader.png").toString();

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.cutout(TEXTURE);
        // 1.7: getMaxRenderDistanceSquared 65536, a 256-block view distance
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint().translate(.5F, 0F, .5F);
        root.child(MODEL.part("Base"), skin);
        root.child(MODEL.part("Piston"), skin).slide(Gem.Axes.Y, state -> 4F - state.piston * 4F);
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKAutoloader be, State state, float partial, Vec3 camera) {
        state.piston = (float) (be.lastPiston + (be.renderPiston - be.lastPiston) * partial);
    }

    public static final class State extends CompatRenderState {
        float piston;
    }
}
