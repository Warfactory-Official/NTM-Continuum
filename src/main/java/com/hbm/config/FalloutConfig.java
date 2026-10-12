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
 * The fallout block-transform table, 1.7's {@code FalloutConfigJSON} ({@code hbmFallout.json} there, a datapack folder
 * here): {@code data/<ns>/fallout_transform/<name>.json}, each {@code {"priority": 0, "entries": [...]}}.
 * {@code EntityFalloutRain} walks the entries in order per block — first entry whose {@code eval} returns non-null wins
 * — so files are read highest {@code priority} first (then by id), each file's entries in order. HBM's defaults are
 * {@code data/hbm/fallout_transform/default.json} at priority 0; an addon adds rules ahead of them with a higher priority,
 * or replaces the file. GameTest {@code value_map_fallout_transform} pins the defaults to the port's 1.7 table.
 *
 * <p>An entry matches by exact {@code state}, {@code block}, block {@code tag} and/or a named {@code preset} (a
 * predicate registered with {@link #registerPreset}; the 1.12 {@code Material} matchers became presets: wood material
 * → {@code hbm:ignited_by_lava}, plants/vines → {@code hbm:vegetation}, the ROCK/IRON/SAND/GROUND terrain catch-all →
 * {@code hbm:solid_terrain} (excluding grass/mycelium, which the old GRASS-material entry handled with its own ≤45%
 * rings, {@code hbm:grass_like})). The catch-all stands for the four 1.7 materials: an opaque block a pickaxe or shovel
 * mines, except ice, snow and the redstone lamp (1.7's ice, packedIce, craftedSnow and redstoneLight); wool, TNT, gourds
 * and sponges are not tool-mined that way and fall out too. Every specific matcher stays ahead of it.
 *
 * <p>Skipped old entry (source block not ported): volcano_core.
 */
public final class FalloutConfig {

    private FalloutConfig() {}

    /** The datapack folder. */
    public static final String FOLDER = "fallout_transform";

    private static volatile List<FalloutEntry> entries = List.of();

    private static final java.util.Map<String, Predicate<BlockState>> PRESETS = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        registerPreset("hbm:ignited_by_lava", BlockState::ignitedByLava);
        registerPreset("hbm:vegetation", state -> state.getBlock() instanceof VegetationBlock
                || state.is(Blocks.VINE) || state.is(Blocks.SUGAR_CANE));
        // 1.7 Material.rock / iron / sand / ground: what a pickaxe or shovel mines, opaque, without 1.7's other materials
        // that modern tools also mine (ice and packed ice, craftedSnow and snow, redstoneLight)
        registerPreset("hbm:solid_terrain", state -> Compat.isSolidRender(state)
                && (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL))
                && !state.is(net.minecraft.tags.BlockTags.ICE) && !state.is(net.minecraft.tags.BlockTags.SNOW)
                && !state.is(Blocks.REDSTONE_LAMP)
                && !(state.getBlock() instanceof GrassBlock) && !state.is(Blocks.MYCELIUM));
        registerPreset("hbm:grass_like", state -> state.getBlock() instanceof GrassBlock || state.is(Blocks.MYCELIUM));
    }

    /** Names a block predicate for the {@code preset} matcher. Call at mod construction. */
    public static void registerPreset(String id, Predicate<BlockState> predicate) {
        if (PRESETS.putIfAbsent(id, predicate) != null) throw new IllegalStateException("Fallout preset " + id + " registered twice");
    }

    /** The current rules, in evaluation order (empty until the server's datapacks load). */
    public static List<FalloutEntry> entries() {
        return entries;
    }

    /** {@link dev.golem.core.data.DataFiles} consumer for {@link #FOLDER}. */
    public static void apply(java.util.Map<String, com.google.gson.JsonElement> files) {
        record File(String id, int priority, List<FalloutEntry> entries) {}
        List<File> read = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                TransformFile file = read(json);
                read.add(new File(id, file.priority(), file.entries()));
            } catch (RuntimeException e) {
                com.hbm.Constants.LOG.error("Fallout transform file {} is broken, skipping it: {}", id, e.getMessage());
            }
        });
        read.sort(java.util.Comparator.comparingInt(File::priority).reversed().thenComparing(File::id));
        List<FalloutEntry> all = new ArrayList<>();
        for (File file : read) all.addAll(file.entries());
        entries = Collections.unmodifiableList(all);
    }

    /** One file of {@link #FOLDER}: {@code {"priority": 0, "entries": [...]}}. */
    public record TransformFile(int priority, List<FalloutEntry> entries) {}

    /** Reads one file ({@link #apply}); {@code priority} defaults to 0. */
    public static TransformFile read(com.google.gson.JsonElement json) {
        com.google.gson.JsonObject object = json.getAsJsonObject();
        int priority = object.has("priority") ? object.get("priority").getAsInt() : 0;
        List<FalloutEntry> list = new ArrayList<>();
        for (com.google.gson.JsonElement element : object.getAsJsonArray("entries")) {
            var result = CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, element);
            if (result.result().isEmpty()) throw new IllegalArgumentException(
                    result.error().map(e -> e.message()).orElse("?"));
            list.add(result.result().get());
        }
        return new TransformFile(priority, list);
    }

    /** The inverse of {@link #read}: writes one file (the build-time data source's encoder). */
    public static com.google.gson.JsonElement write(TransformFile file) {
        com.google.gson.JsonObject object = new com.google.gson.JsonObject();
        object.addProperty("priority", file.priority());
        com.google.gson.JsonArray entries = new com.google.gson.JsonArray();
        for (FalloutEntry entry : file.entries()) {
            var result = CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, entry);
            if (result.result().isEmpty()) throw new IllegalArgumentException(
                    result.error().map(e -> e.message()).orElse("?"));
            entries.add(result.result().get());
        }
        object.add("entries", entries);
        return object;
    }

    private static final com.mojang.serialization.Codec<BlockState> STATE =com.mojang.serialization.Codec.STRING.comapFlatMap(
            name -> {
                BlockState state = com.hbm.util.SavedIds.stateFromName(name, null);
                return state != null ? com.mojang.serialization.DataResult.success(state)
                        : com.mojang.serialization.DataResult.error(() -> "Unknown block state " + name);
            }, com.hbm.util.SavedIds::stateName);

    private static final com.mojang.serialization.Codec<WeightedState> WEIGHTED = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            STATE.fieldOf("state").forGetter(WeightedState::state),
            com.mojang.serialization.Codec.INT.optionalFieldOf("weight", 1).forGetter(WeightedState::weight)
    ).apply(i, WeightedState::new));

    /** One rule as JSON. */
    public static final com.mojang.serialization.Codec<FalloutEntry> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            STATE.optionalFieldOf("state").forGetter(e -> java.util.Optional.ofNullable(e.matchState)),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("block").forGetter(e -> java.util.Optional.ofNullable(e.matchBlock)
                    .map(b -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString())),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("tag").forGetter(e -> java.util.Optional.ofNullable(e.matchTag).map(t -> t.location().toString())),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("preset").forGetter(e -> java.util.Optional.ofNullable(e.matchPreset)),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("opaque", false).forGetter(e -> e.matchesOpaque),
            WEIGHTED.listOf().optionalFieldOf("primary", List.of()).forGetter(e -> e.primaryBlocks),
            WEIGHTED.listOf().optionalFieldOf("secondary", List.of()).forGetter(e -> e.secondaryBlocks),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("primary_chance", 1D).forGetter(e -> e.primaryChance),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("min", 0D).forGetter(e -> e.minDist),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("max", 100D).forGetter(e -> e.maxDist),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("falloff_start", 0.9D).forGetter(e -> e.falloffStart),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("solid", false).forGetter(e -> e.solid),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("preserve_axis", false).forGetter(e -> e.preserveAxis)
    ).apply(i, (state, block, tag, preset, opaque, primary, secondary, chance, min, max, falloff, solid, axis) -> {
        FalloutEntry e = FalloutEntry.builder();
        state.ifPresent(e::matchesState);
        block.ifPresent(b -> e.matchesBlock(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(b)).orElse(net.minecraft.world.level.block.Blocks.AIR)));
        tag.ifPresent(t -> e.matchesTag(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, dev.golem.core.Ids.parse(t))));
        preset.ifPresent(e::matchesPreset);
        e.opaque(opaque);
        e.primaryBlocks.addAll(primary);
        e.secondaryBlocks.addAll(secondary);
        e.primaryChance(chance).min(min).max(max).solid(solid);
        e.falloffStart = falloff;
        e.preserveAxis = axis;
        return e;
    }));


    /** A weighted conversion outcome. */
    record WeightedState(BlockState state, int weight) {}

    /**
     * One transform rule — the 1.12.2 {@code FalloutEntry}, with the matcher generalised to
     * exact-state / block / tag / predicate.
     */
    public static final class FalloutEntry {

        private BlockState matchState;          // exact state match
        private Block matchBlock;               // block match
        private TagKey<Block> matchTag;         // tag match
        private Predicate<BlockState> matchPredicate;
        private String matchPreset;             // the named predicate, for the JSON form
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
        /** Matches by a predicate registered with {@link FalloutConfig#registerPreset}. */
        public FalloutEntry matchesPreset(String id) {
            Predicate<BlockState> predicate = PRESETS.get(id);
            if (predicate == null) throw new IllegalArgumentException("Unknown fallout preset " + id);
            this.matchPreset = id;
            this.matchPredicate = predicate;
            return this;
        }
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
            // A destroyed block leaves the fluid it stood in, not air. 1.7 had no underwater plants; here
            // seagrass and sea pickles are VegetationBlocks, and turning them to air cut a hole in the ocean
            // floor's water that the sweep then filled with ash (fallout "spawning" deep under the sea).
            if (conversion.isAir() && !state.getFluidState().isEmpty()) {
                return state.getFluidState().createLegacyBlock();
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
