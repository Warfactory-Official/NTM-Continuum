package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.config.FalloutConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.HazardTypeContaminating;
import com.hbm.hazard.type.IHazardType;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Focused contracts for dropped-item hazard cadence, ground contamination, and uranium fallout rules. */
public final class HazardGameTests {

    private HazardGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function DROPPED_HAZARD_TICK =
            TESTS.function("dropped_hazard_tick", 40, () -> HazardGameTests::droppedHazardTick);
    public static final GameTests.Function GROUND_CONTAMINATION =
            TESTS.function("ground_contamination", 40, () -> HazardGameTests::groundContamination);
    public static final GameTests.Function URANIUM_FALLOUT =
            TESTS.function("uranium_fallout", 40, () -> HazardGameTests::uraniumFallout);

    public static void init() {}

    public static void droppedHazardTick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int oldRate = RadiationConfig.itemHazardDropTickRate;
        HazardData oldData = HazardSystem.itemMap.get(Items.STICK);
        RecordingHazard recording = new RecordingHazard();
        HazardData data = new HazardData().addEntry(new HazardEntry(recording, 2D)
                .addMod(new IHazardModifier() {
                    @Override
                    public double modify(ItemStack stack, LivingEntity holder, double value) {
                        helper.assertTrue(holder == null, "dropped-item modifiers must receive a null holder");
                        return value * 3D;
                    }
                }));

        HazardSystem.register(Items.STICK, data);
        RadiationConfig.itemHazardDropTickRate = 2;
        helper.assertTrue(oldRate == 2, "dropped-item hazard cadence default must remain 2 ticks");

        ItemEntity item = new ItemEntity(level, 0.5D, 2D, 0.5D, new ItemStack(Items.STICK));
        recording.item = item;
        runWhenDue(helper, item, RadiationConfig.itemHazardDropTickRate, 4, () -> {
            item.tick();
            helper.assertTrue(recording.updates == 1 && recording.level == 6D,
                    "dropped hazard must run on its own cadence tick with modifiers applied");

            helper.runAfterDelay(1, () -> runOffCadence(helper, item, RadiationConfig.itemHazardDropTickRate,
                    4, () -> {
                        item.tick();
                        helper.assertTrue(recording.updates == 1,
                                "dropped hazard must not run on the off-cadence tick");
                        runWhenDue(helper, item, RadiationConfig.itemHazardDropTickRate, 4, () -> {
                            try {
                                item.tick();
                                helper.assertTrue(recording.updates == 2,
                                        "dropped hazard must run again on the next cadence boundary");
                                helper.succeed();
                            } finally {
                                restoreItemHazard(oldData, oldRate);
                            }
                        });
                    }));
        });
    }

    public static void groundContamination(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean oldEnabled = RadiationConfig.enableContaminationOnGround;
        HazardTypeContaminating contaminating = new HazardTypeContaminating();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));

        try {
            helper.assertTrue(!oldEnabled, "ground contamination must default to disabled");
            ChunkRadiationManager.proxy.clearSystem(level);
            RadiationConfig.enableContaminationOnGround = false;
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            ItemEntity gated = droppedOn(level, pos);
            contaminating.updateEntity(gated, 8D);
            helper.assertTrue(!gated.isRemoved() && level.getBlockState(pos).is(Blocks.GRASS_BLOCK),
                    "ground contamination must be disabled by its default gate");

            ChunkRadiationManager.proxy.clearSystem(level);
            RadiationConfig.enableContaminationOnGround = true;
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            ItemEntity item = droppedOn(level, pos);
            contaminating.updateEntity(item, 8D);
            helper.assertTrue(item.isRemoved(), "on-ground contaminating material must be consumed");
            helper.assertTrue(Math.abs(ChunkRadiationManager.proxy.getRadiation(level, pos) - 8D) < 0.000001D,
                    "ground contamination must add the evaluated radiation at the item position");
            helper.assertTrue(level.getBlockState(pos).is(ModFalloutBlocks.waste_earth.get()),
                    "ground contamination must invoke the waste terrain transform");
            helper.succeed();
        } finally {
            RadiationConfig.enableContaminationOnGround = oldEnabled;
            ChunkRadiationManager.proxy.clearSystem(level);
        }
    }

    public static void uraniumFallout(GameTestHelper helper) {
        Block uranium = ModNuclearBlocks.ore_uranium.get();
        Block gneissUranium = ModNuclearBlocks.ore_gneiss_uranium.get();
        Block schrabidium = ModFalloutBlocks.ore_sellafield_schrabidium.get();
        Block scorched = ModFalloutBlocks.ore_sellafield_uranium_scorched.get();

        for (Block source : new Block[] {uranium, gneissUranium}) {
            for (int shade = 9; shade >= 5; shade--) {
                BlockState result = uraniumTarget(source, shade, 0x51_0000L + shade);
                helper.assertTrue(result != null && (result.getBlock() == schrabidium || result.getBlock() == scorched)
                                && result.getValue(BlockSellafieldSlaked.SHADE) == shade,
                        "uranium fallout must produce a shade-" + shade + " sellafite uranium target");
            }
            for (int shade = 4; shade >= 0; shade--) {
                helper.assertTrue(uraniumTarget(source, shade, 0x52_0000L + shade) == null,
                        "uranium fallout target must be absent at shade " + shade + " (m <= 4)");
            }
        }

        for (Block source : new Block[] {uranium, gneissUranium}) {
            int schrabidiumCount = 0;
            int scorchedCount = 0;
            for (int sample = 0; sample < 1_000; sample++) {
                BlockState result = uraniumTarget(source, 9, 0x53_0000L + sample);
                if (result.getBlock() == schrabidium) schrabidiumCount++;
                if (result.getBlock() == scorched) scorchedCount++;
            }
            // Upstream subtracts a weight and accepts r <= 0, so configured weights 1:9
            // deliberately sample as 2:8. Preserve the oddity rather than normalizing it.
            helper.assertTrue(schrabidiumCount >= 150 && schrabidiumCount <= 250
                            && scorchedCount >= 750 && scorchedCount <= 850,
                    "uranium fallout must retain the legacy configured-1:9 / sampled-2:8 weighting");
        }
        helper.succeed();
    }

    private static BlockState uraniumTarget(Block source, int shade, long seed) {
        List<FalloutConfig.FalloutEntry> entries = FalloutConfig.entries();
        for (int i = 0; i < entries.size(); i++) {
            BlockState result = entries.get(i).eval(1, source.defaultBlockState(), 0D,
                    RandomSource.create(seed + i), 0);
            if (result != null && (result.getBlock() == ModFalloutBlocks.ore_sellafield_schrabidium.get()
                    || result.getBlock() == ModFalloutBlocks.ore_sellafield_uranium_scorched.get())
                    && result.getValue(BlockSellafieldSlaked.SHADE) == shade) {
                return result;
            }
        }
        return null;
    }

    private static ItemEntity droppedOn(ServerLevel level, BlockPos support) {
        ItemEntity item = new ItemEntity(level, support.getX() + 0.5D, support.getY() + 1D,
                support.getZ() + 0.5D, new ItemStack(Items.STICK));
        item.setOnGround(true);
        return item;
    }

    /** Waits for the world-time cadence used by the legacy dropped-item hook. */
    private static void runWhenDue(GameTestHelper helper, ItemEntity item, int rate, int triesLeft,
                                   Runnable action) {
        if (Math.floorMod(item.level().getGameTime(), rate) == 0) {
            action.run();
            return;
        }
        if (triesLeft <= 0) {
            helper.fail("a dropped item never came due within one full cadence of " + rate + " ticks");
            return;
        }
        helper.runAfterDelay(1, () -> runWhenDue(helper, item, rate, triesLeft - 1, action));
    }

    private static void runOffCadence(GameTestHelper helper, ItemEntity item, int rate, int triesLeft,
                                      Runnable action) {
        if (Math.floorMod(item.level().getGameTime(), rate) != 0) {
            action.run();
            return;
        }
        if (triesLeft <= 0) {
            helper.fail("a dropped item never reached an off-cadence tick within " + rate + " ticks");
            return;
        }
        helper.runAfterDelay(1, () -> runOffCadence(helper, item, rate, triesLeft - 1, action));
    }

    private static void restoreItemHazard(HazardData oldData, int oldRate) {
        if (oldData == null) HazardSystem.itemMap.remove(Items.STICK);
        else HazardSystem.itemMap.put(Items.STICK, oldData);
        HazardSystem.clearCaches();
        RadiationConfig.itemHazardDropTickRate = oldRate;
    }

    private static final class RecordingHazard implements IHazardType {
        private ItemEntity item;
        private int updates;
        private double level;

        @Override
        public void onUpdate(LivingEntity target, double level, ItemStack stack) {}

        @Override
        public void updateEntity(ItemEntity item, double level) {
            if (this.item != item) return;
            updates++;
            this.level = level;
        }

        @Override
        public void addHazardInformation(Player player, List<Component> list, double level,
                                         ItemStack stack, List<IHazardModifier> modifiers) {}
    }
}
