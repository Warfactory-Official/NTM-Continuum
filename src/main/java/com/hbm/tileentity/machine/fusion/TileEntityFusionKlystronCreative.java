package com.hbm.tileentity.machine.fusion;

import com.hbm.capability.MachineCaps;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative klystron — emits the highest ignition temperature any fuel cycle asks for, unconditionally,
 * with no power draw, no air and no target to set. It exists so the rest of the fusion chain can be
 * built and tested without first standing up a power grid.
 *
 * <p>Having no throttle, its only readout is whether the beam is actually landing on a torus;
 * {@link #isConnected} drives both the fan and the indicator.
 */
public class TileEntityFusionKlystronCreative extends TileEntityFusionKlystron {

    public boolean isConnected = false;

    public TileEntityFusionKlystronCreative(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(), pos, state);
    }

    @Override public String getDefaultName() { return "container.fusionKlystronCreative"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionKlystronCreative te) {
        // maxInput is only known once the cycles are loaded; registration is idempotent.
        FusionRecipes.INSTANCE.registerRecipes();
        FusionLink.claim(level, te, FusionNodes.emitter(pos, FusionNodes.facing(state)),
                MachineCaps.KLYSTRON_OUT);
        te.isConnected = FusionNodes.provideKyU(level, pos, FusionNodes.facing(state),
                FusionRecipes.INSTANCE.maxInput);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFusionKlystronCreative te) {
        te.spinFan(te.isConnected, 1F);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isConnected);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isConnected = buf.readBoolean();
    }
}
