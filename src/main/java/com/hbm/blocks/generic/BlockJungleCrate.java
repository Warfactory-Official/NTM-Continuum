package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 1.7 {@code BlockJungleCrate} ("Enargite Crate"): breaks into a small hoard of gold. */
public class BlockJungleCrate extends Block {

    private final Random rand = new Random();

    public BlockJungleCrate(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 1.7 harvestBlock checks silk touch before getDrops, and this is a plain cube without a tile entity, so silk touch keeps the crate.
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        List<ItemStack> ret = new ArrayList<>();
        ret.add(new ItemStack(Items.GOLD_INGOT, 4 + rand.nextInt(4)));
        ret.add(new ItemStack(Items.GOLD_NUGGET, 8 + rand.nextInt(10)));
        ret.add(BlockAmmoCrate.stack("powder_gold", 2 + rand.nextInt(3)));
        ret.add(BlockAmmoCrate.stack("wire_fine_gold", 4 + rand.nextInt(5)));
        ret.add(BlockAmmoCrate.stack("wire_dense_gold", 1 + rand.nextInt(2)));
        if (rand.nextInt(2) == 0) ret.add(BlockAmmoCrate.stack("plate_gold", 1 + rand.nextInt(2)));
        if (rand.nextInt(3) == 0) ret.add(BlockAmmoCrate.stack("crystal_gold", 1));
        ret.removeIf(ItemStack::isEmpty);
        return ret;
    }
}
