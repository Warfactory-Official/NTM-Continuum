package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Powered condenser: a static shell with its two fans turning against each other. */
public final class CondenserRenderer extends GemBER<TileEntityCondenserPowered, CondenserRenderState> {

    /** An addon's wrecked condenser ({@link DamagedView}, ADDON-API-INVENTORY C-23); null in plain HBM. */
    public static volatile @org.jspecify.annotations.Nullable DamagedView DAMAGED;

    /** The view this renderer was declared with (read once, in {@link #setup}). */
    private @org.jspecify.annotations.Nullable DamagedView damagedView;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/condenser.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/condenser.png");

    @Override
    protected void setup(Gem.Model<CondenserRenderState> declaration) {
        Gem.Part<CondenserRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        Gem.Part<CondenserRenderState> intact = machine.joint().visibleIf(state -> !state.damaged);
        intact.child(model.part("Condenser"), surface);
        intact.child(model.part("Fan1"), surface)
                .spin(Gem.Axes.X, 0F, 1.5F, 0F, state -> state.spin);
        intact.child(model.part("Fan2"), surface)
                .spin(Gem.Axes.X, 0F, 1.5F, 0F, state -> -state.spin);
        damagedView = DAMAGED;
        if (damagedView != null) {
            machine.joint().visibleIf(state -> state.damaged).rotate(Gem.Axes.Y, damagedView.extraYaw())
                    .child(WavefrontObj.load(Ids.parse(damagedView.model())).all(!damagedView.cull()),
                            damagedView.cull() ? Surface.cutoutCull(damagedView.texture()) : Surface.cutoutNoCull(damagedView.texture()));
        }
    }

    @Override
    public CondenserRenderState createRenderState() {
        return new CondenserRenderState();
    }

    @Override
    public void extract(TileEntityCondenserPowered be, CondenserRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        ForgeDirection facing = ForgeDirection.getOrientation(meta);
        state.yaw = 90F + facing.getRotationDegrees();
        state.spin = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        state.damaged = damagedView != null && damagedView.damaged().test(be);
    }
}
