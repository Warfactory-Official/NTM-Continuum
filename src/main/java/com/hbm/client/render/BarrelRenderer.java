package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityBarrel;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a drum and the short connector that bridges it to a neighbouring drum. Ported from 1.12.2's
 * Forge-blockstate submodel setup, which composed the same two OBJ pieces per connected side.
 *
 * <p>Each variant is its own model and texture, so both are looked up from the block once and cached;
 * the connector borrows the drum's flipped texture, which is what makes a row read as one vessel. Five
 * declared parts serve every drum: the body, whose mesh and skin come off the state, and one connector
 * per horizontal side, each shown only where the blockstate says a neighbour is there.
 */
public final class BarrelRenderer extends GemBER<TileEntityBarrel, BarrelRenderer.State> {

    private static final Geometry CONNECTOR =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/barrel_connector.obj")).all();

    private static final Map<Block, Geometry> MODELS = new HashMap<>();
    private static final Map<Block, Surface> SKINS = new HashMap<>();
    private static final Map<Block, Surface> FLIPPED = new HashMap<>();

    /** The first drum registered, so the declaration has a mesh and a skin before any state arrives. */
    private static Geometry fallbackMesh;
    private static Surface fallbackSkin;

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Block first = com.hbm.blocks.machine.ModMachineBlocks.barrel_plastic.get();
        fallbackMesh = meshOf(first);
        fallbackSkin = skinOf(first);

        declaration.part(fallbackMesh, fallbackSkin)
                .translate(0.5F, 0.0F, 0.5F)
                .mesh(state -> state.mesh == null ? fallbackMesh : state.mesh)
                .skin(state -> state.skin == null ? fallbackSkin : state.skin);

        connector(declaration, 0F, state -> state.posX);
        connector(declaration, 90F, state -> state.posZ);
        connector(declaration, 180F, state -> state.negX);
        connector(declaration, 270F, state -> state.negZ);
    }

    private static void connector(Gem.Model<State> declaration, float yaw, Gem.Test<State> present) {
        declaration.part(CONNECTOR, fallbackSkin)
                .visibleIf(present)
                .translate(0.5F, 0.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> yaw)
                .skin(state -> state.flipped == null ? fallbackSkin : state.flipped);
    }

    private static Geometry meshOf(Block block) {
        return MODELS.computeIfAbsent(block, b -> WavefrontObj.load(
                Ids.of(Constants.MOD_ID, "models/machines/" + path(b) + ".obj")).all());
    }

    private static Surface skinOf(Block block) {
        return SKINS.computeIfAbsent(block,
                b -> Surface.solid(Constants.MOD_ID + ":textures/blocks/" + path(b) + ".png"));
    }

    private static Surface flippedOf(Block block) {
        return FLIPPED.computeIfAbsent(block,
                b -> Surface.solid(Constants.MOD_ID + ":textures/blocks/" + path(b) + "_flipped.png"));
    }

    private static String path(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityBarrel barrel, State state, float partial, Vec3 camera) {
        BlockState block = barrel.getBlockState();
        if (!(block.getBlock() instanceof BlockFluidBarrel)) return;

        state.mesh = meshOf(block.getBlock());
        state.skin = skinOf(block.getBlock());
        state.flipped = flippedOf(block.getBlock());

        state.posX = block.getValue(BlockFluidBarrel.CONN_POS_X);
        state.negX = block.getValue(BlockFluidBarrel.CONN_NEG_X);
        state.posZ = block.getValue(BlockFluidBarrel.CONN_POS_Z);
        state.negZ = block.getValue(BlockFluidBarrel.CONN_NEG_Z);
    }

    public static final class State extends CompatRenderState {
        Geometry mesh;
        Surface skin;
        Surface flipped;
        boolean posX, negX, posZ, negZ;
    }
}
