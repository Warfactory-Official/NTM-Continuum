package com.hbm.compat.sable;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import dev.golem.core.Ids;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** HBM block masses in iron blocks: hardness over iron's, unless {@code hbm.cfg} sets one. Machine cells weigh as their core. */
final class BlockMasses {

    /** Obsidian's hardness: HBM's 100 and 150 blocks would outweigh a ship of iron by orders of magnitude. */
    static final float MAX_HARDNESS = 50.0F;
    /** Sable's lightest class (super_light, 0.25 kpg) against its iron block (4 kpg). */
    static final double MIN_MASS = 1.0 / 16.0;

    private static Map<Block, Double> configured;

    private BlockMasses() {}

    static double inIronBlocks(Block block) {
        Double set = configured().get(block);
        if (set != null) return set;
        if (!GeneralConfig.sableHardnessMass) return Double.NaN;
        if (!Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) return Double.NaN;
        return Math.max(MIN_MASS, cappedHardness(block) / hardness(Blocks.IRON_BLOCK));
    }

    /** Read on first use: the registries are frozen by the time Sable asks. */
    private static synchronized Map<Block, Double> configured() {
        if (configured != null) return configured;
        Map<Block, Double> masses = new HashMap<>();
        for (String entry : GeneralConfig.sableBlockMasses) {
            int split = entry.lastIndexOf('=');
            try {
                if (split < 0) throw new IllegalArgumentException("no '='");
                Block block = BuiltInRegistries.BLOCK.getOptional(Ids.parse(entry.substring(0, split).trim()))
                        .orElseThrow(() -> new IllegalArgumentException("unknown block"));
                double mass = Double.parseDouble(entry.substring(split + 1).trim());
                if (!(mass > 0.0)) throw new IllegalArgumentException("mass must be above 0");
                masses.put(block, mass);
            } catch (RuntimeException e) {
                // bad number, bad id (the parser's exception type differs by version) or unknown block: skip the line
                Constants.LOG.warn("hbm.cfg 1.92_sableBlockMasses: skipping '{}': {}", entry, e.getMessage());
            }
        }
        configured = masses;
        return masses;
    }

    /** Unbreakable blocks (hardness -1) count as the heaviest breakable ones. */
    private static float cappedHardness(Block block) {
        float hardness = hardness(block);
        return hardness < 0.0F ? MAX_HARDNESS : Math.min(hardness, MAX_HARDNESS);
    }

    private static float hardness(Block block) {
        return block.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
