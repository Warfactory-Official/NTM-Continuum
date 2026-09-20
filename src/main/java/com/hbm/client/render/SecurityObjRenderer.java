package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.generic.BarbedWire;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.tileentity.TileEntitySecurityObj;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the OBJ perimeter blocks: {@code barbed_wire.obj} skinned with the variant's
 * {@code <id>_model.png} and {@code spikes.obj} with {@code spikes.png}, each at the block's centre
 * ({@code x + 0.5, y, z + 0.5}) like 1.7.10's {@code RenderBarbedWire} / {@code RenderSpikeBlock}.
 * The wire is yawed the way the old renderer yawed it from the placement metadata: -90° for an
 * east/west placement and -180° for north/south; the spikes are never rotated.
 */
public class SecurityObjRenderer extends GemBER<TileEntitySecurityObj, SecurityObjRenderState> {

    private record Piece(Geometry mesh, Surface surface) {}

    private final Map<Block, Piece> pieces = new HashMap<>();
    private Piece fallback;

    @Override
    protected void setup(Gem.Model<SecurityObjRenderState> declaration) {
        build();
        declaration.joint().translate(0.5F, 0.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(fallback.mesh(), fallback.surface())
                .mesh(state -> piece(state).mesh())
                .skin(state -> piece(state).surface());
    }

    private Piece piece(SecurityObjRenderState state) {
        return pieces.getOrDefault(state.block, fallback);
    }

    private void build() {
        Geometry wire = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/blocks/barbed_wire.obj")).all();
        Geometry spikes = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/blocks/spikes.obj")).all();

        for (RegistryObject<BarbedWire> block : ModSecurityBlocks.BARBED_WIRES) {
            pieces.put(block.get(), new Piece(wire, Surface.cutout(
                    Constants.MOD_ID + ":textures/blocks/" + block.get().kind().texture + "_model.png")));
        }
        pieces.put(ModSecurityBlocks.spikes.get(), new Piece(spikes,
                Surface.cutout(Constants.MOD_ID + ":textures/blocks/spikes.png")));
        fallback = pieces.get(ModSecurityBlocks.barbed_wire.get());
    }

    @Override
    public SecurityObjRenderState createRenderState() {
        return new SecurityObjRenderState();
    }

    @Override
    public void extract(TileEntitySecurityObj blockEntity, SecurityObjRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = 0F;
        if (bs.hasProperty(BarbedWire.FACING)) {
            Direction facing = bs.getValue(BarbedWire.FACING);
            // Old metadata 2/3 (east/west placements) drew at -90°, 4/5 (south/north) at -180°.
            state.yaw = facing.getAxis() == Direction.Axis.X ? -90F : -180F;
        }
    }
}
