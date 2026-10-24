package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.tileentity.BedrockOreBlockEntity;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** The dynamic 3x3 bedrock-ore patch and its surrounding depth-rock cap. */
public final class BedrockOreFeature {

    private BedrockOreFeature() {}

    public static int tierForDensity(double density) {
        if (density > 1.5D) return 4;
        if (density > 1D) return 3;
        if (density > 0.75D) return 2;
        return 1;
    }

    public static @Nullable FluidStack boreFluidForDensity(double density) {
        return boreFluidForTier(tierForDensity(density));
    }

    public static @Nullable FluidStack boreFluidForTier(int tier) {
        return switch (tier) {
            case 1 -> null;
            case 2 -> new FluidStack(Fluids.WATER, 1_000);
            case 3 -> new FluidStack(Fluids.SULFURIC_ACID, 1_000);
            default -> new FluidStack(Fluids.SOLVENT, 2_000);
        };
    }

    /**
     * 1.7 {@code BedrockOre.generateAuto}, rebased from y=0 to the modern world's floor. The old one-in-ten roll
     * is the placed feature's {@code rarity_filter} (see {@code BedrockDepositFeature}).
     */
    public static void generateAutoChunk(WorldGenLevel level, RandomSource random, int chunkMinX, int chunkMinZ,
                                         Block depthRock, RuleTest host, int color) {
        int x = chunkMinX + random.nextInt(2) + 8;
        int z = chunkMinZ + random.nextInt(2) + 8;
        // hbm-space generateAuto: the mean over the column's types (ItemBedrockOreBase.SOURCE), the body's acid if it
        // names one; plain HBM: averageOreLevel over its six
        ItemBedrockOreBase.OreField field = ItemBedrockOreBase.field(level, x, z);
        double density = field.average();
        int tier = tierForDensity(density);
        FluidStack acid = field.boreFluid() != null ? field.boreFluid() : boreFluidForTier(tier);
        placeDeposit(level, random, new BlockPos(x, level.getMinY(), z),
                new Deposit(() -> new ItemStack(ModOreBlocks.bedrock_ore_base.get()), acid, color, tier, depthRock), host);
    }

    /**
     * 1.7's Nether-only weighted deposit, the ore kind fixed deterministically per chunk. The one-in-ten roll is the
     * placed feature's {@code rarity_filter}.
     */
    public static void generateNetherChunk(WorldGenLevel level, RandomSource random, int chunkMinX, int chunkMinZ,
                                           Block depthRock, RuleTest host, List<BedrockDepositFeature.Nether.Entry> entries) {
        if (entries.isEmpty()) return;
        int x = chunkMinX + random.nextInt(2) + 8;
        int z = chunkMinZ + random.nextInt(2) + 8;
        Deposit deposit = weightedNetherDeposit(level.getSeed(), chunkMinX >> 4, chunkMinZ >> 4, entries, depthRock);
        placeDeposit(level, random, new BlockPos(x, level.getMinY(), z), deposit, host);
    }

    /** The default (1.7) nether deposit of a chunk. */
    public static Deposit weightedNetherDeposit(long worldSeed, int chunkX, int chunkZ) {
        return weightedNetherDeposit(worldSeed, chunkX, chunkZ, BedrockDepositFeature.Nether.Config.entries17(),
                ModOreBlocks.stone_depth_nether.get());
    }

    /** A weighted pick, the last entry taking what is left over as 1.7's quartz did. */
    static Deposit weightedNetherDeposit(long worldSeed, int chunkX, int chunkZ, List<BedrockDepositFeature.Nether.Entry> entries,
                                         Block depthRock) {
        int total = 0;
        for (BedrockDepositFeature.Nether.Entry e : entries) total += Math.max(0, e.weight().get());
        int roll = nextIntDeterministic(worldSeed, chunkX, chunkZ, Math.max(1, total));
        BedrockDepositFeature.Nether.Entry pick = entries.get(entries.size() - 1);
        for (int i = 0; i < entries.size() - 1; i++) {
            int weight = Math.max(0, entries.get(i).weight().get());
            if (roll < weight) {
                pick = entries.get(i);
                break;
            }
            roll -= weight;
        }
        BedrockDepositFeature.Nether.Entry e = pick;
        return new Deposit(() -> new ItemStack(e.item(), e.count()), null, e.color(), e.tier(), depthRock);
    }

    /** Public deterministic placement core used by GameTests; the cap replaces {@code #minecraft:base_stone_overworld}. */
    public static int placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, Deposit deposit) {
        return placeDeposit(level, random, origin, deposit, FeatureCodecs.tag(BlockTags.BASE_STONE_OVERWORLD));
    }

    static int placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, Deposit deposit, RuleTest host) {
        if (!level.getBlockState(origin).is(Blocks.BEDROCK)) return 0;

        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = origin.getX() - 1; x <= origin.getX() + 1; x++) {
            for (int z = origin.getZ() - 1; z <= origin.getZ() + 1; z++) {
                pos.set(x, origin.getY(), z);
                BlockState previous = level.getBlockState(pos);
                if (!previous.is(Blocks.BEDROCK)) continue;
                if ((x != origin.getX() || z != origin.getZ()) && !random.nextBoolean()) continue;

                level.setBlock(pos, ModOreBlocks.ore_bedrock_block.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
                    ore.setDeposit(deposit.resource().get(), deposit.acidRequirement(), deposit.tier(),
                            deposit.color(), random.nextInt(10));
                }
                placed++;
            }
        }

        for (int x = origin.getX() - 3; x <= origin.getX() + 3; x++) {
            for (int z = origin.getZ() - 3; z <= origin.getZ() + 3; z++) {
                for (int dy = 1; dy < 7; dy++) {
                    pos.set(x, origin.getY() + dy, z);
                    BlockState state = level.getBlockState(pos);
                    // 1.7 replaced stone here; at the modern world floor that stone is deepslate, or TerraFirmaCraft's rock
                    if ((dy < 3 || state.is(Blocks.BEDROCK)) && (host.test(state, random) || state.is(Blocks.BEDROCK))) {
                        level.setBlock(pos, deposit.depthRock().defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    private static int nextIntDeterministic(long seed, int chunkX, int chunkZ, int bound) {
        long chunkKey = (chunkX & 0xFFFF_FFFFL) | ((chunkZ & 0xFFFF_FFFFL) << 32);
        long state = seed ^ chunkKey;
        long threshold = Integer.remainderUnsigned(-bound, bound) & 0xFFFF_FFFFL;
        while (true) {
            state += 0x9E3779B97F4A7C15L;
            long random = HashCommon.murmurHash3(state) >>> 32;
            long product = random * bound;
            if ((product & 0xFFFF_FFFFL) >= threshold) return (int) (product >>> 32);
        }
    }

    public record Deposit(Supplier<ItemStack> resource, @Nullable FluidStack acidRequirement,
                          int color, int tier, Block depthRock) {
        public Deposit {
            if (acidRequirement != null) acidRequirement = acidRequirement.copy();
        }

        @Override
        public @Nullable FluidStack acidRequirement() {
            return acidRequirement == null ? null : acidRequirement.copy();
        }
    }
}
