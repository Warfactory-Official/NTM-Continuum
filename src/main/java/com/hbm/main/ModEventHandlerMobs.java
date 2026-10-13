package com.hbm.main;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBurningFOEQ;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.items.ModItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.util.MobUtil;
import com.hbm.util.ShadyUtil;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Deaths;
import dev.golem.core.world.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The mob parts of 1.7 {@code ModEventHandler}: the rest of {@code onEntityDeath} (radiation reset,
 * {@code enableCataclysm}, the HbMinecraft book, the hostile-mob key/launch-code drops and the zombie ingots; the
 * module items' share is in {@link com.hbm.handler.ArmorModEvents} and the boxcar achievement in
 * {@link com.hbm.entity.mob.EntityCreeperTainted}), {@code decorateMob} and {@code addAITasks}.
 */
public final class ModEventHandlerMobs {

    private ModEventHandlerMobs() {}

    public static void init() {
        Deaths.install();
        Deaths.onDeath(ModEventHandlerMobs::onEntityDeath);
        // 1.7 decorateMob listens to LivingSpawnEvent, whose CheckSpawn fires before a natural spawn is placed and before
        // onSpawnWithEgg (so vanilla's own equipment roll still runs after it). The check never refuses a spawn.
        // Patrols, phantoms and wandering traders (WORLD_SPAWNER) have no 1.7 counterpart that reached decorateMob.
        Spawns.onSpawnCheck((mob, level, reason) -> {
            if (reason == Spawns.Reason.WORLD_SPAWNER) return false;
            decorateMob(mob, level, mob.getRandom());
            return false;
        });
    }

    /** 1.7 {@code onEntityDeath} (the parts not ported elsewhere). */
    public static void onEntityDeath(LivingEntity entity, DamageSource source) {
        HbmLivingProps.setRadiation(entity, 0);
        if (!(entity.level() instanceof ServerLevel level)) return;

        if (GeneralConfig.enableCataclysm) {
            EntityBurningFOEQ foeq = new EntityBurningFOEQ(ModNtmEntities.BURNING_FOEQ.get(), level);
            foeq.snapTo(entity.getX(), 500, entity.getZ(), 0.0F, 0.0F);
            level.addFreshEntity(foeq);
        }

        // 1.7 also matches any entity whose name is "HbMinecraft" (a name tag will do)
        if (ShadyUtil.is(entity, ShadyUtil.HbMinecraft, "HbMinecraft")) {
            drop(entity, level, ModMeleeKitItems.book_of_.get());
        }

        if (!Compat.mobLoot(level)) return;
        if (!(source.getEntity() instanceof Player player) || Platform.isFakePlayer(player)) return;
        for (Item item : playerKillDrops(entity, entity.getRandom())) drop(entity, level, item);
    }

    /**
     * The player-kill rolls of 1.7 {@code onEntityDeath} not made by ArmorModEvents (spider_milk, serum, bandaid,
     * heart_piece and wd40 are), in 1.7 order: key_red_cracked and launch_code_piece 1/250 each from any hostile mob,
     * and a copper, aluminium and titanium ingot 1/200 each from a zombie.
     */
    public static List<Item> playerKillDrops(LivingEntity entity, RandomSource rng) {
        List<Item> drops = new ArrayList<>();
        if (entity instanceof Enemy) {
            if (rng.nextInt(250) == 0) drops.add(ModItems.key_red_cracked.get());
            if (rng.nextInt(250) == 0) drops.add(ModItems.launch_code_piece.get());
        }

        // 1.7 EntityZombie also covers zombie pigmen and zombie villagers; the modern Zombie class covers them and the
        // husk and drowned.
        if (entity instanceof Zombie) {
            if (rng.nextInt(200) == 0) drops.add(ModItems.ingot_copper.get());
            if (rng.nextInt(200) == 0) drops.add(ModItems.ingot_aluminium.get());
            if (rng.nextInt(200) == 0) drops.add(ModItems.ingot_titanium.get());
        }
        return drops;
    }

    private static void drop(LivingEntity entity, ServerLevel level, Item item) {
        // 1.7 dropItem(item, 1) = entityDropItem(stack, 0F)
        Compat.spawnAtLocation(entity, level, new ItemStack(item), 0F);
    }

    /**
     * 1.7 {@code decorateMob}: natural zombies and skeletons draw the soot-mob equipment pools; a zombie in heavy soot
     * may be a full hazmat "zombine", a skeleton may get a gun.
     */
    public static void decorateMob(LivingEntity entity, ServerLevel world, RandomSource rand) {
        if (!MobConfig.enableMobGear || entity.isBaby()) return;

        float soot = PollutionHandler.getPollution(world,
                BlockPos.containing(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ())),
                PollutionType.SOOT);

        Map<Integer, List<MobUtil.Choice>> slotPools;
        if (entity instanceof Zombie) {
            if (rand.nextFloat() < 0.005F && soot > 2) { // full hazmat zombine
                MobUtil.equipFullSet(entity, "hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots");
                return;
            }
            slotPools = MobUtil.slotPoolCommonS;
        } else if (entity instanceof AbstractSkeleton) {
            // 1.7 puts the held-item pool into the shared slotPoolRangedS map; it is rebuilt before every use, so a
            // per-call copy behaves the same.
            slotPools = new TreeMap<>(MobUtil.slotPoolRangedS);
            String gun = getSkelegun(soot, rand);
            slotPools.put(0, gun != null ? MobUtil.pool(50, gun.substring("hbm:".length()), 1) : MobUtil.pool(50));
        } else {
            return;
        }

        MobUtil.assignItemsToEntity(entity, slotPools, rand);
    }

    /**
     * 1.7 {@code getSkelegun}. Odd but kept: {@code log(soot) * 0.25} is negative below a soot of 1, so the pepperbox
     * branch can never be reached, and a soot of exactly 0.3 falls through to the 1.0 pool.
     */
    public static @Nullable String getSkelegun(float soot, RandomSource rand) {
        if (!MobConfig.enableMobWeapons) return null;

        soot -= (float) MobConfig.mobWeaponSootReduction;
        if (rand.nextDouble() > Math.log(soot) * 0.25) return null;

        List<MobUtil.Choice> pool = new ArrayList<>();
        if (soot < 0.3) {
            pool.add(new MobUtil.Choice("hbm:gun_pepperbox", 5));
            pool.add(new MobUtil.Choice(null, 20));
        } else if (soot > 0.3 && soot < 1) {
            pool.addAll(MobUtil.slotPoolGuns.get(0.3));
        } else if (soot < 3) {
            pool.addAll(MobUtil.slotPoolGuns.get(1D));
        } else if (soot < 5) {
            pool.addAll(MobUtil.slotPoolGuns.get(3D));
        } else {
            pool.addAll(MobUtil.slotPoolGuns.get(5D));
        }

        MobUtil.Choice selected = MobUtil.draw(pool, rand);
        return selected == null ? null : selected.id();
    }

    /**
     * Called at the end of every living tick (MixinLivingEntity).
     *
     * <p>1.7 {@code addAITasks} (EntityJoinWorldEvent): a mob holding a gun gets the gun task. Goals are not saved, so
     * the first tick after the mob is created or loaded stands in for joining the world.
     *
     * <p>1.7 {@code decorateMob} also listens to {@code LivingSpawnEvent.AllowDespawn}, which Forge 1.7.10 posts from
     * {@code despawnEntity} every 32nd tick of {@code entityAge} for a mob that may despawn; {@code entityAge} is reset
     * while a player is within 32 blocks. So zombies and skeletons left alone re-draw their equipment every 32 ticks -
     * odd, and the only path by which a skeleton keeps a gun (on a natural spawn vanilla's bow overwrites it). The
     * modern counter is {@code noActionTime}, reset the same way by {@code checkDespawn}.
     */
    public static void onLivingUpdate(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level)) return;

        if (mob.tickCount == 1 && mob.getMainHandItem().getItem() instanceof ItemGunBaseNT) {
            MobUtil.addFireTask(mob);
        }

        if ((mob instanceof Zombie || mob instanceof AbstractSkeleton) && !mob.isPersistenceRequired()
                && (mob.getNoActionTime() & 0x1F) == 0x1F) {
            decorateMob(mob, level, level.getRandom());
        }
    }
}
