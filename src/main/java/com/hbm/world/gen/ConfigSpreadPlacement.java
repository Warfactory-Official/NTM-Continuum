package com.hbm.world.gen;

import com.hbm.config.DungeonConfig;
import com.hbm.config.StructureConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code hbm:config_spread}: a {@code random_spread} placement whose spacing, separation and keep chance are read from
 * {@code hbm.cfg} when a chunk is asked, not fixed in the data pack - the structure counterpart of the {@code hbm:config}
 * placement modifier. A subclass of {@link RandomSpreadStructurePlacement}, so {@code /locate}
 * walks it like vanilla's (it reads {@link #spacing()} and {@link #getPotentialStructureChunk}).
 *
 * <p>{@code key}:
 * <ul>
 *   <li>{@code ntm}: 1.7 {@code NBTStructure} regions, spacing {@code 5.02_structureMaxChunks}, separation
 *       {@code 5.01_structureMinChunks} - 1.7's {@code x += rand.nextInt(max - min)} is exactly the linear spread;</li>
 *   <li>{@code dungeon:<id>}: a legacy dungeon's 1.7 {@code rand.nextInt(n) == 0} per chunk, as spacing
 *       {@code floor(sqrt(n))}, separation 0 and keep chance {@code spacing^2 / n} (see {@code LegacyDungeonDeclarations});
 *       {@code n <= 0} turns it off.</li>
 * </ul>
 * The keep chance is drawn as vanilla's {@code frequency_reduction_method: default} draws it, so with the default config
 * every candidate is the same as the fixed data.
 */
public class ConfigSpreadPlacement extends RandomSpreadStructurePlacement {

    public static final MapCodec<ConfigSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i)
            .and(Codec.STRING.fieldOf("key").forGetter(ConfigSpreadPlacement::key))
            .apply(i, ConfigSpreadPlacement::new));

    /** The registered type, set by {@link NTMWorldGenerator} (registration happens after this class loads). */
    public static Supplier<StructurePlacementType<ConfigSpreadPlacement>> TYPE;

    private final String key;

    public ConfigSpreadPlacement(Vec3i locateOffset, StructurePlacement.FrequencyReductionMethod method,
                                 float frequency, int salt, Optional<StructurePlacement.ExclusionZone> exclusionZone, String key) {
        super(locateOffset, method, frequency, salt, exclusionZone, 2, 1, RandomSpreadType.LINEAR);
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** The legacy dungeon's 1.7 chance n ({@code 4.xx_*Spawn}), or 0 for an unknown key. */
    static int dungeonChance(String id) {
        return switch (id) {
            case "antenna" -> DungeonConfig.antennaStructure;
            case "desert_atom" -> DungeonConfig.atomStructure;
            case "library_dungeon" -> DungeonConfig.dungeonStructure;
            case "dud" -> DungeonConfig.dudStructure;
            case "spaceship" -> DungeonConfig.spaceshipStructure;
            case "barrel" -> DungeonConfig.barrelStructure;
            case "jungle_dungeon" -> DungeonConfig.jungleStructure;
            case "arctic_vault" -> DungeonConfig.arcticStructure;
            case "ancient_tomb" -> DungeonConfig.pyramidStructure;
            default -> 0;
        };
    }

    private int chance() {
        return key.startsWith("dungeon:") ? dungeonChance(key.substring(8)) : 1;
    }

    @Override
    public int spacing() {
        if (key.startsWith("dungeon:")) return LegacyDungeonDeclarations.spacing(Math.max(1, chance()));
        return Math.max(2, StructureConfig.structureMaxChunks);
    }

    @Override
    public int separation() {
        if (key.startsWith("dungeon:")) return 0;
        // 1.7 nextInt(max - min) needs max > min; clamp what 1.7 would have crashed on
        return Math.max(0, Math.min(StructureConfig.structureMinChunks, spacing() - 1));
    }

    /** The keep chance of a candidate: {@code spacing^2 / n} for a legacy dungeon, 1 otherwise. */
    public float keepChance() {
        if (!key.startsWith("dungeon:")) return 1F;
        return (float) LegacyDungeonDeclarations.frequency(Math.max(1, chance()));
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int sourceX, int sourceZ) {
        int spacing = spacing();
        int spacedGridX = Math.floorDiv(sourceX, spacing);
        int spacedGridZ = Math.floorDiv(sourceZ, spacing);
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, spacedGridX, spacedGridZ, this.salt());
        int limit = spacing - separation();
        int spreadX = spreadType().evaluate(random, limit);
        int spreadZ = spreadType().evaluate(random, limit);
        return new ChunkPos(spacedGridX * spacing + spreadX, spacedGridZ * spacing + spreadZ);
    }

    /** Whether chunk (x, z) is this placement's candidate and survives the keep chance. */
    public boolean isCandidate(long seed, int sourceX, int sourceZ) {
        if (key.startsWith("dungeon:") && chance() <= 0) return false; // 1.7: 0 turns the spawn off
        ChunkPos pos = getPotentialStructureChunk(seed, sourceX, sourceZ);
        if (pos.getMinBlockX() != sourceX << 4 || pos.getMinBlockZ() != sourceZ << 4) return false;
        return keeps(seed, sourceX, sourceZ);
    }

    private boolean keeps(long seed, int sourceX, int sourceZ) {
        float keep = keepChance();
        return !(keep < 1.0F) || StructurePlacement.FrequencyReductionMethod.DEFAULT.shouldGenerate(seed, salt(), sourceX, sourceZ, keep);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
        return isCandidate(state.getLevelSeed(), sourceX, sourceZ);
    }

    /**
     * The keep chance where 1.21+ asks it separately ({@code StructureCheck}, used by {@code /locate}); no
     * {@code @Override} because 1.20.1's placement has no such method (there the check above is the only one).
     */
    public boolean applyAdditionalChunkRestrictions(int sourceX, int sourceZ, long levelSeed) {
        return keeps(levelSeed, sourceX, sourceZ);
    }

    @Override
    public StructurePlacementType<?> type() {
        return TYPE.get();
    }
}
