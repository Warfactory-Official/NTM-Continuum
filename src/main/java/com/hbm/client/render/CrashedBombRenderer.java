package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.blocks.bomb.BlockCrashedBomb.EnumDudType;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderCrashedBomb}: the dud lies at a yaw/pitch/roll and lateral offset rolled from a
 * {@code Random} seeded with the 1.7 position identity {@code (y + z * 27644437) * 27644437 + x}, so
 * the same dud always lies the same way. Nuke and salted duds slide along their axis by 1.25 / 0.5.
 * Culling stayed on upstream; the meshes are drawn single-sided with back faces culled.
 */
public final class CrashedBombRenderer extends GemBER<TileEntityCrashedBomb, CrashedBombRenderer.State> {

    private static final WavefrontObj DUD_BALEFIRE = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/dud_balefire.obj"));
    private static final WavefrontObj DUD_CONVENTIONAL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/dud_conventional.obj"));
    private static final WavefrontObj DUD_NUKE = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/dud_nuke.obj"));
    private static final WavefrontObj DUD_SALTED = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/dud_salted.obj"));

    private static final Surface TEX_BALEFIRE = Surface.cutoutCull("hbm:textures/models/bombs/dud_balefire.png");
    private static final Surface TEX_CONVENTIONAL = Surface.cutoutCull("hbm:textures/models/bombs/dud_conventional.png");
    private static final Surface TEX_NUKE = Surface.cutoutCull("hbm:textures/models/bombs/dud_nuke.png");
    private static final Surface TEX_SALTED = Surface.cutoutCull("hbm:textures/models/bombs/dud_salted.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .angle(Gem.Axes.X, state -> state.pitch)
                .angle(Gem.Axes.Z, state -> state.roll)
                .slide(Gem.Axes.Z, state -> -state.offset);

        root.child(DUD_BALEFIRE.all(), TEX_BALEFIRE).visibleIf(state -> state.type == EnumDudType.BALEFIRE);
        root.child(DUD_CONVENTIONAL.all(), TEX_CONVENTIONAL).visibleIf(state -> state.type == EnumDudType.CONVENTIONAL);
        root.child(DUD_NUKE.all(), TEX_NUKE).visibleIf(state -> state.type == EnumDudType.NUKE).translate(0F, 0F, 1.25F);
        root.child(DUD_SALTED.all(), TEX_SALTED).visibleIf(state -> state.type == EnumDudType.SALTED).translate(0F, 0F, 0.5F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCrashedBomb be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockPos pos = be.getBlockPos();
        Random rand = new Random((pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX());
        state.yaw = (float) (rand.nextDouble() * 360);
        state.pitch = (float) (rand.nextDouble() * 45 + 45);
        state.roll = (float) (rand.nextDouble() * 360);
        state.offset = (float) (rand.nextDouble() * 2 - 1);
        state.type = be.getBlockState().getBlock() instanceof BlockCrashedBomb dud ? dud.type : EnumDudType.CONVENTIONAL;
    }

    public static final class State extends CompatRenderState {
        EnumDudType type = EnumDudType.CONVENTIONAL;
        float yaw;
        float pitch;
        float roll;
        float offset;
    }
}
