package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The sat link dish: a fixed base, a rotor that swings round, and a dish that tips up when the machine has
 * something to talk to. The dish pivots about the point 7.375 up the mast, which is where the model's own
 * hinge is.
 *
 * <p>The mast stands six blocks above the core cell, so the machine must not be culled by its own cell.
 */
public class SatLinkRenderer extends GemBER<TileEntityMachineSatLink, SatLinkRenderState> {

    private static final float PIVOT = 7.375F;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/satlink.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/satlink.png");

    @Override
    protected void setup(Gem.Model<SatLinkRenderState> declaration) {
        Gem.Part<SatLinkRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F)
                .slide(Gem.Axes.X, state -> state.offsetX)
                .slide(Gem.Axes.Z, state -> state.offsetZ);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Rotor"), surface)
                .angle(Gem.Axes.Y, state -> state.rot)
                .child(model.part("Dish"), surface)
                .spin(Gem.Axes.Z, 0F, PIVOT, 0F, state -> state.lift);
    }

    @Override
    public SatLinkRenderState createRenderState() {
        return new SatLinkRenderState();
    }

    @Override
    public void extract(TileEntityMachineSatLink be, SatLinkRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.rot = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        state.lift = be.prevLift + (be.lift - be.prevLift) * partialTicks;

        BlockState bs = be.getBlockState();
        ForgeDirection dir = MachineCore.facing(bs);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        state.offsetX = (dir.offsetX + rot.offsetX) * 0.5F;
        state.offsetZ = (dir.offsetZ + rot.offsetZ) * 0.5F;
    }
}
