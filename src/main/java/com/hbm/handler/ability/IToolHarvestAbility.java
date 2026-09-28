package com.hbm.handler.ability;

import com.hbm.config.ToolConfig;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemToolAbility;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code IToolHarvestAbility}: what a block claimed by the dig turns into. */
public interface IToolHarvestAbility extends IBaseAbility {

    default void preHarvestAll(int level, ServerLevel world, ServerPlayer player) { }
    default void postHarvestAll(int level, ServerLevel world, ServerPlayer player) { }

    // You must call harvestBlock to actually break the block.
    // If you don't, visual glitches ensue
    default void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
        harvestBlock(false, world, pos, player);
    }

    static void harvestBlock(boolean skipDefaultDrops, ServerLevel world, BlockPos pos, ServerPlayer player) {
        if (skipDefaultDrops) {
            // Emulate the block breaking without drops
            world.removeBlock(pos, false);
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof ItemToolAbility tool) tool.damageTool(stack, 1, player);
        } else {
            // Break the block conventionally
            ItemToolAbility.standardDigPost(world, pos, player);
        }
    }

    /** 1.7 spawns everything an ability makes on the block the player actually mined. */
    static void dropAtReference(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) return;
        BlockPos drop = ItemToolAbility.dropPos;
        world.addFreshEntity(new ItemEntity(world, drop.getX() + 0.5, drop.getY() + 0.5, drop.getZ() + 0.5, stack));
    }

    /** The item form of a block, as 1.7's {@code new ItemStack(block, 1, meta)}. */
    static ItemStack blockStack(BlockState state) {
        Item item = state.getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    int SORT_ORDER_BASE = 100;

    IToolHarvestAbility NONE = new IToolHarvestAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
    };

    IToolHarvestAbility SILK = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.silktouch"; }
        @Override public boolean isAllowed() { return ToolConfig.abilitySilk; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        // 1.7 enchanted the held stack for the tick and removed it again; the port enchants the copy the
        // drops are rolled against instead, which has the same effect without touching the player's item.
        @Override
        public void preHarvestAll(int level, ServerLevel world, ServerPlayer player) {
            CompatEnchantments.enchantSilkTouch(world, ItemToolAbility.harvestTool);
        }
    };

    IToolHarvestAbility LUCK = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.luck"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityLuck; }

        public final int[] powerAtLevel = { 1, 2, 3, 4, 5, 9 };

        @Override public int levels() { return powerAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + powerAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public void preHarvestAll(int level, ServerLevel world, ServerPlayer player) {
            // 1.7 EnchantmentUtil.addEnchantment replaced any existing fortune level outright.
            CompatEnchantments.enchantFortune(world, ItemToolAbility.harvestTool, powerAtLevel[level]);
        }
    };

    IToolHarvestAbility SMELTER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.smelter"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityFurnace; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
            // 1.7 block.getDrops(world, x, y, z, meta, 0): the plain drops, no tool and no fortune.
            List<ItemStack> drops = new java.util.ArrayList<>(Block.getDrops(state, world, pos, world.getBlockEntity(pos)));

            boolean doesSmelt = false;

            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i).copy();
                Optional<ItemStack> result = Recipes.smeltingResult(world, stack);

                if (result.isPresent() && !result.get().isEmpty()) {
                    ItemStack smelted = result.get().copy();
                    smelted.setCount(smelted.getCount() * stack.getCount());
                    drops.set(i, smelted);
                    doesSmelt = true;
                }
            }

            harvestBlock(doesSmelt, world, pos, player);

            if (doesSmelt) {
                for (ItemStack stack : drops) dropAtReference(world, stack.copy());
            }
        }
    };

    IToolHarvestAbility SHREDDER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.shredder"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityShredder; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
            // 1.7's lit redstone ore band-aid is implicit: the modern ore is one block with a LIT property.
            ItemStack stack = blockStack(state);
            ItemStack result = stack.isEmpty() ? ItemStack.EMPTY : ShredderRecipes.getShredderResult(stack);

            boolean doesShred = !result.isEmpty() && !result.is(ModItems.scrap.get());

            harvestBlock(doesShred, world, pos, player);

            if (doesShred) dropAtReference(world, result.copy());
        }
    };

    IToolHarvestAbility CENTRIFUGE = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.centrifuge"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityCentrifuge; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 5; }

        @Override
        public void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
            ItemStack stack = blockStack(state);
            ItemStack[] result = CentrifugeRecipes.getOutput(stack);

            boolean doesCentrifuge = result != null;

            harvestBlock(doesCentrifuge, world, pos, player);

            if (doesCentrifuge) {
                for (ItemStack st : result) {
                    if (st != null && !st.isEmpty()) dropAtReference(world, st.copy());
                }
            }
        }
    };

    IToolHarvestAbility CRYSTALLIZER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.crystallizer"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityCrystallizer; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 6; }

        @Override
        public void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
            ItemStack stack = blockStack(state);
            CrystallizerRecipe result = stack.isEmpty() ? null : CrystallizerRecipes.getOutput(stack, Fluids.PEROXIDE);

            boolean doesCrystallize = result != null;

            harvestBlock(doesCrystallize, world, pos, player);

            if (doesCrystallize) dropAtReference(world, result.output.copy());
        }
    };

    IToolHarvestAbility MERCURY = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.mercury"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityMercury; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 7; }

        @Override
        public void onHarvestBlock(int level, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
            int mercury = 0;

            if (state.is(Blocks.REDSTONE_ORE)) mercury = player.getRandom().nextInt(5) + 4;
            if (state.is(Blocks.REDSTONE_BLOCK)) mercury = player.getRandom().nextInt(7) + 8;

            boolean doesConvert = mercury > 0;

            harvestBlock(doesConvert, world, pos, player);

            // 1.7 ModItems.ingot_mercury was registered with the name "nugget_mercury", which is the id the
            // port gives it.
            if (doesConvert) dropAtReference(world, new ItemStack(ModItems.nugget_mercury.get(), mercury));
        }
    };

    IToolHarvestAbility[] abilities = { NONE, SILK, LUCK, SMELTER, SHREDDER, CENTRIFUGE, CRYSTALLIZER, MERCURY };

    static IToolHarvestAbility getByName(String name) {
        for (IToolHarvestAbility ability : abilities) {
            if (ability.getName().equals(name)) return ability;
        }
        return NONE;
    }
}
