package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Ammo press: the frame, the descending press, and the shell tray that rises to meet it. */
public class AmmoPressRenderer extends GemBER<TileEntityMachineAmmoPress, AmmoPressRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of("hbm", "models/machines/ammo_press.obj"));
    /** The legacy TESR explicitly disabled face culling for this model. */
    private final Surface surface = Surface.cutoutNoCull("hbm:textures/models/machines/ammo_press.png");

    @Override
    protected void setup(Gem.Model<AmmoPressRenderState> declaration) {
        Gem.Part<AmmoPressRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Frame"), surface);
        machine.child(model.part("Press"), surface)
                .slide(Gem.Axes.Y, state -> -state.press * 0.25F);

        Gem.Part<AmmoPressRenderState> tray = machine.joint()
                .slide(Gem.Axes.Y, state -> state.lift * 0.5F - 0.5F);
        tray.child(model.part("Shells"), surface);
        tray.child(model.part("Bullets"), surface)
                .visibleIf(state -> state.showBullets);
    }

    @Override
    public AmmoPressRenderState createRenderState() {
        return new AmmoPressRenderState();
    }

    @Override
    public void extract(TileEntityMachineAmmoPress be, AmmoPressRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.lift = be.prevLift + (be.lift - be.prevLift) * partialTicks;
        state.press = be.prevPress + (be.press - be.prevPress) * partialTicks;
        state.showBullets = be.animState == TileEntityMachineAmmoPress.AnimationState.RETRACTING
                || be.animState == TileEntityMachineAmmoPress.AnimationState.LOWERING;
    }
}
