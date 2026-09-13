package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Steam engine: flywheel, shaft, and the connecting rod and piston the crank drives.
 *
 * <p>The rod's anchor moves on two axes at once, which is two slides rather than one — translations
 * commute, so splitting upstream's single call in two is the same placement written where the
 * declaration can read it.
 */
public final class SteamEngineRenderer
        extends GemBER<TileEntityMachineSteamEngine, SteamEngineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/steam_engine.obj"));
    /** old-hbm disabled GL face culling around every steam-engine part, including the hollow backs. */
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/steam_engine.png");

    @Override
    protected void setup(Gem.Model<SteamEngineRenderState> declaration) {
        Gem.Part<SteamEngineRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(2F, 0F, 0F);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Flywheel"), surface)
                .spin(Gem.Axes.Z, 2F, 1.375F, 0F, state -> -state.rotor);
        machine.child(model.part("Shaft"), surface)
                .translate(0F, 1.375F, -0.5F)
                .angle(Gem.Axes.X, state -> state.rotor * 2F)
                .translate(0F, -1.375F, 0.5F);
        machine.child(model.part("Transmission"), surface)
                .slide(Gem.Axes.X, state -> state.crankX)
                .slide(Gem.Axes.Y, state -> state.crankY)
                .translate(2.25F, 1.375F, 0F)
                .angle(Gem.Axes.Z, state -> -state.rodAngle)
                .translate(-2.25F, -1.375F, 0F);
        machine.child(model.part("Piston"), surface)
                .slide(Gem.Axes.X, state -> state.pistonX);
    }

    @Override
    public SteamEngineRenderState createRenderState() {
        return new SteamEngineRenderState();
    }

    @Override
    public void extract(TileEntityMachineSteamEngine be, SteamEngineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (ForgeDirection.getOrientation(meta)) {
            case SOUTH -> 90F;
            case EAST -> 180F;
            case NORTH -> 270F;
            default -> 0F;
        };
        state.rotor = be.getRotor(partialTicks);

        double sin = Math.sin(state.rotor * Math.PI / 180D) * 0.25D - 0.25D;
        double cos = Math.cos(state.rotor * Math.PI / 180D) * 0.25D;
        double cath = Math.sqrt(3.515625D - (cos * cos) / 2D);
        state.crankX = (float) sin;
        state.crankY = (float) cos;
        state.rodAngle = (float) (Math.acos(cos / 1.875D) * 180D / Math.PI - 90D);
        state.pistonX = (float) (1.875D - cath + sin);
    }
}
