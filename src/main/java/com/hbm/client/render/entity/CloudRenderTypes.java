package com.hbm.client.render.entity;

import com.hbm.main.ResourceManager;
import dev.golem.core.render.CompatRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Render types for the nuke-cloud sphere shells (Fleija / Solinium / rainbow / death blast). The 1.7 renderers share
 * one GL state machine: lighting off, texturing off, GL_SMOOTH and a flat glColor; the core sphere is opaque except
 * the death blast's (SRC_ALPHA/ONE_MINUS_SRC_ALPHA) and the outer shells add SRC_ALPHA, ONE.
 *
 * <p>Translated onto the vanilla types Golem exposes on every node: an emissive, full-bright pass over a solid white
 * sheet for the opaque and translucent cores, and the alpha-weighted additive glow for the shells. Both are unculled;
 * 1.7's shells were culled, which for a closed sphere only removes back faces the front already covers.
 */
public final class CloudRenderTypes {

    public static final int FULL_BRIGHT = 0xF000F0;

    public static final RenderType OPAQUE = CompatRenderTypes.entityTranslucentEmissive(ResourceManager.white_tex);
    public static final RenderType OPAQUE_UNCULLED = OPAQUE;
    public static final RenderType TRANSLUCENT = OPAQUE;
    public static final RenderType ADDITIVE = CompatRenderTypes.plasmaGlow(ResourceManager.white_tex);

    private CloudRenderTypes() {
    }

    /** {@code glColor3f} left alpha at 1: an opaque ARGB from three floats. */
    public static int rgb(float r, float g, float b) {
        return argb(1F, r, g, b);
    }

    public static int argb(float a, float r, float g, float b) {
        return ((int) (a * 255F) & 0xFF) << 24 | ((int) (r * 255F) & 0xFF) << 16 | ((int) (g * 255F) & 0xFF) << 8
                | ((int) (b * 255F) & 0xFF);
    }
}
