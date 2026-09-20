package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.bomb.Landmine;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderLandmine}: one renderer for the five mines, choosing the OBJ and transform by the
 * block. The AP mine picks its skin from the terrain: stone when buried more than two blocks below
 * the surface, snow in a snowy biome, sand in a hot dry one, grass otherwise. Culling was disabled
 * upstream, so every mesh is drawn double-sided.
 */
public final class LandmineRenderer extends GemBER<TileEntityLandmine, LandmineRenderer.State> {

    private static final WavefrontObj MINE_AP = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/ap_mine.obj"));
    private static final WavefrontObj MINE_MARELET = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/marelet.obj"));
    private static final WavefrontObj MINE_FAT = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/mine_fat.obj"));
    private static final WavefrontObj MINE_NAVAL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/naval_mine.obj"));

    private static final Surface[] AP_SKINS = {
            Surface.cutout("hbm:textures/models/bombs/mine_ap_grass.png"),
            Surface.cutout("hbm:textures/models/bombs/mine_ap_desert.png"),
            Surface.cutout("hbm:textures/models/bombs/mine_ap_snow.png"),
            Surface.cutout("hbm:textures/models/bombs/mine_ap_stone.png")
    };
    private static final Surface MARELET = Surface.cutout("hbm:textures/models/bombs/mine_marelet.png");
    private static final Surface SHRAPNEL = Surface.cutout("hbm:textures/models/bombs/mine_shrapnel.png");
    private static final Surface FAT = Surface.cutout("hbm:textures/models/bombs/mine_fat.png");
    private static final Surface NAVAL = Surface.cutout("hbm:textures/models/bombs/mine_naval.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);

        root.child(MINE_AP.all(true), AP_SKINS[0])
                .visibleIf(state -> state.type == Landmine.Type.AP)
                .skin(state -> AP_SKINS[state.apSkin])
                .scale(0.375F)
                .translate(0F, -0.0625F * 3.5F, 0F);

        root.child(MINE_MARELET.all(true), MARELET)
                .visibleIf(state -> state.type == Landmine.Type.HE)
                .rotate(Gem.Axes.Y, -90F);

        root.child(MINE_AP.all(true), SHRAPNEL)
                .visibleIf(state -> state.type == Landmine.Type.SHRAP)
                .scale(0.375F)
                .translate(0F, -0.0625F * 3.5F, 0F);

        root.child(MINE_FAT.all(true), FAT)
                .visibleIf(state -> state.type == Landmine.Type.FAT)
                .scale(0.25F);

        root.child(MINE_NAVAL.all(true), NAVAL)
                .visibleIf(state -> state.type == Landmine.Type.NAVAL)
                .translate(0F, 0.5F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityLandmine be, State state, float partialTicks, Vec3 cameraPosition) {
        state.type = be.getBlockState().getBlock() instanceof Landmine mine ? mine.type : Landmine.Type.AP;
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (state.type == Landmine.Type.AP && level != null) {
            Biome biome = level.getBiome(pos).value();
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 2) state.apSkin = 3;
            // 1.7 getEnableSnow: the snowy-biome flag; vanilla's own sea-level snow threshold is temperature < 0.15.
            else if (biome.getBaseTemperature() < 0.15F) state.apSkin = 2;
            // 1.7: temperature >= 1.5 && rainfall <= 0.1; downfall has no public getter, "no precipitation" is that.
            else if (biome.getBaseTemperature() >= 1.5F && !biome.hasPrecipitation()) state.apSkin = 1;
            else state.apSkin = 0;
        } else {
            state.apSkin = 0;
        }
    }

    public static final class State extends CompatRenderState {
        Landmine.Type type = Landmine.Type.AP;
        int apSkin;
    }
}
