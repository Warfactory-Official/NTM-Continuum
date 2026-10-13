package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModArmorItems;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code ExplosionThermo}: the endothermic / exothermic block conversion sweeps and their entity passes.
 * The sweeps walk a cube of side {@code 4 * strength} and convert a block when its squared offset is under
 * {@code r²/2 + rand(r²/4)}, which is a ragged sphere of about {@code 1.4 * strength}. The vanilla block
 * families that had one 1.7 id (log/log2, leaves/leaves2, flowing/still fluids) map onto their modern tags.
 */
public final class ExplosionThermo {

    private ExplosionThermo() {}

    public static void freeze(Level level, BlockPos center, int bombStartStrength) {
        sweep(level, center, bombStartStrength, true, false);
    }

    public static void scorch(Level level, BlockPos center, int bombStartStrength) {
        sweep(level, center, bombStartStrength, false, false);
    }

    public static void scorchLight(Level level, BlockPos center, int bombStartStrength) {
        sweep(level, center, bombStartStrength, false, true);
    }

    private static void sweep(Level level, BlockPos center, int bombStartStrength, boolean freeze, boolean light) {
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int xxSq = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int zzSq = yySq + zz * zz;
                    if (zzSq < r22 + rand.nextInt(r22 / 2)) {
                        pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                        if (freeze) freezeDest(level, pos);
                        else if (light) scorchDestLight(level, pos);
                        else scorchDest(level, pos);
                    }
                }
            }
        }
    }

    /** 1.7 {@code snow}: a snow layer on every exposed block inside the plain sphere. */
    public static void snow(Level level, BlockPos center, int bound) {
        int r2 = bound * bound;
        int r22 = r2 / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -bound; xx < bound; xx++) {
            int xxSq = xx * xx;
            for (int yy = -bound; yy < bound; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -bound; zz < bound; zz++) {
                    if (yySq + zz * zz >= r22) continue;
                    pos.set(center.getX() + xx, center.getY() + yy + 1, center.getZ() + zz);
                    BlockState above = level.getBlockState(pos);
                    if ((above.isAir() || above.is(Blocks.FIRE))
                            && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                        level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void set(Level level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    public static void freezeDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == ModFluidNetBlocks.volcanic_lava_block.get()) set(level, pos, Blocks.COBBLESTONE);
        if (block == Blocks.GRASS_BLOCK) set(level, pos, ModBombs2Blocks.frozen_grass.get());
        if (block == Blocks.DIRT) set(level, pos, ModBombs2Blocks.frozen_dirt.get());
        if (state.is(BlockTags.LOGS) || block == ModFalloutBlocks.waste_log.get()) set(level, pos, ModBombs2Blocks.frozen_log.get());
        if (state.is(BlockTags.PLANKS) || block == ModBlocks.waste_planks.get()) set(level, pos, ModBombs2Blocks.frozen_planks.get());
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.STONE_BRICKS) set(level, pos, Blocks.PACKED_ICE);
        if (state.is(BlockTags.LEAVES)) set(level, pos, Blocks.SNOW_BLOCK);
        if (block == Blocks.LAVA) set(level, pos, Blocks.OBSIDIAN);
        if (block == Blocks.WATER) set(level, pos, Blocks.ICE);
    }

    public static void scorchDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1.7 compared the block read once at the top, so grass -> dirt never cascades into dirt -> netherrack.
        if (block == Blocks.GRASS_BLOCK || block == ModBombs2Blocks.frozen_grass.get()) set(level, pos, Blocks.DIRT);
        if (block == Blocks.DIRT) set(level, pos, Blocks.NETHERRACK);
        if (block == ModBombs2Blocks.frozen_dirt.get()) set(level, pos, Blocks.DIRT);
        if (block == Blocks.NETHERRACK) set(level, pos, Blocks.LAVA);
        if (state.is(BlockTags.LOGS) || block == ModBombs2Blocks.frozen_log.get()) set(level, pos, ModFalloutBlocks.waste_log.get());
        if (state.is(BlockTags.PLANKS) || block == ModBombs2Blocks.frozen_planks.get()) set(level, pos, ModBlocks.waste_planks.get());
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.STONE_BRICKS || block == Blocks.OBSIDIAN) set(level, pos, Blocks.LAVA);
        if (state.is(BlockTags.LEAVES)) set(level, pos, Blocks.AIR);
        if (block == Blocks.WATER) set(level, pos, Blocks.AIR);
        if (block == Blocks.PACKED_ICE) set(level, pos, Blocks.WATER);
        if (block == Blocks.ICE) set(level, pos, Blocks.AIR);
    }

    public static void scorchDestLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK || block == ModBombs2Blocks.frozen_grass.get()) set(level, pos, Blocks.DIRT);
        if (block == Blocks.DIRT) set(level, pos, Blocks.NETHERRACK);
        if (block == ModBombs2Blocks.frozen_dirt.get()) set(level, pos, Blocks.DIRT);
        if (block == ModFalloutBlocks.waste_earth.get()) set(level, pos, Blocks.NETHERRACK);
        if (state.is(BlockTags.LOGS) || block == ModBombs2Blocks.frozen_log.get()) set(level, pos, ModFalloutBlocks.waste_log.get());
        if (state.is(BlockTags.PLANKS) || block == ModBombs2Blocks.frozen_planks.get()) set(level, pos, ModBlocks.waste_planks.get());
        if (block == Blocks.OBSIDIAN) set(level, pos, ModFallingBlocks.gravel_obsidian.get());
        if (state.is(BlockTags.LEAVES)) set(level, pos, Blocks.AIR);
        if (block == Blocks.WATER) set(level, pos, Blocks.AIR);
        if (block == Blocks.PACKED_ICE) set(level, pos, Blocks.WATER);
        if (block == Blocks.ICE) set(level, pos, Blocks.AIR);
        if (block == Blocks.SAND) set(level, pos, Blocks.GLASS);
        if (block == Blocks.CLAY) set(level, pos, TERRACOTTA[level.getRandom().nextInt(TERRACOTTA.length)]);
    }

    /** 1.7 stained hardened clay, metadata 0..15 in dye order. */
    private static final Block[] TERRACOTTA = {
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA
    };

    /**
     * 1.7 {@code freezer}: every living thing (ocelots excepted) within the radius is encased in a 3x3x3
     * column of ice around its feet and hit with weakness IV (2 min), slowness II (90 s) and mining fatigue
     * II (3 min). The doubled radius only widens the AABB search, as it did in 1.7.
     */
    public static void freezer(Level level, BlockPos center, int bombStartStrength) {
        double wat = bombStartStrength;
        int doubled = bombStartStrength * 2;
        double x = center.getX(), y = center.getY(), z = center.getZ();
        AABB box = new AABB(Math.floor(x - wat - 1.0D), Math.floor(y - wat - 1.0D), Math.floor(z - wat - 1.0D),
                Math.floor(x + wat + 1.0D), Math.floor(y + wat + 1.0D), Math.floor(z + wat + 1.0D));
        List<Entity> list = level.getEntities((Entity) null, box, e -> true);

        for (Entity entity : list) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / doubled;
            if (d4 > 1.0D) continue;

            double d5 = entity.getX() - x;
            double d6 = entity.getY() + entity.getEyeHeight() - y;
            double d7 = entity.getZ() - z;
            double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
            if (d9 < wat && !(entity instanceof Ocelot) && entity instanceof LivingEntity living) {
                for (int a = (int) entity.getX() - 2; a < (int) entity.getX() + 1; a++) {
                    for (int b = (int) entity.getY(); b < (int) entity.getY() + 3; b++) {
                        for (int c = (int) entity.getZ() - 1; c < (int) entity.getZ() + 2; c++) {
                            level.setBlockAndUpdate(new BlockPos(a, b, c), Blocks.ICE.defaultBlockState());
                        }
                    }
                }

                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 90 * 20, 2));
                living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 3 * 60 * 20, 2));
            }
        }
    }

    /** 1.7 {@code setEntitiesOnFire}: ten seconds of fire and weakness IV (15 s), asbestos suits exempt. */
    public static void setEntitiesOnFire(Level level, double x, double y, double z, int radius) {
        List<Entity> list = level.getEntities((Entity) null,
                new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius), e -> true);

        for (Entity e : list) {
            if (Math.sqrt(e.distanceToSqr(x, y, z)) > radius) continue;
            if (e instanceof Player player && checkForAsbestos(player)) continue;

            if (e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 4));
            }
            Effects.ignite(e, 10); // 1.7 setFire(10)
        }
    }

    /** 1.7 {@code ArmorUtil.checkForAsbestos}. */
    public static boolean checkForAsbestos(LivingEntity entity) {
        return ArmorUtil.checkArmor(entity, ModArmorItems.asbestos_helmet.get(), ModArmorItems.asbestos_plate.get(),
                ModArmorItems.asbestos_legs.get(), ModArmorItems.asbestos_boots.get());
    }
}
