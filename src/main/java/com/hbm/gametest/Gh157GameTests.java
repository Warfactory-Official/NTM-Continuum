package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModToolItems;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntitySawmill;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.hbm.tileentity.machine.storage.TileEntitySafe;
import com.hbm.util.TooltipColours;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** GitHub issue 157: drops of a burst boiler and of a cogless or bladeless machine, glyphid reach, lock picking, tooltips. */
public final class Gh157GameTests {

    private Gh157GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BURST_BOILER_PART =
            TESTS.function("gh157_burst_boiler_part_drops_scrap", 20, () -> h -> boiler(h, true, false));
    public static final GameTests.Function BURST_BOILER_CORE =
            TESTS.function("gh157_burst_boiler_core_drops_scrap", 20, () -> h -> boiler(h, true, true));
    public static final GameTests.Function SOUND_BOILER_PART =
            TESTS.function("gh157_sound_boiler_part_drops_itself", 20, () -> h -> boiler(h, false, false));
    public static final GameTests.Function COGLESS_STIRLING =
            TESTS.function("gh157_cogless_stirling_part_drops_without_cog", 20, () -> Gh157GameTests::stirling);
    public static final GameTests.Function BLADELESS_SAWMILL =
            TESTS.function("gh157_bladeless_sawmill_part_drops_without_blade", 20, () -> Gh157GameTests::sawmill);
    public static final GameTests.Function GLYPHID_REACH =
            TESTS.function("gh157_glyphid_hits_within_2_blocks", 200, 8, () -> Gh157GameTests::glyphid);
    public static final GameTests.Function LOCK_PICK =
            TESTS.function("gh157_lock_pick_offhand_and_desh_screwdriver", 20, () -> Gh157GameTests::lockPick);
    public static final GameTests.Function FOREIGN_TOOLTIP_LINES =
            TESTS.function("gh157_tooltip_colours_keep_foreign_lines", 20, () -> Gh157GameTests::foreignLines);

    public static void init() {}

    /** 1.7 {@code MachineHeatBoiler.getDrops}: 4 steel ingots and 8 copper plates once exploded, from any part. */
    private static void boiler(GameTestHelper helper, boolean exploded, boolean core) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_boiler.get(), new BlockPos(2, 2, 2));
        ((TileEntityHeatBoiler) helper.getLevel().getBlockEntity(at)).hasExploded = exploded;
        Map<String, Integer> drops = mine(helper, core ? at : at.above(2), at);
        if (exploded) {
            helper.assertTrue(drops.equals(Map.of("hbm:ingot_steel", 4, "hbm:plate_copper", 8)),
                    "a burst boiler breaks into 4 steel and 8 copper: " + drops);
        } else {
            helper.assertTrue(drops.equals(Map.of("hbm:heat_boiler", 1)), "a sound boiler drops itself once: " + drops);
        }
        helper.succeed();
    }

    /** 1.7 {@code MachineStirling.getDrops}: metadata 1, the cogless item, when the core has no cog. */
    private static void stirling(GameTestHelper helper) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_stirling.get(), new BlockPos(2, 2, 2));
        ((TileEntityStirling) helper.getLevel().getBlockEntity(at)).hasCog = false;
        List<ItemStack> drops = mineStacks(helper, at.above(), at);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModMachineBlocks.machine_stirling.get().asItem())
                && !ModDataComponents.HAS_COG.getOr(drops.get(0), true), "one Stirling engine without its cog: " + drops);
        helper.succeed();
    }

    /** 1.7 {@code MachineSawmill.getDrops}: metadata 1, the bladeless item, when the core has no blade. */
    private static void sawmill(GameTestHelper helper) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_sawmill.get(), new BlockPos(2, 2, 2));
        ((TileEntitySawmill) helper.getLevel().getBlockEntity(at)).hasBlade = false;
        List<ItemStack> drops = mineStacks(helper, at.above(), at);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModMachineBlocks.machine_sawmill.get().asItem())
                && !ModDataComponents.HAS_BLADE.getOr(drops.get(0), true), "one sawmill without its blade: " + drops);
        helper.succeed();
    }

    private static Map<String, Integer> mine(GameTestHelper helper, BlockPos part, BlockPos core) {
        Map<String, Integer> out = new TreeMap<>();
        for (ItemStack stack : mineStacks(helper, part, core)) {
            out.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }
        return out;
    }

    /** A survival player breaks {@code part} with a diamond pickaxe; the drops on the ground around the machine. */
    private static List<ItemStack> mineStacks(GameTestHelper helper, BlockPos part, BlockPos core) {
        helper.assertTrue(helper.getLevel().getBlockState(part).getBlock() instanceof BlockMultiblockCore,
                "a machine part at " + part);
        ServerPlayer player = GameTestHelpers.survivalServerPlayerInLevel(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.gameMode.destroyBlock(part);
        helper.assertTrue(helper.getLevel().getBlockState(core).isAir(), "the whole machine is gone");
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(5))) {
            stacks.add(item.getItem());
            item.discard();
        }
        player.discard();
        return stacks;
    }

    /**
     * 1.7 {@code EntityMob.attackEntity}: a hit only within 2 blocks, centre to centre. The grunt walks up to a pig 4
     * blocks away; the distance in the tick before the first hit must be under 2 (plus one tick's walk).
     */
    private static void glyphid(GameTestHelper helper) {
        Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 2, 5.5));
        EntityGlyphid bug = helper.spawn(ModNtmEntities.GLYPHID.get(), new Vec3(2.5, 2, 1.5));
        bug.setTarget(pig);
        double[] before = {bug.distanceTo(pig)};
        helper.onEachTick(() -> {
            if (pig.getLastHurtByMob() == bug) {
                helper.assertTrue(before[0] < 2.1D, "the grunt hit from " + before[0] + " blocks away, 1.7 reaches 2");
                helper.succeed();
            }
            before[0] = bug.distanceTo(pig);
        });
    }

    /** 1.7 {@code tryPick}: a pin with a screwdriver anywhere on the player, plain or desh; the offhand counts. */
    private static void lockPick(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModDecoCrateBlocks.safe.get());
        TileEntitySafe safe = GameTestHelpers.blockEntity(helper, pos, TileEntitySafe.class);
        safe.setPins(123);
        safe.setMod(1.0D);
        safe.lock();
        ServerPlayer player = GameTestHelpers.survivalServerPlayerInLevel(helper);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.pin.get(), 4));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModToolItems.screwdriver.get()));
        helper.assertTrue(safe.canAccess(player), "a pin in hand with a screwdriver in the offhand picks the lock");
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "the pick spent one pin");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModMeleeKitItems.screwdriver_desh.get()));
        helper.assertTrue(safe.canAccess(player), "a desh screwdriver counts as a screwdriver");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMeleeKitItems.screwdriver_desh.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.pin.get(), 2));
        helper.assertTrue(safe.canAccess(player), "a desh screwdriver in hand with a pin in the offhand picks the lock");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "the pick spent the offhand pin");

        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertFalse(safe.canAccess(player), "a screwdriver without a pin does nothing");
        player.discard();
        helper.succeed();
    }

    /** Another mod's tooltip line is its own Component class; graying it through a copy would turn it into text. */
    private static void foreignLines(GameTestHelper helper) {
        Component marker = new Component() {
            @Override
            public Style getStyle() {
                return Style.EMPTY;
            }

            @Override
            public ComponentContents getContents() {
                return Component.empty().getContents();
            }

            @Override
            public List<Component> getSiblings() {
                return List.of();
            }

            @Override
            public FormattedCharSequence getVisualOrderText() {
                return FormattedCharSequence.EMPTY;
            }
        };
        ItemStack item = new ItemStack(ModItems.pin.get());
        List<Component> lines = new ArrayList<>(List.of(item.getHoverName(), Component.literal("plain"), marker));
        TooltipColours.apply(item, lines);
        helper.assertTrue(lines.get(2) == marker, "a foreign line stays the same object: " + lines.get(2).getClass());
        helper.assertTrue(lines.get(1).getStyle().getColor() != null, "a plain line is still grayed");
        helper.succeed();
    }
}
