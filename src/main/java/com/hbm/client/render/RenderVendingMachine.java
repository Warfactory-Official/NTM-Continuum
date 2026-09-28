package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockVendingMachine;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityVendingMachine;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderVendingMachine}: at the core's centre, turned by facing (north 90, west 180, south 270, east
 * 0), smooth-shaded and unculled, the {@code Soda} or the {@code Obamna} (snack) body of {@code vending_machine.obj}.
 */
public final class RenderVendingMachine extends GemBER<TileEntityVendingMachine, RenderVendingMachine.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public boolean snacks;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/vending_machine.obj"));
    private final Surface skin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/vending_machine.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        root.child(model.part("Soda"), skin).visibleIf(state -> !state.snacks);
        root.child(model.part("Obamna"), skin).visibleIf(state -> state.snacks);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityVendingMachine te, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = te.getBlockState();
        Direction dir = bs.hasProperty(BlockVendingMachine.FACING) ? bs.getValue(BlockVendingMachine.FACING) : Direction.EAST;
        state.yaw = switch (dir) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
        state.snacks = bs.hasProperty(BlockVendingMachine.SNACKS) && bs.getValue(BlockVendingMachine.SNACKS);
    }
}
