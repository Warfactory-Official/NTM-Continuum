package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBonemealableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.Locale;
import com.hbm.blocks.ModTerrainBlocks;

/**
 * 1.7 {@code BlockNTMFlower}: the single-block crop/flower stage. {@code EnumFlowerType} is flattened to
 * {@code plant_flower_<type>}; {@code CD1} drops the {@code CD0} item ({@code damageDropped}).
 *
 * <p>Growth (random tick, one in three when the grow condition and the 33% chance pass, and bonemeal):
 * hemp on dead/oily dirt withers into a generic dead plant, otherwise becomes the two-block tall hemp;
 * mustard willow {@code CD0 -> CD1 -> tall CD2}, and only with water diagonally under it. Every other
 * flower "grows" by dropping a copy of itself, as upstream did.
 */
public class BlockNTMFlower extends BlockTerrainPlant implements CompatBonemealableBlock {

    public enum Type {
        FOXGLOVE(false), TOBACCO(false), NIGHTSHADE(false), WEED(false), CD0(true), CD1(true);

        public final boolean needsOil;
        Type(boolean needsOil) { this.needsOil = needsOil; }

        public String id() { return "plant_flower_" + name().toLowerCase(Locale.ROOT); }
        public String texture() { return "hbm:blocks/plant_flower." + name().toLowerCase(Locale.ROOT); }
        /** 1.7 {@code getRenderColor}: metas 1 (tobacco) and 3 (hemp) take the biome grass colour. */
        public boolean tinted() { return this == TOBACCO || this == WEED; }
    }

    public final Type type;

    public BlockNTMFlower(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isFlowerGround(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code damageDropped}: CD1 drops as CD0; everything else drops itself. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (type == Type.CD1) return List.of(new ItemStack(ModTerrainBlocks.plant_flower(Type.CD0)));
        return List.of(new ItemStack(this));
    }

    /** 1.7 {@code func_149851_a}: cadmium willows need water beside the block below; hemp/CD1 need air above. */
    public static boolean hasWaterBeside(LevelReader level, BlockPos below) {
        return level.getBlockState(below.east()).is(Blocks.WATER) || level.getBlockState(below.west()).is(Blocks.WATER)
                || level.getBlockState(below.south()).is(Blocks.WATER) || level.getBlockState(below.north()).is(Blocks.WATER);
    }

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        if ((type == Type.CD0 || type == Type.CD1) && !hasWaterBeside(level, pos.below())) return false;
        if (type == Type.WEED || type == Type.CD1) return level.getBlockState(pos.above()).isAir();
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        if (type == Type.WEED || type == Type.CD0 || type == Type.CD1) return random.nextFloat() < 0.33F;
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState onTop = level.getBlockState(pos.below());

        if (type == Type.WEED) {
            if (isDeadDirt(onTop)) {
                level.setBlock(pos, ModTerrainBlocks.plant_dead(BlockDeadPlant.Type.GENERIC).defaultBlockState(), 3);
                return;
            }
            BlockTallPlant.place(level, pos, BlockTallPlant.Type.WEED);
            return;
        }
        if (type == Type.CD0) {
            level.setBlock(pos, ModTerrainBlocks.plant_flower(Type.CD1).defaultBlockState(), 3);
            return;
        }
        if (type == Type.CD1) {
            BlockTallPlant.place(level, pos, BlockTallPlant.Type.CD2);
            return;
        }
        dropResources(state, level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(type == Type.WEED || type == Type.CD0 || type == Type.CD1)) return;
        if (canBonemeal(level, pos, state) && isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }
}
