package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The reformer stands still; only its facing is read. */
public final class CatalyticReformerRenderer
        extends GemBER<TileEntityMachineCatalyticReformer, CatalyticReformerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/catalytic_reformer.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/catalytic_reformer.png");

    @Override
    protected void setup(Gem.Model<CatalyticReformerRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public CatalyticReformerRenderState createRenderState() {
        return new CatalyticReformerRenderState();
    }

    @Override
    public void extract(TileEntityMachineCatalyticReformer be, CatalyticReformerRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = 90F + switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
    }
}
