package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.core.Direction;

/** What a dark fusion core part's renderer needs: which way it points, and what its beam is doing. */
public class CoreComponentRenderState extends CompatRenderState {
    public Direction facing = Direction.NORTH;
    public int beam;
    public long gameTime;
    /** The emitter's last output, which sets how thick its beam is drawn. */
    public long output;
    /** The injector's two fuel colours, or −1 for a tank with nothing in it. */
    public int fluidColorA = -1;
    public int fluidColorB = -1;
}
