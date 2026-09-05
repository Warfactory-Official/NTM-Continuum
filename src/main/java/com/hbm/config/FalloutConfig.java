package com.hbm.config;

import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The fallout block-transform table, ported from 1.12.2's {@code FalloutConfigJSON} default entries
 * (the user-editable JSON reload is deferred; the entry structure stays JSON-shaped so it can be
 * wired to a file later). {@code EntityFalloutRain} walks the list in order per block — first entry
 * whose {@code eval} returns non-null wins.
 *
 * <p>1.12 {@code Material} matchers are mapped to 26.1 equivalents: logs/planks/leaves → vanilla
 * block tags, plants/vines → {@code instanceof VegetationBlock}, wood-material → {@code
 * ignitedByLava()}, and the ROCK/IRON/SAND/GROUND terrain catch-all → "solid-render full cube"
 * (excluding grass/mycelium, which the old GRASS-material entry handled with its own ≤45% rings).
 * The catch-all is slightly broader than the four old materials (e.g. wool is opaque), which is why
 * every specific matcher stays ahead of it.
 *
 * <p>Skipped old entries (source blocks not ported, marked TODO): ore_nether_uranium,
 * glyphid base/spawner, volcano_core.
 *
 * <p>MUST be built lazily ({@link #entries()}): the outcome states resolve registry objects, which
 * throws "Components not bound yet" during class-init/mod-setup.
 */
public final class FalloutConfig {

    private FalloutConfig() {}

    private static List<FalloutEntry> entries;

    public static List<FalloutEntry> entries() {
        if (entries == null) {
            List<FalloutEntry> list = new ArrayList<>();
            initDefault(list);
            entries = Collections.unmodifiableList(list);
        }
        return entries;
    }

    private static void initDefault(List<FalloutEntry> e) {
        double woodEffectRange = 65D;

        // petrify all wooden things possible
        e.add(FalloutEntry.builder()
                .matchesTag(BlockTags.LOGS)
                .preserveAxis()
                .addPrimary(ModFalloutBlocks.waste_log.get().defaultBlockState(), 1)
                .max(woodEffectRange).solid(true).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.MUSHROOM_STEM) // old RED/BROWN_MUSHROOM_BLOCK meta 10 (the stem)
                .addPrimary(ModFalloutBlocks.waste_log.get().defaultBlockState(), 1)
                .max(woodEffectRange).solid(true).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.RED_MUSHROOM_BLOCK)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.BROWN_MUSHROOM_BLOCK)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.SNOW_BLOCK) // old Blocks.SNOW (the full block)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.SNOW) // the layer (would block the sweep otherwise)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesTag(BlockTags.PLANKS)
                .addPrimary(ModBlocks.waste_planks.get().defaultBlockState(), 1)
                .max(woodEffectRange).solid(true).build());

        // if it can't be petrified, destroy it (old WOOD/LEAVES/PLANTS/VINE material kill list)
        e.add(FalloutEntry.builder()
                .matchesPredicate(state -> state.ignitedByLava()) // wood-material stand-in (stairs/fences/doors/…)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesTag(BlockTags.LEAVES)
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesPredicate(state -> state.getBlock() instanceof VegetationBlock
                        || state.is(Blocks.VINE) || state.is(Blocks.SUGAR_CANE))
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(ModFalloutBlocks.waste_leaves.get())
                .addPrimary(Blocks.AIR.defaultBlockState(), 1)
                .max(woodEffectRange).build());

        e.add(FalloutEntry.builder()
                .matchesTag(BlockTags.LEAVES)
                .addPrimary(ModFalloutBlocks.waste_leaves.get().defaultBlockState(), 1)
                .min(woodEffectRange - 5D).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.MOSSY_COBBLESTONE)
                .addPrimary(Blocks.COAL_ORE.defaultBlockState(), 1)
                .solid(true).build());

        // TODO(port): ore_nether_uranium -> ore_nether_schrabidium/scorched (nether ores not in the transform set yet)
        // TODO(port): glyphid_base/glyphid_spawner -> RAD variants (glyphids not ported)

        // the sellafite shade rings: i-th ring = <= i*5 percent, shade m = 10-i (9 = darkest at GZ)
        for (int i = 1; i <= 10; i++) {
            int m = 10 - i;
            double max = i * 5;

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_diamond.get(), m), 3)
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_emerald.get(), m), 2)
                    .primaryChance(0.5)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(Blocks.COAL_ORE).build());
            e.add(FalloutEntry.builder() // deepslate coal behaves like coal (1.12 had one coal ore)
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_diamond.get(), m), 3)
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_emerald.get(), m), 2)
                    .primaryChance(0.5)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(Blocks.DEEPSLATE_COAL_ORE).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_diamond.get(), m), 1)
                    .primaryChance(0.2)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(ModBlocks.ore_lignite.get()).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_emerald.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(ModBlocks.ore_beryllium.get()).build());

            if (m > 4) {
                e.add(FalloutEntry.builder()
                        .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_schrabidium.get(), m), 1)
                        .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_uranium_scorched.get(), m), 9)
                        .max(max).opaque(true).solid(true)
                        .matchesBlock(ModNuclearBlocks.ore_uranium.get()).build());
                e.add(FalloutEntry.builder()
                        .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_schrabidium.get(), m), 1)
                        .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_uranium_scorched.get(), m), 9)
                        .max(max).opaque(true).solid(true)
                        .matchesBlock(ModNuclearBlocks.ore_gneiss_uranium.get()).build());
            }

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_radgem.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(Blocks.DIAMOND_ORE).build());
            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.ore_sellafield_radgem.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(Blocks.DEEPSLATE_DIAMOND_ORE).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.sellafield_bedrock.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(Blocks.BEDROCK).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.sellafield_bedrock.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(ModOreBlocks.ore_bedrock_block.get()).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.sellafield_bedrock.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(ModBlocks.ore_bedrock_oil.get()).build());

            e.add(FalloutEntry.builder()
                    .addPrimary(sellafield(ModFalloutBlocks.sellafield_bedrock.get(), m), 1)
                    .max(max).opaque(true).solid(true)
                    .matchesBlock(ModFalloutBlocks.sellafield_bedrock.get()).build());

            // the ROCK/IRON/SAND/GROUND terrain catch-all (grass/mycelium had their own GRASS-material entry)
            e.add(FalloutEntry.builder()
                    .matchesPredicate(state -> Compat.isSolidRender(state)
                            && !(state.getBlock() instanceof GrassBlock) && !state.is(Blocks.MYCELIUM))
                    .addPrimary(sellafield(ModFalloutBlocks.sellafield_slaked.get(), m), 1)
                    .max(max).opaque(true).solid(true).build());

            if (i <= 9) {
                e.add(FalloutEntry.builder()
                        .matchesPredicate(state -> state.getBlock() instanceof GrassBlock || state.is(Blocks.MYCELIUM))
                        .addPrimary(sellafield(ModFalloutBlocks.sellafield_slaked.get(), m), 1)
                        .max(max).opaque(true).solid(true).build());
            }
        }

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.MYCELIUM)
                .addPrimary(ModFalloutBlocks.waste_mycelium.get().defaultBlockState(), 1)
                .solid(true).build());

        e.add(FalloutEntry.builder()
                .matchesState(Blocks.SAND.defaultBlockState())
                .addPrimary(ModFalloutBlocks.waste_trinitite.get().defaultBlockState(), 1)
                .primaryChance(0.05).solid(true).build());

        e.add(FalloutEntry.builder()
                .matchesState(Blocks.RED_SAND.defaultBlockState())
                .addPrimary(ModFalloutBlocks.waste_trinitite_red.get().defaultBlockState(), 1)
                .primaryChance(0.05).solid(true).build());

        e.add(FalloutEntry.builder()
                .matchesBlock(Blocks.CLAY)
                .addPrimary(Blocks.TERRACOTTA.defaultBlockState(), 1) // old HARDENED_CLAY
                .solid(true).build());
    }

    private static BlockState sellafield(Block block, int shade) {
        return block.defaultBlockState().setValue(BlockSellafieldSlaked.SHADE, shade);
    }

    /** A weighted conversion outcome. */
    private record WeightedState(BlockState state, int weight) {}

    /**
     * One transform rule — the 1.12.2 {@code FalloutEntry}, with the matcher generalised to
     * exact-state / block / tag / predicate.
     */
    public static final class FalloutEntry {

        private BlockState matchState;          // exact state match
        private Block matchBlock;               // block match
        private TagKey<Block> matchTag;         // tag match
        private Predicate<BlockState> matchPredicate;
        private boolean matchesOpaque = false;

        private final List<WeightedState> primaryBlocks = new ArrayList<>();
        private final List<WeightedState> secondaryBlocks = new ArrayList<>();

        private double primaryChance = 1.0D;
        private double minDist = 0.0D;
        private double maxDist = 100.0D;
        private double falloffStart = 0.9D;

        /** Whether the depth counter is incremented when this entry converts a block. */
        private boolean solid = false;
        private boolean preserveAxis = false;

        public static FalloutEntry builder() { return new FalloutEntry(); }

        public FalloutEntry matchesState(BlockState state) { this.matchState = state; return this; }
        public FalloutEntry matchesBlock(Block block) { this.matchBlock = block; return this; }
        public FalloutEntry matchesTag(TagKey<Block> tag) { this.matchTag = tag; return this; }
        public FalloutEntry matchesPredicate(Predicate<BlockState> predicate) { this.matchPredicate = predicate; return this; }
        public FalloutEntry opaque(boolean opaque) { this.matchesOpaque = opaque; return this; }
        public FalloutEntry addPrimary(BlockState state, int weight) { this.primaryBlocks.add(new WeightedState(state, weight)); return this; }
        public FalloutEntry addSecondary(BlockState state, int weight) { this.secondaryBlocks.add(new WeightedState(state, weight)); return this; }
        public FalloutEntry primaryChance(double chance) { this.primaryChance = chance; return this; }
        public FalloutEntry min(double min) { this.minDist = min; return this; }
        public FalloutEntry max(double max) { this.maxDist = max; return this; }
        public FalloutEntry solid(boolean solid) { this.solid = solid; return this; }
        public FalloutEntry preserveAxis() { this.preserveAxis = true; return this; }
        public FalloutEntry build() { return this; }

        public boolean isSolid() { return solid; }

        /**
         * Evaluates whether this entry converts the given block. Returns the replacement state, or
         * null for no-op. {@code dist} is the distance from ground zero as a percentage of the
         * fallout scale; {@code bottomY} is the level's min build height (the old {@code y == 0}).
         */
        public BlockState eval(int y, BlockState state, double dist, RandomSource rand, int bottomY) {
            if (dist > maxDist || dist < minDist) return null;

            if (matchState != null && !state.equals(matchState)) return null;
            if (matchBlock != null && !state.is(matchBlock)) return null;
            if (matchTag != null && !state.is(matchTag)) return null;
            if (matchPredicate != null && !matchPredicate.test(state)) return null;
            if (matchesOpaque && ! Compat.isSolidRender(state)) return null;

            // gaussian falloff tail: increasingly likely rejection over the last (1-falloffStart) of maxDist
            if (dist > maxDist * falloffStart) {
                double t = (dist - maxDist * falloffStart) / (maxDist - maxDist * falloffStart);
                if (Math.abs(rand.nextGaussian()) < t * t * 3.0) return null;
            }

            List<WeightedState> pool = (primaryChance == 1D || rand.nextDouble() < primaryChance) ? primaryBlocks : secondaryBlocks;
            BlockState conversion = chooseRandomOutcome(pool, rand);
            if (conversion == null) return null;

            Block originalBlock = state.getBlock();
            Block conversionBlock = conversion.getBlock();

            // sellafite never downgrades: slaked->slaked / bedrock->bedrock only to a HIGHER shade
            if (conversionBlock instanceof BlockSellafieldSlaked && originalBlock == conversionBlock
                    && conversion.getValue(BlockSellafieldSlaked.SHADE) <= state.getValue(BlockSellafieldSlaked.SHADE)) {
                return null;
            }
            // sellafite bedrock never converts into anything else
            if (originalBlock == com.hbm.blocks.ModFalloutBlocks.sellafield_bedrock.get()
                    && conversionBlock != originalBlock) {
                return null;
            }
            // the world floor only converts to sellafite bedrock (the old y == 0 guard)
            if (y == bottomY && conversionBlock != com.hbm.blocks.ModFalloutBlocks.sellafield_bedrock.get()) {
                return null;
            }

            if (preserveAxis && state.hasProperty(BlockStateProperties.AXIS) && conversion.hasProperty(BlockStateProperties.AXIS)) {
                conversion = conversion.setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
            }
            return conversion;
        }

        private static BlockState chooseRandomOutcome(List<WeightedState> pool, RandomSource rand) {
            if (pool.isEmpty()) return null;
            int weight = 0;
            for (WeightedState choice : pool) weight += choice.weight();
            int r = rand.nextInt(weight);
            for (WeightedState choice : pool) {
                r -= choice.weight();
                if (r <= 0) return choice.state();
            }
            return pool.get(0).state();
        }
    }
}
