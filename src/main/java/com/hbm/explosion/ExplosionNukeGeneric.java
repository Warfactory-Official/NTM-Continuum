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
 * Shared nuclear-explosion helpers, ported from 1.12.2. Only the blast-damage path used by the Fat Man
 * is ported here; the radiation/contamination, confetti gibbing, EMP and silk-block conversion paths
 * are deferred until the hazard system lands (see IMPLEMENTATION.md §18).
 */
public final class ExplosionNukeGeneric {

    private ExplosionNukeGeneric() {}

    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius) {
        dealDamage(world, list, x, y, z, radius, 250F);
    }

    /**
     * Damages, ignites and knocks back every entity within {@code radius} of the centre, scaled linearly
     * by distance and blocked by line-of-sight obstruction. Server-side only ({@code hurtServer}).
     */
    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius, float maxDamage) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        DamageSource source = world.damageSources().explosion(null, null);

        for (Entity e : list) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            double entX = e.getX();
            double entY = e.getY() + e.getEyeHeight();
            double entZ = e.getZ();

            if (isObstructed(world, x, y, z, entX, entY, entZ, e)) continue;

            double damage = maxDamage * (radius - dist) / radius;
            boolean doKnockback = Compat.hurt(e, serverLevel, source, (float) damage);

            Effects.ignite(e, 5);

            if (doKnockback) {
                Vec3 knock = new Vec3(entX - x, entY - y, entZ - z).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(knock.x * 0.2D, knock.y * 0.2D, knock.z * 0.2D));
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
     * Small, server-only remnant of the old nuclear wasteland conversion. It deliberately changes only
     * natural terrain and never creates fallout layers, weather, nuclear entities, or blast effects.
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

    // --- ntm-next entity port (merge_explosion_helpers.py) ---
    // ntm-next's wasteDest/waste/wasteNoSchrab (the 1.7 ore/plank/schrabidium transmutation sweep) are not
    // carried: they resolve against ntm-next's ore/waste block set, which this port lays out differently;
    // transformWaste above is this port's own sweep and the burning FOEQ calls it instead.


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
        List<Entity> list = world.getEntities(null, box);
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
                doKnockback = EntityDamageUtil.attackEntityFromNT(living, source, (float) damage, true, true, 0, 100F,
                        0);
                // 1.7:88 gores only what the blast KILLED; balefire/TOM re-damage every tick
                if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
            } else if (world instanceof ServerLevel server) {
                Compat.hurt(e, server, source, (float) damage);
            }

            dev.golem.core.world.Effects.ignite(e, 5F);

            if (doKnockback) {
                double knockX = e.getX() - x;
                double knockY = e.getY() + e.getEyeHeight() - y;
                double knockZ = e.getZ() - z;
                double len = Math.sqrt(knockX * knockX + knockY * knockY + knockZ * knockZ);
                if (len != 0D) {
                    knockX /= len;
                    knockY /= len;
                    knockZ /= len;
                    e.setDeltaMovement(e.getDeltaMovement().add(knockX * 0.2D, knockY * 0.2D, knockZ * 0.2D));
                }
            }
        }
    }
}
