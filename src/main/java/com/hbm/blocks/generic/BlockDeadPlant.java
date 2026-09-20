package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code BlockDeadPlant}: a withered cross plant that only stands on grass, dirt, dead grass, oily or
 * dead dirt and drops nothing ({@code getItemDropped} returned null). The five {@code EnumDeadPlantType}
 * metas are flattened to {@code plant_dead_<type>}.
 */
public class BlockDeadPlant extends BlockTerrainPlant {

    public enum Type {
        GENERIC, GRASS, FLOWER, BIGFLOWER, FERN;

        public String id() { return "plant_dead_" + name().toLowerCase(Locale.ROOT); }
        public String texture() { return "hbm:blocks/plant_dead." + name().toLowerCase(Locale.ROOT); }
    }

    public final Type type;

    public BlockDeadPlant(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isDeadPlantGround(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code getItemDropped -> null}: the creative item exists, but breaking one yields nothing. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
