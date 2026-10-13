package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.TileEntityAnvil;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the 12 OBJ-modelled anvils, replacing their 1.12.2 {@code forge_marker} OBJ blockstates. One
 * renderer serves all of them (the per-block model + texture are looked up from the BE's block, like
 * {@link DecoObjRenderer}).
 *
 * <p>The anvil OBJs are authored in 0..1 block-local space, so the per-facing yaw (west=0 / north=90 / east=180 /
 * south=270, from the 1.12.2 blockstate variants, applied as the vanilla clockwise blockstate "y" rotation) spins
 * around the block centre.
 */
public class AnvilObjRenderer extends GemBER<TileEntityAnvil, AnvilRenderState> {

    /** The old blockstate "y" per facing. */
    private static final Map<Direction, Float> YAW = Map.of(
            Direction.WEST, 0F, Direction.NORTH, 90F, Direction.EAST, 180F, Direction.SOUTH, 270F);

    private record Tier(Geometry mesh, Surface surface) {}

    private final Map<Block, Tier> tiers = new HashMap<>();

    /**
     * One anvil-shaped slot in the model, filled with whichever of the twelve anvils this block is.
     *
     * <p>Vanilla blockstate "y" rotations are clockwise seen from above, negative around +Y, about the
     * block centre, since the OBJ vertices are in 0..1 corner space.
     */
    @Override
    protected void setup(Gem.Model<AnvilRenderState> declaration) {
        for (RegistryObject<NTMAnvil> anvil : ModAnvilBlocks.ANVILS) {
            String id = anvil.getId().getPath();
            tiers.put(anvil.get(), new Tier(
                    WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/anvils/" + id + ".obj")).all(),
                    Surface.cutout(Constants.MOD_ID + ":textures/models/anvils/" + id + ".png")));
        }
        Tier fallback = tiers.get(ModAnvilBlocks.ANVILS.get(0).get());

        declaration.part(fallback.mesh(), fallback.surface())
                .visibleIf(state -> tiers.containsKey(state.block))
                .mesh(state -> tiers.getOrDefault(state.block, fallback).mesh())
                .skin(state -> tiers.getOrDefault(state.block, fallback).surface())
                .translate(0.5F, 0.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> -state.yaw)
                .translate(-0.5F, 0.0F, -0.5F);
    }

    @Override
    public AnvilRenderState createRenderState() {
        return new AnvilRenderState();
    }

    @Override
    public void extract(TileEntityAnvil blockEntity, AnvilRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = bs.hasProperty(NTMAnvil.FACING) ? YAW.getOrDefault(bs.getValue(NTMAnvil.FACING), 0F) : 0F;
    }
}
