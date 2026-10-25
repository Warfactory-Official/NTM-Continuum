package com.hbm.world.gen;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.config.DungeonConfig;
import com.hbm.world.dungeon.AncientTomb;
import com.hbm.world.dungeon.Antenna;
import com.hbm.world.dungeon.ArcticVault;
import com.hbm.world.dungeon.Barrel;
import com.hbm.world.dungeon.DesertAtom001;
import com.hbm.world.dungeon.LibraryDungeon;
import com.hbm.world.dungeon.Spaceship;
import com.hbm.world.feature.Dud;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.generator.CellularDungeonFactory;
import com.hbm.world.generator.JungleDungeon;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Structures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The legacy hardcoded dungeons of 1.7 {@code HbmWorldGen.generateSurface} (the overworld, behind
 * {@code enableDungeons}) as modern structures: {@code hbm:legacy_dungeon} ({@link LegacyDungeonStructure}, codec field
 * {@code dungeon} = a {@link Kind}) with one piece type {@code hbm:legacy_dungeon_piece} ({@link Piece}).
 *
 * <p><b>Where.</b> 1.7 rolled {@code rand.nextInt(n) == 0} per populated chunk after a biome test, then picked the site
 * inside the chunk. The port's {@code structure_set} per dungeon ({@link LegacyDungeonDeclarations}) gives each chunk
 * the same 1/n chance, the biome test is the structure's biome tag, and {@link #findGenerationPoint} picks the site
 * inside the chunk the way 1.7 did ({@code i + rand.nextInt(16)}, y rules per kind).
 *
 * <p><b>How.</b> The start runs the 1.7 builder once on {@link LegacyDungeonWorld} against the generator's terrain:
 * if the builder's own spawn-site test fails, nothing was written and there is no structure (1.7 silently built
 * nothing either); otherwise the written blocks give the piece's box. Each chunk of that box later runs the same
 * builder (same seed, same terrain, so the same result; cached) and copies its own share into the world. The biome is
 * sampled at the site's surface, as 1.7's 2D biome was, also for the buried vault and library.
 *
 * <p><b>Step.</b> {@code top_layer_modification}: 1.7's {@code HbmWorldGen} ran after the biome decorator (trees, ores,
 * lakes), so nothing vanilla placed later cut into these; terrain adaptation {@code none} (1.7 neither dug nor filled).
 */
public class LegacyDungeonStructure extends Structure {

    /** The 1.7 builders, by structure id. */
    public enum Kind {
        /** 1.7 {@code Antenna}: {@code antennaStructure}, biome temperature >= 0.4 and rainfall <= 0.6. */
        ANTENNA("antenna"),
        /** 1.7 {@code DesertAtom001}..{@code 003}, the abandoned power plant: no lightning, temperature >= 1.5. */
        DESERT_ATOM("desert_atom"),
        /** 1.7 {@code LibraryDungeon}: at a random y 0..255, only where it is walled in by solid ground. */
        LIBRARY_DUNGEON("library_dungeon"),
        /** 1.7 {@code Dud}, the crashed bomb. */
        DUD("dud"),
        /** 1.7 {@code Spaceship} + {@code Spaceship2} (one structure: 1.7's first half calls the second). */
        SPACESHIP("spaceship"),
        /** 1.7 {@code Barrel}, the waste tank: no lightning, temperature >= 1.5. */
        BARREL("barrel"),
        /** 1.7 {@code ArcticVault}: at y 16..47, under a biome colder than 0.2. */
        ARCTIC_VAULT("arctic_vault"),
        /** 1.7 {@code AncientTomb}, the pyramid: no lightning, temperature >= 2. */
        ANCIENT_TOMB("ancient_tomb"),
        /** 1.7 {@code CellularDungeonFactory.jungle}: three floors at y 20/24/28 in jungles, a titanium marker on top. */
        JUNGLE_DUNGEON("jungle_dungeon");

        public final String id;

        Kind(String id) {
            this.id = id;
        }

        public static @Nullable Kind byId(String id) {
            for (Kind kind : values()) if (kind.id.equals(id)) return kind;
            return null;
        }
    }

    public static final MapCodec<LegacyDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("dungeon").forGetter(s -> s.kind.id)
    ).apply(i, (settings, id) -> {
        Kind kind = Kind.byId(id);
        if (kind == null) throw new IllegalArgumentException("unknown legacy dungeon '" + id + "'");
        return new LegacyDungeonStructure(settings, kind);
    }));

    // Registration

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(STRUCTURE_TYPES, STRUCTURE_PIECES);

    public static final RegistryObject<StructureType<LegacyDungeonStructure>> TYPE =
            REG.register(Registries.STRUCTURE_TYPE, "legacy_dungeon", () -> Structures.type(CODEC));
    public static final RegistryObject<StructurePieceType> PIECE =
            REG.register(Registries.STRUCTURE_PIECE, "legacy_dungeon_piece", () -> (StructurePieceType) Piece::new);

    public static void init() {}

    // Structure

    private final Kind kind;

    public LegacyDungeonStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (DungeonConfig.enableDungeons == 0) return Optional.empty();

        int i = context.chunkPos().getMinBlockX();
        int j = context.chunkPos().getMinBlockZ();
        RandomSource rand = context.random();
        LegacyDungeonWorld.Terrain terrain = LegacyDungeonWorld.terrain(context.chunkGenerator(), context.randomState(), context.heightAccessor());

        // 1.7 HbmWorldGen.generateSurface: the site inside the chunk
        int x, y, z;
        switch (kind) {
            case LIBRARY_DUNGEON -> {
                x = i + rand.nextInt(16);
                y = rand.nextInt(256);
                z = j + rand.nextInt(16);
            }
            case DUD -> {
                x = i + 8 + rand.nextInt(16);
                z = j + 8 + rand.nextInt(16);
                y = terrain.height(x, z);
            }
            case ARCTIC_VAULT -> {
                x = i + rand.nextInt(16);
                z = j + rand.nextInt(16);
                y = 16 + rand.nextInt(32);
            }
            case JUNGLE_DUNGEON -> {
                x = i + rand.nextInt(16);
                z = j + rand.nextInt(16);
                y = 20;
            }
            default -> {
                x = i + rand.nextInt(16);
                z = j + rand.nextInt(16);
                y = terrain.height(x, z);
            }
        }
        long seed = rand.nextLong();

        BlockPos site = new BlockPos(x, terrain.height(x, z), z); // biome sampled at the surface, like 1.7's 2D biome

        // 1.7's climate rule on the site's biome: the tag holds the table's biomes plus every other overworld biome, a
        // table biome answers through its row, a modded one through its own climate (not where /place turns biomes off)
        if (NBTStructure.GenStructure.checksBiome(context)
                && !LegacyBiomes.acceptsDungeon(kind, LegacyBiomes.biomeAt(context.biomeSource(), context.randomState(), x, site.getY(), z))) {
            return Optional.empty();
        }

        LegacyDungeonWorld.Result result = run(kind, x, y, z, seed, terrain);
        if (result == null) return Optional.empty();

        BoundingBox box = result.bounds();
        return Optional.of(new GenerationStub(site, builder -> builder.addPiece(new Piece(kind, x, y, z, seed, box))));
    }

    // The builders

    /** Recent runs by site; the start and every chunk of the piece ask for the same one. */
    private static final Map<String, LegacyDungeonWorld.Result> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LegacyDungeonWorld.Result> eldest) {
            return size() > 48;
        }
    };

    /** The run of {@code kind} at a site; null when the builder's spawn test failed (nothing written). */
    public static LegacyDungeonWorld.@Nullable Result run(Kind kind, int x, int y, int z, long seed, LegacyDungeonWorld.Terrain terrain) {
        String key = kind.id + "@" + x + "," + y + "," + z + "#" + seed;
        synchronized (CACHE) {
            LegacyDungeonWorld.Result cached = CACHE.get(key);
            if (cached != null) return cached.size() == 0 ? null : cached;
        }
        LegacyDungeonWorld.Result result = simulate(kind, x, y, z, seed, terrain);
        synchronized (CACHE) {
            CACHE.put(key, result);
        }
        return result.size() == 0 ? null : result;
    }

    /** Runs the 1.7 builder, as {@code HbmWorldGen} called it, on a fresh virtual world. */
    public static LegacyDungeonWorld.Result simulate(Kind kind, int x, int y, int z, long seed, LegacyDungeonWorld.Terrain terrain) {
        RandomSource rand = RandomSource.create(seed);
        // 1.7 world.rand and Library.rand were their own streams
        LegacyDungeonWorld world = new LegacyDungeonWorld(terrain, RandomSource.create(seed ^ 0x5DEECE66DL), RandomSource.create(seed * 31L + 17L));
        switch (kind) {
            case ANTENNA -> new Antenna().generate(world, rand, x, y, z);
            case DESERT_ATOM -> new DesertAtom001().generate(world, rand, x, y, z);
            case LIBRARY_DUNGEON -> new LibraryDungeon().generate(world, rand, x, y, z);
            case DUD -> new Dud().generate(world, rand, x, y, z);
            case SPACESHIP -> new Spaceship().generate(world, rand, x, y, z);
            case BARREL -> new Barrel().generate(world, rand, x, y, z);
            case ARCTIC_VAULT -> new ArcticVault().trySpawn(world, x, y, z);
            case ANCIENT_TOMB -> new AncientTomb().build(world, rand, x, y, z);
            case JUNGLE_DUNGEON -> {
                // 1.7 passed world.rand; three floors on one dungeon object and one job queue
                JungleDungeon jungle = CellularDungeonFactory.jungle();
                jungle.generate(world, x, 20, z, world.rand);
                jungle.generate(world, x, 24, z, world.rand);
                jungle.generate(world, x, 28, z, world.rand);
                jungle.timed.drain();

                int top = world.getHeightValue(x, z);

                for (int f = 0; f < 3; f++)
                    world.setBlock(x, top + f, z, ModBlocks.deco_titanium);
                world.setBlock(x, top + 3, z, Blocks.redstone_block);
            }
        }
        return world.result();
    }

    // Piece

    /** One legacy dungeon: its kind, 1.7 site and seed; every chunk of its box rebuilds the run and copies its share. */
    public static class Piece extends StructurePiece {

        private final Kind kind;
        private final int x, y, z;
        private final long seed;

        public Piece(Kind kind, int x, int y, int z, long seed, BoundingBox box) {
            super(PIECE.get(), 0, box);
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.seed = seed;
        }

        public Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(PIECE.get(), tag);
            Kind k = Kind.byId(Tags.getStringOr(tag, "dungeon", ""));
            this.kind = k == null ? Kind.DUD : k;
            this.x = Tags.getIntOr(tag, "sx", 0);
            this.y = Tags.getIntOr(tag, "sy", 0);
            this.z = Tags.getIntOr(tag, "sz", 0);
            this.seed = Tags.getLongOr(tag, "seed", 0L);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("dungeon", kind.id);
            tag.putInt("sx", x);
            tag.putInt("sy", y);
            tag.putInt("sz", z);
            tag.putLong("seed", seed);
        }

        public Kind kind() {
            return kind;
        }

        public BlockPos site() {
            return new BlockPos(x, y, z);
        }

        /** The run of this piece in {@code level}'s generator (cached). */
        public LegacyDungeonWorld.@Nullable Result result(WorldGenLevel level, ChunkGenerator generator) {
            LegacyDungeonWorld.Terrain terrain = LegacyDungeonWorld.terrain(generator, level.getLevel().getChunkSource().randomState(), level);
            return run(kind, x, y, z, seed, terrain);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            LegacyDungeonWorld.Result result = result(level, generator);
            if (result != null) result.apply(level, chunkBox);
        }
    }
}
