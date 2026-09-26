package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.config.FalloutConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardData;
import com.hbm.api.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.hazard.transformer.HazardTransformerCustom;
import com.hbm.api.hazard.HazardType;
import com.hbm.hazard.type.HazardTypeContaminating;
import com.hbm.api.hazard.IHazardType;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.player.InventoryScan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

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
    public static final GameTests.Function CARRIED_HAZARD =
            TESTS.function("carried_hazard", 40, () -> HazardGameTests::carriedHazard);
    public static final GameTests.Function CARRIED_HAZARD_SLOT_EVENT =
            TESTS.function("carried_hazard_slot_event", 40, () -> HazardGameTests::carriedHazardSlotEvent);
    public static final GameTests.Function LAMBDA_HAZARD_PAYLOAD =
            TESTS.function("lambda_hazard_payload", 40, () -> HazardGameTests::lambdaHazardPayload);
    public static final GameTests.Function CUSTOM_HAZARD_HOOKS =
            TESTS.function("custom_hazard_hooks", 40, () -> HazardGameTests::customHazardHooks);

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

    /**
     * A hazard on a carried item reaches its holder, and stops when the item does.
     *
     * <p>The pass is driven directly rather than waited for: its cadence belongs to the server tick
     * and its resolve to a worker thread, neither of which a test should be racing.
     */
    public static void carriedHazard(GameTestHelper helper) {
        HazardData oldData = HazardSystem.itemMap.get(Items.STICK);
        RecordingHazard recording = new RecordingHazard();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        try {
            HazardSystem.register(Items.STICK, new HazardData().addEntry(new HazardEntry(recording, 2D)));

            player.getInventory().setItem(0, new ItemStack(Items.STICK, 3));
            HazardSystem.scanCarried(player);
            helper.assertTrue(recording.carried == 1,
                    "a carried hazard ran " + recording.carried + " times in one pass, expected once");
            helper.assertTrue(recording.carriedLevel == 2D,
                    "the carried hazard was applied at " + recording.carriedLevel + ", expected its base level");
            helper.assertTrue(recording.carriedStack != null && recording.carriedStack.getCount() == 3,
                    "the hazard must be handed the live stack, not the one the resolve saw");

            player.getInventory().setItem(0, ItemStack.EMPTY);
            HazardSystem.scanCarried(player);
            helper.assertTrue(recording.carried == 1, "an emptied slot must stop applying its hazard");
            helper.succeed();
        } finally {
            if (oldData == null) HazardSystem.itemMap.remove(Items.STICK);
            else HazardSystem.itemMap.put(Items.STICK, oldData);
            HazardSystem.clearCaches();
        }
    }

    /**
     * The delivery the carried-hazard pass is built on, end to end: a real server player, whose menu
     * carries the listener Core's {@code ServerPlayer} mixin installed, and whose slot edit becomes a
     * delta, then a memo entry, then one application — and only for the item it was resolved for.
     *
     * <p>A mixin applies when its target class loads, so a server that starts is evidence of nothing;
     * a player built through the constructor that calls {@code initMenu} is. The scan is this test's
     * own rather than the hazard system's because the assertion is about what arrives and when, and
     * the server tick would otherwise resolve the same deltas on a worker thread between two lines.
     */
    public static void carriedHazardSlotEvent(GameTestHelper helper) {
        List<ItemStack> applied = new ArrayList<>();
        InventoryScan<Item> scan = InventoryScan.of(
                stack -> stack.is(Items.STICK) ? stack.getItem() : null,
                (holder, stack, resolved) -> applied.add(stack));
        scan.install();

        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = mock.player();

            player.getInventory().setItem(0, new ItemStack(Items.STICK, 2));
            player.containerMenu.broadcastChanges();
            helper.assertTrue(scan.drainNow() == 1,
                    "a slot edit must reach the scan as exactly one delta — the menu listener is not installed");

            scan.applyNow(player);
            helper.assertTrue(applied.size() == 1,
                    "the resolved slot was applied " + applied.size() + " times, expected once");
            helper.assertTrue(applied.get(0).getCount() == 2, "the application must receive the live stack");

            applied.clear();
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
            scan.applyNow(player);
            helper.assertTrue(applied.isEmpty(),
                    "a memo entry must not be spent on a slot that now holds a different item");

            player.containerMenu.broadcastChanges();
            scan.drainNow();
            scan.applyNow(player);
            helper.assertTrue(applied.isEmpty(), "an item the resolver refuses must leave no memo entry");
            helper.succeed();
        } finally {
            scan.forget();
        }
    }

    /**
     * A hazard nobody wrote a class for: built from lambdas, carrying a payload of the author's own
     * type, and indistinguishable from a built-in one to the rest of the system.
     *
     * <p>The type is a static field rather than a local so that re-running the suite in one session
     * re-registers the same instance instead of colliding with itself under the same name.
     */
    public static void lambdaHazardPayload(GameTestHelper helper) {
        HazardData oldData = HazardSystem.itemMap.get(Items.FEATHER);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        HOLDER_LOG.clear();
        DROPPED_LOG.clear();
        TOOLTIP_LOG.clear();

        try {
            helper.assertTrue(HazardRegistry.registerType(BURST.id(), BURST) == BURST,
                    "registering a hazard type must hand it back, so it can initialise the field that holds it");
            helper.assertTrue(HazardRegistry.getType(BURST.id()) == BURST
                            && BURST.id().equals(HazardRegistry.getTypeId(BURST)),
                    "a named hazard type must round-trip through the name table in both directions");
            helper.assertTrue(HazardRegistry.getType(dev.golem.core.Ids.of(Constants.MOD_ID, "radiation"))
                            == HazardRegistry.RADIATION,
                    "the built-in hazards must be reachable by name too, not just by field");

            HazardSystem.register(Items.FEATHER, new HazardData()
                    .addEntry(BURST, 3D, new Burst("alpha"))
                    .addEntry(BURST, 5D, new Burst("beta")));

            player.getInventory().setItem(0, new ItemStack(Items.FEATHER, 2));
            HazardSystem.scanCarried(player);
            helper.assertTrue(HOLDER_LOG.equals(List.of("alpha", "beta")),
                    "two entries of one type must each carry their own payload, got " + HOLDER_LOG);
            helper.assertTrue(lastLevel == 5D, "the lambda must receive the evaluated level, got " + lastLevel);
            helper.assertTrue(lastStack.getCount() == 2, "the lambda must be handed the live stack");

            HOLDER_LOG.clear();
            HazardSystem.register(Items.FEATHER, new HazardData()
                    .addEntry(new HazardEntry(BURST, 1D, "a payload of the wrong type")));
            HazardSystem.scanCarried(player);
            helper.assertTrue(HOLDER_LOG.equals(List.of("fallback")),
                    "a foreign payload must degrade to the type's default instead of throwing, got " + HOLDER_LOG);

            HOLDER_LOG.clear();
            HazardSystem.register(Items.FEATHER, new HazardData().addEntry(BURST, 2D));
            HazardSystem.scanCarried(player);
            helper.assertTrue(HOLDER_LOG.equals(List.of("fallback")),
                    "an entry registered without a payload must receive the declared default");

            HazardSystem.register(Items.FEATHER, new HazardData()
                    .addEntry(BURST, 4D, new Burst("dropped"))
                    .addEntry(PLAIN, 1D));
            ItemEntity dropped = new ItemEntity(helper.getLevel(), 0.5D, 2D, 0.5D, new ItemStack(Items.FEATHER));
            for (HazardEntry entry : HazardSystem.getHazardsFromStack(dropped.getItem())) entry.applyDropped(dropped);
            helper.assertTrue(DROPPED_LOG.equals(List.of("dropped@4")),
                    "the dropped-item lambda must receive the payload and the evaluated level, got " + DROPPED_LOG);

            List<Component> tooltip = new ArrayList<>();
            HazardSystem.addHazardInfo(new ItemStack(Items.FEATHER), player, tooltip);
            helper.assertTrue(TOOLTIP_LOG.equals(List.of("dropped")),
                    "the tooltip lambda must receive the payload too, got " + TOOLTIP_LOG);
            helper.assertTrue(tooltip.stream().anyMatch(line -> line.getString().contains("trait.gametest.plain")),
                    "a hazard with no tooltip lambda must print its own trait line, namespaced outside hbm");
            helper.succeed();
        } finally {
            player.getInventory().setItem(0, ItemStack.EMPTY);
            if (oldData == null) HazardSystem.itemMap.remove(Items.FEATHER);
            else HazardSystem.itemMap.put(Items.FEATHER, oldData);
            HazardSystem.clearCaches();
        }
    }

    /**
     * Hazards bolted onto an item from outside the registry, after registration has finished: the
     * case {@link HazardSystem#register} cannot answer because the hazard depends on the stack, or
     * because whoever is adding it does not own the item.
     */
    public static void customHazardHooks(GameTestHelper helper) {
        HazardData oldData = HazardSystem.itemMap.get(Items.BRICK);
        BiConsumer<ItemStack, List<HazardEntry>> perCount = (stack, entries) -> entries.add(PLAIN.entry(stack.getCount()));
        BiConsumer<ItemStack, List<HazardEntry>> throwing = (stack, entries) -> {
            throw new IllegalStateException("a third-party hook that does not work");
        };
        BiConsumer<ItemStack, List<HazardEntry>> onBigStacks = (stack, entries) -> entries.add(PLAIN.entry(99D));

        try {
            HazardSystem.register(Items.BRICK, new HazardData().addEntry(HazardRegistry.RADIATION, 4D));
            helper.assertTrue(radiationOf(1) == 4D, "the registered hazard must be the baseline, got " + radiationOf(1));

            HazardTransformerCustom.setMultiplier(Items.BRICK, 2.5D);
            helper.assertTrue(radiationOf(1) == 10D,
                    "a multiplier must scale what the item already carries and be visible at once, got " + radiationOf(1));

            HazardTransformerCustom.onItem(Items.BRICK, perCount);
            List<HazardEntry> entries = HazardSystem.getHazardsFromStack(new ItemStack(Items.BRICK, 3));
            helper.assertTrue(entries.size() == 2 && entries.get(1).type == PLAIN && entries.get(1).baseLevel == 3D,
                    "an item hook must append its own entry and see the live stack");

            HazardTransformerCustom.onItem(Items.BRICK, throwing);
            entries = HazardSystem.getHazardsFromStack(new ItemStack(Items.BRICK, 3));
            helper.assertTrue(entries.size() == 2,
                    "a throwing hook must not take the lookup down with it, got " + entries.size() + " entries");

            HazardTransformerCustom.onMatching(stack -> stack.getCount() > 8, onBigStacks);
            helper.assertTrue(HazardSystem.getHazardsFromStack(new ItemStack(Items.BRICK, 2)).size() == 2,
                    "a predicate hook must not fire for a stack it refuses");
            helper.assertTrue(HazardSystem.getHazardsFromStack(new ItemStack(Items.BRICK, 16)).size() == 3,
                    "a predicate hook must select on stack state that no item or tag key can express");

            helper.assertTrue(HazardTransformerCustom.removeItem(Items.BRICK, perCount)
                            && HazardTransformerCustom.removeItem(Items.BRICK, throwing)
                            && HazardTransformerCustom.removeMatching(onBigStacks),
                    "each hook must be removable by the reference it was added with");
            HazardTransformerCustom.clearMultiplier(Items.BRICK);
            helper.assertTrue(HazardSystem.getHazardsFromStack(new ItemStack(Items.BRICK, 16)).size() == 1
                            && radiationOf(1) == 4D,
                    "removing every hook must leave exactly the registered hazards behind");
            helper.succeed();
        } finally {
            HazardTransformerCustom.clear();
            if (oldData == null) HazardSystem.itemMap.remove(Items.BRICK);
            else HazardSystem.itemMap.put(Items.BRICK, oldData);
            HazardSystem.clearCaches();
        }
    }

    private static double radiationOf(int count) {
        return HazardSystem.getHazardLevelFromStack(new ItemStack(Items.BRICK, count), HazardRegistry.RADIATION);
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

    /** The payload a lambda hazard is registered with — any type the author likes. */
    private record Burst(String tag) {}

    private static final List<String> HOLDER_LOG = new ArrayList<>();
    private static final List<String> DROPPED_LOG = new ArrayList<>();
    private static final List<String> TOOLTIP_LOG = new ArrayList<>();
    private static double lastLevel;
    private static ItemStack lastStack = ItemStack.EMPTY;

    private static final HazardType<Burst> BURST = HazardType
            .builder(dev.golem.core.Ids.of("gametest", "burst"), Burst.class)
            .defaultPayload(new Burst("fallback"))
            .trait(ChatFormatting.LIGHT_PURPLE)
            .onCarried((target, stack, level, payload) -> {
                HOLDER_LOG.add(payload.tag());
                lastLevel = level;
                lastStack = stack;
            })
            .onDropped((item, level, payload) -> DROPPED_LOG.add(payload.tag() + "@" + (int) level))
            .tooltip((player, list, level, stack, mods, payload) -> TOOLTIP_LOG.add(payload.tag()))
            .build();

    /** A hazard with no lambdas at all, to pin the generated trait line and the no-op behaviours. */
    private static final HazardType<Void> PLAIN = HazardType
            .builder(dev.golem.core.Ids.of("gametest", "plain"))
            .trait(ChatFormatting.AQUA)
            .build();

    private static final class RecordingHazard implements IHazardType {
        private ItemEntity item;
        private int updates;
        private double level;
        private int carried;
        private double carriedLevel;
        private ItemStack carriedStack;

        @Override
        public void onUpdate(LivingEntity target, double level, ItemStack stack) {
            carried++;
            carriedLevel = level;
            carriedStack = stack;
        }

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
