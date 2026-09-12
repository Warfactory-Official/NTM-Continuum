package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDebris;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Draws the four wreck blocks with their authored pribris meshes rather than cube placeholders. */
public final class RBMKDebrisBlockRenderer extends GemBER<BlockEntityRBMKDebris, RBMKDebrisBlockRenderer.State> {

    private static final String[] VARIANTS = {"", "_burning", "_radiating", "_digamma"};
    private static final String[] MESHES = {"pribris", "pribris_burning", "pribris_radiating", "pribris_digamma"};

    /** One debris-shaped slot, filled with whichever of the four wrecks this block is. */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        Geometry[] meshes = new Geometry[MESHES.length];
        Surface[] skins = new Surface[MESHES.length];
        for (int variant = 0; variant < MESHES.length; variant++) {
            meshes[variant] = obj(MESHES[variant] + ".obj").all();
            skins[variant] = Surface.cutout(texture("rbmk_debris" + VARIANTS[variant]));
        }
        declaration.part(meshes[0], skins[0])
                .mesh(state -> meshes[state.variant])
                .skin(state -> skins[state.variant]);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockEntityRBMKDebris be, State state, float partial, Vec3 camera) {
        Block block = be.getBlockState().getBlock();
        state.variant = block == ModRBMKBlocks.rbmk_debris_burning.get() ? 1
                : block == ModRBMKBlocks.rbmk_debris_radiating.get() ? 2
                : block == ModRBMKBlocks.rbmk_debris_digamma.get() ? 3 : 0;
    }

    private static WavefrontObj obj(String name) {
        return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/" + name));
    }

    private static String texture(String name) {
        return Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + name + ".png").toString();
    }

    public static final class State extends CompatRenderState {
        int variant;
    }
}
