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
import com.hbm.itempool.PoolLoot;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.util.LootGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.GenerationOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;
import java.util.function.Supplier;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * The scattered world blocks of 1.7 {@code HbmWorldGen.generateSurface} (overworld, behind {@code enableDungeons}), one
 * feature type per block, each 1:1 with its 1.7 branch. The chunk gate ({@code rand.nextInt(n) == 0}), the random
 * column and the {@code getHeightValue} height are placement ({@code rarity_filter}, {@code in_square},
 * {@code heightmap MOTION_BLOCKING}; see {@code LegacyDungeonDeclarations}); the origin is that surface position, and
 * everything after it is here. 1.7's {@code canPlaceTorchOnTop} is vanilla's torch rule, {@link Block#canSupportCenter}.
 */
public abstract class DungeonBlockFeature<C extends FeatureConfiguration> extends Feature<C> {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(FEATURES);

    public static final RegistryObject<Feature<LockedSafe.Config>> LOCKED_SAFE =
            REG.register(Registries.FEATURE, "locked_safe", LockedSafe::new);
    public static final RegistryObject<Feature<Landmine.Config>> LANDMINE_AP =
            REG.register(Registries.FEATURE, "landmine_ap", () -> new Landmine(Landmine.AP_CODEC));
    public static final RegistryObject<Feature<Landmine.Config>> LANDMINE_HE =
            REG.register(Registries.FEATURE, "landmine_he", () -> new Landmine(Landmine.HE_CODEC));
    public static final RegistryObject<Feature<BrokenLantern.Config>> BROKEN_LANTERN =
            REG.register(Registries.FEATURE, "broken_lantern", BrokenLantern::new);
    public static final RegistryObject<Feature<LandedCapsule.Config>> LANDED_CAPSULE =
            REG.register(Registries.FEATURE, "landed_capsule", LandedCapsule::new);
    public static final RegistryObject<Feature<Broadcaster.Config>> CORRUPTED_BROADCASTER =
            REG.register(Registries.FEATURE, "corrupted_broadcaster", Broadcaster::new);
    public static final RegistryObject<Feature<BlueprintChest.Config>> BLUEPRINT_CHEST =
            REG.register(Registries.FEATURE, "blueprint_chest", BlueprintChest::new);

    public static void init() {}

    protected DungeonBlockFeature(Codec<C> codec) {
        super(codec);
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
    public boolean place(FeaturePlaceContext<C> context) {
        WorldGenLevel level = context.level();
        if (!dungeonsEnabled(level)) return false;
        return generate(context.config(), level, context.random(), context.origin());
    }

    protected abstract boolean generate(C c, WorldGenLevel level, RandomSource rand, BlockPos origin);

    static int between(RandomSource rand, int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    /**
     * {@code hbm:locked_safe}, 1.7 {@code vaultfreq} (2500, {@code enableVaults}): a locked safe filled from one weighted
     * tier, with pins 1..999; {@code spider_chance} in {@code spider_bound} hold cave spiders.
     */
    public static class LockedSafe extends DungeonBlockFeature<LockedSafe.Config> {

        /** {@code mod} is the lock's pick difficulty ({@code TileEntityLockableBase.setMod}). */
        public record Tier(int weight, double mod, String pool, int min, int max) {
            public static final Codec<Tier> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").forGetter(Tier::weight),
                    Codec.DOUBLE.fieldOf("mod").forGetter(Tier::mod),
                    Codec.STRING.fieldOf("pool").forGetter(Tier::pool),
                    Codec.INT.fieldOf("min").forGetter(Tier::min),
                    Codec.INT.fieldOf("max").forGetter(Tier::max)
            ).apply(i, Tier::new));
        }

        public record Config(List<Tier> tiers, int spiderChance, int spiderBound) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    field(Tier.CODEC.listOf(), "tiers", List.of(
                            new Tier(4, 1.0D, ItemPools.POOL_VAULT_RUSTY, 3, 6),
                            new Tier(3, 0.1D, ItemPools.POOL_VAULT_STANDARD, 2, 4),
                            new Tier(2, 0.02D, ItemPools.POOL_VAULT_REINFORCED, 1, 3),
                            new Tier(1, 0.0D, ItemPools.POOL_VAULT_UNBREAKABLE, 1, 2))).forGetter(Config::tiers),
                    field(Codec.INT, "spider_chance", 3).forGetter(Config::spiderChance),
                    FeatureCodecs.positive("spider_bound", 10).forGetter(Config::spiderBound)
            ).apply(i, Config::new));
        }

        public LockedSafe() {
            super(Config.CODEC);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos pos) {
            if (!torchTop(level, pos.below())) return false;
            level.setBlock(pos, ModDecoCrateBlocks.safe.get().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from3DDataValue(rand.nextInt(4) + 2)), 2);

            if (level.getBlockEntity(pos) instanceof TileEntityCrateBase safe) {
                int total = 0;
                for (Tier tier : c.tiers()) total += tier.weight();
                int roll = rand.nextInt(total);
                Tier pick = c.tiers().get(c.tiers().size() - 1);
                for (Tier tier : c.tiers()) {
                    if (roll < tier.weight()) {
                        pick = tier;
                        break;
                    }
                    roll -= tier.weight();
                }
                safe.setMod(pick.mod());
                PoolLoot.fill(level, pos, safe, pick.pool(), rand, between(rand, pick.min(), pick.max()));

                safe.setPins(rand.nextInt(999) + 1);
                safe.lock();

                // those safes have been sitting there for ages, they gotta have some spiders in them
                if (rand.nextInt(c.spiderBound()) < c.spiderChance()) safe.hasSpiders = true;
                safe.setChanged();
            }
            return true;
        }
    }

    /**
     * {@code hbm:landmine_ap}, 1.7 {@code minefreq} (64, {@code enableMines}): an armed mine on the first torch-safe block
     * from {@code scan_up} above the surface down to it; {@code hbm:landmine_he}, 1.7 {@code enable528BosniaSimulator}:
     * the same with an HE mine and no scan.
     */
    public static class Landmine extends DungeonBlockFeature<Landmine.Config> {

        public record Config(BlockState mine, int scanUp) implements FeatureConfiguration {
            static Codec<Config> codec(Supplier<Block> mine, int scanUp) {
                return RecordCodecBuilder.create(i -> i.group(
                        FeatureCodecs.state("mine", mine).forGetter(Config::mine),
                        field(Codec.INT, "scan_up", scanUp).forGetter(Config::scanUp)
                ).apply(i, Config::new));
            }
        }

        public static final Codec<Config> AP_CODEC = Config.codec(() -> ModExplosivesBlocks.mine_ap.get(), 2);
        public static final Codec<Config> HE_CODEC = Config.codec(() -> ModExplosivesBlocks.mine_he.get(), 0);

        public Landmine(Codec<Config> codec) {
            super(codec);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos origin) {
            for (int g = origin.getY() + c.scanUp(); g >= origin.getY(); g--) {
                if (torchTop(level, new BlockPos(origin.getX(), g - 1, origin.getZ()))) {
                    BlockPos pos = new BlockPos(origin.getX(), g, origin.getZ());
                    level.setBlock(pos, c.mine(), 3);
                    if (level.getBlockEntity(pos) instanceof TileEntityLandmine landmine) {
                        landmine.waitingForPlayer = true;
                        landmine.setChanged();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * {@code hbm:broken_lantern}, 1.7 hardcoded 1 in 2000: a broken behemoth lantern, one in {@code loot_chance} with a
     * lore booklet two blocks north.
     */
    public static class BrokenLantern extends DungeonBlockFeature<BrokenLantern.Config> {

        public record Config(int lootChance) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    FeatureCodecs.positive("loot_chance", 2).forGetter(Config::lootChance)
            ).apply(i, Config::new));
        }

        public BrokenLantern() {
            super(Config.CODEC);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos pos) {
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

            if (rand.nextInt(c.lootChance()) == 0) {
                BlockPos loot = pos.north(2);
                level.setBlock(loot, ModDungeonBlocks.deco_loot.get().defaultBlockState(), 3);
                LootGenerator.lootBooklet(level, loot);
            }
            return true;
        }
    }

    /**
     * {@code hbm:landed_capsule}, 1.7 {@code capsuleStructure} (100, beach only): a Soyuz capsule {@code depth} blocks
     * down, rusted by default, with {@code item} in a random slot.
     */
    public static class LandedCapsule extends DungeonBlockFeature<LandedCapsule.Config> {

        public record Config(int depth, Item item, boolean rusted) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    field(Codec.INT, "depth", 4).forGetter(Config::depth),
                    lazy(BuiltInRegistries.ITEM.byNameCodec(), "item", () -> ModFinalConsumableItems.record_glass.get()).forGetter(Config::item),
                    field(Codec.BOOL, "rusted", true).forGetter(Config::rusted)
            ).apply(i, Config::new));
        }

        public LandedCapsule() {
            super(Config.CODEC);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos origin) {
            BlockPos pos = origin.below(c.depth());
            if (!torchTop(level, pos.above())) return false;
            // world.setBlock(x, y, z, ModBlocks.soyuz_capsule, 3, 2): metadata 3 is the rusted skin
            level.setBlock(pos, ModRadarUtilityBlocks.soyuz_capsule.get().defaultBlockState().setValue(SoyuzCapsule.RUSTED, c.rusted()), 2);
            if (level.getBlockEntity(pos) instanceof Container cap) {
                cap.setItem(rand.nextInt(cap.getContainerSize()), new ItemStack(c.item()));
                if (cap instanceof BlockEntity be) be.setChanged();
            }
            return true;
        }
    }

    /** {@code hbm:corrupted_broadcaster}, 1.7 {@code broadcaster} (5000): {@code state} facing a random side. */
    public static class Broadcaster extends DungeonBlockFeature<Broadcaster.Config> {

        public record Config(BlockState state) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    FeatureCodecs.state("state", ModDecoCrateBlocks.broadcaster_pc).forGetter(Config::state)
            ).apply(i, Config::new));
        }

        public Broadcaster() {
            super(Config.CODEC);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos pos) {
            if (!torchTop(level, pos.below())) return false;
            // world.setBlock(x, y, z, ModBlocks.broadcaster_pc, rand.nextInt(4) + 2, 2): metadata = ForgeDirection
            Direction facing = Direction.from3DDataValue(rand.nextInt(4) + 2);
            BlockState state = c.state();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            level.setBlock(pos, state, 2);
            return true;
        }
    }

    /**
     * {@code hbm:blueprint_chest}, 1.7 {@code genBlueprintChest}: one chest of blueprints in an obsidian shell per
     * {@code grid} x {@code grid} cell, skipped when {@code rand.nextInt(roll) < skip}, never within {@code min_distance}
     * of the origin. The origin is the chunk corner.
     */
    public static class BlueprintChest extends DungeonBlockFeature<BlueprintChest.Config> {

        public record Config(int grid, int minDistance, int roll, int skip, String pool, int rolls, BlockState container,
                             BlockState shell) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    field(Codec.intRange(1, Integer.MAX_VALUE), "grid", DungeonConfig.blueprintGrid).forGetter(Config::grid),
                    field(Codec.INT, "min_distance", 100).forGetter(Config::minDistance),
                    FeatureCodecs.positive("roll", 20).forGetter(Config::roll),
                    field(Codec.INT, "skip", 10).forGetter(Config::skip),
                    field(Codec.STRING, "pool", ItemPools.POOL_BLUEPRINTS).forGetter(Config::pool),
                    field(Codec.INT, "rolls", 50).forGetter(Config::rolls),
                    FeatureCodecs.state("container", () -> Blocks.CHEST).forGetter(Config::container),
                    FeatureCodecs.state("shell", () -> Blocks.OBSIDIAN).forGetter(Config::shell)
            ).apply(i, Config::new));
        }

        public BlueprintChest() {
            super(Config.CODEC);
        }

        @Override
        protected boolean generate(Config c, WorldGenLevel level, RandomSource rand, BlockPos origin) {
            int i = origin.getX(), j = origin.getZ();
            if (Math.abs(i) < c.minDistance() && Math.abs(j) < c.minDistance()) return false;
            // nextBoolean would have weird periodicity to it, hoping that a larger int range has more variance
            if (rand.nextInt(c.roll()) < c.skip()) return false;

            int cX = Math.abs(i) % c.grid();
            int cZ = Math.abs(j) % c.grid();
            if (cX >= 16 || cZ >= 16) return false;

            int x = i + 8;
            int z = j + 8;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - rand.nextInt(2);

            BlockPos chest = new BlockPos(x, y, z);
            level.setBlock(chest, c.container(), 2);
            for (int a = x - 1; a <= x + 1; a++) for (int b = y - 1; b <= y + 1; b++) for (int d = z - 1; d <= z + 1; d++) {
                if (a != x || b != y || d != z) level.setBlock(new BlockPos(a, b, d), c.shell(), 2);
            }

            if (level.getBlockEntity(chest) instanceof Container inventory) {
                PoolLoot.fill(level, chest, inventory, c.pool(), rand, c.rolls());
            }
            return true;
        }
    }
}
