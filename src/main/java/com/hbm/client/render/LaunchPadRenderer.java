package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The launch pad: the silo, plus <b>the missile standing on it</b>, as 1.12.2's {@code RenderLaunchPad} draws it.
 *
 * <p>A loaded pad shows its missile sitting on it, so a player can see at a glance whether the pad is loaded.
 *
 * <p>Upstream's geometry: the silo at the pad's facing, then {@code translate(0, 1, 0)} and the missile body drawn
 * <em>inside the same matrix</em>, so it inherits the pad's rotation. Which body and at what scale comes from
 * {@link MissileModels}.
 *
 * <p>The missile comes from {@code TileEntityLaunchPad.toRender}, which the block entity syncs from
 * slot 0 in its network pack. The client never sees the pad's inventory otherwise.
 */
public class LaunchPadRenderer extends GemBER<TileEntityLaunchPad, LaunchPadRenderState> {

    /** Upstream's TESR base yaw for the silo. */
    private static final float BASE_YAW = 90F;

    private final WavefrontObj silo = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/weapons/launch_pad_silo.obj"));
    private final Surface siloSurface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/launchpad/silo.png");

    /**
     * The silo, and one missile-shaped slot above it holding whichever body the loaded missile has, in
     * that missile's own paint and at its own scale. The silo and a tier-4 missile stand well outside
     * the core's own block section, hence the off-screen pass.
     */
    @Override
    protected void setup(Gem.Model<LaunchPadRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(silo.all(true), siloSurface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        Map<WavefrontObj, Geometry> bodies = new IdentityHashMap<>();
        for (WavefrontObj body : MissileModels.meshes()) bodies.put(body, body.all(true));
        Geometry fallback = bodies.values().iterator().next();

        declaration.part(fallback, siloSurface)
                .visibleIf(state -> state.missile != null)
                .mesh(state -> state.missile == null ? fallback : bodies.get(state.missile.model()))
                .skin(state -> state.missile == null ? siloSurface : state.missile.surface())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw)
                .translate(0F, 1F, 0F)
                .stretch(state -> state.missile == null ? 1F : state.missile.scale());
    }

    @Override
    public LaunchPadRenderState createRenderState() {
        return new LaunchPadRenderState();
    }

    @Override
    public void extract(TileEntityLaunchPad pad, LaunchPadRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = pad.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };

        ItemStack loaded = pad.toRender;
        state.missile = loaded == null || loaded.isEmpty() ? null : MissileModels.get(loaded.getItem());
    }
}
