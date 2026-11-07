package com.hbm.world.gen;

import com.hbm.Constants;
import com.hbm.config.DungeonConfig;
import com.hbm.registration.Reg;
import com.hbm.world.feature.DungeonBlockFeature;
import com.hbm.world.feature.FallenMeteoriteFeature;
import com.hbm.world.feature.GlyphidHiveFeature;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.component.LegacyBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.RegistryLookups;
import dev.golem.core.world.Structures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Optional;

/**
 * The small things of 1.7 {@code HbmWorldGen.generateSurface} a player hunts for, as structures so {@code /locate} finds
 * them: one {@code hbm:scattered} structure per feature ({@code feature} = the configured feature that builds it), placed
 * by {@code hbm:config_spread} at 1.7's rate. A column that fails the feature's 1.7 test on the noise terrain is no site.
 * The feature runs at the piece's column once the chunk decorates, on the real terrain, so {@code /locate} can name a site
 * it then declines (a column under a tree). The placed feature stays for worlds with Generate Structures off.
 */
public class ScatteredStructure extends Structure {

    public static final MapCodec<ScatteredStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("feature").forGetter(s -> s.feature)
    ).apply(i, ScatteredStructure::new));

    private static final Reg REG = Reg.of(LegacyDungeonStructure.STRUCTURE_TYPES, LegacyDungeonStructure.STRUCTURE_PIECES);

    public static final RegistryObject<StructureType<ScatteredStructure>> TYPE =
            REG.register(Registries.STRUCTURE_TYPE, "scattered", () -> Structures.type(CODEC));
    public static final RegistryObject<StructurePieceType> PIECE =
            REG.register(Registries.STRUCTURE_PIECE, "scattered_piece", () -> (StructurePieceType) Piece::new);

    public static void init() {}

    private final String feature;

    public ScatteredStructure(StructureSettings settings, String feature) {
        super(settings);
        this.feature = feature;
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        LegacyDungeonWorld.Terrain terrain = LegacyDungeonWorld.terrain(context.chunkGenerator(), context.randomState(), context.heightAccessor());
        return site(context.registryAccess(), context.random(), context.chunkPos(), terrain).filter(Site::buildable).map(site -> {
            BoundingBox box = new BoundingBox(site.x(), context.heightAccessor().getMinY(), site.z(), site.x(), context.heightAccessor().getMaxY() - 1, site.z());
            return new GenerationStub(new BlockPos(site.x(), site.y(), site.z()), builder -> builder.addPiece(new Piece(feature, site.x(), site.z(), site.seed(), box)));
        });
    }

    /** The column chunk {@code chunk} offers, with the feature's random; empty when the config or the chest's roll says none. */
    public Optional<Site> site(RegistryAccess registries, RandomSource rand, ChunkPos chunk, LegacyDungeonWorld.Terrain terrain) {
        if (ConfigSpreadPlacement.dungeonChance(feature) <= 0) return Optional.empty();
        FeatureConfiguration config = configured(registries, feature).config();
        boolean chest = config instanceof DungeonBlockFeature.BlueprintChest.Config;
        // 1.7 ran the meteorite and the chest outside its enableDungeons block
        if (DungeonConfig.enableDungeons == 0 && !chest && !feature.equals(LegacyDungeonDeclarations.METEORITE)) return Optional.empty();

        int i = chunk.getMinBlockX();
        int j = chunk.getMinBlockZ();
        if (config instanceof DungeonBlockFeature.BlueprintChest.Config c && !DungeonBlockFeature.BlueprintChest.passes(c, i, j, rand)) return Optional.empty();
        int x = chest ? i + 8 : i + rand.nextInt(16);
        int z = chest ? j + 8 : j + rand.nextInt(16);
        long seed = rand.nextLong();
        int y = terrain.height(x, z);
        return Optional.of(new Site(x, y, z, seed, buildable(config, terrain, x, y, z, seed)));
    }

    /** The column test of the feature's 1.7 branch, on noise terrain where stone stands for any solid; {@code y} is 1.7's {@code getHeightValue}. */
    public static boolean buildable(FeatureConfiguration config, LegacyDungeonWorld.Terrain terrain, int x, int y, int z, long seed) {
        if (config instanceof DungeonBlockFeature.BlueprintChest.Config) return true;
        if (config instanceof DungeonBlockFeature.LandedCapsule.Config c) return terrain.block(x, y - c.depth() + 1, z) == Blocks.stone;
        if (config instanceof GlyphidHiveFeature.Config c) {
            for (int k = c.scanUp(); k >= -c.scanDown(); k--) if (terrain.block(x, y + k - 1, z) == Blocks.stone) return true;
            return false;
        }
        if (config instanceof FallenMeteoriteFeature.Config c) {
            // the same first draw the feature makes from this seed
            LegacyBlock floor = terrain.block(x, y - RandomSource.create(seed).nextInt(c.maxSink()) - 2, z);
            return floor != Blocks.air && floor != Blocks.water && floor != Blocks.lava;
        }
        return terrain.block(x, y - 1, z) == Blocks.stone;
    }

    private static ConfiguredFeature<?, ?> configured(RegistryAccess registries, String feature) {
        ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Ids.of(Constants.MOD_ID, feature));
        return RegistryLookups.holder(registries, Registries.CONFIGURED_FEATURE, key).value();
    }

    /** A chunk's column and whether the feature can stand on it. */
    public record Site(int x, int y, int z, long seed, boolean buildable) {}

    /** One site: the feature, its column and its random. */
    public static class Piece extends StructurePiece {

        private final String feature;
        private final int x, z;
        private final long seed;

        Piece(String feature, int x, int z, long seed, BoundingBox box) {
            super(PIECE.get(), 0, box);
            this.feature = feature;
            this.x = x;
            this.z = z;
            this.seed = seed;
        }

        public Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(PIECE.get(), tag);
            this.feature = Tags.getStringOr(tag, "feature", LegacyDungeonDeclarations.LOCKED_SAFE);
            this.x = Tags.getIntOr(tag, "sx", 0);
            this.z = Tags.getIntOr(tag, "sz", 0);
            this.seed = Tags.getLongOr(tag, "seed", 0L);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("feature", feature);
            tag.putInt("sx", x);
            tag.putInt("sz", z);
            tag.putLong("seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            ConfiguredFeature<?, ?> configured = configured(level.registryAccess(), feature);
            RandomSource rand = RandomSource.create(seed);
            if (configured.feature() instanceof DungeonBlockFeature.BlueprintChest chest) {
                // the placement and the site already passed 1.7's grid test and roll
                chest.build((DungeonBlockFeature.BlueprintChest.Config) configured.config(), level, rand, x, z);
            } else {
                configured.place(level, generator, rand, new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z));
            }
        }
    }
}
