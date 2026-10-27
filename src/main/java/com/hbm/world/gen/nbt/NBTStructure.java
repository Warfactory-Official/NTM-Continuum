package com.hbm.world.gen.nbt;

import java.util.LinkedHashMap;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockWand;
import com.hbm.blocks.generic.BlockWandLogic;
import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.blocks.generic.BlockWandTandem.TileEntityWandTandem;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.blocks.generic.Waterlogging;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.config.ServerConfig;
import com.hbm.config.StructureConfig;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.SavedIds;
import com.hbm.world.gen.LegacyBiomes;
import com.hbm.world.gen.NTMStructureDeclarations;
import com.hbm.world.gen.NTMWorldGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import dev.golem.core.platform.ConfigFiles;
import dev.golem.core.registry.BlockEntities;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.7 {@code NBTStructure}: HBM's structure file format (a vanilla structure template plus HBM's own keys), its
 * loader, its placer, and the area saver the structure wand uses.
 *
 * <p><b>Two palettes.</b> Files 1.7 wrote ({@code version} 1, all of the bundled {@code structures/*.nbt}) name each
 * block by its 1.7 registry name with {@code Properties.meta} holding the metadata, and carry 1.7 tile-entity NBT.
 * They are read as written and mapped at build time by {@link LegacyStructurePalette}. Files this port writes
 * ({@code version} 2) are ordinary vanilla palette entries ({@code Name} + string {@code Properties}) with this
 * version's block-entity data, since a flattened state has no metadata to store. Both place through the same code.
 *
 * <p><b>What is ported:</b> loading from the jar or a file, saving an area, {@link #build} (plain, rotated and
 * centred), the jigsaw connection table the jigsaw/tandem placeholders read, the piece build tandems use, and the
 * placeholder replacement: air shown as {@code wand_air} and jigsaw blocks kept while {@code STRUCTURE_DEBUG} is on,
 * otherwise jigsaws turn into their configured block and loot/logic placeholders arm their one-tick replacement.
 *
 * <p><b>World generation</b> ({@link Component}, {@link Start}, {@link GenStructure}) is the 1.7 generator on vanilla's
 * structure system: a {@code hbm:nbt} structure per registered {@link SpawnCondition}, all in one {@code random_spread}
 * structure set that reproduces 1.7's region pick (see {@link GenStructure}). {@code com.hbm.world.gen.NTMWorldGenerator}
 * registers the spawns; {@code NTMStructureDeclarations} writes the structure, set and biome-tag JSON. Biome block
 * selectors ({@code blockTable}/{@code platform}) are not ported (only the meteor dungeon uses them).
 *
 * <p>1.7 oddities kept: a block below y = 1 (world bottom + 1 here) is skipped; the centre offset uses the rotated
 * footprint; {@code wipeExisting} writes air only where the file has no entry. 1.7 tile-entity NBT is carried into the
 * port's block entities by {@link ILegacyStructureTile} (the structure wands) and {@link LegacyStructureTiles} (the
 * rest, with 1.7's {@code itemPalette} relinking of numeric item ids).
 */
public class NBTStructure {

    private static final Map<String, SpawnCondition> namedMap = new HashMap<>();

    private String name;
    private boolean isLoaded;
    /** True for files 1.7 wrote: 1.7 names and metadata, 1.7 tile-entity NBT. */
    private boolean legacy;
    /** 1.7 {@code itemPalette}: numeric item id -> 1.7 registry name, for {@link LegacyStructureTiles}. */
    private Map<Integer, String> itemPalette = new HashMap<>();

    protected int sizeX, sizeY, sizeZ;
    private Entry[][][] blockArray;

    private List<List<JigsawConnection>> fromConnections;
    private Map<String, List<JigsawConnection>> toTopConnections;
    private Map<String, List<JigsawConnection>> toBottomConnections;
    private Map<String, List<JigsawConnection>> toHorizontalConnections;

    /** Tile-entity ids whose 1.7 NBT was dropped, each logged once. */
    private static final Set<String> UNMIGRATED = new HashSet<>();

    /** A bundled structure: {@code assets/<namespace>/<path>} from the mod jar (servers have no resource manager). */
    public NBTStructure(String namespace, String path) {
        InputStream stream = NBTStructure.class.getResourceAsStream("/assets/" + namespace + "/" + path);
        if (stream != null) {
            name = path;
            loadStructure(stream);
        } else {
            name = path;
            Constants.LOG.error("NBT Structure not found: " + path);
        }
    }

    public NBTStructure(String name, InputStream stream) {
        this.name = name;
        loadStructure(stream);
    }

    public NBTStructure(Path file) throws IOException {
        this.name = file.getFileName().toString();
        loadStructure(Files.newInputStream(file));
    }

    public String getName() {
        return name.substring(0, name.length() - 4); // trim .nbt
    }

    public boolean isLoaded() { return isLoaded; }
    public boolean isLegacy() { return legacy; }
    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    // Register a new named structure (1.7 also keyed it by dimension for world generation, which is not ported)
    public static void registerStructure(SpawnCondition spawn) {
        if (namedMap.containsKey(spawn.name) && namedMap.get(spawn.name) != spawn)
            throw new IllegalStateException("A severe error has occurred in NBTStructure! A SpawnCondition has been registered with the same name as another: " + spawn.name);
        namedMap.put(spawn.name, spawn);
    }

    // Presents a list of all structures registered (so far)
    public static List<String> listStructures() {
        List<String> names = new ArrayList<>(namedMap.keySet());
        names.sort(String::compareTo);
        return names;
    }

    // Fetches a registered structure by name; null if there is none
    public static @Nullable SpawnCondition getStructure(@Nullable String name) {
        return name == null ? null : namedMap.get(name);
    }

    // saving

    /**
     * Saves a selected area into an NBT structure. {@code exclude} holds states to leave out; a {@link BlockWand}
     * is written as the block it stands for (so {@code wand_air} saves an explicit air entry).
     */
    public static CompoundTag saveArea(Level level, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockState> exclude) {
        CompoundTag structure = new CompoundTag();
        ListTag nbtBlocks = new ListTag();
        ListTag nbtPalette = new ListTag();

        Map<BlockState, Integer> palette = new HashMap<>();

        structure.putInt("version", 2);

        int ox = Math.min(x1, x2);
        int oy = Math.min(y1, y2);
        int oz = Math.min(z1, z2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = ox; x <= Math.max(x1, x2); x++) {
            for (int y = oy; y <= Math.max(y1, y2); y++) {
                for (int z = oz; z <= Math.max(z1, z2); z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (exclude.contains(state)) continue;

                    if (state.getBlock() instanceof BlockWand wand) state = wand.exportAs.get().defaultBlockState();

                    Integer paletteId = palette.get(state);
                    if (paletteId == null) {
                        paletteId = palette.size();
                        palette.put(state, paletteId);
                        nbtPalette.add(LegacyStructurePalette.writeState(state));
                    }

                    CompoundTag nbtBlock = new CompoundTag();
                    nbtBlock.putInt("state", paletteId);
                    nbtBlock.put("pos", intList(x - ox, y - oy, z - oz));

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        CompoundTag nbt = BlockEntities.saveWithoutMetadata(be, level.registryAccess());
                        nbt.remove("x");
                        nbt.remove("y");
                        nbt.remove("z");
                        nbtBlock.put("nbt", nbt);
                    }

                    nbtBlocks.add(nbtBlock);
                }
            }
        }

        structure.put("blocks", nbtBlocks);
        structure.put("palette", nbtPalette);
        structure.put("size", intList(Math.abs(x1 - x2) + 1, Math.abs(y1 - y2) + 1, Math.abs(z1 - z2) + 1));
        structure.put("entities", new ListTag());

        return structure;
    }

    /** The game directory's {@code structures} folder (1.7 {@code mcDataDir/structures}). */
    public static Path structureDirectory() {
        return ConfigFiles.configDir().toAbsolutePath().getParent().resolve("structures");
    }

    // Writes out a specified area to an .nbt file with a given name
    public static @Nullable Path quickSaveArea(String filename, Level level, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockState> exclude) {
        CompoundTag structure = saveArea(level, x1, y1, z1, x2, y2, z2, exclude);

        try {
            Path directory = structureDirectory();
            Files.createDirectories(directory);
            Path file = directory.resolve(filename);
            try (OutputStream out = Files.newOutputStream(file)) {
                NbtFiles.writeCompressed(structure, out);
            }
            return file;
        } catch (Exception ex) {
            Constants.LOG.warn("Failed to save NBT structure", ex);
            return null;
        }
    }

    private static ListTag intList(int x, int y, int z) {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(x));
        list.add(IntTag.valueOf(y));
        list.add(IntTag.valueOf(z));
        return list;
    }

    // loading

    private void loadStructure(InputStream inputStream) {
        try {
            CompoundTag data = NbtFiles.readCompressed(inputStream);

            legacy = Tags.getIntOr(data, "version", 0) == 1;

            ListTag sizeList = NbtFiles.list(data, "size");
            sizeX = NbtFiles.intAt(sizeList, 0);
            sizeY = NbtFiles.intAt(sizeList, 1);
            sizeZ = NbtFiles.intAt(sizeList, 2);

            // PARSE BLOCK PALETTE
            ListTag paletteList = NbtFiles.list(data, "palette");
            Entry[] palette = new Entry[paletteList.size()];

            for (int i = 0; i < paletteList.size(); i++) {
                CompoundTag p = Tags.getCompoundOrEmpty(paletteList, i);
                String blockName = Tags.getStringOr(p, "Name", "minecraft:air");

                if (legacy) {
                    CompoundTag prop = NbtFiles.compoundOrNull(p, "Properties");
                    String metaText = prop == null ? "" : Tags.getStringOr(prop, "meta", "");
                    int meta;
                    try {
                        meta = Integer.parseInt(metaText);
                    } catch (NumberFormatException ex) {
                        Constants.LOG.info("Failed to parse: " + metaText);
                        meta = 0;
                    }
                    palette[i] = Entry.legacy(blockName, meta);
                } else {
                    palette[i] = Entry.modern(blockName, p);
                }
            }

            // PARSE ITEM PALETTE (1.7: numeric item ids of the tile entities' inventories -> 1.7 registry names)
            ListTag itemPaletteList = NbtFiles.list(data, "itemPalette");
            itemPalette = new HashMap<>();
            for (int i = 0; i < itemPaletteList.size(); i++) {
                CompoundTag p = Tags.getCompoundOrEmpty(itemPaletteList, i);
                itemPalette.put(Tags.getIntOr(p, "ID", 0), Tags.getStringOr(p, "Name", ""));
            }

            // LOAD IN BLOCKS
            ListTag blockData = NbtFiles.list(data, "blocks");
            blockArray = new Entry[sizeX][sizeY][sizeZ];

            List<JigsawConnection> connections = new ArrayList<>();

            for (int i = 0; i < blockData.size(); i++) {
                CompoundTag block = Tags.getCompoundOrEmpty(blockData, i);
                int state = Tags.getIntOr(block, "state", 0);
                ListTag posList = NbtFiles.list(block, "pos");
                int px = NbtFiles.intAt(posList, 0), py = NbtFiles.intAt(posList, 1), pz = NbtFiles.intAt(posList, 2);
                if (state < 0 || state >= palette.length) continue;
                if (px < 0 || py < 0 || pz < 0 || px >= sizeX || py >= sizeY || pz >= sizeZ) continue;

                Entry entry = palette[state];
                CompoundTag nbt = NbtFiles.compoundOrNull(block, "nbt");

                if (nbt != null) {
                    entry = entry.withNbt(nbt);

                    // Load in connection points for jigsaws
                    if (LegacyStructurePalette.isJigsaw(entry.name)) {
                        if (toTopConnections == null) toTopConnections = new HashMap<>();
                        if (toBottomConnections == null) toBottomConnections = new HashMap<>();
                        if (toHorizontalConnections == null) toHorizontalConnections = new HashMap<>();

                        int selectionPriority = Tags.getIntOr(nbt, "selection", 0);
                        int placementPriority = Tags.getIntOr(nbt, "placement", 0);
                        // 1.7 files number the direction, port files name it
                        ForgeDirection direction = Tags.hasInt(nbt, "direction")
                                ? ForgeDirection.getOrientation(Tags.getIntOr(nbt, "direction", 0))
                                : SavedIds.getEnum(nbt, "direction", ForgeDirection.DOWN);
                        String poolName = Tags.getStringOr(nbt, "pool", "");
                        String ourName = Tags.getStringOr(nbt, "name", "");
                        String targetName = Tags.getStringOr(nbt, "target", "");
                        String replaceBlock = Tags.getStringOr(nbt, "block", "minecraft:air");
                        int replaceMeta = Tags.getIntOr(nbt, "meta", 0);
                        boolean isRollable = Tags.getBooleanOr(nbt, "roll", false);

                        JigsawConnection connection = new JigsawConnection(px, py, pz, direction, poolName, targetName, isRollable, selectionPriority, placementPriority);
                        connections.add(connection);

                        Map<String, List<JigsawConnection>> toConnections;
                        if (direction == ForgeDirection.UP) {
                            toConnections = toTopConnections;
                        } else if (direction == ForgeDirection.DOWN) {
                            toConnections = toBottomConnections;
                        } else {
                            toConnections = toHorizontalConnections;
                        }

                        toConnections.computeIfAbsent(ourName, n -> new ArrayList<>()).add(connection);

                        if (!ServerConfig.STRUCTURE_DEBUG.get()) {
                            String replaceState = Tags.getStringOr(nbt, "stateString", "");
                            entry = legacy || replaceState.isEmpty()
                                    ? Entry.legacy(replaceBlock, replaceMeta)
                                    : Entry.state(LegacyStructurePalette.parseState(replaceState));
                        }
                    }
                }

                blockArray[px][py][pz] = entry;
            }

            // MAP OUT CONNECTIONS + PRIORITIES
            if (!connections.isEmpty()) {
                fromConnections = new ArrayList<>();

                connections.sort((a, b) -> b.selectionPriority - a.selectionPriority); // highest first

                List<JigsawConnection> innerList = null;
                int currentPriority = 0;
                for (JigsawConnection connection : connections) {
                    if (innerList == null || currentPriority != connection.selectionPriority) {
                        innerList = new ArrayList<>();
                        fromConnections.add(innerList);
                        currentPriority = connection.selectionPriority;
                    }
                    innerList.add(connection);
                }
            }

            isLoaded = true;

        } catch (Exception e) {
            Constants.LOG.error("Exception reading NBT Structure format", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // hush
            }
        }
    }

    /** Converts the loaded legacy placement data without retaining a second copy of the source file. */
    public LegacyStructureTemplate.Result exportLegacy(HolderLookup.Provider registries,
                                                        boolean intactSpotlights) {
        if (!isLoaded || !legacy) throw new IllegalStateException(name + ": not a loaded legacy structure");
        var source = new CompoundTag();
        source.put("size", intList(sizeX, sizeY, sizeZ));
        var palette = new ListTag();
        var blocks = new ListTag();
        var indexes = new LinkedHashMap<LegacyStructurePalette.Legacy, Integer>();
        for (var x = 0; x < sizeX; x++) {
            for (var y = 0; y < sizeY; y++) {
                for (var z = 0; z < sizeZ; z++) {
                    var entry = blockArray[x][y][z];
                    if (entry == null) continue;
                    var index = indexes.computeIfAbsent(new LegacyStructurePalette.Legacy(entry.name, entry.meta), key -> {
                        var row = new CompoundTag();
                        row.putString("Name", key.name());
                        var properties = new CompoundTag();
                        properties.putString("meta", Integer.toString(key.meta()));
                        row.put("Properties", properties);
                        palette.add(row);
                        return indexes.size();
                    });
                    var block = new CompoundTag();
                    block.put("pos", intList(x, y, z));
                    block.putInt("state", index);
                    if (entry.nbt != null) block.put("nbt", entry.nbt.copy());
                    blocks.add(block);
                }
            }
        }
        var items = new ListTag();
        for (var entry : new TreeMap<>(itemPalette).entrySet()) {
            var item = new CompoundTag();
            item.putInt("ID", entry.getKey());
            item.putString("Name", entry.getValue());
            items.add(item);
        }
        source.put("palette", palette);
        source.put("blocks", blocks);
        source.put("itemPalette", items);
        source.put("entities", new ListTag());
        return LegacyStructureTemplate.convert(name, source, registries, intactSpotlights);
    }

    // building

    public void build(Level level, int x, int y, int z) {
        build(level, x, y, z, 0, true, false);
    }

    public void build(Level level, int x, int y, int z, int coordBaseMode) {
        build(level, x, y, z, coordBaseMode, true, false);
    }

    public void build(Level level, int x, int y, int z, int coordBaseMode, boolean center, boolean wipeExisting) {
        if (!isLoaded) {
            Constants.LOG.info("NBTStructure is invalid");
            return;
        }

        if (center) {
            boolean swizzle = coordBaseMode == 1 || coordBaseMode == 3;
            x -= (swizzle ? sizeZ : sizeX) / 2;
            z -= (swizzle ? sizeX : sizeZ) / 2;
        }

        Placer placer = new Placer(level, coordBaseMode, null);

        for (int bx = 0; bx < sizeX; bx++) {
            for (int bz = 0; bz < sizeZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + x;
                int rz = rotateZ(bx, bz, coordBaseMode) + z;

                for (int by = 0; by < sizeY; by++) {
                    Entry state = blockArray[bx][by][bz];
                    if (state == null) {
                        if (wipeExisting) level.setBlock(new BlockPos(rx, by + y, rz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        continue;
                    }

                    int ry = by + y;
                    if (ry < level.getMinY() + 1) continue;

                    placer.place(bx, by, bz, new BlockPos(rx, ry, rz), state);
                }
            }
        }

        placer.finish();
    }

    // Used to construct tandems
    public void build(Level level, JigsawPiece piece, int x, int y, int z, int coordBaseMode, @Nullable String structureName) {
        BoundingBox bb = switch (coordBaseMode) {
            case 1, 3 -> new BoundingBox(x, y, z, x + piece.structure.sizeZ - 1, y + piece.structure.sizeY - 1, z + piece.structure.sizeX - 1);
            default -> new BoundingBox(x, y, z, x + piece.structure.sizeX - 1, y + piece.structure.sizeY - 1, z + piece.structure.sizeZ - 1);
        };

        build(level, piece, bb, bb, coordBaseMode, structureName);
    }

    protected boolean build(LevelAccessor level, JigsawPiece piece, BoundingBox totalBounds, BoundingBox generatingBounds, int coordBaseMode, @Nullable String structureName) {
        if (!isLoaded) {
            Constants.LOG.info("NBTStructure is invalid");
            return false;
        }

        int bSizeX = totalBounds.maxX() - totalBounds.minX();
        int bSizeZ = totalBounds.maxZ() - totalBounds.minZ();

        // a piece saved before its .nbt changed size would place out of its box
        boolean swizzle = coordBaseMode == 1 || coordBaseMode == 3;
        int expectedX = (swizzle ? sizeZ : sizeX) - 1;
        int expectedZ = (swizzle ? sizeX : sizeZ) - 1;
        if (bSizeX != expectedX || bSizeZ != expectedZ) {
            Constants.LOG.warn("NBT structure {} bounds mismatch (saved {}x{}, expected {}x{}) - structure changed since worldgen, skipping piece",
                    name, bSizeX, bSizeZ, expectedX, expectedZ);
            return false;
        }

        // voxel grid transforms can fuck you up
        // you have my respect, vaer
        int absMinX = Math.max(generatingBounds.minX() - totalBounds.minX(), 0);
        int absMaxX = Math.min(generatingBounds.maxX() - totalBounds.minX(), bSizeX);
        int absMinZ = Math.max(generatingBounds.minZ() - totalBounds.minZ(), 0);
        int absMaxZ = Math.min(generatingBounds.maxZ() - totalBounds.minZ(), bSizeZ);

        // A check to see that we're actually inside the generating area at all
        if (absMinX > bSizeX || absMaxX < 0 || absMinZ > bSizeZ || absMaxZ < 0) return true;

        int rotMinX = unrotateX(absMinX, absMinZ, coordBaseMode);
        int rotMaxX = unrotateX(absMaxX, absMaxZ, coordBaseMode);
        int rotMinZ = unrotateZ(absMinX, absMinZ, coordBaseMode);
        int rotMaxZ = unrotateZ(absMaxX, absMaxZ, coordBaseMode);

        int minX = Math.min(rotMinX, rotMaxX);
        int maxX = Math.max(rotMinX, rotMaxX);
        int minZ = Math.min(rotMinZ, rotMaxZ);
        int maxZ = Math.max(rotMinZ, rotMaxZ);

        Placer placer = new Placer(level, coordBaseMode, structureName);
        placer.blockTable = piece.blockTable; // WG-C: 1.7 JigsawPiece.blockTable

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + totalBounds.minX();
                int rz = rotateZ(bx, bz, coordBaseMode) + totalBounds.minZ();
                // 1.7 getTopSolidOrLiquidBlock, despite its name, skips liquids (water does not block movement) and
                // leaves; a neighbour's canopy may already overhang this column at placement time
                int oy = piece.conformToTerrain
                        ? com.hbm.world.gen.component.Component.topSolid(level, rx, rz) + piece.heightOffset
                        : totalBounds.minY();

                for (int by = 0; by < sizeY; by++) {
                    Entry state = blockArray[bx][by][bz];
                    if (state == null) continue;

                    int ry = by + oy;
                    if (ry < level.getMinY() + 1) continue;

                    placer.place(bx, by, bz, new BlockPos(rx, ry, rz), state);
                }
            }
        }

        placer.finish();
        return true;
    }

    public @Nullable List<JigsawConnection> getConnectionPool(ForgeDirection dir, String target) {
        if (dir == ForgeDirection.DOWN) {
            return toTopConnections == null ? null : toTopConnections.get(target);
        } else if (dir == ForgeDirection.UP) {
            return toBottomConnections == null ? null : toBottomConnections.get(target);
        }

        return toHorizontalConnections == null ? null : toHorizontalConnections.get(target);
    }

    public int rotateX(int x, int z, int coordBaseMode) {
        return switch (coordBaseMode) {
            case 1 -> sizeZ - 1 - z;
            case 2 -> sizeX - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    public int rotateZ(int x, int z, int coordBaseMode) {
        return switch (coordBaseMode) {
            case 1 -> x;
            case 2 -> sizeZ - 1 - z;
            case 3 -> sizeX - 1 - x;
            default -> z;
        };
    }

    private int unrotateX(int x, int z, int coordBaseMode) {
        return switch (coordBaseMode) {
            case 3 -> sizeX - 1 - z;
            case 2 -> sizeX - 1 - x;
            case 1 -> z;
            default -> x;
        };
    }

    private int unrotateZ(int x, int z, int coordBaseMode) {
        return switch (coordBaseMode) {
            case 3 -> x;
            case 2 -> sizeZ - 1 - z;
            case 1 -> sizeZ - 1 - x;
            default -> z;
        };
    }

    /** The 1.7 coordBaseMode as a vanilla rotation: 1 turns +X into +Z, i.e. 90 degrees clockwise seen from above. */
    private static Rotation rotation(int coordBaseMode) {
        return switch (coordBaseMode) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Places entries of one build: the 1.7 per-block transform, the flattened state, the block entity, and at the
     * end the multiblock cores and the neighbour-shape pass (fences, panes, stair corners, chain ends), which 1.7
     * computed at render time and a flattened world stores in the state.
     */
    private final class Placer {
        private final LevelAccessor level;
        private final int coordBaseMode;
        private final @Nullable String structureName;
        private final boolean debug = ServerConfig.STRUCTURE_DEBUG.get();
        private final List<BlockPos> placed = new ArrayList<>();
        private final List<BlockPos> cores = new ArrayList<>();
        /** WG-C: 1.7 {@code JigsawPiece.blockTable}, keyed by port block id path; null for no swaps. */
        java.util.@Nullable Map<String, com.hbm.world.gen.component.Component.BlockSelector> blockTable;

        Placer(LevelAccessor level, int coordBaseMode, @Nullable String structureName) {
            this.level = level;
            this.coordBaseMode = coordBaseMode;
            this.structureName = structureName;
        }

        void place(int bx, int by, int bz, BlockPos pos, Entry entry) {
            BlockState state;
            boolean core = false;

            if (entry.modern != null) {
                state = entry.modern.rotate(rotation(coordBaseMode));
                // a saved multiblock: the core rebuilds its cells (and their index), the saved cells are skipped
                if (state.getBlock() instanceof BlockMultiblockCell) return;
                if (state.getBlock() instanceof BlockMultiblockCore multiblock) {
                    if (multiblock.isCellState(state)) return;
                    core = true;
                }
            } else {
                LegacyStructurePalette.Legacy t = LegacyStructurePalette.transform(entry.name, entry.meta, coordBaseMode);
                LegacyStructurePalette.Resolved r = LegacyStructurePalette.resolve(t.name(), t.meta());
                switch (r.kind()) {
                    case CELL -> { return; }
                    case CORE -> { state = r.state(); core = true; }
                    case DOOR -> state = LegacyStructurePalette.door(r.state(), t.meta(), otherDoorMeta(bx, by, bz, entry, t.meta()));
                    default -> state = r.state();
                }
            }

            // WG-C: 1.7 JigsawPiece.blockTable (the meteor dungeon's bricks/crates/spawners/ooze) swaps a matching 1.7
            // block for the selector's pick, unrotated; a swapped block does not get the original's block entity data
            boolean swapped = false;
            if (blockTable != null) {
                BlockState picked = com.hbm.world.gen.component.Component.blockTablePick(blockTable, entry.modern != null
                        ? entry.modern.getBlock() : LegacyStructurePalette.resolve(entry.name, entry.meta).state().getBlock(), level.getRandom());
                if (picked != null) {
                    swapped = picked.getBlock() != state.getBlock();
                    state = picked;
                    core = false;
                }
            }

            // 1.7: with STRUCTURE_DEBUG on, palette air is shown as wand_air
            if (debug && state.isAir()) state = ModStructureWandBlocks.wand_air.get().defaultBlockState();

            level.setBlock(pos, Waterlogging.inWorld(state, level, pos), Block.UPDATE_CLIENTS);
            placed.add(pos);
            if (core) cores.add(pos);

            if (entry.nbt != null && !swapped) buildTileEntity(pos, entry);
        }

        /** The metadata of the other half of a door, rotated the same way; -1 if that half is not a door part. */
        private int otherDoorMeta(int bx, int by, int bz, Entry entry, int ownMeta) {
            int oy = (ownMeta & 8) != 0 ? by - 1 : by + 1;
            if (oy < 0 || oy >= sizeY) return -1;
            Entry other = blockArray[bx][oy][bz];
            if (other == null || other.modern != null || !other.name.equals(entry.name)) return -1;
            return LegacyStructurePalette.transform(other.name, other.meta, coordBaseMode).meta();
        }

        private void buildTileEntity(BlockPos pos, Entry entry) {
            BlockEntity be = level.getBlockEntity(pos);
            CompoundTag nbt = entry.nbt.copy();

            if (be == null) {
                if (legacy && !LegacyStructureTiles.apply(level, pos, null, nbt, itemPalette, coordBaseMode)) {
                    unmigrated(Tags.getStringOr(nbt, "id", "?") + " (no block entity at the placed block)");
                }
                return;
            }

            if (legacy) {
                if (be instanceof ILegacyStructureTile tile) tile.readLegacyStructureNBT(nbt);
                else if (!LegacyStructureTiles.apply(level, pos, be, nbt, itemPalette, coordBaseMode)) {
                    unmigrated(Tags.getStringOr(nbt, "id", "?"));
                }
            } else {
                BlockEntities.loadWithComponents(be, nbt, level.registryAccess());
            }

            if (be instanceof INBTTileEntityTransformable transformable) {
                transformable.transformTE(level, coordBaseMode);
            }

            if (be instanceof TileEntityWandTandem tandem) {
                tandem.arm(getStructure(structureName));
            }

            be.setChanged();
        }

        void finish() {
            for (BlockPos core : cores) {
                BlockState state = level.getBlockState(core);
                if (!(state.getBlock() instanceof BlockMultiblockCore block)) continue;
                Direction facing = state.getValue(BlockMultiblockCore.FACING);
                BlockMultiblockCore.withoutTeardown(() -> block.fillSpace(level, core, facing));
                // world generation: the shape pass below and the chunk's own neighbour updates stand in for this
                if (level instanceof Level real) MultiblockHandlerXR.refreshSpace(real, core, block.getDimensions(), facing);
            }
            for (BlockPos pos : placed) {
                BlockState state = level.getBlockState(pos);
                BlockState shaped = Block.updateFromNeighbourShapes(state, level, pos);
                if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private static void unmigrated(String id) {
        if (UNMIGRATED.add(id)) Constants.LOG.info("[NBTStructure] 1.7 tile entity data for {} is not migrated; the block starts from its defaults", id);
    }

    /** One palette entry: a 1.7 name + metadata, or a port-saved state; plus its tile-entity NBT. */
    private static final class Entry {
        final String name;
        final int meta;
        final @Nullable BlockState modern;
        final @Nullable CompoundTag nbt;

        private Entry(String name, int meta, @Nullable BlockState modern, @Nullable CompoundTag nbt) {
            this.name = name;
            this.meta = meta;
            this.modern = modern;
            this.nbt = nbt;
        }

        static Entry legacy(String name, int meta) {
            return new Entry(name, meta, null, null);
        }

        static Entry modern(String name, CompoundTag tag) {
            return new Entry(name, 0, LegacyStructurePalette.readState(tag), null);
        }

        static Entry state(BlockState state) {
            return new Entry(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())), 0, state, null);
        }

        Entry withNbt(CompoundTag nbt) {
            return new Entry(name, meta, modern, nbt);
        }
    }

    // Each jigsaw block in a structure will instance one of these
    public static class JigsawConnection {

        public final int x, y, z;
        public final ForgeDirection dir;

        // what pool should we look through to find a connection
        private final String poolName;

        // when we successfully find a pool, what connections in that jigsaw piece can we target
        private final String targetName;

        private final boolean isRollable;

        private final int selectionPriority;
        private final int placementPriority;

        private JigsawConnection(int x, int y, int z, ForgeDirection dir, String poolName, String targetName, boolean isRollable, int selectionPriority, int placementPriority) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dir = dir;
            this.poolName = poolName;
            this.targetName = targetName;
            this.isRollable = isRollable;
            this.selectionPriority = selectionPriority;
            this.placementPriority = placementPriority;
        }
    }

    // world generation (1.7 Component / Start / GenStructure)

    /**
     * 1.7 {@code Component}: one placed {@link JigsawPiece} of a generated structure, as a vanilla structure piece.
     *
     * <p>1.7 anchored the piece at the start chunk's corner and decided its Y on the first chunk that populated it; the
     * port decides Y once, when the start is assembled ({@link Start}), from the generator's own terrain, so every
     * chunk of the piece agrees without shared mutable state across world-generation threads. The bounding box is the
     * 1.7 one (not centred, rotated footprint). {@link #postProcess} builds the part inside the chunk being generated,
     * exactly like 1.7's {@code addComponentParts} with the populating box.
     */
    public static class Component extends StructurePiece {

        @Nullable JigsawPiece piece;

        int minHeight = 1;
        int maxHeight = 128;

        boolean heightUpdated = false;

        int priority;

        int coordBaseMode;

        /** 1.7 {@code parent.name}: the structure's registered name, which tandem placeholders look up. */
        String structureName = "";

        // only used while the start is assembled
        @Nullable private JigsawConnection connectedFrom;

        public Component(SpawnCondition spawn, JigsawPiece piece, int x, int y, int z, int coordBaseMode) {
            super(NTMWorldGenerator.NBT_COMPONENT.get(), 0, box(piece, x, y, z, coordBaseMode));
            this.piece = piece;
            this.coordBaseMode = coordBaseMode;
            this.minHeight = spawn.minHeight;
            this.maxHeight = spawn.maxHeight;
            this.structureName = spawn.name;
        }

        /** A start that claims its region and places nothing: 1.7's null weights ({@code registerNullWeight}). */
        private Component(String name, BlockPos pos) {
            super(NTMWorldGenerator.NBT_COMPONENT.get(), 0,
                    new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
            this.piece = null;
            this.structureName = name;
            this.heightUpdated = true;
        }

        public Component(StructurePieceSerializationContext context, CompoundTag tag) {
            super(NTMWorldGenerator.NBT_COMPONENT.get(), tag);
            piece = JigsawPiece.jigsawMap.get(Tags.getStringOr(tag, "piece", "NULL"));
            minHeight = Tags.getIntOr(tag, "min", 1);
            maxHeight = Tags.getIntOr(tag, "max", 128);
            heightUpdated = Tags.getBooleanOr(tag, "hasHeight", false);
            coordBaseMode = Tags.getIntOr(tag, "coordBase", 0);
            structureName = Tags.getStringOr(tag, "structure", "");
        }

        private static BoundingBox box(JigsawPiece piece, int x, int y, int z, int coordBaseMode) {
            NBTStructure s = piece.structure;
            return switch (coordBaseMode) {
                case 1, 3 -> new BoundingBox(x, y, z, x + s.sizeZ - 1, y + s.sizeY - 1, z + s.sizeX - 1);
                default -> new BoundingBox(x, y, z, x + s.sizeX - 1, y + s.sizeY - 1, z + s.sizeZ - 1);
            };
        }

        public @Nullable JigsawPiece getPiece() { return piece; }
        public int getCoordBaseMode() { return coordBaseMode; }
        public String getStructureName() { return structureName; }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("piece", piece != null ? piece.name : "NULL");
            tag.putInt("min", minHeight);
            tag.putInt("max", maxHeight);
            tag.putBoolean("hasHeight", heightUpdated);
            tag.putInt("coordBase", coordBaseMode);
            tag.putString("structure", structureName);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBB, ChunkPos chunkPos, BlockPos pivot) {
            if (piece == null) return;
            try {
                piece.structure.build(level, piece, boundingBox, chunkBB, coordBaseMode, structureName);
            } catch (RuntimeException e) {
                // 1.7 catches any structure failure so the chunk still generates, without the structure
                Constants.LOG.error("NTM structure gen failed at chunk {}, {} ({}) - chunk generates without it",
                        chunkPos.getMinBlockX() >> 4, chunkPos.getMinBlockZ() >> 4, structureName, e);
            }
        }

        void offsetYHeight(int y) {
            move(0, y, 0);
            heightUpdated = true;
        }

        /**
         * Tests and the harness: of this piece's solid palette blocks (placeholders, multiblock cells, air and the
         * plain stone/dirt/gravel that later ore features legitimately replace left out), how many stand in {@code level} as the block the palette names, compared by block, not state (the
         * neighbour-shape pass and loot replacement legitimately change states). Returns {matched, total}. A
         * conforming piece has a per-column height, so each column is matched at its best vertical offset.
         */
        public int[] verify(LevelReader level) {
            return verify(level, null);
        }

        /** {@link #verify(net.minecraft.world.level.LevelReader)}, counting {@code expected->found} misses (non-conforming pieces). */
        public int[] verify(LevelReader level, @Nullable Map<String, Integer> misses) {
            if (piece == null) return new int[] {0, 0};
            NBTStructure s = piece.structure;
            int matched = 0, total = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int bx = 0; bx < s.sizeX; bx++) {
                for (int bz = 0; bz < s.sizeZ; bz++) {
                    int rx = boundingBox.minX() + s.rotateX(bx, bz, coordBaseMode);
                    int rz = boundingBox.minZ() + s.rotateZ(bx, bz, coordBaseMode);
                    Block[] expected = new Block[s.sizeY];
                    int columnTotal = 0;
                    for (int by = 0; by < s.sizeY; by++) {
                        Entry e = s.blockArray[bx][by][bz];
                        if (e == null) continue;
                        Block block;
                        if (e.modern != null) {
                            block = e.modern.getBlock();
                        } else {
                            LegacyStructurePalette.Legacy t = LegacyStructurePalette.transform(e.name, e.meta, coordBaseMode);
                            LegacyStructurePalette.Resolved r = LegacyStructurePalette.resolve(t.name(), t.meta());
                            if (r.kind() == LegacyStructurePalette.Kind.CELL) continue;
                            block = r.state().getBlock();
                        }
                        if (block.defaultBlockState().isAir() || block instanceof BlockWand || LegacyStructurePalette.isJigsaw(e.name)
                                || block instanceof BlockWandLoot || block instanceof BlockWandLogic) continue;
                        // terrain the structure carries (the lighthouse's rock): later ore/disk features replace it, as 1.7's did
                        if (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL) continue;
                        // never placed (1.7 skipped y < 1): the superflat rule does not clamp, so a sunk piece reaches the floor
                        int placedY = boundingBox.minY() + by;
                        if (!piece.conformToTerrain && (level.isOutsideBuildHeight(placedY) || placedY < level.getMinY() + 1)) continue;
                        expected[by] = block;
                        columnTotal++;
                    }
                    if (columnTotal == 0) continue;
                    total += columnTotal;
                    int best = 0;
                    int from = piece.conformToTerrain ? -48 : 0;
                    int to = piece.conformToTerrain ? 48 : 0;
                    for (int d = from; d <= to; d++) {
                        int hits = 0;
                        for (int by = 0; by < s.sizeY; by++) {
                            if (expected[by] == null) continue;
                            pos.set(rx, boundingBox.minY() + by + d, rz);
                            if (level.getBlockState(pos).is(expected[by])) hits++;
                        }
                        best = Math.max(best, hits);
                    }
                    matched += best;
                    if (misses != null && !piece.conformToTerrain && best < columnTotal) {
                        for (int by = 0; by < s.sizeY; by++) {
                            if (expected[by] == null) continue;
                            pos.set(rx, boundingBox.minY() + by, rz);
                            Block found = level.getBlockState(pos).getBlock();
                            if (found != expected[by]) {
                                misses.merge(BuiltInRegistries.BLOCK.getKey(expected[by]).getPath() + "->"
                                        + BuiltInRegistries.BLOCK.getKey(found).getPath(), 1, Integer::sum);
                            }
                        }
                    }
                }
            }
            return new int[] {matched, total};
        }

        // Overrides to fix Mojang's rotations which FLIP instead of rotating in two instances (1.7)
        int getXWithOffset(int x, int z) {
            return boundingBox.minX() + piece.structure.rotateX(x, z, coordBaseMode);
        }

        int getYWithOffset(int y) {
            return boundingBox.minY() + y;
        }

        int getZWithOffset(int x, int z) {
            return boundingBox.minZ() + piece.structure.rotateZ(x, z, coordBaseMode);
        }

        private ForgeDirection rotateDir(ForgeDirection dir) {
            if (dir == ForgeDirection.UP || dir == ForgeDirection.DOWN) return dir;
            return switch (coordBaseMode) {
                case 1 -> dir.getRotation(ForgeDirection.UP);
                case 2 -> dir.getOpposite();
                case 3 -> dir.getRotation(ForgeDirection.DOWN);
                default -> dir;
            };
        }

        private int getNextCoordBase(JigsawConnection fromConnection, JigsawConnection toConnection, RandomSource rand) {
            if (fromConnection.dir == ForgeDirection.DOWN || fromConnection.dir == ForgeDirection.UP) {
                if (fromConnection.isRollable) return rand.nextInt(4);
                return coordBaseMode;
            }

            return directionOffsetToCoordBase(fromConnection.dir.getOpposite(), toConnection.dir);
        }

        private int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
            for (int i = 0; i < 4; i++) {
                if (from == to) return (i + coordBaseMode) % 4;
                from = from.getRotation(ForgeDirection.DOWN);
            }
            return coordBaseMode;
        }

        boolean hasIntersectionIgnoringSelf(List<Component> components, BoundingBox box) {
            for (Component component : components) {
                if (component == this) continue;
                if (component.getBoundingBox().intersects(box)) return true;
            }
            return false;
        }

        boolean isInsideIgnoringSelf(List<Component> components, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            for (Component component : components) {
                if (component == this) continue;
                if (component.getBoundingBox().isInside(pos)) return true;
            }
            return false;
        }
    }

    /**
     * 1.7 {@code Start}: assembles a structure's components from its {@link SpawnCondition} (one piece, or jigsaw pools
     * grown from the start pool with 1.7's size/range limits, required pieces, fallbacks and placement priorities), then
     * sets their height.
     *
     * <p>Height, 1.7: the average {@code getTopSolidOrLiquidBlock} over the first populating chunk's box plus the piece's
     * {@code heightOffset}, clamped to {@code minHeight..maxHeight} unless the world is superflat; conforming pieces
     * follow each column instead, aligning pieces take their own average. The port uses the start chunk's populate box
     * (x+8..x+23, 1.7's populate offset) through the generator's {@code OCEAN_FLOOR_WG} height, which is the same
     * rule (highest movement-blocking block, liquids ignored, + 1); aligning pieces average over their own footprint.
     */
    public static final class Start {

        private Start() {}

        public static void assemble(StructurePiecesBuilder builder,
                                    Structure.GenerationContext context, SpawnCondition spawn) {
            assemble(builder, context, spawn, context.random());
        }

        /**
         * {@link #assemble(net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder,
         * net.minecraft.world.level.levelgen.structure.Structure.GenerationContext, SpawnCondition)} drawing from
         * {@code rand} instead of the context's random. 1.7's {@code MapGenStructure} built
         * the start with the random its region pick had just drawn from; an addon whose structure type repeats that pick
         * (NTM: Space's planet structures) hands that random on, so the pieces, rotations and pools are 1.7's.
         */
        public static void assemble(StructurePiecesBuilder builder,
                                    Structure.GenerationContext context, SpawnCondition spawn,
                                    RandomSource rand) {
            int x = context.chunkPos().getMinBlockX();
            int z = context.chunkPos().getMinBlockZ();

            JigsawPiece startPiece = spawn.structure != null ? spawn.structure : spawn.pools.get(spawn.startPool).get(rand);
            if (startPiece == null || !startPiece.structure.isLoaded) return;

            List<Component> components = new ArrayList<>();
            Component startComponent = new Component(spawn, startPiece, x, 0, z, rand.nextInt(4));
            components.add(startComponent);

            if (spawn.structure == null) grow(components, startComponent, spawn, rand, context.chunkPos());

            setHeights(components, context);

            for (Component component : components) builder.addPiece(component);
        }

        /** The marker start of a null weight: valid (so the region is taken), empty. */
        static void claim(StructurePiecesBuilder builder, String name, BlockPos pos) {
            builder.addPiece(new Component(name, pos));
        }

        private static void grow(List<Component> components, Component startComponent, SpawnCondition spawn, RandomSource rand,
                                 ChunkPos origin) {
            List<Component> queuedComponents = new ArrayList<>();
            queuedComponents.add(startComponent);

            Set<JigsawPiece> requiredPieces = findRequiredPieces(spawn);

            // Iterate through and build out all the components we intend to spawn
            while (!queuedComponents.isEmpty()) {
                queuedComponents.sort((a, b) -> b.priority - a.priority); // sort by placement priority descending
                int matchPriority = queuedComponents.get(0).priority;
                int max = 1;
                while (max < queuedComponents.size()) {
                    if (queuedComponents.get(max).priority != matchPriority) break;
                    max++;
                }

                final int i = rand.nextInt(max);
                Component fromComponent = queuedComponents.remove(i);

                if (fromComponent.piece.structure.fromConnections == null) continue;

                int distance = getDistanceTo(fromComponent.getBoundingBox(), origin);

                // Only generate fallback pieces once we hit our size limit, unless we have a required component
                // There is a HARD limit of 1024 pieces to prevent infinite generation
                boolean fallbacksOnly = requiredPieces.isEmpty() && (components.size() >= spawn.sizeLimit || distance >= spawn.rangeLimit) || components.size() > 1024;

                for (List<JigsawConnection> unshuffledList : fromComponent.piece.structure.fromConnections) {
                    List<JigsawConnection> connectionList = new ArrayList<>(unshuffledList);
                    shuffle(connectionList, rand);

                    for (JigsawConnection fromConnection : connectionList) {
                        if (fromComponent.connectedFrom == fromConnection) continue; // if we already connected to this piece, don't process

                        if (fallbacksOnly) {
                            JigsawPool pool = spawn.pools.get(fromConnection.poolName);
                            String fallback = pool == null ? null : pool.fallback;

                            if (fallback != null) {
                                Component fallbackComponent = buildNextComponent(components, rand, spawn, spawn.pools.get(fallback), fromComponent, fromConnection);
                                addComponent(components, fallbackComponent, fromConnection.placementPriority);
                            }

                            continue;
                        }

                        JigsawPool nextPool = spawn.getPool(fromConnection.poolName);
                        if (nextPool == null) {
                            Constants.LOG.warn("[Jigsaw] Jigsaw block points to invalid pool: " + fromConnection.poolName);
                            continue;
                        }

                        Component nextComponent = null;

                        // Iterate randomly through the pool, attempting each piece until one fits
                        while (nextPool.totalWeight > 0) {
                            nextComponent = buildNextComponent(components, rand, spawn, nextPool, fromComponent, fromConnection);
                            if (nextComponent != null && !fromComponent.hasIntersectionIgnoringSelf(components, nextComponent.getBoundingBox())) break;
                            nextComponent = null;
                        }

                        if (nextComponent != null) {
                            addComponent(components, nextComponent, fromConnection.placementPriority);
                            queuedComponents.add(nextComponent);

                            requiredPieces.remove(nextComponent.piece);
                        } else {
                            // If we failed to fit anything in, grab something from the fallback pool, ignoring bounds check
                            // unless we are perfectly abutting another piece, so grid layouts can work!
                            if (nextPool.fallback != null) {
                                BlockPos checkPos = getConnectionTargetPosition(fromComponent, fromConnection);

                                if (!fromComponent.isInsideIgnoringSelf(components, checkPos.getX(), checkPos.getY(), checkPos.getZ())) {
                                    nextComponent = buildNextComponent(components, rand, spawn, spawn.pools.get(nextPool.fallback), fromComponent, fromConnection);
                                    addComponent(components, nextComponent, fromConnection.placementPriority); // don't add to queued list, we don't want to try continue from fallback
                                }
                            }
                        }
                    }
                }
            }
        }

        /** {@code Collections.shuffle} (Fisher-Yates from the end) over a world-generation random. */
        private static <T> void shuffle(List<T> list, RandomSource rand) {
            for (int i = list.size(); i > 1; i--) {
                int j = rand.nextInt(i);
                T t = list.get(i - 1);
                list.set(i - 1, list.get(j));
                list.set(j, t);
            }
        }

        private static void addComponent(List<Component> components, @Nullable Component component, int placementPriority) {
            if (component == null) return;
            components.add(component);
            component.priority = placementPriority;
        }

        private static BlockPos getConnectionTargetPosition(Component component, JigsawConnection connection) {
            // The direction this component is extending towards in ABSOLUTE direction
            ForgeDirection extendDir = component.rotateDir(connection.dir);

            // Set the starting point for the next structure to the location of the connector block
            int x = component.getXWithOffset(connection.x, connection.z) + extendDir.offsetX;
            int y = component.getYWithOffset(connection.y) + extendDir.offsetY;
            int z = component.getZWithOffset(connection.x, connection.z) + extendDir.offsetZ;

            return new BlockPos(x, y, z);
        }

        private static Set<JigsawPiece> findRequiredPieces(SpawnCondition spawn) {
            Set<JigsawPiece> requiredPieces = new HashSet<>();

            if (spawn.pools == null) return requiredPieces;

            for (JigsawPool pool : spawn.pools.values()) {
                for (JigsawPool.Weighted weight : pool.pieces) {
                    if (weight.piece().required) requiredPieces.add(weight.piece());
                }
            }

            return requiredPieces;
        }

        private static @Nullable Component buildNextComponent(List<Component> components, RandomSource rand, SpawnCondition spawn,
                                                              @Nullable JigsawPool pool, Component fromComponent, JigsawConnection fromConnection) {
            JigsawPiece nextPiece = pool == null ? null : pool.get(rand);
            if (nextPiece == null) {
                Constants.LOG.warn("[Jigsaw] Pool returned null piece: " + fromConnection.poolName);
                return null;
            }

            if (nextPiece.instanceLimit > 0) {
                int instances = 0;
                for (Component component : components) {
                    if (component.piece == nextPiece) {
                        instances++;

                        if (instances >= nextPiece.instanceLimit) return null;
                    }
                }
            }

            List<JigsawConnection> connectionPool = nextPiece.structure.getConnectionPool(fromConnection.dir, fromConnection.targetName);
            if (connectionPool == null || connectionPool.isEmpty()) {
                Constants.LOG.warn("[Jigsaw] No valid connections for: " + fromConnection.targetName + " - in piece: " + nextPiece.name);
                return null;
            }

            JigsawConnection toConnection = connectionPool.get(rand.nextInt(connectionPool.size()));

            // Rotate our incoming piece to plug it in
            int nextCoordBase = fromComponent.getNextCoordBase(fromConnection, toConnection, rand);

            BlockPos pos = getConnectionTargetPosition(fromComponent, fromConnection);

            // offset the starting point to the connecting point
            int ox = nextPiece.structure.rotateX(toConnection.x, toConnection.z, nextCoordBase);
            int oy = toConnection.y;
            int oz = nextPiece.structure.rotateZ(toConnection.x, toConnection.z, nextCoordBase);

            Component next = new Component(spawn, nextPiece, pos.getX() - ox, pos.getY() - oy, pos.getZ() - oz, nextCoordBase);
            next.connectedFrom = toConnection;
            return next;
        }

        private static int getDistanceTo(BoundingBox box, ChunkPos origin) {
            // 1.7 StructureBoundingBox.getCenterX/Z: min + (max - min + 1) / 2
            int x = box.minX() + (box.maxX() - box.minX() + 1) / 2;
            int z = box.minZ() + (box.maxZ() - box.minZ() + 1) / 2;

            return Math.max(Math.abs(x - origin.getMinBlockX()), Math.abs(z - origin.getMinBlockZ()));
        }

        private static void setHeights(List<Component> components, Structure.GenerationContext context) {
            boolean isFlatWorld = context.chunkGenerator() instanceof FlatLevelSource;
            int x = context.chunkPos().getMinBlockX() + 8;
            int z = context.chunkPos().getMinBlockZ() + 8;
            int chunkAverage = averageHeight(context, x, z, x + 15, z + 15);

            // the first component that neither conforms nor aligns sets the height of all such (1.7 parent.offsetYHeight)
            Integer shared = null;
            for (Component component : components) {
                if (component.piece.conformToTerrain || component.piece.alignToTerrain) continue;
                int averageHeight = chunkAverage + component.piece.heightOffset;
                shared = isFlatWorld ? averageHeight : Mth.clamp(averageHeight, component.minHeight, component.maxHeight);
                break;
            }

            for (Component component : components) {
                if (component.piece.conformToTerrain) {
                    // placed column by column at build time; the box only has to sit near the surface
                    component.move(0, chunkAverage + component.piece.heightOffset, 0);
                } else if (component.piece.alignToTerrain) {
                    BoundingBox b = component.getBoundingBox();
                    int averageHeight = averageHeight(context, b.minX(), b.minZ(), b.maxX(), b.maxZ()) + component.piece.heightOffset;
                    component.offsetYHeight(isFlatWorld ? averageHeight : Mth.clamp(averageHeight, component.minHeight, component.maxHeight));
                } else if (shared != null) {
                    component.offsetYHeight(shared);
                }
            }
        }

        /** 1.7 {@code getAverageHeight}: the mean {@code getTopSolidOrLiquidBlock} over a box, 64 if the box is empty. */
        static int averageHeight(Structure.GenerationContext context, int minX, int minZ, int maxX, int maxZ) {
            int total = 0;
            int iterations = 0;

            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    total += context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                    iterations++;
                }
            }

            if (iterations == 0) return 64;

            return total / iterations;
        }
    }

    /**
     * 1.7 {@code GenStructure} as a vanilla {@link net.minecraft.world.level.levelgen.structure.Structure} of type
     * {@code hbm:nbt}: one per registered {@link SpawnCondition} ({@code "spawn"} names it), all of them in the
     * {@code hbm:ntm_structures} structure set.
     *
     * <p>1.7 cut the world into regions of {@code structureMaxChunks} chunks, picked one chunk per region from a
     * seeded random ({@code x * 341873128712 + z * 132897987541 + seed + 996996996 - dimension}, two
     * {@code nextInt(max - min)}), then made one weighted pick among the structures valid for the biome at that chunk's
     * centre, null weights included. The set's {@code random_spread} placement (spacing 16, separation 4, salt
     * 996996996, linear) is that region pick bit for bit. Vanilla then draws among the set's structures by weight and
     * redraws without the pick while it is invalid for its biome, which is the same distribution as 1.7's draw among
     * the valid ones (not the same draw: the random differs). A null weight is a structure whose start claims the
     * region and places nothing.
     *
     * <p>The biome is sampled where 1.7 read it: the start chunk's centre column, at the surface
     * ({@code WORLD_SURFACE_WG}, so an ocean is sampled at its water, not in a cave biome below it).
     */
    public static class GenStructure extends Structure {

        public static final MapCodec<GenStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                settingsCodec(i),
                Codec.STRING.fieldOf("spawn").forGetter(GenStructure::spawnName)
        ).apply(i, GenStructure::new));

        private static final Set<String> MISSING = new HashSet<>();

        private final String spawnName;

        public GenStructure(StructureSettings settings, String spawnName) {
            super(settings);
            this.spawnName = spawnName;
        }

        public String spawnName() {
            return spawnName;
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            if (StructureConfig.enableStructures == 0) return Optional.empty();

            SpawnCondition spawn = getStructure(spawnName);
            if (spawn == null) {
                synchronized (MISSING) {
                    if (MISSING.add(spawnName)) Constants.LOG.error("[NBTStructure] no structure registered as '{}'", spawnName);
                }
                return Optional.empty();
            }

            int cx = context.chunkPos().getMiddleBlockX();
            int cz = context.chunkPos().getMiddleBlockZ();
            int surface = context.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            BlockPos center = new BlockPos(cx, surface, cz);

            // 1.7 ran the spawn's biome predicate on this biome. The structure's tag only narrows the candidates (a
            // modded biome is in it through the convention tags of the types the predicate needs); a table biome answers
            // through its row, any other through LegacyBiomes.guess. Skipped where the context's own biome rule is off
            // (the /place command generates in any biome): there a biome no tag can hold passes it.
            if (checksBiome(context)) {
                var biome = LegacyBiomes.biomeAt(context.biomeSource(), context.randomState(), cx, surface, cz);
                if (!LegacyBiomes.accepts(spawnName, biome, context)) return Optional.empty();
                // 1.7 picked one spawn per region by the hbm.cfg weights (5.05..5.43); only that one generates here
                if (!wins(spawnName, biome, context)) return Optional.empty();
            }

            return Optional.of(new GenerationStub(center, builder -> {
                if (spawn.start != null) {
                    spawn.start.build(builder, context);
                } else if (spawn.structure != null || spawn.pools != null) {
                    Start.assemble(builder, context, spawn);
                } else {
                    Start.claim(builder, spawn.name, center);
                }
            }));
        }

        private static final Map<String, TagKey<Biome>> HAS_STRUCTURE =
                new ConcurrentHashMap<>();

        /**
         * 1.7 {@code NBTStructure.findSpawn}: one weighted pick, by the live {@code hbm.cfg} weights
         * ({@code NTMStructureDeclarations.runtimeWeight}), among the spawns whose biome rule accepts the region's biome
         * (its {@code has_structure} tag and its 1.7 predicate, as this structure's own check). True when {@code spawn}
         * is the pick. Every structure of the set computes the same pick from the same seed, so exactly one of them
         * (or a null weight) generates per region, whichever order vanilla tries them in. The data pack's weights do not
         * decide anything. The draw is vanilla's own large-feature seed of the chunk.
         */
        public static boolean wins(String spawn, Holder<Biome> biome,
                                   GenerationContext context) {
            List<NTMStructureDeclarations.Entry> candidates = new ArrayList<>();
            int total = 0;
            for (NTMStructureDeclarations.Entry entry : NTMStructureDeclarations.entries()) {
                int weight = NTMStructureDeclarations.runtimeWeight(entry.id());
                if (weight <= 0) continue;
                TagKey<Biome> tag = HAS_STRUCTURE.computeIfAbsent(entry.id(),
                        id -> TagKey.create(Registries.BIOME,
                                Ids.of(Constants.MOD_ID, "has_structure/" + id)));
                if (!biome.is(tag) || !LegacyBiomes.accepts(entry.spawn(), biome, context)) continue;
                candidates.add(entry);
                total += weight;
            }
            if (total <= 0) return false;
            WorldgenRandom random =
                    new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(context.seed(), context.chunkPos().getMinBlockX() >> 4, context.chunkPos().getMinBlockZ() >> 4);
            int roll = random.nextInt(total);
            for (NTMStructureDeclarations.Entry entry : candidates) {
                roll -= NTMStructureDeclarations.runtimeWeight(entry.id());
                if (roll < 0) return entry.spawn().equals(spawn);
            }
            return false;
        }

        /**
         * Whether the context tests biomes at all: world generation and {@code /locate} pass the structure's biome set,
         * which cannot hold an unregistered (direct) holder; {@code /place structure} passes "any biome".
         */
        public static boolean checksBiome(GenerationContext context) {
            var any = context.biomeSource().possibleBiomes().stream().findFirst();
            return any.isEmpty() || !context.validBiome().test(Holder.direct(any.get().value()));
        }

        /**
         * Whether vanilla's {@code generate} of this structure in the region is valid, without building it: the checks
         * of {@link #findGenerationPoint}, {@code Structure.isValidBiome} at the stub, and a non-empty pieces builder
         * ({@link Start#assemble} returns empty only for a missing or unloaded start piece, drawn from the context's
         * random, whose seed is the draw's).
         */
        private boolean generates(GenerationContext context, Holder<Biome> biome, int chunkX, int chunkZ) {
            if (StructureConfig.enableStructures == 0) return false;
            SpawnCondition spawn = getStructure(spawnName);
            if (spawn == null) return false;
            if (!biomes().contains(biome)) return false;
            if (spawn.start != null) return true; // features, bunker: one start piece at least
            if (spawn.structure != null) return spawn.structure.structure.isLoaded;
            if (spawn.pools != null) {
                JigsawPool pool = spawn.pools.get(spawn.startPool);
                if (pool == null) return false;
                WorldgenRandom random =
                        new WorldgenRandom(new LegacyRandomSource(0L));
                random.setLargeFeatureSeed(context.seed(), chunkX, chunkZ);
                JigsawPiece startPiece = pool.get(random);
                return startPiece != null && startPiece.structure.isLoaded;
            }
            return true; // null weight: the claim marker
        }

        @Override
        public StructureType<?> type() {
            return NTMWorldGenerator.NBT_STRUCTURE.get();
        }
    }
}
