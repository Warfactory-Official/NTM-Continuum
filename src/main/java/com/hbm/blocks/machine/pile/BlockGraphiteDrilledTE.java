package com.hbm.blocks.machine.pile;

import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.EntityBlock;

/** The drilled graphite blocks that carry a pile tile (fuel, source, breeding fuel, detector). */
@Deprecated
public abstract class BlockGraphiteDrilledTE extends BlockGraphiteDrilledBase implements EntityBlock, TickingBlock {

    protected BlockGraphiteDrilledTE(Properties properties) {
        super(properties);
    }
}
