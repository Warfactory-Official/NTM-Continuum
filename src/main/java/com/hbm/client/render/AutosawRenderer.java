package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Autosaw: a turret base carrying a three-segment arm with the blade on its tip.
 *
 * <p>Upstream walks one pose stack down the arm without pushing, so each segment inherits everything
 * before it — a bone chain written as a straight line. Declared it is the same chain nested, and the
 * two short {@code ±0.01} shifts around the lower arm are kept where they were: the second is upstream
 * undoing the first before the tip's own transform, not a shift of its own.
 */
public final class AutosawRenderer extends GemBER<TileEntityMachineAutosaw, AutosawRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/autosaw.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/autosaw.png");

    @Override
    protected void setup(Gem.Model<AutosawRenderState> declaration) {
        Gem.Part<AutosawRenderState> ground = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F);
        ground.child(model.part("Base"), surface);

        Gem.Part<AutosawRenderState> turret = ground.joint()
                .angle(Gem.Axes.Y, state -> -state.yaw);
        turret.child(model.part("Main"), surface);
        turret.child(model.part("Engine"), surface)
                .slide(Gem.Axes.Y, state -> (float) (state.engine * 0.01D));

        Gem.Part<AutosawRenderState> upper = turret.joint()
                .translate(0F, 1.75F, 0F)
                .angle(Gem.Axes.X, state -> state.angle)
                .translate(0F, -1.75F, 0F);
        upper.child(model.part("ArmUpper"), surface);

        Gem.Part<AutosawRenderState> lower = upper.joint()
                .translate(0F, 1.75F, -4F)
                .angle(Gem.Axes.X, state -> -state.angle * 2F)
                .translate(0F, -1.75F, 4F)
                .translate(-0.01F, 0F, 0F);
        lower.child(model.part("ArmLower"), surface);

        Gem.Part<AutosawRenderState> tip = lower.joint()
                .translate(0.01F, 0F, 0F)
                .translate(0F, 1.75F, -8F)
                .angle(Gem.Axes.X, state -> state.angle)
                .translate(0F, -1.75F, 8F);
        tip.child(model.part("ArmTip"), surface);

        tip.joint()
                .translate(0F, 1.75F, -10F)
                .angle(Gem.Axes.Y, state -> -state.spin)
                .translate(0F, -1.75F, 10F)
                .child(model.part("Sawblade"), surface);
    }

    @Override
    public AutosawRenderState createRenderState() {
        return new AutosawRenderState();
    }

    @Override
    public void extract(TileEntityMachineAutosaw blockEntity, AutosawRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        state.yaw = blockEntity.prevRotationYaw
                + (blockEntity.rotationYaw - blockEntity.prevRotationYaw) * partialTicks;
        state.angle = 80F - (blockEntity.prevRotationPitch
                + (blockEntity.rotationPitch - blockEntity.prevRotationPitch) * partialTicks);
        state.spin = blockEntity.lastSpin
                + (blockEntity.spin - blockEntity.lastSpin) * partialTicks;
        Level level = blockEntity.getLevel();
        state.engine = blockEntity.isOn && level != null
                ? Math.sin(level.getGameTime() * 2D % (Math.PI * 2D) + partialTicks) : 0D;
    }
}
