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

/**
 * 1.7 {@code RenderPribris}: all four wreck blocks draw the one {@code models/rbmk/debris.obj}
 * ({@code ResourceManager.rbmk_debris}) with their own block icon, translated to the block centre.
 */
public final class RBMKDebrisBlockRenderer extends GemBER<BlockEntityRBMKDebris, RBMKDebrisBlockRenderer.State> {

    private static final String[] VARIANTS = {"", "_burning", "_radiating", "_digamma"};

    /** One debris-shaped slot, filled with whichever of the four wrecks this block is. */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        Geometry mesh = obj("debris.obj").all();
        Surface[] skins = new Surface[VARIANTS.length];
        for (int variant = 0; variant < VARIANTS.length; variant++) {
            skins[variant] = Surface.cutout(texture("rbmk_debris" + VARIANTS[variant]));
        }
        declaration.part(mesh, skins[0])
                .skin(state -> skins[state.variant])
                .translate(.5F, 0F, .5F);
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
