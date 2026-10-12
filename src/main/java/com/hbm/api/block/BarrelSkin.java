package com.hbm.api.block;

/**
 * A barrel block of another mod drawn by HBM's barrel renderer ({@code client.render.BarrelRenderer}, 1.7
 * {@code RenderBarrel}: the {@code barrel.obj} drum): the skin it wears, any texture, named in full
 * ({@code "ns:textures/blocks/x.png"}). The mod registers {@code BarrelRenderer} for its own block entity type. HBM's
 * own barrels name their skins through {@code BlockFluidBarrel}/{@code BlockBarrelDeco} and do not implement this.
 */
public interface BarrelSkin {

    String barrelSkin();
}
