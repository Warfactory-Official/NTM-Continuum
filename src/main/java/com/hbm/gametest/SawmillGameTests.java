package com.hbm.gametest;

import com.hbm.blocks.machine.MachineSawmill;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntitySawmill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

/** Focused contracts for the heat-powered sawmill. */
public final class SawmillGameTests {

    private SawmillGameTests() {}

    public static void recipes(GameTestHelper helper) {
        helper.assertTrue(SawmillRecipes.getOutput(helper.getLevel(), new ItemStack(Items.OAK_LOG))
                        .is(Items.OAK_PLANKS), "oak log did not become planks");
        helper.assertTrue(SawmillRecipes.getOutput(helper.getLevel(), new ItemStack(Items.OAK_LOG)).getCount() == 6,
                "oak log output was not six planks");
        helper.assertTrue(SawmillRecipes.getOutput(helper.getLevel(), new ItemStack(Items.OAK_PLANKS)).getCount() == 6,
                "plank output was not six sticks");
        helper.assertTrue(SawmillRecipes.getOutput(helper.getLevel(), new ItemStack(Items.STICK))
                        .is(ModItems.powder_sawdust.get()), "stick output was not sawdust");
        helper.succeed();
    }

    public static void layout(GameTestHelper helper) {
        var block = ModMachineBlocks.machine_sawmill.get();
        BlockPos core = new BlockPos(2, 1, 2);
        helper.setBlock(core, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);
        helper.assertTrue(MultiblockSurface.foldedCore(helper.getLevel().getBlockState(helper.absolutePos(core))) != null,
                "sawmill core was not formed");
        helper.assertTrue(helper.absolutePos(core).equals(MultiblockSurface.coreOfAny(helper.getLevel(),
                        helper.absolutePos(core.offset(-1, 0, 0)))),
                "sawmill dummy did not resolve to the core");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos statePos = helper.absolutePos(new BlockPos(2, 1, 2));
        TileEntitySawmill sawmill = place(helper, new BlockPos(2, 1, 2));
        sawmill.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        for (int i = 0; i < 7; i++) {
            sawmill.heat = 1000;
            TileEntitySawmill.serverTick(level, statePos, level.getBlockState(statePos), sawmill);
        }
        helper.assertTrue(sawmill.inventory.getStackInSlot(0).isEmpty(), "sawmill did not consume its log");
        helper.assertTrue(sawmill.inventory.getStackInSlot(1).is(Items.OAK_PLANKS)
                        && sawmill.inventory.getStackInSlot(1).getCount() == 6,
                "sawmill did not produce six oak planks");
        helper.succeed();
    }

    public static void contracts(GameTestHelper helper) {
        MachineSawmill block = ModMachineBlocks.machine_sawmill.get();
        TileEntitySawmill sawmill = place(helper, new BlockPos(2, 1, 2));
        ItemStack remainder = sawmill.inventory.insertItem(0, new ItemStack(Items.OAK_LOG, 64), false);
        helper.assertTrue(sawmill.inventory.getStackInSlot(0).getCount() == 1 && remainder.getCount() == 63,
                "sawmill input must accept only one item");

        sawmill.inventory.setStackInSlot(0, ItemStack.EMPTY);
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack candidate = new ItemStack(item);
            if (candidate.is(ItemTags.LOGS) && SawmillRecipes.getOutput(helper.getLevel(), candidate).isEmpty()) {
                helper.assertFalse(sawmill.canAcceptInput(candidate),
                        "sawmill accepted a log without a plank output: " + item);
            }
        }

        AnvilRecipes.AnvilConstructionRecipe recipe = AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(block.asItem()))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null && recipe.tierLower == 2 && recipe.input.size() == 5,
                "sawmill tier-2 construction recipe is missing or malformed");

        helper.succeed();
    }

    public static void bladeState(GameTestHelper helper) {
        MachineSawmill block = ModMachineBlocks.machine_sawmill.get();
        net.minecraft.world.entity.player.Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(
                helper, net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0F);
        BlockPos clicked = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        ItemStack missingBlade = new ItemStack(block);
        ModDataComponents.ITEM_VARIANT.set(missingBlade, 1);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, missingBlade);

        BlockPos core = clicked.south();
        TileEntitySawmill sawmill = (TileEntitySawmill) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(sawmill != null && !sawmill.hasBlade,
                "missing-blade machine item must place a sawmill without its blade");
        ItemStack clone = block.pickBlockStack(helper.getLevel(), core, helper.getLevel().getBlockState(core));
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(clone, 0) == 1,
                "cloning a missing-blade sawmill must preserve its item variant");
        ItemStack drop = block.getDrops(helper.getLevel().getBlockState(core),
                new LootParams.Builder((ServerLevel) helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(core))).get(0);
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(drop, 0) == 1,
                "breaking a missing-blade sawmill must preserve its item variant");
        helper.succeed();
    }

    public static void bladeCollision(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        TileEntitySawmill sawmill = place(helper, pos);
        LivingEntity victim = helper.spawn(EntityType.PIG, pos);
        victim.setPos(helper.absolutePos(pos).getX() + 0.5D, helper.absolutePos(pos).getY() + 1.5D,
                helper.absolutePos(pos).getZ() - 0.45D);
        sawmill.heat = 101;
        BlockPos absolute = helper.absolutePos(pos);
        TileEntitySawmill.serverTick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), sawmill);
        helper.assertFalse(victim.isAlive(), "the running sawmill blade must damage entities in its rotated volume");
        helper.succeed();
    }

    private static TileEntitySawmill place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachineSawmill block = ModMachineBlocks.machine_sawmill.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntitySawmill,
                "sawmill core block entity was not created");
        return (TileEntitySawmill) helper.getLevel().getBlockEntity(pos);
    }

}
