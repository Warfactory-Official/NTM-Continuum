package com.hbm.explosion;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import dev.golem.core.world.Effects;

import dev.golem.core.world.Compat;
import dev.golem.core.world.Raycasts;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared nuclear-explosion helpers, 1.7 {@code ExplosionNukeGeneric}: blast damage, EMP, solinium and
 * the wasteland sweeps ({@link #waste}, {@link #wasteNoSchrab}).
 */
public final class ExplosionNukeGeneric {

    private ExplosionNukeGeneric() {}

    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius) {
        dealDamage(world, list, x, y, z, radius, 250F);
    }

    /**
     * 1.7 ExplosionNukeGeneric.dealDamage (:67-110) over a caller-supplied entity list (the MK5 and the
     * small nukes already gathered one). Damages, ignites and knocks back every entity within
     * {@code radius}, scaled linearly by distance and blocked by line of sight: {@code nuclearBlast}
     * through {@code attackEntityFromNT(..., true, true, 0, 100F, 0)} (bypasses armor, pierces up to
     * DT 100), creative players and the 1.7 exempt projectiles are skipped, and a living entity the
     * blast killed is gibbed by {@code ConfettiUtil}. Server-side only.
     */
    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius, float maxDamage) {
        if (!(world instanceof ServerLevel server)) return;
        DamageSource source = ModDamageSources.source(world, ModDamageSources.NUCLEAR_BLAST); // 1.7: ModDamageSource.nuclearBlast

        for (Entity e : list) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            double entX = e.getX();
            double entY = e.getY() + e.getEyeHeight();
            double entZ = e.getZ();

            if (isExplosionExempt(e) || isObstructed(world, x, y, z, entX, entY, entZ, e)) continue;

            boolean doKnockback = true;
            double damage = maxDamage * (radius - dist) / radius;
            if (e instanceof LivingEntity living && living.isAlive()) {
                doKnockback = EntityDamageUtil.attackEntityFromNT(living, source, (float) damage, true, true, 0, 100F, 0);
                // 1.7:88 gores only what the blast KILLED; balefire/TOM re-damage every tick
                if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
            } else {
                Compat.hurt(e, server, source, (float) damage);
            }

            Effects.ignite(e, 5F);

            if (doKnockback) {
                double knockX = e.getX() - x;
                double knockY = e.getY() + e.getEyeHeight() - y;
                double knockZ = e.getZ() - z;
                double len = Math.sqrt(knockX * knockX + knockY * knockY + knockZ * knockZ);
                // 1.7 Vec3.normalize() returns the zero vector below 1e-4
                if (len >= 1.0E-4D) {
                    e.setDeltaMovement(e.getDeltaMovement().add(knockX / len * 0.2D, knockY / len * 0.2D, knockZ / len * 0.2D));
                }
            }
        }
    }

    /** True if a solid block lies between the two points (line-of-sight blocked). */
    private static boolean isExplosionExempt(Entity e) {
        // TODO: 1.7 ExplosionNukeGeneric.java:117 also exempts EntityBulletBaseNT, unported.
        if (e instanceof Ocelot || e instanceof EntityB92Beam || e instanceof EntityBulletBaseMK4
                || e instanceof EntityGrenadeUniversal) {
            return true;
        }

        // 1.7: getGameType() creative-only, NOT spectator.
        return e instanceof Player player && player.isCreative();
    }

    private static boolean isObstructed(Level world, double x1, double y1, double z1,
                                        double x2, double y2, double z2, Entity contextEntity) {
        Vec3 from = new Vec3(x1, y1, z1);
        Vec3 to = new Vec3(x2, y2, z2);
        return Raycasts.isObstructed(world, from, to, contextEntity);
    }

    /**
     * Port-only reduced terrain sweep, used solely by the dropped-item contamination hazard (which has no
     * 1.7 counterpart). Every 1.7 caller uses {@link #waste} or {@link #wasteNoSchrab}.
     */
    public static void transformWaste(Level level, BlockPos center, int radius) {
        if (!(level instanceof ServerLevel) || radius <= 0) return;

        int radiusSquared = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        RandomSource random = level.getRandom();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) continue;
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(pos, ModFalloutBlocks.waste_earth.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.MYCELIUM)) {
                        level.setBlock(pos, ModFalloutBlocks.waste_mycelium.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.SAND) && random.nextInt(20) == 0) {
                        level.setBlock(pos, ModFalloutBlocks.waste_trinitite.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.RED_SAND) && random.nextInt(20) == 0) {
                        level.setBlock(pos, ModFalloutBlocks.waste_trinitite_red.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.CLAY)) {
                        level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /**
     * 1.7 ExplosionNukeGeneric.waste (:216): the wasteland sweep of the yellow barrel, the burning FOEQ
     * and the ZIRNOX meltdown. The sphere edge is jittered per block by {@code nextInt(r22 / 5)} and
     * the half-open loop bounds ({@code -r <= d < r}) are 1.7's. 1.7 threw for radius below 4
     * ({@code nextInt(0)}); the jitter bound is clamped to 1 there instead.
     */
    public static void waste(Level world, BlockPos center, int radius) {
        sweep(world, center, radius, false);
    }

    /** 1.7 ExplosionNukeGeneric.wasteNoSchrab (:336), the multi-bomb's poison sweep. */
    public static void wasteNoSchrab(Level world, BlockPos center, int radius) {
        sweep(world, center, radius, true);
    }

    private static void sweep(Level world, BlockPos center, int radius, boolean noSchrab) {
        if (!(world instanceof ServerLevel)) return;
        int r = radius;
        int r22 = r * r / 2;
        int jitter = Math.max(1, r22 / 5);
        RandomSource rand = world.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int xs = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int ys = xs + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int zs = ys + zz * zz;
                    if (zs < r22 + rand.nextInt(jitter)) {
                        pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                        if (world.getBlockState(pos).isAir()) continue;
                        if (noSchrab) wasteDestNoSchrab(world, pos);
                        else wasteDest(world, pos);
                    }
                }
            }
        }
    }

    private static void set(Level world, BlockPos pos, Block block) {
        world.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL); // 1.7 setBlock(x, y, z, b) is flag 3
    }

    private static void clear(Level world, BlockPos pos) {
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS); // 1.7 setBlock(..., air, 0, 2)
    }

    /** 1.7 wasteDest (:238). 1.7's sand meta 0/1 are sand and red sand; meta-10 mushroom blocks are stems. */
    public static void wasteDest(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        RandomSource random = world.getRandom();
        if (state.is(BlockTags.WOODEN_DOORS) || b == Blocks.IRON_DOOR) {
            clear(world, pos);
        } else if (b == Blocks.GRASS_BLOCK) {
            set(world, pos, ModFalloutBlocks.waste_earth.get());
        } else if (b == Blocks.MYCELIUM) {
            set(world, pos, ModFalloutBlocks.waste_mycelium.get());
        } else if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            if (random.nextInt(20) == 1) {
                set(world, pos, b == Blocks.SAND ? ModFalloutBlocks.waste_trinitite.get() : ModFalloutBlocks.waste_trinitite_red.get());
            }
        } else if (b == Blocks.CLAY) {
            set(world, pos, Blocks.TERRACOTTA);
        } else if (b == Blocks.MOSSY_COBBLESTONE) {
            set(world, pos, Blocks.COAL_ORE);
        } else if (b == Blocks.COAL_ORE) {
            int rand = random.nextInt(10);
            if (rand == 1 || rand == 2 || rand == 3) set(world, pos, Blocks.DIAMOND_ORE);
            if (rand == 9) set(world, pos, Blocks.EMERALD_ORE);
        } else if (isLegacyLog(state)) {
            set(world, pos, ModFalloutBlocks.waste_log.get());
        } else if (b == Blocks.MUSHROOM_STEM) {
            set(world, pos, ModFalloutBlocks.waste_log.get());
        } else if (b == Blocks.BROWN_MUSHROOM_BLOCK || b == Blocks.RED_MUSHROOM_BLOCK) {
            clear(world, pos);
        } else if (state.instrument() == NoteBlockInstrument.BASS && Compat.isSolidRender(state)
                && b != ModFalloutBlocks.waste_log.get()) {
            // 1.7 Material.wood and isOpaqueCube; the bass instrument is the modern wood marker (as in solinium)
            set(world, pos, ModBlocks.waste_planks.get());
        } else if (b == ModNuclearBlocks.ore_uranium.get()) {
            set(world, pos, random.nextInt(getSchrabOreChance()) == 1
                    ? ModBlocks.ore_schrabidium.get() : ModNuclearBlocks.ore_uranium_scorched.get());
        } else if (b == ModNuclearBlocks.ore_nether_uranium.get()) {
            set(world, pos, random.nextInt(getSchrabOreChance()) == 1
                    ? ModBlocks.ore_nether_schrabidium.get() : ModNuclearBlocks.ore_nether_uranium_scorched.get());
        } else if (b == ModNuclearBlocks.ore_gneiss_uranium.get()) {
            set(world, pos, random.nextInt(getSchrabOreChance()) == 1
                    ? ModBlocks.ore_gneiss_schrabidium.get() : ModNuclearBlocks.ore_gneiss_uranium_scorched.get());
        }
    }

    /** 1.7 wasteDestNoSchrab (:358). */
    public static void wasteDestNoSchrab(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        RandomSource random = world.getRandom();
        if (b == Blocks.GLASS || b instanceof StainedGlassBlock || state.is(BlockTags.WOODEN_DOORS)
                || b == Blocks.IRON_DOOR || state.is(BlockTags.LEAVES)) {
            set(world, pos, Blocks.AIR); // 1.7 setBlock(x, y, z, air) is flag 3 in this branch
        } else if (b == Blocks.GRASS_BLOCK) {
            set(world, pos, ModFalloutBlocks.waste_earth.get());
        } else if (b == Blocks.MYCELIUM) {
            set(world, pos, ModFalloutBlocks.waste_mycelium.get());
        } else if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            if (random.nextInt(20) == 1) {
                set(world, pos, b == Blocks.SAND ? ModFalloutBlocks.waste_trinitite.get() : ModFalloutBlocks.waste_trinitite_red.get());
            }
        } else if (b == Blocks.CLAY) {
            set(world, pos, Blocks.TERRACOTTA);
        } else if (b == Blocks.MOSSY_COBBLESTONE) {
            set(world, pos, Blocks.COAL_ORE);
        } else if (b == Blocks.COAL_ORE) {
            int rand = random.nextInt(30);
            if (rand == 1 || rand == 2 || rand == 3) set(world, pos, Blocks.DIAMOND_ORE);
            if (rand == 29) set(world, pos, Blocks.EMERALD_ORE);
        } else if (isLegacyLog(state)) {
            set(world, pos, ModFalloutBlocks.waste_log.get());
        } else if (state.is(BlockTags.PLANKS)) {
            set(world, pos, ModBlocks.waste_planks.get());
        } else if (b == Blocks.MUSHROOM_STEM) {
            set(world, pos, ModFalloutBlocks.waste_log.get());
        } else if (b == Blocks.BROWN_MUSHROOM_BLOCK || b == Blocks.RED_MUSHROOM_BLOCK) {
            clear(world, pos);
        }
    }

    /** 1.7 {@code Blocks.log || Blocks.log2}: the overworld logs (not nether stems, not this mod's logs). */
    private static boolean isLegacyLog(BlockState state) {
        return state.is(BlockTags.LOGS_THAT_BURN) && state.getBlock() != ModFalloutBlocks.waste_log.get();
    }

    /** 1.7 VersatileConfig.getSchrabOreChance: hbm.cfg LBSM_schrabOreRate (20) in LBSM mode, else 250. */
    public static int getSchrabOreChance() {
        if (com.hbm.config.GeneralConfig.enableLBSM) return com.hbm.config.GeneralConfig.schrabRate;
        return 250;
    }

    public static void empBlast(Level world, BlockPos center, int bombStartStrength) {
        if (world.isClientSide()) return;
        int radius = bombStartStrength;
        int threshold = radius * radius / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -radius; xx < radius; xx++)
            for (int yy = -radius; yy < radius; yy++)
                for (int zz = -radius; zz < radius; zz++) {
                    if (xx * xx + yy * yy + zz * zz >= threshold) continue;
                    pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    emp(world, pos);
                }
    }

/**
     * 1.7 ExplosionNukeGeneric.emp. The two 1.7 branches roll scrap on different odds - the MK2 one on
     * {@code nextInt(5) < 1}, the foreign one on {@code <= 1} - and both are kept.
     * 26.2: 1.7's foreign branch was CoFH {@code IEnergyProvider}; FE is its multiloader successor.
     */
    public static void emp(Level world, BlockPos pos) {
        if (world.isClientSide()) return;

        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof IEnergyHandlerMK2 power) {
            power.setPower(0);
            if (world.getRandom().nextInt(5) < 1) {
                world.setBlock(pos, ModFallingBlocks.block_electrical_scrap.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        if (drainForeignEnergy(world, pos) && world.getRandom().nextInt(5) <= 1) {
            world.setBlock(pos, ModFallingBlocks.block_electrical_scrap.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

/**
     * 1.7 drained an {@code IEnergyProvider}'s stored RF once per face; the FE view is per-face too, so the
     * six-way sweep carries over unchanged. Only what a machine will hand over is taken, exactly as 1.7's
     * provider-only branch did - a pure consumer keeps its charge.
     *
     * @return whether any face gave up energy
     */
    public static boolean drainForeignEnergy(Level world, BlockPos pos) {
        boolean drained = false;
        for (Direction side : Direction.values()) {
            dev.golem.core.world.EnergyView view = dev.golem.core.world.EnergyCompat.find(world, pos, side);
            if (view == null) continue;
            long stored = view.amount();
            if (stored > 0 && view.extract(stored) > 0) drained = true;
        }
        return drained;
    }

/**
     * Whether an EMP has anything to do here - either NTM power or a foreign FE machine.
     */
    public static boolean hasDrainableEnergy(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof IEnergyHandlerMK2) return true;
        for (Direction side : Direction.values()) {
            if (dev.golem.core.world.EnergyCompat.find(world, pos, side) != null) return true;
        }
        return false;
    }

/**
     * 1.7 ExplosionNukeGeneric.solinium, the terrain side effect of EntityOrangeFX collisions.
     */
    public static void solinium(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.is(Blocks.GRASS_BLOCK) || state.is(
                Blocks.MYCELIUM) || block == ModFalloutBlocks.waste_earth.get() || block == ModFalloutBlocks.waste_mycelium.get()) {
            world.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        // 26.2: 1.7 Material.{cactus,coral,leaves,plants,sponge,vine,gourd,wood}; no 1.7 vanilla block is
        // Material.coral. plants -> VegetationBlock plus the two 1.7 non-BlockBush plants; wood -> the note-block
        // instrument, as in wasteDest.
        // TODO: hbm blocks declare no instrument, so 1.7's Material.wood arm misses waste_planks/waste_log and the
        // wooden deco family (com/hbm/blocks/ModBlocks.java).
        if (state.is(Blocks.CACTUS) || state.is(Blocks.VINE) || state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE)
                || state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.MELON) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.COCOA)
                || block instanceof VegetationBlock || state.instrument() == NoteBlockInstrument.BASS) {
            world.removeBlock(pos, false);
        }
    }


    public static void dealDamage(Level world, double x, double y, double z, double radius) {
        dealDamage(world, x, y, z, radius, 250F);
    }


    public static void dealDamage(Level world, double x, double y, double z, double radius, float maxDamage) {
        AABB box = new AABB(x, y, z, x, y, z).inflate(radius);
        // 1.7: not just LivingEntity.
        dealDamage(world, world.getEntities(null, box), x, y, z, radius, maxDamage);
    }
}
