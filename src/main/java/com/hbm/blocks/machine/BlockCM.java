package com.hbm.blocks.machine;

import dev.golem.core.world.CompatBlock;

/**
 * 1.7 {@code BlockCM}, a {@code BlockEnumMulti} over {@code EnumCMMaterials}/{@code EnumCMEngines}/{@code EnumCMCircuit}
 * with one icon per variant ({@code <texture>_<variant>}). Flattened: every variant is its own block,
 * {@code cm_block_steel}, {@code cm_engine_desh} and so on, and a custom machine config's {@code metas} select among
 * them (see {@link com.hbm.config.CustomMachineConfigJSON#resolveBlock}).
 */
public class BlockCM extends CompatBlock {

    public BlockCM(Properties properties) {
        super(properties);
    }
}
