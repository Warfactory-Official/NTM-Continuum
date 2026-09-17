package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.PumpAnimationState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Faithful split-part declaration for both legacy groundwater pump variants. */
public final class PumpRenderer<T extends BlockEntity & PumpAnimationState>
        extends GemBER<T, PumpRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/pump.obj"));
    private final Surface surface;

    public PumpRenderer(Identifier texture) {
        this.surface = Surface.cutoutNoCull(texture.toString());
    }

    @Override
    protected void setup(Gem.Model<PumpRenderState> declaration) {
        Gem.Part<PumpRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Rotor"), surface)
                .spin(Gem.Axes.Z, 0F, 2.25F, 0F, state -> state.rotation - 90F);
        machine.child(model.part("Arms"), surface)
                .slide(Gem.Axes.Y, state -> state.lift)
                .spin(Gem.Axes.Z, 0F, 4.75F, 0F, state -> -state.armAngle);
        machine.child(model.part("Piston"), surface)
                .slide(Gem.Axes.Y, state -> state.lift);
    }

    @Override
    public PumpRenderState createRenderState() {
        return new PumpRenderState();
    }

    /**
     * The crank geometry upstream solves inline: the piston rides the sine of the rotor while the arms
     * swing by the angle that keeps their fixed length spanning the gap. It is resolved here rather than
     * in the declaration so the trigonometry runs once a frame instead of once per part that reads it.
     */
    @Override
    public void extract(T be, PumpRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (ForgeDirection.getOrientation(meta)) {
            case SOUTH -> 90F;
            case EAST -> 180F;
            case NORTH -> 270F;
            default -> 0F;
        };
        state.rotation = be.getLastRotor() + (be.getRotor() - be.getLastRotor()) * partialTicks;

        double sin = Math.sin(state.rotation * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(state.rotation * Math.PI / 180D) * 0.5D;
        double cath = Math.sqrt(1D + (cos * cos) / 2D);
        state.armAngle = (float) (Math.acos(cos / 2D) * 180D / Math.PI - 90D);
        state.lift = (float) (1D - cath + sin);
    }
}
