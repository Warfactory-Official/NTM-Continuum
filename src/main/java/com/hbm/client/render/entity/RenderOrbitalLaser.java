package com.hbm.client.render.entity;

import com.hbm.entity.logic.EntityOrbitalLaser;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * 1.7 {@code RenderOrbitalLaser}: the same two nested eight-sided 250-block additive columns as the death ray's
 * beam, red outside (radius 0.5) and white inside (radius 0.25), untextured and unlit. 1.7 rotated its facet
 * vector by 45 RADIANS per step; {@link RenderLaserColumn} documents the project's closed-prism decision for
 * that shared quirk.
 */
public class RenderOrbitalLaser extends RenderLaserColumn<EntityOrbitalLaser> {

    public RenderOrbitalLaser(EntityRendererProvider.Context context) {
        super(context, 0xFF0000, 0xFFFFFF);
    }
}
