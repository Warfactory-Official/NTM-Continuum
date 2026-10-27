package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import com.hbm.blocks.machine.Spotlight;

/** Render state for {@link DecoModelRenderer}: the block (picks mesh + skin), its pose angles and the fluoro part. */
public class DecoModelRenderState extends CompatRenderState {
    public @Nullable Block block;
    /** Spotlight: the old {@code ObjUtil} yaw/pitch/roll in degrees (already sign-corrected for JOML), and the offset onto the support face. */
    public float yaw, pitch, roll;
    public float offX, offY, offZ;
    /** Spotlight: OBJ group to draw (CageLamp / FloodLamp / FluoroSingle / FluoroCap / FluoroMid). */
    public String part = "";
    /** Blinking CRT: the animation frame of its texture strip. */
    public int frame;
}
