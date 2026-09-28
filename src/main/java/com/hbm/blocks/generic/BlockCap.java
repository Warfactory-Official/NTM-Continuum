package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;

/**
 * 1.7 {@code BlockCap}, flattened: one block per {@code EnumCapBlock} (block_cap_nuka, _quantum, _sparkle,
 * _rad, _korl, _fritz), each {@code block_cap_<type>} on the sides and {@code block_cap_<type>_top} on top and
 * bottom. Mined, it breaks back into 128 of its bottle caps (1.7 {@code quantityDropped} 128, no fortune);
 * the drop is its loot table.
 */
public class BlockCap extends CompatBlock {

    public enum EnumCapBlock { NUKA, QUANTUM, SPARKLE, RAD, KORL, FRITZ }

    public final EnumCapBlock type;

    public BlockCap(Properties properties, EnumCapBlock type) {
        super(properties);
        this.type = type;
    }
}
