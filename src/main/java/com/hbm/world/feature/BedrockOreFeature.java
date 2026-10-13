package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.WorldConfig;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFuelItems;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.tileentity.BedrockOreBlockEntity;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** The dynamic 3x3 bedrock-ore patch and its surrounding depth-rock cap. */
public final class BedrockOreFeature {

    private BedrockOreFeature() {}

    // 1.7 BedrockOre.weightedOresNether weights: hbm.cfg 2.BN00_bedrockGlowstoneWeight, 2.BN01_bedrockPhosphorusWeight and
    // 2.BN01_bedrockQuartzWeight (WorldConfig)

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
    public static void generateAutoChunk(WorldGenLevel level, RandomSource random, int chunkMinX, int chunkMinZ) {
        int x = chunkMinX + random.nextInt(2) + 8;
        int z = chunkMinZ + random.nextInt(2) + 8;
        // hbm-space generateAuto: the mean over the column's types (ItemBedrockOreBase.SOURCE), the body's acid if it
        // names one; plain HBM: averageOreLevel over its six
        ItemBedrockOreBase.OreField field = ItemBedrockOreBase.field(level, x, z);
        double density = field.average();
        int tier = tierForDensity(density);
        FluidStack acid = field.boreFluid() != null ? field.boreFluid() : boreFluidForTier(tier);
        placeDeposit(level, random, new BlockPos(x, level.getMinY(), z),
                new Deposit(() -> new ItemStack(ModOreBlocks.bedrock_ore_base.get()),
                        acid, 0xD78A16, tier, ModOreBlocks.stone_depth.get()));
    }

    /**
     * 1.7's Nether-only weighted deposit, the ore kind fixed deterministically per chunk. The one-in-ten roll is the
     * placed feature's {@code rarity_filter}.
     */
    public static void generateNetherChunk(WorldGenLevel level, RandomSource random, int chunkMinX, int chunkMinZ) {
        int x = chunkMinX + random.nextInt(2) + 8;
        int z = chunkMinZ + random.nextInt(2) + 8;
        Deposit deposit = weightedNetherDeposit(level.getSeed(), chunkMinX >> 4, chunkMinZ >> 4);
        placeDeposit(level, random, new BlockPos(x, level.getMinY(), z), deposit);
    }

    public static Deposit weightedNetherDeposit(long worldSeed, int chunkX, int chunkZ) {
        int glowstone = Math.max(0, WorldConfig.bedrockGlowstoneSpawn);
        int phosphorus = Math.max(0, WorldConfig.bedrockPhosphorusSpawn);
        int total = Math.max(1, glowstone + phosphorus + Math.max(0, WorldConfig.bedrockQuartzSpawn));
        int roll = nextIntDeterministic(worldSeed, chunkX, chunkZ, total);
        if (roll < glowstone) {
            return new Deposit(() -> new ItemStack(Items.GLOWSTONE_DUST, 4), null,
                    0xF9FF4D, 1, ModOreBlocks.stone_depth_nether.get());
        }
        roll -= glowstone;
        if (roll < phosphorus) {
            return new Deposit(() -> new ItemStack(ModFuelItems.powder_fire.get(), 4), null,
                    0xD7341F, 1, ModOreBlocks.stone_depth_nether.get());
        }
        return new Deposit(() -> new ItemStack(Items.QUARTZ, 4), null,
                0xF0EFDD, 1, ModOreBlocks.stone_depth_nether.get());
    }

    /** Public deterministic placement core used by GameTests. */
    public static int placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, Deposit deposit) {
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
                    // 1.7 replaced stone here; at the modern world floor that stone is deepslate
                    if ((dy < 3 || state.is(Blocks.BEDROCK))
                            && (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.BEDROCK))) {
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
