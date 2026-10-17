package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.blocks.machine.ModWeaponTableBlocks;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 tool rules: a pickaxe is effective on Material rock, iron and anvil, an axe on wood, and a block of a
 * Material that needs a tool drops nothing without it. 1.7 breaks such a block at speed / hardness / 30 with
 * the tool and / 100 without, which is vanilla's {@code getDestroyProgress} once the tag and flag are set.
 */
public final class MiningToolGameTests {

    private MiningToolGameTests() {}

    private static List<RegistryObject<? extends Block>> pickaxeBlocks() {
        return List.of(ModBlocks.deco_aluminium, ModBlocks.deco_titanium, ModBlocks.deco_red_copper,
                ModBlocks.deco_tungsten, ModBlocks.deco_steel, ModBlocks.deco_rusty_steel, ModBlocks.deco_lead,
                ModBlocks.deco_beryllium, ModBlocks.block_titanium, ModBlocks.block_steel, ModBlocks.concrete,
                ModBlocks.basalt, ModEnergyNetBlocks.machine_capacitor_copper, ModEnergyNetBlocks.red_pylon,
                ModFallingBlocks.gravel_obsidian, ModWeaponTableBlocks.MACHINE_WEAPON_TABLE);
    }

    public static void pickaxeEffective(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        for (RegistryObject<? extends Block> handle : pickaxeBlocks()) {
            BlockState state = handle.get().defaultBlockState();
            String id = handle.getId().toString();
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), id + " must be mineable/pickaxe");
            helper.assertTrue(pickaxe.getDestroySpeed(state) > 1.0F, id + ": iron pickaxe speed must exceed 1");
            helper.assertTrue(state.requiresCorrectToolForDrops(), id + " must need a tool to drop");
            helper.assertTrue(pickaxe.isCorrectToolForDrops(state), id + ": an iron pickaxe must harvest it");
        }
        helper.assertFalse(ModFallingBlocks.gravel_obsidian.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL),
                "gravel_obsidian is Material.iron in 1.7, not a shovel block");

        for (RegistryObject<? extends Block> handle : List.of(ModBlocks.waste_planks, ModBombs2Blocks.frozen_planks)) {
            BlockState state = handle.get().defaultBlockState();
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), handle.getId() + " must be mineable/axe");
            helper.assertFalse(state.is(BlockTags.MINEABLE_WITH_PICKAXE), handle.getId() + " is wood in 1.7");
        }
        helper.assertFalse(ModOutgasBlocks.block_asbestos.get().defaultBlockState().requiresCorrectToolForDrops(),
                "block_asbestos is Material.cloth in 1.7 and drops by hand");
        helper.succeed();
    }

    /** deco_aluminium, hardness 5: iron pickaxe 6 / 5 / 30 per tick (25 ticks), bare hand 1 / 5 / 100. */
    public static void breakSpeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.deco_aluminium.get());
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        float hand = state.getDestroyProgress(player, helper.getLevel(), abs);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        float pick = state.getDestroyProgress(player, helper.getLevel(), abs);
        // A ratio, so the mock player's on-ground and in-water factors cancel.
        helper.assertTrue(new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(state) == 6.0F, "iron pickaxe speed");
        helper.assertTrue(Math.abs(pick / hand - 20.0F) < 1.0E-3F, "pickaxe / hand progress " + pick + " / " + hand);
        helper.succeed();
    }
}
