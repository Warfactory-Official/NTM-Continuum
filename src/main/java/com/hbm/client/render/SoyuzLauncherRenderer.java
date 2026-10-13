package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.entity.SoyuzMesh;
import com.hbm.blocks.machine.soyuz.SoyuzLauncher;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * The launch table: legs, deck and the two service structures, with the tower and the support arm swung
 * back off the vehicle. Ported from 1.7's {@code RenderSoyuzLauncher} and {@code SoyuzLauncherPronter}.
 *
 * <p>The arms stand open at {@link #OPEN} degrees with an empty pad, close onto a loaded vehicle, and
 * swing back over the last {@link #SWING} ticks of the countdown. The vehicle itself is the same body the
 * flying entity wears, standing five blocks up the tower.
 */
public final class SoyuzLauncherRenderer extends GemBER<TileEntitySoyuzLauncher, SoyuzLauncherRenderer.State> {

    /** How far the tower and the support arm lean back when nothing is on the pad. */
    private static final float OPEN = 45F;
    /** The ticks of countdown over which they lean back again before launch. */
    private static final int SWING = 20;

    private static final Map<String, Geometry> MODELS = new HashMap<>();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> table = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, -SoyuzLauncher.HEIGHT, 0.5F);

        table.child(mesh("legs"), skin("launcher_leg"));
        table.child(mesh("table"), skin("launcher_table"));
        table.child(mesh("tower_base"), skin("launcher_tower_base"));
        table.child(mesh("tower"), skin("launcher_tower"))
                .spin(Gem.Axes.X, 0F, 5.5F, 5.5F, state -> state.swing);
        table.child(mesh("support_base"), skin("launcher_support_base"));
        table.child(mesh("support"), skin("launcher_support"))
                .spin(Gem.Axes.X, 0F, 5.5F, -6.5F, state -> -state.swing);

        SoyuzMesh.declare(table.joint().translate(0F, 5F, 0F).visibleIf(state -> state.loaded),
                state -> state.rocket);
    }

    private static Geometry mesh(String name) {
        return MODELS.computeIfAbsent(name, part -> WavefrontObj.load(
                Ids.of(Constants.MOD_ID, "models/launch_table/soyuz_launcher_" + part + ".obj")).all());
    }

    /** 1.7 draws the whole table with culling off; the deck and the lattices are single-sided sheets. */
    private static Surface skin(String name) {
        return Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/soyuz_launcher/" + name + ".png");
    }


    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntitySoyuzLauncher launcher, State state, float partial, Vec3 camera) {
        state.rocket = launcher.rocketType;
        state.loaded = launcher.rocketType >= 0 && SoyuzMesh.hasSkin(launcher.rocketType);

        if (launcher.starting && launcher.countdown < SWING) {
            state.swing = (SWING - launcher.countdown + partial) * OPEN / SWING;
        } else {
            state.swing = launcher.rocketType >= 0 ? 0F : OPEN;
        }
    }

    public static final class State extends CompatRenderState {
        int rocket;
        boolean loaded;
        float swing;
    }
}
