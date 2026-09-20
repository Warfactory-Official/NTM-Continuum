package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.bomb.BombMulti;
import com.hbm.tileentity.bomb.TileEntityBombMulti;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderBombMulti}: {@code BombGeneric.obj} drawn upside down (a 180-degree X flip) with
 * the yaw map 5 -> 90, 2 -> 0, 4 -> 270, 3 -> 180 and culling off.
 */
public final class BombMultiRenderer extends GemBER<TileEntityBombMulti, MachineRenderState> {

    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/bomb_generic.obj"));
    private static final Surface TEXTURE = Surface.cutout("hbm:textures/models/bombs/bomb_generic.png");

    @Override
    protected void setup(Gem.Model<MachineRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(MODEL.all(true), TEXTURE)
                .translate(0.5F, 0.5F, 0.5F)
                .rotate(Gem.Axes.X, 180F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public void extract(TileEntityBombMulti be, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int facing = bs.hasProperty(BombMulti.FACING) ? bs.getValue(BombMulti.FACING) : 2;
        state.yaw = switch (facing) {
            case 5 -> 90F;
            case 4 -> 270F;
            case 3 -> 180F;
            default -> 0F;
        };
    }
}
