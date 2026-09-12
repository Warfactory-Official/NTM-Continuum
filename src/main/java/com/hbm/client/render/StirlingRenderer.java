package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityStirling;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * Faithful split-part declaration for the legacy Stirling engine OBJ.
 *
 * <p>The engine's three tiers are three textures over one mesh, so each part names the mesh once and
 * picks its skin from the tier.
 */
public final class StirlingRenderer extends GemBER<TileEntityStirling, StirlingRenderState> {

    private static final Surface[] SURFACES = {skin("stirling"), skin("stirling_steel"), skin("stirling_creative")};

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/stirling.obj"));

    @Override
    protected void setup(Gem.Model<StirlingRenderState> declaration) {
        Gem.Part<StirlingRenderState> engine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        engine.child(model.part("Base"), SURFACES[0]).skin(StirlingRenderer::skinOf);
        engine.child(model.part("Cog"), SURFACES[0]).skin(StirlingRenderer::skinOf)
                .visibleIf(state -> state.hasCog)
                .spin(Gem.Axes.Z, 0F, 1.375F, 0F, state -> -state.rotation);
        engine.child(model.part("CogSmall"), SURFACES[0]).skin(StirlingRenderer::skinOf)
                .translate(0F, 1.375F, 0.25F)
                .angle(Gem.Axes.X, state -> state.rotation * 2F + 3F)
                .translate(0F, -1.375F, -0.25F);
        engine.child(model.part("Piston"), SURFACES[0]).skin(StirlingRenderer::skinOf)
                .slide(Gem.Axes.X, state -> (float) (Math.sin(state.rotation * Math.PI / 90D) * 0.25D + 0.125D));
    }

    private static Surface skinOf(StirlingRenderState state) {
        return SURFACES[variantOf(state)];
    }

    private static Surface skin(String texture) {
        return Surface.cutout(Constants.MOD_ID + ":textures/models/machines/" + texture + ".png");
    }

    /** Tier 0 is the plain engine, 2 the creative one; everything else draws as steel. */
    private static int variantOf(StirlingRenderState state) {
        return state.type == 0 ? 0 : state.type == 2 ? 2 : 1;
    }

    @Override
    public StirlingRenderState createRenderState() {
        return new StirlingRenderState();
    }

    @Override
    public void extract(TileEntityStirling be, StirlingRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = be.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.yaw = switch (ForgeDirection.getOrientation(meta)) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
        state.rotation = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        state.type = be.getGearVariant();
        state.hasCog = be.hasCog;
    }
}
