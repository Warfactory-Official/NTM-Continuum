package com.hbm.world.feature;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.SoyuzCapsule;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.config.DungeonConfig;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.util.LootGenerator;
import com.mojang.serialization.Codec;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.GenerationOptions;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The scattered world blocks of 1.7 {@code HbmWorldGen.generateSurface} (overworld, behind {@code enableDungeons}), one
 * feature type per block, each 1:1 with its 1.7 branch. The chunk gate ({@code rand.nextInt(n) == 0}), the random
 * column and the {@code getHeightValue} height are placement ({@code rarity_filter}, {@code in_square},
 * {@code heightmap MOTION_BLOCKING}; see {@code LegacyDungeonDeclarations}); the origin is that surface position, and
 * everything after it is here. {@code /place feature hbm:<id>} places one.
 *
 * <p>1.7's {@code canPlaceTorchOnTop} is vanilla's torch rule, {@link Block#canSupportCenter}; its tile-entity setup
 * (safe lock and pins, mine arming, broken lantern, capsule record) is applied right after the block is placed, as 1.7
 * did in the same populate call.
 */
public class DungeonBlockFeature extends Feature<NoneFeatureConfiguration> {

    public enum Kind {
        /** 1.7 {@code vaultfreq} (2500, {@code enableVaults}): a locked, filled safe; 30% hold cave spiders. */
        LOCKED_SAFE("locked_safe"),
        /** 1.7 {@code minefreq} (64, {@code enableMines}): an armed AP mine on the first torch-safe block of y+2..y. */
        LANDMINE_AP("landmine_ap"),
        /** 1.7 {@code enable528BosniaSimulator} (1 in 16 chunks, 528 mode only): an armed HE mine. */
        LANDMINE_HE("landmine_he"),
        /** 1.7 hardcoded 1 in 2000: a broken behemoth lantern, half of them with a lore booklet two blocks north. */
        BROKEN_LANTERN("broken_lantern"),
        /** 1.7 {@code capsuleStructure} (100, beach only): a rusted Soyuz capsule four blocks down, a glass record inside. */
        LANDED_CAPSULE("landed_capsule"),
        /** 1.7 {@code broadcaster} (5000): the corrupted broadcaster. */
        CORRUPTED_BROADCASTER("corrupted_broadcaster"),
        /** 1.7 {@code genBlueprintChest}: one chest of blueprints in obsidian per 5000 x 5000 grid cell, 50%. */
        BLUEPRINT_CHEST("blueprint_chest");

        public final String id;

        Kind(String id) {
            this.id = id;
        }
    }

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(FEATURES);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LOCKED_SAFE = register(Kind.LOCKED_SAFE);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LANDMINE_AP = register(Kind.LANDMINE_AP);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LANDMINE_HE = register(Kind.LANDMINE_HE);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BROKEN_LANTERN = register(Kind.BROKEN_LANTERN);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LANDED_CAPSULE = register(Kind.LANDED_CAPSULE);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CORRUPTED_BROADCASTER = register(Kind.CORRUPTED_BROADCASTER);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLUEPRINT_CHEST = register(Kind.BLUEPRINT_CHEST);

    private static RegistryObject<Feature<NoneFeatureConfiguration>> register(Kind kind) {
        return REG.register(Registries.FEATURE, kind.id, () -> new DungeonBlockFeature(NoneFeatureConfiguration.CODEC, kind));
    }

    public static void init() {}

    private final Kind kind;

    public DungeonBlockFeature(Codec<NoneFeatureConfiguration> codec, Kind kind) {
        super(codec);
        this.kind = kind;
    }

    /** 1.7 {@code enableDungeons}: 1 always, 0 never, 2 the world's Generate Structures option. */
    static boolean dungeonsEnabled(WorldGenLevel level) {
        if (DungeonConfig.enableDungeons == 1) return true;
        if (DungeonConfig.enableDungeons == 0) return false;
        MinecraftServer server = level.getLevel().getServer();
        return server == null || GenerationOptions.structures(server);
    }

    /** 1.7 {@code getBlock(x, y, z).canPlaceTorchOnTop(world, x, y, z)}. */
    static boolean torchTop(WorldGenLevel level, BlockPos pos) {
        return Block.canSupportCenter(level, pos, Direction.UP);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!dungeonsEnabled(level)) return false;
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();

        return switch (kind) {
            case LOCKED_SAFE -> safe(level, rand, x, y, z);
            case LANDMINE_AP -> {
                for (int g = y + 2; g >= y; g--) {
                    if (torchTop(level, new BlockPos(x, g - 1, z))) {
                        yield mine(level, new BlockPos(x, g, z), ModExplosivesBlocks.mine_ap.get().defaultBlockState());
                    }
                }
                yield false;
            }
            case LANDMINE_HE -> torchTop(level, new BlockPos(x, y - 1, z))
                    && mine(level, new BlockPos(x, y, z), ModExplosivesBlocks.mine_he.get().defaultBlockState());
            case BROKEN_LANTERN -> lantern(level, rand, x, y, z);
            case LANDED_CAPSULE -> capsule(level, rand, x, y - 4, z);
            case CORRUPTED_BROADCASTER -> {
                if (!torchTop(level, new BlockPos(x, y - 1, z))) yield false;
                // world.setBlock(x, y, z, ModBlocks.broadcaster_pc, rand.nextInt(4) + 2, 2): metadata = ForgeDirection
                level.setBlock(new BlockPos(x, y, z), ModDecoCrateBlocks.broadcaster_pc.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from3DDataValue(rand.nextInt(4) + 2)), 2);
                yield true;
            }
            case BLUEPRINT_CHEST -> blueprintChest(level, rand, x, z, DungeonConfig.blueprintGrid, DungeonConfig.blueprintGrid);
        };
    }

    // 1.7 branches

    private static boolean safe(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        if (!torchTop(level, new BlockPos(x, y - 1, z))) return false;
        BlockPos pos = new BlockPos(x, y, z);
        level.setBlock(pos, ModDecoCrateBlocks.safe.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from3DDataValue(rand.nextInt(4) + 2)), 2);

        if (level.getBlockEntity(pos) instanceof TileEntityCrateBase safe) {

            switch (rand.nextInt(10)) {
                case 0, 1, 2, 3 -> {
                    safe.setMod(1);
                    generateChestContents(rand, ItemPool.getPool(ItemPools.POOL_VAULT_RUSTY), safe, rand.nextInt(4) + 3);
                }
                case 4, 5, 6 -> {
                    safe.setMod(0.1);
                    generateChestContents(rand, ItemPool.getPool(ItemPools.POOL_VAULT_STANDARD), safe, rand.nextInt(3) + 2);
                }
                case 7, 8 -> {
                    safe.setMod(0.02);
                    generateChestContents(rand, ItemPool.getPool(ItemPools.POOL_VAULT_REINFORCED), safe, rand.nextInt(3) + 1);
                }
                default -> {
                    safe.setMod(0.0);
                    generateChestContents(rand, ItemPool.getPool(ItemPools.POOL_VAULT_UNBREAKABLE), safe, rand.nextInt(2) + 1);
                }
            }

            safe.setPins(rand.nextInt(999) + 1);
            safe.lock();

            // 30% chance; those safes have been sitting there for ages, they gotta have some spiders in them
            if (rand.nextInt(10) < 3) safe.hasSpiders = true;
            safe.setChanged();
        }
        return true;
    }

    private static boolean mine(WorldGenLevel level, BlockPos pos, BlockState mine) {
        level.setBlock(pos, mine, 3);
        if (level.getBlockEntity(pos) instanceof TileEntityLandmine landmine) {
            landmine.waitingForPlayer = true;
            landmine.setChanged();
        }
        return true;
    }

    private static boolean lantern(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!torchTop(level, pos.below()) || !level.getBlockState(pos).canBeReplaced()) return false;

        // world.setBlock(x, y, z, ModBlocks.lantern_behemoth, 12, 3) + fillSpace(..., {4, 0, 0, 0, 0, 0}, NORTH):
        // metadata 12 is BlockDummyable's core facing ForgeDirection 2 (north)
        var block = ModDungeonBlocks.lantern_behemoth.get();
        level.setBlock(pos, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        BlockMultiblockCore.withoutTeardown(() -> block.fillSpace(level, pos, Direction.NORTH));

        if (level.getBlockEntity(pos) instanceof TileEntityLanternBehemoth lantern) {
            lantern.isBroken = true;
            lantern.setChanged();
        }

        if (rand.nextInt(2) == 0) {
            BlockPos loot = new BlockPos(x, y, z - 2);
            level.setBlock(loot, ModDungeonBlocks.deco_loot.get().defaultBlockState(), 3);
            LootGenerator.lootBooklet(level, loot);
        }
        return true;
    }

    private static boolean capsule(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        if (!torchTop(level, new BlockPos(x, y + 1, z))) return false;
        BlockPos pos = new BlockPos(x, y, z);
        // world.setBlock(x, y, z, ModBlocks.soyuz_capsule, 3, 2): metadata 3 is the rusted skin
        level.setBlock(pos, ModRadarUtilityBlocks.soyuz_capsule.get().defaultBlockState().setValue(SoyuzCapsule.RUSTED, true), 2);
        if (level.getBlockEntity(pos) instanceof Container cap) {
            cap.setItem(rand.nextInt(cap.getContainerSize()), new ItemStack(ModFinalConsumableItems.record_glass.get()));
            if (cap instanceof BlockEntity be) be.setChanged();
        }
        return true;
    }

    /** 1.7 {@code genBlueprintChest(world, rand, i, j, boundsX, boundsZ)}; {@code i, j} is the chunk's corner. */
    private static boolean blueprintChest(WorldGenLevel level, RandomSource rand, int i, int j, int boundsX, int boundsZ) {
        if (Math.abs(i) < 100 && Math.abs(j) < 100) return false;
        if (rand.nextInt(20) < 10) return false; // nextBoolean would have weird periodicity to it, hoping that a larger int range has more variance

        int cX = Math.abs(i) % boundsX;
        int cZ = Math.abs(j) % boundsZ;

        if (cX >= 0 && cX < 16 && cZ >= 0 && cZ < 16) {
            int x = i + 8;
            int z = j + 8;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - rand.nextInt(2);

            BlockPos chest = new BlockPos(x, y, z);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);

            for (int a = x - 1; a <= x + 1; a++) for (int b = y - 1; b <= y + 1; b++) for (int c = z - 1; c <= z + 1; c++) {
                if (a != x || b != y || c != z) level.setBlock(new BlockPos(a, b, c), Blocks.OBSIDIAN.defaultBlockState(), 2);
            }

            if (level.getBlockEntity(chest) instanceof Container inventory) {
                generateChestContents(rand, ItemPool.getPool(ItemPools.POOL_BLUEPRINTS), inventory, 50);
            }
            return true;
        }
        return false;
    }

    /**
     * 1.7 {@code WeightedRandomChestContent.generateChestContents}: {@code count} weighted draws, each stack into a random
     * slot (overwriting), a draw larger than a stack split into single items.
     */
    static void generateChestContents(RandomSource rand, List<ItemPool.Entry> content, @Nullable Container inventory, int count) {
        if (inventory == null) return;
        int size = inventory.getContainerSize();
        if (size <= 0) return;
        for (int j = 0; j < count; ++j) {
            ItemStack drawn = ItemPool.getStack(content, rand);
            if (drawn.isEmpty()) continue;
            if (drawn.getCount() <= drawn.getMaxStackSize()) {
                inventory.setItem(rand.nextInt(size), drawn);
            } else {
                for (int i = 0; i < drawn.getCount(); i++) inventory.setItem(rand.nextInt(size), ItemStacks.copyWithCount(drawn, 1));
            }
        }
        if (inventory instanceof BlockEntity be) be.setChanged();
    }
}
