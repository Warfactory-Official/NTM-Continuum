package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Client snapshot of the cyclotron's four plug states and rotating inscription. */
public final class CyclotronRenderState extends CompatRenderState {
    public boolean ashes;
    public boolean book;
    public boolean gavel;
    public boolean coin;
    public float rotation;
}
