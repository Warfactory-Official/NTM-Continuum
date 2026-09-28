package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.generic.OreDrops;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DrillBitType;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.tileentity.BedrockOreBlockEntity;
import com.hbm.world.feature.BedrockOreFeature;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Focused old-hbm contracts for bedrock/depth ore generation and extraction. */
public final class BedrockOreGameTests {

    private BedrockOreGameTests() {}

    // Produced independently from the old java.util.Random + NoiseGeneratorPerlin implementation.
    private static final int[][] NOISE_POINTS = {{123, -456}, {-9001, 31415}};
    private static final long[][] OLD_NOISE_BITS = {
            {0x3fe51899bdf4e65dL, 0x3ffb4aac36ee50d0L, 0x3fe0c173ddb0eec9L,
                    0x3fef14f5d00aba27L, 0x3ff6f29197370fccL, 0x3fbc423b13902d45L},
            {0x3fb339185e574dacL, 0x3fb674a1b2434fc1L, 0x3f9b6bbe1b08afe7L,
                    0x3fda826b3c360da5L, 0x3fc3a470782517b2L, 0x3fd1876adaf50d91L},
    };
    private static final long[] OLD_FORTUNE_TWO_BITS = {
            0x3fefa4e69cef598cL, 0x400478012932bc9cL, 0x3fe9222dcc89662eL,
            0x3ff74fb85c080b9dL, 0x400135ed31694bd9L, 0x3fc531ac4eac21f4L,
    };

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PLACEMENT =
            TESTS.function("bedrock_ore_placement_density", 40, 8, () -> BedrockOreGameTests::placementAndDensity);
    public static final GameTests.Function GATES =
            TESTS.function("bedrock_ore_tool_acid_gates", 40, () -> BedrockOreGameTests::toolAndAcidGates);
    public static final GameTests.Function OUTPUT =
            TESTS.function("bedrock_ore_exact_output", 40, () -> BedrockOreGameTests::exactOutput);
    public static final GameTests.Function NETHER =
            TESTS.function("bedrock_ore_nether_variants", 40, () -> BedrockOreGameTests::netherVariants);
    public static final GameTests.Function DEPTH =
            TESTS.function("depth_ore_unbreakable_and_fragments", 40, () -> BedrockOreGameTests::depthRockAndFragments);
    public static final GameTests.Function SAVE =
            TESTS.function("bedrock_ore_save_load", 40, () -> BedrockOreGameTests::saveLoad);
    public static final GameTests.Function RECIPES =
            TESTS.function("bedrock_ore_feasible_recipes", 40, () -> BedrockOreGameTests::recipePresence);

    public static void init() {
        EnergyBedrockGameTests.init(); // grade chain + converter rates, on this suite
    }

    public static void placementAndDensity(GameTestHelper helper) {
        BlockPos relative = new BlockPos(4, 2, 4);
        BlockPos origin = helper.absolutePos(relative);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                helper.getLevel().setBlock(origin.offset(dx, 0, dz), Blocks.BEDROCK.defaultBlockState(), 2);
                helper.getLevel().setBlock(origin.offset(dx, 1, dz), Blocks.STONE.defaultBlockState(), 2);
                helper.getLevel().setBlock(origin.offset(dx, 2, dz), Blocks.STONE.defaultBlockState(), 2);
            }
        }

        int placed = BedrockOreFeature.placeDeposit(helper.getLevel(), RandomSource.create(91L), origin,
                new BedrockOreFeature.Deposit(() -> new ItemStack(ModOreBlocks.bedrock_ore_base.get()),
                        new FluidStack(Fluids.SULFURIC_ACID, 1_000), 0xD78A16, 3,
                        ModOreBlocks.stone_depth.get()));
        helper.assertTrue(placed >= 1 && placed <= 9, "The floor patch must contain the mandatory centre and at most 8 random neighbours");
        helper.assertTrue(helper.getLevel().getBlockState(origin).is(ModOreBlocks.ore_bedrock_block.get()),
                "The mandatory centre bedrock ore was not placed");
        BedrockOreBlockEntity ore = (BedrockOreBlockEntity) helper.getLevel().getBlockEntity(origin);
        helper.assertTrue(ore != null && ore.getTier() == 3 && ore.getColor() == 0xD78A16
                        && ore.getShape() >= 0 && ore.getShape() < 10,
                "Placed deposit did not retain tier/color/shape state");
        helper.assertTrue(helper.getLevel().getBlockState(origin.above()).is(ModOreBlocks.stone_depth.get())
                        && helper.getLevel().getBlockState(origin.offset(3, 2, 3)).is(ModOreBlocks.stone_depth.get()),
                "The exact 7x7, two-layer depth-rock cap was not placed");

        for (int point = 0; point < NOISE_POINTS.length; point++) {
            for (BedrockOreType type : BedrockOreType.VALUES) {
                long actual = Double.doubleToLongBits(ItemBedrockOreBase.getOreLevel(
                        NOISE_POINTS[point][0], NOISE_POINTS[point][1], type));
                helper.assertTrue(actual == OLD_NOISE_BITS[point][type.ordinal()],
                        "Bedrock density noise differs from old source at point " + point + "/" + type);
            }
        }
        helper.assertTrue(BedrockOreFeature.tierForDensity(0.75D) == 1
                        && BedrockOreFeature.tierForDensity(Math.nextUp(0.75D)) == 2
                        && BedrockOreFeature.tierForDensity(1D) == 2
                        && BedrockOreFeature.tierForDensity(Math.nextUp(1D)) == 3
                        && BedrockOreFeature.tierForDensity(1.5D) == 3
                        && BedrockOreFeature.tierForDensity(Math.nextUp(1.5D)) == 4,
                "Legacy strict density thresholds changed");
        FluidStack tier2 = BedrockOreFeature.boreFluidForTier(2);
        FluidStack tier3 = BedrockOreFeature.boreFluidForTier(3);
        FluidStack tier4 = BedrockOreFeature.boreFluidForTier(4);
        helper.assertTrue(BedrockOreFeature.boreFluidForTier(1) == null
                        && tier2 != null && tier2.type == Fluids.WATER && tier2.fill == 1_000
                        && tier3 != null && tier3.type == Fluids.SULFURIC_ACID && tier3.fill == 1_000
                        && tier4 != null && tier4.type == Fluids.SOLVENT && tier4.fill == 2_000,
                "Bedrock bore-fluid requirements changed");
        helper.succeed();
    }

    public static void toolAndAcidGates(GameTestHelper helper) {
        BedrockOreBlockEntity ore = place(helper, new BlockPos(1, 1, 1));
        ore.setDeposit(new ItemStack(ModOreBlocks.bedrock_ore_base.get()),
                new FluidStack(Fluids.SULFURIC_ACID, 1_000), 3, 0xD78A16, 4);

        helper.assertTrue(ore.extract(null, null).failure() == BedrockOreBlockEntity.ExtractionFailure.NO_DRILL,
                "A missing drill must be rejected");
        helper.assertTrue(ore.extract(DrillBitType.HSS_DIAMOND, null).failure()
                        == BedrockOreBlockEntity.ExtractionFailure.TIER,
                "Tier 2 drill extracted a tier 3 deposit");

        FluidTankNTM wrong = tank(Fluids.WATER, 2_000);
        helper.assertTrue(ore.extract(DrillBitType.DESH, wrong).failure()
                        == BedrockOreBlockEntity.ExtractionFailure.FLUID_TYPE && wrong.getFill() == 2_000,
                "Wrong acid type must reject without consumption");
        FluidTankNTM shortTank = tank(Fluids.SULFURIC_ACID, 999);
        helper.assertTrue(ore.extract(DrillBitType.DESH, shortTank).failure()
                        == BedrockOreBlockEntity.ExtractionFailure.FLUID_AMOUNT && shortTank.getFill() == 999,
                "Insufficient acid must reject without consumption");

        FluidTankNTM exact = tank(Fluids.SULFURIC_ACID, 1_250);
        BedrockOreBlockEntity.ExtractionResult result = ore.extract(DrillBitType.DESH, exact);
        helper.assertTrue(result.successful() && result.output().is(ModOreBlocks.bedrock_ore_base.get())
                        && result.output().getCount() == 1 && exact.getFill() == 250,
                "Tier 3 + exact acid must emit one raw ore and consume exactly 1000 mB");
        helper.assertTrue(helper.getLevel().getBlockState(ore.getBlockPos()).is(ModOreBlocks.ore_bedrock_block.get()),
                "Successful extraction consumed the infinite deposit");
        helper.assertTrue(ore.getBlockState().getDestroySpeed(helper.getLevel(), ore.getBlockPos()) < 0F,
                "Bedrock ore became ordinarily breakable");
        helper.succeed();
    }

    public static void exactOutput(GameTestHelper helper) {
        BedrockOreBlockEntity ore = new BedrockOreBlockEntity(new BlockPos(123, 0, -456),
                ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        ore.setDeposit(new ItemStack(ModOreBlocks.bedrock_ore_base.get()), null, 1, 0xD78A16, 0);
        ItemStack output = ore.extract(DrillBitType.STEEL_DIAMOND, null).output();
        for (BedrockOreType type : BedrockOreType.VALUES) {
            helper.assertTrue(Double.doubleToLongBits(ItemBedrockOreBase.getOreAmount(output, type))
                            == OLD_FORTUNE_TWO_BITS[type.ordinal()],
                    type + " density did not receive the drill's exact 1 + fortune*0.25 multiplier");
        }
        ItemStack copied = output.copy();
        helper.assertTrue(ItemBedrockOreBase.DENSITY.get(copied).equals(ItemBedrockOreBase.DENSITY.get(output)),
                "Raw ore density data did not survive an item-stack copy");

        ItemStack iron = BedrockOreType.fragment(BedrockOreType.LIGHT_METAL.primary1, 1D);
        ItemStack technetium = BedrockOreType.fragment(BedrockOreType.ACTINIDE.byproductRad1, 1.25D);
        helper.assertTrue(iron.is(ModAutogenItems.get(MaterialShapes.FRAGMENT, Mats.MAT_IRON))
                        && iron.getCount() == 9,
                "Light-metal primary output must be exactly 9 iron bedrock fragments");
        helper.assertTrue(technetium.is(ModAutogenItems.get(MaterialShapes.FRAGMENT, Mats.MAT_TECHNETIUM))
                        && technetium.getCount() == 2,
                "Fragment counts must use legacy ceil(amount * multiplier) arithmetic");
        for (BedrockOreType oreType : BedrockOreType.VALUES) {
            for (BedrockOreType.BedrockOreOutput fragment : oreType.outputs()) {
                ItemStack stack = BedrockOreType.fragment(fragment, 1D);
                helper.assertTrue(!stack.isEmpty() && stack.getCount() == fragment.amount(),
                        "Missing or miscounted fragment output for " + oreType + "/" + fragment.material().name);
            }
        }
        helper.succeed();
    }

    public static void netherVariants(GameTestHelper helper) {
        ItemStack phosphorus = BedrockOreFeature.weightedNetherDeposit(0L, 7, -11).resource().get();
        ItemStack glowstone = BedrockOreFeature.weightedNetherDeposit(1L, 7, -11).resource().get();
        ItemStack quartz = BedrockOreFeature.weightedNetherDeposit(2L, 7, -11).resource().get();
        helper.assertTrue(phosphorus.is(ModFuelItems.powder_fire.get()) && phosphorus.getCount() == 4,
                "Old deterministic Nether roll 143 must select phosphorus");
        helper.assertTrue(glowstone.is(net.minecraft.world.item.Items.GLOWSTONE_DUST) && glowstone.getCount() == 4,
                "Old deterministic Nether roll 65 must select glowstone");
        helper.assertTrue(quartz.is(net.minecraft.world.item.Items.QUARTZ) && quartz.getCount() == 4,
                "Old deterministic Nether roll 224 must select quartz");
        helper.succeed();
    }

    public static void depthRockAndFragments(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockState depth = ModOreBlocks.stone_depth.get().defaultBlockState();
        helper.getLevel().setBlock(pos, depth, 3);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE));
        helper.assertTrue(depth.getDestroySpeed(helper.getLevel(), pos) < 0F
                        && depth.getDestroyProgress(player, helper.getLevel(), pos) == 0F,
                "Ordinary top-tier tools must not mine depth rock");

        helper.assertTrue(com.hbm.blocks.generic.BlockDepth.DEPTH_TOOL_PROGRESS == 1F / 100F,
                "An accepted IDepthRockTool must receive the exact 1/100 relative progress");

        int fortune = 3;
        RandomSource expectedRandom = RandomSource.create(0xBEEFL);
        int expected = 2 + expectedRandom.nextInt(2) + fortune;
        int actual = OreDrops.base2Rand2Fortune(fortune, RandomSource.create(0xBEEFL));
        helper.assertTrue(actual == expected && actual >= 5 && actual <= 6,
                "Nether depth neodymium must use exactly 2 + rand(2) + fortune fragments");
        List<ItemStack> drops = ModOreBlocks.ore_depth_nether_neodymium.get().getDrops(
                ModOreBlocks.ore_depth_nether_neodymium.get().defaultBlockState(),
                new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)));
        helper.assertTrue(drops.size() >= 2 && drops.size() <= 3
                        && drops.stream().allMatch(stack -> stack.is(ModItems.fragment_neodymium.get())
                        && stack.getCount() == 1),
                "Depth-neodymium must drop 2..3 individual legacy neodymium fragments at fortune 0");
        helper.succeed();
    }

    public static void saveLoad(GameTestHelper helper) {
        BedrockOreBlockEntity original = place(helper, new BlockPos(4, 1, 1));
        original.setDeposit(new ItemStack(net.minecraft.world.item.Items.QUARTZ, 4),
                new FluidStack(Fluids.SOLVENT, 2_000), 4, 0xF0EFDD, 9);
        Map<String, Object> data = new HashMap<>();
        original.saveDeposit(new MemoryOutput(data));

        BedrockOreBlockEntity copy = new BedrockOreBlockEntity(original.getBlockPos().east(), original.getBlockState());
        copy.loadDeposit(new MemoryInput(data));
        FluidStack acid = copy.getAcidRequirement();
        helper.assertTrue(copy.getResource().is(net.minecraft.world.item.Items.QUARTZ)
                        && copy.getResource().getCount() == 4 && acid != null
                        && acid.type == Fluids.SOLVENT && acid.fill == 2_000
                        && copy.getTier() == 4 && copy.getColor() == 0xF0EFDD && copy.getShape() == 9,
                "Resource/fluid/tier/color/shape did not survive the persistence contract");
        helper.succeed();
    }

    public static void recipePresence(GameTestHelper helper) {
        helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), "hbm", "bedrock_ore_fragment_neodymium_compression"),
                "The obtainable Nether depth-neodymium fragment compression recipe is missing");
        helper.succeed();
    }

    private static BedrockOreBlockEntity place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, relative, BedrockOreBlockEntity.class);
    }

    private static FluidTankNTM tank(com.hbm.inventory.fluid.FluidType type, int amount) {
        FluidTankNTM tank = new FluidTankNTM(type, 16_000);
        tank.setFill(amount);
        return tank;
    }


    /** Minimal in-memory DataIn/DataOut used to test the cross-era persistence shape without vanilla spelling. */
    private static final class MemoryOutput implements DataOut {
        private final Map<String, Object> values;

        private MemoryOutput(Map<String, Object> values) { this.values = values; }

        @Override public void putBoolean(String name, boolean value) { values.put(name, value); }
        @Override public void putByte(String name, byte value) { values.put(name, value); }
        @Override public void putShort(String name, short value) { values.put(name, value); }
        @Override public void putInt(String name, int value) { values.put(name, value); }
        @Override public void putLong(String name, long value) { values.put(name, value); }
        @Override public void putFloat(String name, float value) { values.put(name, value); }
        @Override public void putDouble(String name, double value) { values.put(name, value); }
        @Override public void putString(String name, String value) { values.put(name, value); }
        @Override public void putIntArray(String name, int[] value) { values.put(name, value.clone()); }
        @Override public <T> void store(String name, Codec<T> codec, T value) { values.put(name, value); }
        @Override public <T> void storeNullable(String name, Codec<T> codec, T value) { if (value != null) values.put(name, value); }
        @Override public DataOut child(String name) {
            Map<String, Object> child = new HashMap<>();
            values.put(name, child);
            return new MemoryOutput(child);
        }
        @Override public ChildList childrenList(String name) {
            List<Map<String, Object>> children = new ArrayList<>();
            values.put(name, children);
            return () -> {
                Map<String, Object> child = new HashMap<>();
                children.add(child);
                return new MemoryOutput(child);
            };
        }
        @Override public void discard(String name) { values.remove(name); }
    }

    private static final class MemoryInput implements DataIn {
        private final Map<String, Object> values;

        private MemoryInput(Map<String, Object> values) { this.values = values; }

        @Override public boolean getBooleanOr(String name, boolean fallback) { return value(name, Boolean.class).orElse(fallback); }
        @Override public byte getByteOr(String name, byte fallback) { return value(name, Byte.class).orElse(fallback); }
        @Override public short getShortOr(String name, short fallback) { return value(name, Short.class).orElse(fallback); }
        @Override public int getIntOr(String name, int fallback) { return value(name, Integer.class).orElse(fallback); }
        @Override public long getLongOr(String name, long fallback) { return value(name, Long.class).orElse(fallback); }
        @Override public float getFloatOr(String name, float fallback) { return value(name, Float.class).orElse(fallback); }
        @Override public double getDoubleOr(String name, double fallback) { return value(name, Double.class).orElse(fallback); }
        @Override public String getStringOr(String name, String fallback) { return value(name, String.class).orElse(fallback); }
        @Override public Optional<int[]> getIntArray(String name) { return value(name, int[].class).map(int[]::clone); }
        @Override @SuppressWarnings("unchecked") public <T> Optional<T> read(String name, Codec<T> codec) {
            return Optional.ofNullable((T) values.get(name));
        }
        @Override @SuppressWarnings("unchecked") public Optional<DataIn> child(String name) {
            Object value = values.get(name);
            return value instanceof Map<?, ?> map
                    ? Optional.of(new MemoryInput((Map<String, Object>) map)) : Optional.empty();
        }
        @Override public DataIn childOrEmpty(String name) {
            return child(name).orElseGet(() -> new MemoryInput(new HashMap<>()));
        }
        @Override public List<DataIn> childrenListOrEmpty(String name) {
            Object value = values.get(name);
            if (!(value instanceof List<?> list)) return List.of();
            List<DataIn> out = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked") Map<String, Object> child = (Map<String, Object>) map;
                    out.add(new MemoryInput(child));
                }
            }
            return out;
        }

        private <T> Optional<T> value(String name, Class<T> type) {
            Object value = values.get(name);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }
    }
}
