package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockAbsorber;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModItems;
import com.hbm.items.block.CrateHeldOpening;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.tileentity.machine.storage.TileEntityCrateTungsten;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Contracts for the crate and deco blocks: the tungsten crate smelting under a laser (and the 10 MHE polonium and
 * crucible conversions), the elite radiation absorber draining its chunk on its ten-tick loop, and the crates'
 * stacklock.
 */
public final class DecoCrateGameTests {

    private DecoCrateGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TUNGSTEN =
            TESTS.function("deco_crate_tungsten_laser", 20, () -> DecoCrateGameTests::tungsten);
    public static final GameTests.Function ABSORBER =
            TESTS.function("deco_crate_rad_absorber", 60, () -> DecoCrateGameTests::absorber);

    public static final GameTests.Function SPAWNER =
            TESTS.function("deco_crate_glyphid_spawner", 60, () -> DecoCrateGameTests::spawner);
    public static final GameTests.Function HELD_NO_MERGE =
            TESTS.function("deco_crate_held_no_merge", 20, () -> DecoCrateGameTests::heldNoMerge);
    public static final GameTests.Function BROKEN_STACKLOCK =
            TESTS.function("deco_crate_broken_stacklock", 20, () -> DecoCrateGameTests::brokenStacklock);

    public static void init() {}

    /** 1.7 TileEntityGlpyhidSpawner: on its first tick, off peaceful, a swarm spawns around the block. */
    public static void spawner(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, ModDecoCrateBlocks.glyphid_spawner.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(rel);
        helper.succeedWhen(() -> {
            int n = helper.getLevel().getEntitiesOfClass(EntityGlyphid.class,
                    new AABB(abs).inflate(12)).size();
            helper.assertTrue(n > 0, "glyphids near the spawner: " + n);
        });
    }

    /** TileEntityCrateTungsten.addEnergy: smelt in place, over 10 MHE polonium to yharonite and (1.12.2) crucibles under 3 charges recharged. */
    public static void tungsten(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModDecoCrateBlocks.crate_tungsten.get().defaultBlockState());
        TileEntityCrateTungsten crate = (TileEntityCrateTungsten) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(crate != null, "tungsten crate block entity");
        crate.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        crate.setItem(1, new ItemStack(ModItems.billet_polonium.get(), 2));
        crate.setItem(2, new ItemStack(ModAbilityToolItems.crucible.get())); // 1.12.2: crafted empty, 0 charges
        crate.setItem(3, new ItemStack(Items.RAW_IRON, 40)); // 40 ingots still fit one stack
        crate.addEnergy(helper.getLevel(), helper.absolutePos(rel), 1000, Direction.UP);
        helper.assertTrue(crate.getItem(0).is(Items.IRON_INGOT) && crate.getItem(0).getCount() == 5, "raw iron smelted in place");
        helper.assertTrue(crate.getItem(1).is(ModItems.billet_polonium.get()), "polonium needs over 10 MHE");
        helper.assertTrue(ItemCrucible.getCharges(crate.getItem(2)) == 0, "the crucible needs over 10 MHE");
        crate.addEnergy(helper.getLevel(), helper.absolutePos(rel), 10_000_001, Direction.UP);
        helper.assertTrue(crate.getItem(1).is(ModItems.billet_yharonite.get()) && crate.getItem(1).getCount() == 2,
                "polonium became yharonite: " + crate.getItem(1));
        helper.assertTrue(ItemCrucible.getCharges(crate.getItem(2)) == ItemCrucible.maxCharges(),
                "the crucible was recharged to " + ItemCrucible.maxCharges() + ": " + ItemCrucible.getCharges(crate.getItem(2)));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(rel).getValue(BlockStorageCrate.HOT), "the lasered crate is hot");
            helper.succeed();
        });
    }

    /** 1.7 BlockAbsorber: every ten ticks, the tier's amount out of the chunk (pink: 10000). */
    public static void absorber(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockPos abs = helper.absolutePos(rel);
        ChunkRadiationManager.proxy.setRadiation(helper.getLevel(), abs, 5000);
        helper.setBlock(rel, ModDecoCrateBlocks.ABSORBERS.get(BlockAbsorber.EnumAbsorberTier.PINK).get().defaultBlockState());
        helper.succeedWhen(() -> helper.assertTrue(
                ChunkRadiationManager.proxy.getRadiation(helper.getLevel(), abs) < 1,
                "radiation left: " + ChunkRadiationManager.proxy.getRadiation(helper.getLevel(), abs)));
    }

    /** Crates picked up while a held crate is open must not join its stack and share its contents. */
    public static void heldNoMerge(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            ItemStack held = new ItemStack(ModStorageBlocks.crate_iron.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            CrateHeldOpening.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu.slots.get(0).container instanceof CrateHeldOpening.HeldCrate,
                    "right-click did not open the held crate");

            ItemEntity dropped = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(),
                    new ItemStack(ModStorageBlocks.crate_iron.get(), 63));
            dropped.setNoPickUpDelay();
            helper.getLevel().addFreshEntity(dropped);
            dropped.playerTouch(player);
            helper.assertTrue(held.getCount() == 1, "picked-up crates merged into the open crate: " + held.getCount());

            player.containerMenu.slots.get(0).set(new ItemStack(Items.DIAMOND, 64));
            player.closeContainer();
            int filled = 0;
            int crates = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(ModStorageBlocks.crate_iron.get().asItem())) continue;
                crates += stack.getCount();
                if (ItemStacks.carriedItem(stack, "inventory", 36, 0).is(Items.DIAMOND)) filled += stack.getCount();
            }
            helper.assertTrue(crates == 64 && filled == 1, "crates " + crates + ", holding diamonds " + filled);

            CrateHeldOpening.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            player.containerMenu.slots.get(0).set(ItemStack.EMPTY);
            player.closeContainer();
            helper.assertTrue(ItemStacks.isPlain(held), "an emptied crate must stack with fresh ones again");
        }
        helper.succeed();
    }

    /** 1.7 BlockStorageCrate.removedByPlayer: a broken crate that holds anything never stacks, an empty one does. */
    public static void brokenStacklock(GameTestHelper helper) {
        BlockPos full = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos empty = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.getLevel().setBlock(full, ModDecoCrateBlocks.safe.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(empty, ModDecoCrateBlocks.safe.get().defaultBlockState(), 3);
        ((TileEntityCrateBase) helper.getLevel().getBlockEntity(full)).setItem(0, new ItemStack(Items.DIAMOND));
        helper.getLevel().destroyBlock(full, true);
        helper.getLevel().destroyBlock(empty, true);
        ItemStack fullDrop = drop(helper, full);
        ItemStack emptyDrop = drop(helper, empty);
        helper.assertTrue(ItemStacks.carriedLong(fullDrop, CrateHeldOpening.STACKLOCK) != 0,
                "a broken crate with contents carries no stacklock");
        helper.assertTrue(ItemStacks.carriedLong(emptyDrop, CrateHeldOpening.STACKLOCK) == 0,
                "a broken empty crate carries a stacklock");
        helper.succeed();
    }

    private static ItemStack drop(GameTestHelper helper, BlockPos pos) {
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5D),
                entity -> entity.getItem().is(ModDecoCrateBlocks.safe.get().asItem()));
        helper.assertTrue(drops.size() == 1, "safe drops at " + pos + ": " + drops.size());
        return drops.get(0).getItem();
    }
}
