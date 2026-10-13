package com.hbm.world.gen;

import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.blocks.generic.Waterlogging;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.itempool.ItemPool;
import com.hbm.lib.Library;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.component.LegacyBlock;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.world.ItemStacks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The 1.7 {@code World} as the legacy hardcoded dungeons saw it, made deterministic so that a modern structure can be
 * built chunk by chunk.
 *
 * <p><b>Why a virtual world.</b> 1.7's {@code HbmWorldGen} ran each builder once, on the whole world, reading the
 * blocks it had just written ({@code if(world.getBlock(..) == Blocks.chest)}, the jungle rooms' trap and hole tests,
 * the tomb's tunnel lining) and the terrain around it ({@code getHeightValue}, the spawn-site tests). Modern world
 * generation builds a structure one chunk at a time and may only touch that chunk, so the builder runs here:
 * every write lands in a buffer, every read answers from that buffer or else from the generator's own terrain (the
 * noise column before any surface rule, carver or feature: {@link Terrain}). The run depends only on the piece's
 * seed and the world's generator, so every chunk of the structure sees the same result; each chunk copies its
 * share of the buffer into the world ({@link Result#apply}). The builders themselves are 1.7's, call for call.
 *
 * <p><b>Terrain.</b> Before surface rules the generator knows only solid, water, lava and air. A solid answers as
 * {@code Blocks.stone}, which every 1.7 spawn-site list accepts, so the site tests reduce to ground at the right
 * height, air above it, no water. {@code getHeightValue} is the generator's {@code WORLD_SURFACE_WG} height (1.7's
 * height map counted water), raised by what the builder wrote.
 *
 * <p><b>Random streams.</b> 1.7 had three: the populate random handed to the builder, {@code world.rand} (the jungle
 * dungeon, {@code DungeonToolbox.generateBox} lists, the arctic vault) and {@code Library.rand} (the random concrete).
 * They stay three independent seeded streams here.
 *
 * <p><b>Block entities.</b> Chest and crate contents are drawn during the run with the builder's random, exactly as
 * 1.7's {@code WeightedRandomChestContent.generateChestContents} drew them into the inventory (random slot per stack),
 * and copied into the placed container. The library's {@code ChestGenHooks.DUNGEON_CHEST} is vanilla's dungeon chest:
 * the chest gets vanilla's {@code chests/simple_dungeon} loot table; its spawners get vanilla's dungeon mob weights
 * ({@code DungeonHooks}: skeleton 100, zombie 200, spider 100).
 */
public final class LegacyDungeonWorld {

    /** The generator's terrain at a position, and its surface height. */
    public interface Terrain {
        LegacyBlock block(int x, int y, int z);

        /** 1.7 {@code getHeightValue} of the untouched terrain: the y above the highest non-air block. */
        int height(int x, int z);

        int minY();
    }

    /** One written 1.7 block. */
    public record Placed(LegacyBlock block, int meta) {}

    /** 1.7 {@code world.rand}. */
    public final RandomSource rand;
    /** 1.7 {@code Library.rand} ({@code Library.getRandomConcrete}). */
    private final RandomSource libraryRand;
    private final Terrain terrain;

    private final Long2ObjectLinkedOpenHashMap<Placed> blocks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2IntOpenHashMap topWritten = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<Tile> tiles = new Long2ObjectOpenHashMap<>();

    public LegacyDungeonWorld(Terrain terrain, RandomSource worldRand, RandomSource libraryRand) {
        this.terrain = terrain;
        this.rand = worldRand;
        this.libraryRand = libraryRand;
        this.topWritten.defaultReturnValue(Integer.MIN_VALUE);
    }

    // 1.7 World

    public boolean setBlock(int x, int y, int z, LegacyBlock block) {
        return setBlock(x, y, z, block, 0, 3);
    }

    public boolean setBlock(int x, int y, int z, LegacyBlock block, int meta, int flag) {
        long key = BlockPos.asLong(x, y, z);
        blocks.put(key, new Placed(block, meta & 15));
        tiles.remove(key); // a new block starts with a new tile entity
        long column = column(x, z);
        if (y > topWritten.get(column)) topWritten.put(column, y);
        return true;
    }

    public boolean setBlockToAir(int x, int y, int z) {
        return setBlock(x, y, z, Blocks.air, 0, 3);
    }

    /** 1.7 {@code setBlockMetadataWithNotify}: the block (and its tile entity) stays, the metadata changes. */
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flag) {
        long key = BlockPos.asLong(x, y, z);
        Placed placed = blocks.get(key);
        if (placed == null) return false;
        blocks.put(key, new Placed(placed.block(), meta & 15));
        return true;
    }

    public LegacyBlock getBlock(int x, int y, int z) {
        Placed placed = blocks.get(BlockPos.asLong(x, y, z));
        if (placed != null) return placed.block();
        return terrain.block(x, y, z);
    }

    public int getBlockMetadata(int x, int y, int z) {
        Placed placed = blocks.get(BlockPos.asLong(x, y, z));
        return placed != null ? placed.meta() : 0;
    }

    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == Blocks.air;
    }

    /** 1.7 {@code getHeightValue}: the terrain's height, or above the highest block written into the column. */
    public int getHeightValue(int x, int z) {
        int top = Math.max(terrain.height(x, z), topWritten.get(column(x, z)) + 1);
        while (top > terrain.minY() && isAirBlock(x, top - 1, z)) top--;
        return top;
    }

    // 1.7 helpers

    /** 1.7 {@code Library.getRandomConcrete()}, on its own random. */
    public LegacyBlock getRandomConcrete() {
        int i = libraryRand.nextInt(20);
        if (i <= 1) return ModBlocks.brick_concrete_broken;
        if (i <= 4) return ModBlocks.brick_concrete_cracked;
        if (i <= 10) return ModBlocks.brick_concrete_mossy;
        return ModBlocks.brick_concrete;
    }

    /** 1.7 {@code Material.plants} among the blocks these builders see (the builders only write non-plants). */
    public static boolean isPlant(LegacyBlock block) {
        return false;
    }

    /** 1.7 {@code getMaterial().isSolid()}: false for air, liquids, webs, plants, vines, snow layers, torches, wires. */
    public static boolean isSolid(LegacyBlock block) {
        return block != Blocks.air && block != Blocks.water && block != Blocks.lava && block != Blocks.web && block != Blocks.vine
                && block != Blocks.snow_layer && block != Blocks.torch && block != Blocks.redstone_wire && block != Blocks.ladder
                && block != Blocks.lever && block != Blocks.wall_sign;
    }

    /** 1.7 {@code Block.isNormalCube} of what stands at a position. */
    public boolean isNormalCube(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        Placed placed = blocks.get(key);
        if (placed == null) return getBlock(x, y, z) == Blocks.stone;
        BlockState state = resolve(placed, null).state();
        return state.isRedstoneConductor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** 1.7 {@code ItemDoor.placeDoorBlock}: both halves, the hinge from the neighbours. */
    public void placeDoorBlock(int x, int y, int z, int meta, LegacyBlock door) {
        int b0 = 0;
        int b1 = 0;

        if (meta == 0) b1 = 1;
        if (meta == 1) b0 = -1;
        if (meta == 2) b1 = -1;
        if (meta == 3) b0 = 1;

        int i1 = (isNormalCube(x - b0, y, z - b1) ? 1 : 0) + (isNormalCube(x - b0, y + 1, z - b1) ? 1 : 0);
        int j1 = (isNormalCube(x + b0, y, z + b1) ? 1 : 0) + (isNormalCube(x + b0, y + 1, z + b1) ? 1 : 0);
        boolean flag = getBlock(x - b0, y, z - b1) == door || getBlock(x - b0, y + 1, z - b1) == door;
        boolean flag1 = getBlock(x + b0, y, z + b1) == door || getBlock(x + b0, y + 1, z + b1) == door;
        boolean flag2 = false;

        if (flag && !flag1) {
            flag2 = true;
        } else if (j1 > i1) {
            flag2 = true;
        }

        setBlock(x, y, z, door, meta, 2);
        setBlock(x, y + 1, z, door, 8 | (flag2 ? 1 : 0), 2);
    }

    /**
     * 1.7 {@code WeightedRandomChestContent.generateChestContents(rand, pool, (TileEntityChest) world.getTileEntity(x, y,
     * z), count)}: {@code count} weighted draws with {@code rand}, each stack into a random slot (overwriting), a draw
     * larger than a stack split into single items. Nothing happens where no container stands (1.7 threw).
     */
    public void generateChestContents(RandomSource rand, String pool, int x, int y, int z, int count) {
        LegacyBlock block = getBlock(x, y, z);
        int size = block == Blocks.chest ? 27 : block == ModBlocks.crate_steel ? 54 : 0;
        if (size == 0) return;
        Tile tile = tile(x, y, z);
        if (tile.slots == null) tile.slots = new ItemStack[size];
        List<ItemPool.Entry> content = ItemPool.getPool(pool);
        for (int j = 0; j < count; ++j) {
            ItemStack drawn = ItemPool.getStack(content, rand);
            if (drawn.isEmpty()) continue;
            if (drawn.getCount() <= drawn.getMaxStackSize()) {
                tile.slots[rand.nextInt(size)] = drawn;
            } else {
                for (int i = 0; i < drawn.getCount(); i++) tile.slots[rand.nextInt(size)] = ItemStacks.copyWithCount(drawn, 1);
            }
        }
    }

    /** {@code ChestGenHooks.getItems(DUNGEON_CHEST, rand)}: vanilla's dungeon chest loot table, seeded from {@code rand}. */
    public void setLootTable(int x, int y, int z, String table, RandomSource rand) {
        Tile tile = tile(x, y, z);
        tile.lootTable = table;
        tile.lootSeed = rand.nextLong();
    }

    /** {@code TileEntityMobSpawner.func_145881_a().setEntityName(name)}. */
    public void setSpawnerEntity(int x, int y, int z, String entity) {
        tile(x, y, z).spawner = entity;
    }

    /** Anything else a builder did to the tile entity it just placed, applied when the block is placed. */
    public void onTile(int x, int y, int z, Consumer<BlockEntity> action) {
        tile(x, y, z).actions.add(action);
    }

    /** 1.7 {@code DungeonHooks.getRandomDungeonMob}: vanilla's dungeon mobs with Forge's weights. */
    public static String randomDungeonMob(RandomSource rand) {
        int roll = rand.nextInt(400);
        if (roll < 100) return "minecraft:skeleton";
        if (roll < 300) return "minecraft:zombie";
        return "minecraft:spider";
    }

    private Tile tile(int x, int y, int z) {
        return tiles.computeIfAbsent(BlockPos.asLong(x, y, z), k -> new Tile());
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** What a builder did to one tile entity. */
    static final class Tile {
        ItemStack @Nullable [] slots;
        @Nullable String lootTable;
        long lootSeed;
        @Nullable String spawner;
        final List<Consumer<BlockEntity>> actions = new ArrayList<>();
    }

    // Result

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /** Freezes the run into the per-chunk result the piece copies from. */
    public Result result() {
        return new Result(blocks, tiles);
    }

    /** A 1.7 block made flattened: the state and whether it is a multiblock core that fills its cells. */
    record Resolved(BlockState state, boolean core) {}

    /** The flattened state of a written 1.7 block; {@code other} is the other half of a door, if any. */
    static Resolved resolve(Placed placed, @Nullable Placed other) {
        String name = placed.block().name;
        int meta = placed.meta();
        switch (name) {
            // 1.7 BlockSellafield: metadata = heat level 0..5, one flattened block per level
            case "hbm:tile.sellafield" -> { return plain("sellafield_" + Math.min(meta, 5)); }
            // 1.7 TrappedBrick: metadata = Trap ordinal, one flattened block per trap
            case "hbm:tile.brick_jungle_trap" -> {
                TrappedBrick.Trap trap = TrappedBrick.Trap.get(meta);
                return plain("brick_jungle_trap_" + (trap == null ? TrappedBrick.Trap.FALLING_ROCKS : trap).name().toLowerCase(Locale.ROOT));
            }
            // 1.7 BlockGlyph: metadata = glyph 0..15
            case "hbm:tile.brick_jungle_glyph" -> { return plain("brick_jungle_glyph_" + meta); }
            // LegacyBlock's own subtype table (dud type, capacitor facing, ...)
            case "hbm:tile.crashed_bomb", "hbm:tile.capacitor_copper", "minecraft:flower_pot" -> {
                return new Resolved(placed.block().state(meta), false);
            }
            default -> { }
        }
        LegacyStructurePalette.Resolved r = LegacyStructurePalette.resolve(name, meta);
        return switch (r.kind()) {
            case DOOR -> new Resolved(LegacyStructurePalette.door(r.state(), meta, other != null && other.block() == placed.block() ? other.meta() : -1), false);
            case CORE -> new Resolved(r.state(), true);
            case CELL -> {
                // a 1.7 single block the port made a multiblock (the UF6/PuF6 tanks): its metadata was a ForgeDirection
                LegacyStructurePalette.Resolved core = LegacyStructurePalette.resolve(name, 12);
                if (core.kind() != LegacyStructurePalette.Kind.CORE) yield new Resolved(r.state(), false);
                Direction facing = meta >= 2 && meta <= 5 ? Direction.from3DDataValue(meta) : Direction.NORTH;
                yield new Resolved(core.state().setValue(BlockMultiblockCore.FACING, facing), true);
            }
            default -> new Resolved(r.state(), false);
        };
    }

    /** A bare path is HBM's block; {@code "ns:path"} names an addon's (or vanilla's) block. */
    private static Resolved plain(String id) {
        return new Resolved(BuiltInRegistries.BLOCK.getOptional(Library.parseId(id)).map(Block::defaultBlockState)
                .orElse(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()), false);
    }

    /** A finished run: the written blocks grouped by chunk, and the tile-entity work. */
    public static final class Result {
        private final Long2ObjectOpenHashMap<List<long[]>> byChunk = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<Placed> blocks;
        private final Long2ObjectOpenHashMap<Tile> tiles;
        private volatile long @Nullable [] cores;
        private final BoundingBox bounds;

        Result(Long2ObjectLinkedOpenHashMap<Placed> written, Long2ObjectOpenHashMap<Tile> tiles) {
            this.blocks = new Long2ObjectOpenHashMap<>(written);
            this.tiles = tiles;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            // write order is kept per chunk (later writes already replaced earlier ones in the map)
            for (var it = written.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                var e = it.next();
                long key = e.getLongKey();
                int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
                minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
                byChunk.computeIfAbsent(chunk(x >> 4, z >> 4), k -> new ArrayList<>()).add(new long[] {key});
            }
            this.bounds = written.isEmpty() ? new BoundingBox(0, 0, 0, 0, 0, 0) : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public BoundingBox bounds() {
            return bounds;
        }

        public int size() {
            return blocks.size();
        }

        /** The written 1.7 block at a position, or null. */
        public @Nullable Placed at(int x, int y, int z) {
            return blocks.get(BlockPos.asLong(x, y, z));
        }

        /** Every written position (for tests and the harness). */
        public Iterable<Long> positions() {
            return blocks.keySet();
        }

        /** The flattened state the run puts at a written position (for tests and the harness). */
        public BlockState stateAt(long key) {
            Placed placed = blocks.get(key);
            return resolve(placed, doorOther(key, placed)).state();
        }

        private @Nullable Placed doorOther(long key, Placed placed) {
            int y = BlockPos.getY(key);
            int oy = (placed.meta() & 8) != 0 ? y - 1 : y + 1;
            return blocks.get(BlockPos.asLong(BlockPos.getX(key), oy, BlockPos.getZ(key)));
        }

        private static long chunk(int cx, int cz) {
            return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        }

        /** The written multiblock cores (resolved once, on first use: resolution needs the block registry). */
        private long[] cores() {
            long[] list = cores;
            if (list == null) {
                List<Long> found = new ArrayList<>();
                for (var it = blocks.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var e = it.next();
                    if (e.getValue().block().name.startsWith("hbm:") && resolve(e.getValue(), null).core()) found.add(e.getLongKey());
                }
                list = found.stream().mapToLong(Long::longValue).toArray();
                cores = list;
            }
            return list;
        }

        /** Copies the part of the run inside {@code box} (one chunk) into the world. */
        public void apply(WorldGenLevel level, BoundingBox box) {
            List<BlockPos> placed = new ArrayList<>();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    List<long[]> list = byChunk.get(chunk(cx, cz));
                    if (list == null) continue;
                    for (long[] entry : list) {
                        long key = entry[0];
                        BlockPos pos = BlockPos.of(key);
                        if (!box.isInside(pos) || level.isOutsideBuildHeight(pos)) continue;
                        Placed p = blocks.get(key);
                        Resolved r = resolve(p, doorOther(key, p));
                        level.setBlock(pos, Waterlogging.inWorld(r.state(), level, pos),
                                Block.UPDATE_CLIENTS);
                        placed.add(pos);
                        Tile tile = tiles.get(key);
                        if (tile != null) applyTile(level, pos, tile);
                    }
                }
            }
            // multiblock cores (in this chunk or a neighbour) fill the cells of theirs that stand in this chunk
            for (long key : cores()) {
                Resolved r = resolve(blocks.get(key), null);
                if (!(r.state().getBlock() instanceof BlockMultiblockCore core)) continue;
                BlockPos at = BlockPos.of(key);
                if (level.isOutsideBuildHeight(at)) continue;
                Direction facing = r.state().getValue(BlockMultiblockCore.FACING);
                BlockMultiblockCore.withoutTeardown(() -> core.fillSpace(level, at, facing, box));
            }
            // 1.7 computed fence/pane/wire/stair connections at render time; a flattened world stores them
            for (BlockPos pos : placed) {
                BlockState state = level.getBlockState(pos);
                BlockState shaped = Block.updateFromNeighbourShapes(state, level, pos);
                if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        private static void applyTile(WorldGenLevel level, BlockPos pos, Tile tile) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return;
            if (tile.slots != null && be instanceof Container container) {
                for (int i = 0; i < tile.slots.length && i < container.getContainerSize(); i++) {
                    if (tile.slots[i] != null) container.setItem(i, tile.slots[i].copy());
                }
            }
            if (tile.lootTable != null) {
                // the vanilla keys every supported version reads a lootable container's table from
                CompoundTag tag = new CompoundTag();
                tag.putString("LootTable", tile.lootTable);
                tag.putLong("LootTableSeed", tile.lootSeed);
                BlockEntities.loadWithComponents(be, tag, level.registryAccess());
            }
            if (tile.spawner != null && be instanceof SpawnerBlockEntity spawner) {
                EntityType.byString(tile.spawner).ifPresent(type -> spawner.setEntityId(type, level.getRandom()));
            }
            for (Consumer<BlockEntity> action : tile.actions) action.accept(be);
            be.setChanged();
        }
    }

    // Terrain

    /**
     * The generator's terrain before surface rules: solid (as stone), water, lava, air. Columns are cached for the run.
     * The same answers come from a structure start ({@code GenerationContext}) and from a chunk being decorated, so a
     * run is identical wherever it is repeated.
     */
    public static Terrain terrain(ChunkGenerator generator, RandomState random, LevelHeightAccessor height) {
        return new Terrain() {
            private final Long2ObjectOpenHashMap<NoiseColumn> columns = new Long2ObjectOpenHashMap<>();
            private final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();

            {
                heights.defaultReturnValue(Integer.MIN_VALUE);
            }

            @Override
            public LegacyBlock block(int x, int y, int z) {
                if (y < height.getMinY() || y >= height.getMaxY()) return Blocks.air;
                NoiseColumn column = columns.computeIfAbsent(column(x, z), k -> generator.getBaseColumn(x, z, height, random));
                BlockState state = column.getBlock(y);
                if (state.isAir()) return Blocks.air;
                if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) return Blocks.water;
                if (state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA)) return Blocks.lava;
                return Blocks.stone;
            }

            @Override
            public int height(int x, int z) {
                long key = column(x, z);
                int h = heights.get(key);
                if (h == Integer.MIN_VALUE) {
                    h = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
                    heights.put(key, h);
                }
                return h;
            }

            @Override
            public int minY() {
                return height.getMinY();
            }
        };
    }
}
