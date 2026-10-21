package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.gui.CompatContainerMenu.ClickAction;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Registry-wide crash sweep: every HBM block, item and entity type is created, driven and discarded, and every
 * throwable is collected under its registry id so one run reports all offenders. The registries are split over
 * fixed slices because a test cannot know their size when it is declared.
 */
public final class ReleaseSweepGameTests {

    private ReleaseSweepGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PADDING = 40;
    private static final int BLOCK_SLICES = 12;
    private static final int ITEM_SLICES = 12;
    private static final int CELLS = 40;
    private static final int PER_TICK = 8;

    static {
        for (int slice = 0; slice < BLOCK_SLICES; slice++) {
            final int s = slice;
            TESTS.function("release_sweep_blocks_" + slice, 9000, PADDING, () -> h -> blocks(h, s));
        }
        for (int slice = 0; slice < ITEM_SLICES; slice++) {
            final int s = slice;
            TESTS.function("release_sweep_items_" + slice, 9000, PADDING, () -> h -> items(h, s));
        }
        TESTS.function("release_sweep_entities", 9000, PADDING, () -> ReleaseSweepGameTests::entities);
    }

    public static void init() {}

    /** Items whose use detonates or rewrites the world on the spot. */
    private static final Pattern SKIP_ITEMS = Pattern.compile("ItemWandD|LaserDetonator|DropDetonator|MultiDetonator");
    /** Entities that explode, launch or reshape terrain once ticked; they are only created and round-tripped. */
    private static final Pattern NO_TICK = Pattern.compile("Nuke|Balefire|TomBlast|DeathBlast|EMP|Missile|Rocket|Primed"
            + "|Torex|CloudFleija|CloudSolinium|CloudTom|BlackHole|Quasar|Vortex|Meteor|Fallout|Bomber|C130|Orbital"
            + "|Explosion|Torpedo|Artillery|Bomblet|Boxcar|Building|AcidBomb|FOEQ|Soyuz|Satellite|Bobmazon|Shuttle"
            + "|Grenade|Dynamite|Tnt|Mine|Destroyer|Semtex|Chunk|EntityTom$");

    /** Failures by registry id and step, plus non-fatal notes. */
    private static final class Report {
        final String kind;
        final Map<String, String> failures = new TreeMap<>();
        final List<String> notes = new ArrayList<>();
        final List<String> skipped = new ArrayList<>();
        int exercised;
        /** Positive signals: how often each step really ran, so a green sweep cannot hide an inert one. */
        final Map<String, Integer> counts = new TreeMap<>();

        void count(String what) {
            counts.merge(what, 1, Integer::sum);
        }

        Report(String kind) {
            this.kind = kind;
        }

        void attempt(String id, String step, Runnable body) {
            try {
                body.run();
            } catch (RuntimeException | LinkageError | AssertionError e) { // collected, never rethrown: the sweep reports every offender
                failures.putIfAbsent(id + " [" + step + "]", e.getClass().getSimpleName() + ": " + e.getMessage() + " @ " + where(e));
                Constants.LOG.error("[SWEEP] {} {} [{}] failed", kind, id, step, e);
            }
        }

        void finish(GameTestHelper helper) {
            Constants.LOG.info("[SWEEP] {} slice done: {} exercised, {} failures, {} skipped, {} notes, counts {}",
                    kind, exercised, failures.size(), skipped.size(), notes.size(), counts);
            for (String skip : skipped) Constants.LOG.info("[SWEEP] {} skipped {}", kind, skip);
            for (String note : notes) Constants.LOG.warn("[SWEEP] {} note {}", kind, note);
            failures.forEach((key, text) -> Constants.LOG.error("[SWEEP] FAIL {} {} -> {}", kind, key, text));
            StringBuilder summary = new StringBuilder();
            int shown = 0;
            for (Map.Entry<String, String> entry : failures.entrySet()) {
                if (shown++ == 12) break;
                summary.append("\n").append(entry.getKey()).append(" -> ").append(entry.getValue());
            }
            helper.assertTrue(failures.isEmpty(), failures.size() + " " + kind + " failure(s), first:" + summary);
            helper.succeed();
        }
    }

    private static String where(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String name = e.getClassName();
            if (name.startsWith("com.hbm") || name.startsWith("dev.golem")) {
                return name.substring(name.lastIndexOf('.') + 1) + "." + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        return "?";
    }

    /** Runs {@code body} for 0..total-1, {@code PER_TICK} per server tick, then {@code done}. */
    private static void chunked(GameTestHelper helper, int total, IntConsumer body, Runnable done) {
        chunkFrom(helper, 0, total, body, done);
    }

    private static void chunkFrom(GameTestHelper helper, int from, int total,
                                  IntConsumer body, Runnable done) {
        int to = Math.min(total, from + PER_TICK);
        for (int i = from; i < to; i++) body.accept(i);
        if (to >= total) {
            done.run();
        } else {
            helper.runAfterDelay(1, () -> chunkFrom(helper, to, total, body, done));
        }
    }

    private static BlockPos origin(GameTestHelper helper, int y) {
        return helper.absolutePos(GameTests.paddedCentre(PADDING, y));
    }

    private static void floor(ServerLevel level, BlockPos origin, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    private static void clearAbove(ServerLevel level, BlockPos origin, int radius, int height) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y < height; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void clearEntities(ServerLevel level, AABB box, Entity... keep) {
        for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
            boolean kept = false;
            for (Entity k : keep) kept |= k == entity;
            if (!kept && !(entity instanceof ServerPlayer)) entity.discard();
        }
    }

    /** A varied set of stacks the player carries, so menu slot filters see accepted and refused items. */
    private static List<ItemStack> sampleStacks() {
        List<ItemStack> all = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Constants.MOD_ID)) all.add(new ItemStack(item));
        }
        List<ItemStack> sample = new ArrayList<>();
        sample.add(new ItemStack(Items.IRON_INGOT, 16));
        sample.add(new ItemStack(Items.COAL, 16));
        sample.add(new ItemStack(Items.BUCKET));
        sample.add(new ItemStack(Items.REDSTONE, 16));
        sample.add(new ItemStack(Items.COBBLESTONE, 16));
        int step = Math.max(1, all.size() / 22);
        for (int i = 0; i < all.size() && sample.size() < 27; i += step) sample.add(all.get(i));
        return sample;
    }

    // Blocks

    private static List<Block> hbmBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) blocks.add(block);
        }
        blocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));
        return blocks;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }

    /** The default state, then each value of the facing property and of the first other property, capped. */
    private static List<BlockState> variants(Block block) {
        BlockState base = block.defaultBlockState();
        List<BlockState> states = new ArrayList<>();
        states.add(base);
        Property<?> facing = null;
        Property<?> other = null;
        for (Property<?> property : base.getProperties()) {
            if (property.getValueClass() == Direction.class) {
                if (facing == null) facing = property;
            } else if (other == null) {
                other = property;
            }
        }
        for (Property<?> property : new Property<?>[]{facing, other}) {
            if (property == null) continue;
            int count = 0;
            for (Comparable<?> value : property.getPossibleValues()) {
                BlockState variant = with(base, property, value);
                if (variant != base && count++ < 6) states.add(variant);
            }
        }
        return states.size() > 8 ? states.subList(0, 8) : states;
    }

    private static <T extends BlockEntity> void tickOnce(EntityBlock block, ServerLevel level, BlockPos pos,
                                                         BlockState state, T be, int times) {
        @SuppressWarnings("unchecked")
        BlockEntityType<T> type = (BlockEntityType<T>) be.getType();
        BlockEntityTicker<T> ticker = block.getTicker(level, state, type);
        if (ticker == null) return;
        for (int i = 0; i < times; i++) ticker.tick(level, pos, state, be);
    }

    public static void blocks(GameTestHelper helper, int slice) {
        ServerLevel level = helper.getLevel();
        Report report = new Report("block");
        List<BlockState> cells = new ArrayList<>();
        List<Block> blocks = hbmBlocks();
        for (int i = slice; i < blocks.size(); i += BLOCK_SLICES) {
            for (BlockState state : variants(blocks.get(i))) cells.add(state);
        }
        BlockPos origin = origin(helper, 4);
        floor(level, origin, 16);
        clearAbove(level, origin, 16, 6);
        GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper);
        List<ItemStack> carried = sampleStacks();
        int batches = (cells.size() + CELLS - 1) / CELLS;
        blockBatch(helper, level, report, cells, origin, fixture, carried, 0, batches);
    }

    private static void blockBatch(GameTestHelper helper, ServerLevel level, Report report, List<BlockState> cells,
                                   BlockPos origin, GameTestHelpers.MockServerPlayer fixture,
                                   List<ItemStack> carried, int batch, int batches) {
        if (batch >= batches) {
            fixture.close();
            report.finish(helper);
            return;
        }
        floor(level, origin, 16);
        int from = batch * CELLS;
        int to = Math.min(cells.size(), from + CELLS);
        List<BlockPos> placed = new ArrayList<>();
        try {
            ServerPlayer player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            for (int i = from; i < to; i++) {
                BlockPos pos = origin.offset(-12 + ((i - from) % 8) * 3, 0, -12 + ((i - from) / 8) * 3);
                exerciseBlock(level, report, player, carried, pos, cells.get(i));
                placed.add(pos);
            }
        } catch (RuntimeException | LinkageError e) {
            fixture.close();
            throw e;
        }
        helper.runAfterDelay(3, () -> {
            for (int i = 0; i < placed.size(); i++) {
                BlockPos pos = placed.get(i);
                BlockState state = cells.get(from + i);
                BlockState now = level.getBlockState(pos);
                if (now.getBlock() != state.getBlock()) {
                    report.notes.add(state + " is " + now + " three ticks after being placed");
                }
                report.attempt(id(state), "break", () -> {
                    if (!level.getBlockState(pos).isAir()) level.destroyBlock(pos, true);
                });
            }
            clearEntities(level, new AABB(origin).inflate(20.0D, 8.0D, 20.0D), fixture.player());
            clearAbove(level, origin, 16, 6);
            helper.runAfterDelay(1, () -> blockBatch(helper, level, report, cells, origin, fixture, carried,
                    batch + 1, batches));
        });
    }

    private static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static void exerciseBlock(ServerLevel level, Report report, ServerPlayer player, List<ItemStack> carried,
                                      BlockPos pos, BlockState state) {
        String id = id(state);
        report.exercised++;
        boolean[] placedOk = {false};
        report.attempt(id, "place", () -> {
            level.setBlock(pos, state, 3);
            placedOk[0] = true;
        });
        if (!placedOk[0]) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        BlockState current = level.getBlockState(pos);
        report.attempt(id, "state", () -> {
            current.getShape(level, pos);
            current.getCollisionShape(level, pos);
            current.getLightEmission();
            current.getDestroySpeed(level, pos);
            if (current.isRandomlyTicking()) current.randomTick(level, pos, level.getRandom());
        });
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && current.getBlock() instanceof EntityBlock entityBlock) {
            report.count("blockEntities");
            boolean[] ticked = {true};
            report.attempt(id, "tick", () -> {
                try {
                    tickOnce(entityBlock, level, pos, current, be, 40);
                    report.count("ticked40");
                } catch (RuntimeException | LinkageError | AssertionError e) {
                    ticked[0] = false;
                    throw e;
                }
            });
            if (!ticked[0]) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                return;
            }
            report.attempt(id, "container", () -> fillAndProbe(be));
            report.attempt(id, "roundtrip", () -> roundTrip(level, report, pos, current, be, entityBlock));
        }
        if (level.getBlockState(pos).isAir()) return;
        report.attempt(id, "menu", () -> openMenu(report, level, player, carried, pos));
        BlockEntity afterMenu = level.getBlockEntity(pos);
        if (afterMenu != null && current.getBlock() instanceof EntityBlock entityBlock) {
            boolean[] ok = {true};
            report.attempt(id, "tick-with-contents", () -> {
                try {
                    tickOnce(entityBlock, level, pos, current, afterMenu, 20);
                    report.count("tickedWithContents");
                } catch (RuntimeException | LinkageError | AssertionError e) {
                    ok[0] = false;
                    throw e;
                }
            });
            if (!ok[0]) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** Built on use: stacks cannot be created before the registries are bound. */
    private static ItemStack[] candidates() {
        return new ItemStack[]{
                new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.COAL, 4), new ItemStack(Items.BUCKET),
                new ItemStack(Items.REDSTONE, 4), new ItemStack(Items.COBBLESTONE, 4), new ItemStack(Items.DIAMOND)
        };
    }

    /** Puts an accepted candidate stack in every slot, then asks the sided-access methods about each face. */
    private static void fillAndProbe(BlockEntity be) {
        if (!(be instanceof Container container)) return;
        ItemStack[] candidates = candidates();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            for (ItemStack candidate : candidates) {
                if (container.canPlaceItem(slot, candidate)) {
                    container.setItem(slot, candidate.copy());
                    break;
                }
            }
        }
        if (be instanceof WorldlyContainer sided) {
            for (Direction face : Direction.values()) {
                for (int slot : sided.getSlotsForFace(face)) {
                    sided.canPlaceItemThroughFace(slot, candidates[0], face);
                    sided.canTakeItemThroughFace(slot, container.getItem(slot), face);
                }
            }
        }
    }

    private static void roundTrip(ServerLevel level, Report report, BlockPos pos, BlockState state, BlockEntity be,
                                  EntityBlock block) {
        var registries = level.registryAccess();
        CompoundTag saved = BlockEntities.save(be, registries);
        BlockEntity copy = BlockEntities.load(pos, state, saved, registries);
        if (copy == null) {
            report.notes.add(id(state) + " roundtrip: load returned null");
            return;
        }
        copy.setLevel(level);
        report.count("roundTrips");
        CompoundTag again = BlockEntities.save(copy, registries);
        if (!saved.equals(again)) {
            List<String> differing = new ArrayList<>();
            for (String key : Tags.keys(saved)) {
                if (!Objects.equals(saved.get(key), again.get(key))) differing.add(key);
            }
            for (String key : Tags.keys(again)) {
                if (!saved.contains(key)) differing.add(key);
            }
            report.notes.add(id(state) + " roundtrip differs in " + differing);
        }
        // A save holding only the type and position, as an older or damaged save would leave it
        CompoundTag bare = saved.copy();
        for (String key : Tags.keys(saved)) {
            if (!key.equals("id") && !key.equals("x") && !key.equals("y") && !key.equals("z")) bare.remove(key);
        }
        BlockEntity minimal = BlockEntities.load(pos, state, bare, registries);
        if (minimal != null) {
            minimal.setLevel(level);
            tickOnce(block, level, pos, state, minimal, 2);
            BlockEntities.save(minimal, registries);
        }
    }

    /** Right-click with an empty hand, then shift-click and pick up every slot with a stocked inventory. */
    private static void openMenu(Report report, ServerLevel level, ServerPlayer player, List<ItemStack> carried, BlockPos pos) {
        player.setPos(Vec3.atBottomCenterOf(pos.above(2)));
        player.getInventory().clearContent();
        for (int i = 0; i < carried.size() && i < 36; i++) player.getInventory().setItem(i, carried.get(i).copy());
        BlockState state = level.getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try {
            Compat.useBlockWithoutItem(state, level, player, hit);
            AbstractContainerMenu menu = player.containerMenu;
            if (menu != player.inventoryMenu) {
                report.count("menusOpened");
                int slots = menu.slots.size();
                for (int i = 0; i < slots; i++) menu.quickMoveStack(player, i);
                for (int i = 0; i < slots; i++) Menus.click(menu, i, 0, ClickAction.PICKUP, player);
                for (int i = 0; i < slots; i++) Menus.click(menu, i, 1, ClickAction.PICKUP, player);
                menu.broadcastChanges();
            }
        } finally {
            player.closeContainer();
            player.getInventory().clearContent();
        }
    }

    // Items

    private static List<ItemStack> hbmStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Constants.MOD_ID)) stacks.add(new ItemStack(item));
        }
        for (var suppliers : ModTabs.STACK_CONTENTS.values()) {
            for (var supplier : suppliers) {
                for (ItemStack stack : supplier.get()) {
                    if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Constants.MOD_ID)) {
                        stacks.add(stack.copy());
                    }
                }
            }
        }
        stacks.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        return stacks;
    }

    public static void items(GameTestHelper helper, int slice) {
        ServerLevel level = helper.getLevel();
        Report report = new Report("item");
        List<ItemStack> all = hbmStacks();
        List<ItemStack> mine = new ArrayList<>();
        for (int i = slice; i < all.size(); i += ITEM_SLICES) mine.add(all.get(i));
        BlockPos origin = origin(helper, 4);
        floor(level, origin, 8);
        clearAbove(level, origin, 8, 6);
        GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper);
        Entity[] target = {null};
        List<BlockPos> machines = machineTargets(level, origin);
        try {
            ServerPlayer player = fixture.player();
            player.setPos(Vec3.atBottomCenterOf(origin));
            target[0] = pig(level, origin);
            chunked(helper, mine.size(), i -> {
                if (target[0] == null || !target[0].isAlive()) target[0] = pig(level, origin);
                String id = BuiltInRegistries.ITEM.getKey(mine.get(i).getItem()).toString();
                if (SKIP_ITEMS.matcher(mine.get(i).getItem().getClass().getSimpleName()).find()) {
                    if (!report.skipped.contains(id)) report.skipped.add(id);
                    return;
                }
                report.exercised++;
                exerciseItem(level, report, player, target[0], origin, machines, mine.get(i), id);
            }, () -> {
                fixture.close();
                report.finish(helper);
            });
        } catch (RuntimeException | LinkageError e) {
            fixture.close();
            throw e;
        }
    }

    /** Machines the items are used on besides the floor: a fluid tank, a crate, a battery and a processing machine. */
    private static final String[] MACHINE_TARGETS = {"barrel_steel", "crate_steel", "machine_battery", "machine_chemical_plant"};

    private static List<BlockPos> machineTargets(ServerLevel level, BlockPos origin) {
        List<BlockPos> targets = new ArrayList<>();
        for (String name : MACHINE_TARGETS) {
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!BuiltInRegistries.BLOCK.getKey(block).toString().equals(Constants.MOD_ID + ":" + name)) continue;
                BlockPos pos = origin.offset(6, 0, -6 + 4 * targets.size());
                level.setBlock(pos, block.defaultBlockState(), 3);
                targets.add(pos);
            }
        }
        return targets;
    }

    private static Entity pig(ServerLevel level, BlockPos origin) {
        Entity pig = Entities.createDetached(EntityType.PIG, level);
        pig.setPos(Vec3.atBottomCenterOf(origin.east(2)));
        if (pig instanceof Mob mob) mob.setNoAi(true);
        level.addFreshEntity(pig);
        return pig;
    }

    private static void reset(ServerLevel level, ServerPlayer player, Entity target, BlockPos origin) {
        player.closeContainer();
        player.stopUsingItem();
        player.getInventory().clearContent();
        player.setPos(Vec3.atBottomCenterOf(origin));
        clearEntities(level, new AABB(origin).inflate(12.0D, 8.0D, 12.0D), player, target);
        clearAbove(level, origin, 4, 4);
        if (target instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
        target.setPos(Vec3.atBottomCenterOf(origin.east(2)));
    }

    private static void exerciseItem(ServerLevel level, Report report, ServerPlayer player, Entity target,
                                     BlockPos origin, List<BlockPos> machines, ItemStack proto, String id) {
        BlockPos floorPos = origin.below();
        BlockHitResult floorHit = new BlockHitResult(Vec3.atCenterOf(floorPos).add(0, 0.5D, 0), Direction.UP, floorPos, false);
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            String tag = mode == GameType.CREATIVE ? "creative " : "survival ";
            report.attempt(id, tag + "mode", () -> player.setGameMode(mode));
            report.attempt(id, tag + "use", () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                player.getItemInHand(InteractionHand.MAIN_HAND).use(level, player, InteractionHand.MAIN_HAND);
                report.count("use");
                if (player.isUsingItem()) {
                    report.count("usingItem");
                    for (int t = 0; t < 3; t++) player.tick();
                    player.releaseUsingItem();
                }
            });
            reset(level, player, target, origin);
            report.attempt(id, tag + "useOn", () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                player.getItemInHand(InteractionHand.MAIN_HAND)
                        .useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, floorHit));
                report.count("useOn");
            });
            reset(level, player, target, origin);
            for (BlockPos machine : machines) {
                report.attempt(id, tag + "useOn " + BuiltInRegistries.BLOCK.getKey(level.getBlockState(machine).getBlock()).getPath(), () -> {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(machine), Direction.UP, machine, false);
                    player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                    player.getItemInHand(InteractionHand.MAIN_HAND)
                            .useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                    report.count("useOnMachine");
                });
                reset(level, player, target, origin);
            }
            if (mode == GameType.CREATIVE) continue;
            report.attempt(id, "useOnEntity", () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                player.getItemInHand(InteractionHand.MAIN_HAND)
                        .interactLivingEntity(player, (LivingEntity) target, InteractionHand.MAIN_HAND);
            });
            report.attempt(id, "attack", () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                player.attack(target);
                report.count("attack");
            });
            report.attempt(id, "mine", () -> {
                BlockPos ore = origin.north(2);
                level.setBlock(ore, Blocks.STONE.defaultBlockState(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, proto.copy());
                player.gameMode.destroyBlock(ore);
            });
            reset(level, player, target, origin);
            report.attempt(id, "inventory tick", () -> {
                player.getInventory().setItem(0, proto.copy());
                player.getInventory().setItem(1, proto.copy());
                player.tick();
                report.count("inventoryTick");
            });
            reset(level, player, target, origin);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
                report.attempt(id, "worn " + slot.getName(), () -> {
                    player.setItemSlot(slot, proto.copy());
                    player.tick();
                    player.setItemSlot(slot, ItemStack.EMPTY);
                });
            }
            reset(level, player, target, origin);
            report.attempt(id, "dropped", () -> {
                ItemEntity dropped = new ItemEntity(level, origin.getX() + 0.5D, origin.getY() + 1.0D,
                        origin.getZ() + 0.5D, proto.copy());
                dropped.tick();
                dropped.tick();
            });
            report.attempt(id, "codec", () -> {
                var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
                var tagOut = ItemStacks.optionalCodec().encodeStart(ops, proto).result();
                if (tagOut.isPresent()) ItemStacks.optionalCodec().parse(ops, tagOut.get()).result();
            });
            report.attempt(id, "display", () -> {
                proto.getHoverName();
                proto.getDisplayName();
                proto.getItem().getDescriptionId();
            });
            reset(level, player, target, origin);
        }
    }

    // Entities

    public static void entities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Report report = new Report("entity");
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Constants.MOD_ID)) types.add(type);
        }
        types.sort(Comparator.comparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()));
        BlockPos origin = origin(helper, 4);
        floor(level, origin, 16);
        clearAbove(level, origin, 16, 8);
        int group = 20;
        entityGroup(helper, level, report, types, origin, 0, group);
    }

    private static void entityGroup(GameTestHelper helper, ServerLevel level, Report report,
                                    List<EntityType<?>> types, BlockPos origin, int from, int group) {
        if (from >= types.size()) {
            report.finish(helper);
            return;
        }
        int to = Math.min(types.size(), from + group);
        List<Entity> live = new ArrayList<>();
        for (int i = from; i < to; i++) {
            EntityType<?> type = types.get(i);
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            BlockPos cell = origin.offset(-12 + ((i - from) % 5) * 5, 0, -8 + ((i - from) / 5) * 5);
            report.exercised++;
            Entity[] made = {null};
            report.attempt(id, "create", () -> made[0] = Entities.createDetached(type, level));
            Entity entity = made[0];
            if (entity == null) {
                report.notes.add(id + " create returned null");
                continue;
            }
            report.attempt(id, "roundtrip", () -> {
                entity.setPos(Vec3.atBottomCenterOf(cell));
                CompoundTag saved = Entities.saveToTag(entity);
                Entity copy = Entities.createDetached(type, level);
                Entities.loadFromTag(copy, saved);
                CompoundTag again = Entities.saveToTag(copy);
                if (!saved.equals(again)) {
                    List<String> differing = new ArrayList<>();
                    for (String key : Tags.keys(saved)) {
                        if (!Objects.equals(saved.get(key), again.get(key))) differing.add(key);
                    }
                    report.notes.add(id + " roundtrip differs in " + differing);
                }
                // A save of just the position and rotation, as a stripped one would be
                Entity bare = Entities.createDetached(type, level);
                Entities.loadFromTag(bare, new CompoundTag());
                Entities.saveToTag(bare);
            });
            String simple = entity.getClass().getSimpleName();
            if (NO_TICK.matcher(simple).find()) {
                report.skipped.add(id + " (" + simple + ") created and saved only");
                continue;
            }
            boolean[] added = {false};
            report.attempt(id, "add", () -> added[0] = level.addFreshEntity(entity));
            if (!added[0]) continue;
            boolean[] ok = {true};
            report.attempt(id, "tick", () -> {
                try {
                    for (int t = 0; t < 20 && entity.isAlive(); t++) entity.tick();
                    report.count("ticked20");
                } catch (RuntimeException | LinkageError | AssertionError e) {
                    ok[0] = false;
                    throw e;
                }
            });
            if (ok[0]) {
                live.add(entity);
            } else {
                entity.discard();
            }
        }
        helper.runAfterDelay(8, () -> {
            for (Entity entity : live) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                report.attempt(id, "hurt", () -> {
                    if (entity.isAlive()) {
                        Compat.hurt(entity, level, level.damageSources().generic(), 5.0F);
                    }
                });
                report.attempt(id, "kill", () -> {
                    if (entity.isAlive()) Compat.kill(entity, level);
                });
                if (entity.isAlive()) entity.discard();
            }
            clearEntities(level, new AABB(origin).inflate(30.0D, 12.0D, 30.0D));
            clearAbove(level, origin, 16, 8);
            helper.runAfterDelay(1, () -> entityGroup(helper, level, report, types, origin, to, group));
        });
    }
}
