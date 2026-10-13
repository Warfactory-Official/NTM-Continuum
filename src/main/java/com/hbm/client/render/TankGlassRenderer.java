package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** A tank machine: the body, the fluid column standing as tall as it is full, and the glass over it. */
public class TankGlassRenderer<T extends BlockEntity> extends GemBER<T, TankGlassRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    /** The old pass drew the shell at rgb 0.75,1,1 and 15% alpha with lighting disabled. */
    private static final int GLASS = 0x26BFFFFF;

    private final WavefrontObj model;
    private final Surface body;
    private final Surface glass;
    private final Function<T, FluidTankNTM> tankGetter;

    public TankGlassRenderer(String objName, String texName, Function<T, FluidTankNTM> tankGetter) {
        this.model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/" + objName + ".obj"));
        String texture = Constants.MOD_ID + ":textures/models/machines/" + texName + ".png";
        this.body = Surface.cutout(texture);
        this.glass = Surface.translucent(texture);
        this.tankGetter = tankGetter;
    }

    @Override
    protected void setup(Gem.Model<TankGlassRenderState> declaration) {
        Gem.Part<TankGlassRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Main"), body);
        machine.child(model.part("Fluid"), body)
                .visibleIf(state -> state.fillFraction > 0F)
                .light(state -> FULLBRIGHT)
                .tint(state -> 0xFF000000 | (state.fluidColor & 0xFFFFFF))
                .translate(0F, 1F, 0F)
                .stretch(Gem.Axes.Y, state -> state.fillFraction)
                .translate(0F, -1F, 0F);
        machine.child(model.part("Glass"), glass)
                .tint(state -> GLASS);
    }

    @Override
    public TankGlassRenderState createRenderState() {
        return new TankGlassRenderState();
    }

    /** The old renderer applies no facing rotation, but the yaw slot is kept for parity if needed. */
    @Override
    public void extract(T be, TankGlassRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = 0F;
        FluidTankNTM tank = tankGetter.apply(be);
        state.fillFraction = tank.getMaxFill() <= 0 ? 0F : (float) tank.getFill() / (float) tank.getMaxFill();
        state.fluidColor = tank.getTankType().getColor();
    }
}
