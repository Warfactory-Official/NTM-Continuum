package com.hbm.blocks.generic;

import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.handler.ArmorUtil;
import dev.golem.core.fluid.FluidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

/**
 * Radioactive reaction sludge ({@code mud_block}), what a Watz power plant becomes when its mud volume overflows:
 * the 1.7 {@code MudBlock}, a flowing {@code BlockFluidClassic} of quanta 4 on a 15-tick flow. You get stuck in it
 * like a cobweb and it poisons you unless you wear a hazmat suit. After every flow update it erodes what it
 * touches (stone to cobblestone one time in 20, cobblestone to gravel in 15, sandstone to sand in 5, terracotta to
 * clay in 10) and dissolves wood, cobwebs and anything of blast resistance under 1.2; on a neighbour change it
 * drains every other liquid it touches. It never displaces another liquid.
 */
public class BlockMud extends FluidBlock {

    /** Damage per contact tick, matching upstream's mud poisoning. */
    private static final float DAMAGE = 8F;

    public BlockMud(Properties properties) {
        super(properties, ModWorldFluids.MUD);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // The old block called setInWeb(); 26.1 spells the same effect as a movement multiplier.
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));

        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (ArmorUtil.checkForHazmat(living)) return;
        living.hurt(level.damageSources().magic(), DAMAGE);
    }

    /** 1.7 {@code reactToBlocks}: any other liquid next to the mud is drained. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            BlockState there = level.getBlockState(at);
            if (!there.is(this) && isLiquid(there)) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** 1.7 {@code updateTick}: {@code super.updateTick}, then {@code reactToBlocks2} on all six sides. */
    @Override
    protected void afterFlow(ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) erode(level, pos.relative(dir), random);
    }

    private void erode(ServerLevel level, BlockPos pos, RandomSource rand) {
        BlockState b = level.getBlockState(pos);
        if (b.is(this) || b.isAir()) return;
        // 1.7 stone, stonebrick (every variant), stone_brick_stairs and stone_slab (every 1.7 slab variant)
        if (b.is(Blocks.STONE) || b.is(Blocks.STONE_BRICKS) || b.is(Blocks.MOSSY_STONE_BRICKS) || b.is(Blocks.CRACKED_STONE_BRICKS)
                || b.is(Blocks.CHISELED_STONE_BRICKS) || b.is(Blocks.STONE_BRICK_STAIRS) || b.is(Blocks.SMOOTH_STONE_SLAB)
                || b.is(Blocks.STONE_SLAB) || b.is(Blocks.SANDSTONE_SLAB) || b.is(Blocks.COBBLESTONE_SLAB) || b.is(Blocks.BRICK_SLAB)
                || b.is(Blocks.STONE_BRICK_SLAB) || b.is(Blocks.NETHER_BRICK_SLAB) || b.is(Blocks.QUARTZ_SLAB)) {
            if (rand.nextInt(20) == 0) level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        } else if (b.is(Blocks.COBBLESTONE)) {
            if (rand.nextInt(15) == 0) level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
        } else if (b.is(Blocks.SANDSTONE) || b.is(Blocks.CHISELED_SANDSTONE) || b.is(Blocks.CUT_SANDSTONE)) {
            if (rand.nextInt(5) == 0) level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        } else if (b.is(BlockTags.TERRACOTTA)) {
            if (rand.nextInt(10) == 0) level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
        } else if (isWood(b) || b.is(Blocks.COBWEB) || b.getBlock().getExplosionResistance() < 1.2F) {
            // 1.7 Material.wood/web and its other soft materials (plants, leaves, glass, wool, ice, snow, gourds,
            // circuits, ...), all of which are under 1.2 blast resistance, as is anything else that soft
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** 1.7 {@code Material.wood}: 1.20 turned it into the bass instrument on a block lava can ignite. */
    private static boolean isWood(BlockState state) {
        return state.instrument() == NoteBlockInstrument.BASS && state.ignitedByLava();
    }

    @Override
    protected boolean canDisplace(BlockGetter level, BlockPos pos) {
        return !isLiquid(level.getBlockState(pos)) && super.canDisplace(level, pos);
    }

    @Override
    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        return !isLiquid(level.getBlockState(pos)) && super.displaceIfPossible(level, pos);
    }
}
