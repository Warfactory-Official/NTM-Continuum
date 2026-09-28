package com.hbm.handler;

import dev.golem.core.world.Effects;

import dev.golem.core.world.Compat;
import java.util.Iterator;
import java.util.Random;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.EntityHbmProps.ContaminationEffect;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.armor.ArmorDNT;
import com.hbm.items.gear.JetpackGlider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.EntityPropsSyncPayload;
import dev.golem.core.net.GolemNetwork;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Per-living-entity tick logic (radiation sickness, contamination effects, digamma, lung disease),
 * ported from 1.12.2's {@code EntityEffectHandler}. Driven by the {@code MixinLivingEntity} tick
 * injection — the cross-loader stand-in for Forge's {@code LivingUpdateEvent} (runs on both sides).
 *
 * <p>Deferred vs the original: all particle effects (vomit/sweat/radiation-flash — the HBM particle
 * system isn't ported), the crater-biome ambient radiation, oil/pollution/temperature
 * handlers (fields persist but the loops are stubbed), and the un-ported mob transforms (nuclear
 * creeper / quackos / RAD beast).
 */
public class EntityEffectHandler {

    private static final int SYNC_INTERVAL = 5;

    public static void onUpdate(LivingEntity entity) {

        // 1.7 ran the armor-module loop in the same living update, just before EntityEffectHandler.
        ArmorModEvents.onLivingUpdate(entity);

        // Placed first to prevent picking up transient radiation from the handlers below.
        handleRadiationEffect(entity);

        // Full-set armour. Runs on both sides because the footstep sound is client-side and the charge
        // drain is server-side; this is the port's stand-in for 1.12's PlayerTickEvent + onArmorTick.
        if (entity instanceof Player player) handleArmor(player);

        if (!entity.level().isClientSide()) {

            if (entity.tickCount % 20 == 0) {
                HbmLivingProps.setRadBuf(entity, HbmLivingProps.getRadEnv(entity));
                HbmLivingProps.setRadEnv(entity, 0);
            }

            HazardSystem.tickEntity(entity);

            handleContamination(entity);
            handleRadiation(entity);
            handleDigamma(entity);
            handleLungDisease(entity);
            handleContagion(entity);
            handleFire(entity);

            if (entity instanceof ServerPlayer player && entity.tickCount % SYNC_INTERVAL == 0) {
                syncToPlayer(player);
            }
        }
    }

    /**
     * The full-set-bonus tick: the chestplate's effects and footsteps, then each worn piece's own tick
     * (a powered suit's idle drain, an unpowered one's geiger click).
     *
     * <p>1.12.2 split this across {@code TickEvent.PlayerTickEvent} (the set bonus) and
     * {@code Item.onArmorTick} (per piece). Neither has a cross-loader equivalent here — 26.1's
     * {@code inventoryTick} is server-only — so both ride the {@code MixinLivingEntity} tick, which runs
     * on both sides exactly as the two Forge hooks did.
     */
    private static void handleArmor(Player player) {
        ArmorFSB.handleTick(player);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorFSB piece) {
                piece.armorTick(player.level(), player, stack);
            }
            if (ArmorModHandler.hasMods(stack)) {
                for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                    ItemStack before = mod.copy();
                    if (mod.getItem() instanceof JetpackBase jetpack) {
                        jetpack.armorTick(player.level(), player, mod);
                        JetpackHandler.resetFlightTime(player);
                        if (!ItemStack.matches(before, mod)) ArmorModHandler.applyMod(stack, mod);
                    } else if (mod.getItem() instanceof JetpackGlider glider) {
                        glider.tickFlight(player, mod);
                        if (!ItemStack.matches(before, mod)) ArmorModHandler.applyMod(stack, mod);
                    }
                }
            }
        }
        ArmorDNT.updateSprintSpeed(player);
        com.hbm.items.ModArmorSetBItems.tickPlayer(player); // steam/diesel modifiers, mining helmet, beta
        ArmorDashHandler.tick(player); // 1.7 handleDashing: bismuth suit / dash modules
        if (player instanceof ServerPlayer serverPlayer) JetpackHandler.serverTick(serverPlayer);
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * The three burns HBM tracks separately from vanilla fire: they ignore fire resistance, tick down
     * on their own clock and are staggered by entity id so a crowd does not all take damage on the
     * same tick. Water puts ordinary fire out; white phosphorus and balefire keep going.
     */
    private static void handleFire(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel server)) return;

        EntityHbmProps props = HbmLivingProps.getData(entity);
        if (entity.fireImmune()) {
            props.setFire(0);
            props.setPhosphorus(0);
        }
        if (entity.isInWaterOrRain()) props.setFire(0);

        DamageSource source = server.damageSources().onFire();
        if (props.getFire() > 0) {
            props.setFire(props.getFire() - 1);
            if ((entity.tickCount + entity.getId()) % 40 == 0) Compat.hurt(entity, server, source, 2F);
        }
        if (props.getPhosphorus() > 0) {
            props.setPhosphorus(props.getPhosphorus() - 1);
            if ((entity.tickCount + entity.getId()) % 40 == 0) Compat.hurt(entity, server, source, 5F);
        }
        if (props.getBalefire() > 0) {
            props.setBalefire(props.getBalefire() - 1);
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    ContaminationType.CREATIVE, 5F);
            if ((entity.tickCount + entity.getId()) % 20 == 0) Compat.hurt(entity, server, source, 5F);
        }
    }

    private static void handleContamination(LivingEntity entity) {
        Iterator<ContaminationEffect> iterator = HbmLivingProps.getCont(entity).iterator();
        while (iterator.hasNext()) {
            ContaminationEffect con = iterator.next();
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    con.ignoreArmor ? ContaminationType.RAD_BYPASS : ContaminationType.CREATIVE, con.getRad());

            con.time--;
            if (con.time <= 0) iterator.remove();
        }
    }

    private static void handleRadiation(LivingEntity entity) {

        if (ContaminationUtil.isRadImmune(entity))
            return;

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        double offset = com.hbm.handler.radiation.ChunkRadiationManager.proxy.getRadiation(level, pos);
        double radD = Math.max(0D, offset);
        if (radD > 0D) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, radD / 20D);
        }

        // Fallout rain radiation is deferred with the fallout system.

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        // Vomit/sweat particle bursts (>600/>200/>900 rads) deferred with the particle system; the
        // hunger + vomit-sound tick is kept.
        Random rand = new Random(entity.getId());
        int r600 = rand.nextInt(600);
        int r1200 = rand.nextInt(1200);
        long time = level.getGameTime();

        if (HbmLivingProps.getRadiation(entity) > 600 && (time + r600) % 600 < 20 && canVomit(entity)) {
            if ((time + r600) % 600 == 1 && level instanceof ServerLevel) {
                level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                Effects.add(entity, MobEffects.HUNGER, 60, 19);
            }
        } else if (HbmLivingProps.getRadiation(entity) > 200 && (time + r1200) % 1200 < 20 && canVomit(entity)) {
            if ((time + r1200) % 1200 == 1 && level instanceof ServerLevel) {
                level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                Effects.add(entity, MobEffects.HUNGER, 60, 19);
            }
        }
    }

    private static boolean canVomit(LivingEntity entity) {
        return true; // old check excluded skeletons/etc via a mouth flag; kept simple
    }

    private static void handleRadiationEffect(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!GeneralConfig.enableRads) return;
        if (entity instanceof ServerPlayer player && player.isSpectator()) return;

        double eRad = HbmLivingProps.getRadiation(entity);
        if (eRad < 50) return;
        int rng = level.getRandom().nextInt(21000);
        DamageSource radiation = ModDamageSources.radiation(serverLevel);

        // Mob transforms (only those whose target mob is vanilla / ported)
        if (eRad >= 50 && entity instanceof Cow cow && !(entity instanceof MushroomCow)) {
            convert(cow, EntityType.MOOSHROOM, serverLevel);
            return;
        } else if (eRad >= 500 && entity instanceof Villager villager) {
            convert(villager, EntityType.ZOMBIE_VILLAGER, serverLevel);
            return;
        } else if (eRad >= 800 && entity instanceof Horse horse) {
            convert(horse, EntityType.ZOMBIE_HORSE, serverLevel);
            return;
        }

        if (eRad < 200) return;
        if (eRad > 2500000) HbmLivingProps.setRadiation(entity, 2500000);

        if (eRad >= 1000) {
            Compat.hurt(entity, serverLevel, radiation, 1000F);
            HbmLivingProps.setRadiation(entity, 0);
            if (entity.getHealth() > 0) {
                entity.setHealth(0);
                entity.die(radiation);
            }
            // 1.7 achRadDeath
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) com.hbm.advancement.HbmCriteria.RADIATION.fire(player, "death");
        } else if (eRad >= 800) {
            if (rng % 300 == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 300 == 50) Effects.add(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rng % 300 == 100) Effects.add(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rng % 500 == 0) Effects.add(entity, MobEffects.POISON, 3 * 20, 2);
            if (rng % 700 == 0) Effects.add(entity, MobEffects.WITHER, 3 * 20, 1);
            if (rng % 300 == 150) Effects.add(entity, MobEffects.HUNGER, 5 * 20, 3);
            if (rng % 300 == 200) Effects.add(entity, MobEffects.MINING_FATIGUE, 5 * 20, 3);
        } else if (eRad >= 600) {
            if (rng % 300 == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 300 == 50) Effects.add(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rng % 300 == 100) Effects.add(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rng % 500 == 0) Effects.add(entity, MobEffects.POISON, 3 * 20, 1);
            if (rng % 300 == 150) Effects.add(entity, MobEffects.HUNGER, 3 * 20, 3);
            if (rng % 400 == 0) Effects.add(entity, MobEffects.MINING_FATIGUE, 6 * 20, 2);
        } else if (eRad >= 400) {
            if (rng % 300 == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 500 == 50) Effects.add(entity, MobEffects.SLOWNESS, 5 * 20, 0);
            if (rng % 300 == 100) Effects.add(entity, MobEffects.WEAKNESS, 5 * 20, 1);
            if (rng % 500 == 150) Effects.add(entity, MobEffects.HUNGER, 3 * 20, 2);
            if (rng % 600 == 0) Effects.add(entity, MobEffects.MINING_FATIGUE, 4 * 20, 1);
        } else if (eRad >= 200) {
            if (rng % 300 == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 20, 0);
            if (rng % 500 == 0) Effects.add(entity, MobEffects.WEAKNESS, 5 * 20, 0);
            if (rng % 700 == 0) Effects.add(entity, MobEffects.HUNGER, 3 * 20, 2);
            if (rng % 800 == 0) Effects.add(entity, MobEffects.MINING_FATIGUE, 4 * 20, 0);
            // 1.7 achRadPoison: only the 200-400 band grants it, exactly as 1.7's else-if chain did
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) com.hbm.advancement.HbmCriteria.RADIATION.fire(player, "poison");
        }
    }

    private static void convert(Mob from, EntityType<? extends Mob> toType, ServerLevel level) {
        Mob to = dev.golem.core.registry.Entities.createForConversion(toType, level);
        if (to == null) return;
        to.snapTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        if (from.isAlive() && level.addFreshEntity(to)) {
            from.discard();
        }
    }

    private static void handleDigamma(LivingEntity entity) {
        double digamma = HbmLivingProps.getDigamma(entity);
        if (digamma < 0.01F) return;
        // Sweat particle burst deferred (needs the HBM particle system).
    }

    /**
     * 1.7 {@code handleContagion}, the MKU infection, only while {@code /ntmserver ENABLE_MKU} is on: an infected player
     * marks a random unstackable item (1 in 100 an armour piece) {@code ntm_contagion}, a healthy one holding a marked
     * item catches it (3 hours) unless wearing hazmat 2 with a bacteria-proof helmet; from 2:55 left the infected
     * spread it once a second to every living within 2 blocks (16 when wet) and mark dropped items; from 2:00 rare
     * nausea, from 1:00 nausea and weakness V, from 0:30 1 damage (1 in 400), from 0:05 2 damage (1 in 100), and at
     * 0 a 1000-damage death. The blood vomit particle waits for the particle system; its sound plays.
     */
    private static void handleContagion(LivingEntity entity) {
        if (!com.hbm.config.ServerConfig.ENABLE_MKU.get()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        net.minecraft.util.RandomSource rand = entity.getRandom();
        int minute = 60 * 20;
        int hour = 60 * minute;
        int contagion = HbmLivingProps.getContagion(entity);

        if (entity instanceof Player player) {
            int randSlot = rand.nextInt(36);
            ItemStack stack = player.getInventory().getItem(randSlot);
            if (rand.nextInt(100) == 0) {
                stack = player.getItemBySlot(MKU_ARMOR_SLOTS[rand.nextInt(4)]);
            }
            // only affect unstackables (e.g. tools and armor) so that the NBT tag's stack restrictions isn't noticeable
            if (!stack.isEmpty() && stack.getMaxStackSize() == 1) {
                if (contagion > 0) {
                    com.hbm.tileentity.machine.TileEntityMachineRadiolysis.CONTAGION.set(stack, true);
                } else if (com.hbm.tileentity.machine.TileEntityMachineRadiolysis.CONTAGION.getOr(stack, false)) {
                    if (!mkuProof(player)) HbmLivingProps.setContagion(player, 3 * hour);
                }
            }
        }

        if (contagion > 0) {
            HbmLivingProps.setContagion(entity, contagion - 1);

            // aerial transmission only happens once a second 5 minutes into the contagion
            if (contagion < (2 * hour + 55 * minute) && contagion % 20 == 0) {
                double range = entity.isInWaterOrRain() ? 16D : 2D; // avoid rain, just avoid it
                for (net.minecraft.world.entity.Entity ent : level.getEntities(entity, entity.getBoundingBox().inflate(range))) {
                    if (ent instanceof LivingEntity living) {
                        if (HbmLivingProps.getContagion(living) <= 0 && !mkuProof(living)) {
                            HbmLivingProps.setContagion(living, 3 * hour);
                        }
                    }
                    if (ent instanceof net.minecraft.world.entity.item.ItemEntity item) {
                        ItemStack stack = item.getItem().copy();
                        com.hbm.tileentity.machine.TileEntityMachineRadiolysis.CONTAGION.set(stack, true);
                        item.setItem(stack);
                    }
                }
            }

            // one hour in, add rare and subtle screen fuckery
            if (contagion < 2 * hour && rand.nextInt(1000) == 0) {
                Effects.add(entity, MobEffects.NAUSEA, 20, 0);
            }
            // two hours in, give 'em the full blast
            if (contagion < hour && rand.nextInt(100) == 0) {
                Effects.add(entity, MobEffects.NAUSEA, 60, 0);
                Effects.add(entity, MobEffects.WEAKNESS, 300, 4);
            }
            // T-30 minutes, take damage every 20 seconds
            if (contagion < 30 * minute && rand.nextInt(400) == 0) {
                Compat.hurt(entity, level, ModDamageSources.mku(level), 1F);
            }
            // T-5 minutes, take damage every 5 seconds
            if (contagion < 5 * minute && rand.nextInt(100) == 0) {
                Compat.hurt(entity, level, ModDamageSources.mku(level), 2F);
            }
            if (contagion < 30 * minute && (contagion + entity.getId()) % 200 == 19 && canVomit(entity)) {
                level.playSound(null, entity.blockPosition(), ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            // end of contagion, drop dead
            if (contagion == 0) {
                Compat.hurt(entity, level, ModDamageSources.mku(level), 1000F);
            }
        }
    }

    private static final net.minecraft.world.entity.EquipmentSlot[] MKU_ARMOR_SLOTS = {
            net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD};

    /** 1.7: hazmat 2 and a bacteria-proof helmet keep the MKU out ("liable to change to hazmat 1 at bob's pleasure"). */
    private static boolean mkuProof(LivingEntity living) {
        return ArmorUtil.checkForHaz2(living) && com.hbm.util.ArmorRegistry.hasProtection(living,
                net.minecraft.world.entity.EquipmentSlot.HEAD, com.hbm.util.ArmorRegistry.HazardClass.BACTERIA);
    }

    private static void handleLungDisease(LivingEntity entity) {

        if (entity instanceof Player player && player.isCreative()) {
            HbmLivingProps.setBlackLung(entity, 0);
            HbmLivingProps.setAsbestos(entity, 0);
            return;
        }

        int bl = HbmLivingProps.getBlackLung(entity);
        if (bl > 0 && bl < EntityHbmProps.MAX_BLACKLUNG * 0.25)
            HbmLivingProps.setBlackLung(entity, HbmLivingProps.getBlackLung(entity) - 1);

        double blacklung = Math.min(HbmLivingProps.getBlackLung(entity), EntityHbmProps.MAX_BLACKLUNG);
        double asbestos = Math.min(HbmLivingProps.getAsbestos(entity), EntityHbmProps.MAX_ASBESTOS);

        boolean coughs = blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.25D || asbestos / EntityHbmProps.MAX_ASBESTOS > 0.25D;
        if (!coughs) return;

        double blacklungDelta = 1D - (blacklung / (double) EntityHbmProps.MAX_BLACKLUNG);
        double asbestosDelta = 1D - (asbestos / (double) EntityHbmProps.MAX_ASBESTOS);
        double total = 1 - (blacklungDelta * asbestosDelta);

        if (total > 0.8D) {
            Effects.add(entity, MobEffects.WEAKNESS, 100, 6);
            Effects.add(entity, MobEffects.NAUSEA, 100, 0);
            Effects.add(entity, MobEffects.SLOWNESS, 100, 3);
            if (entity.getRandom().nextInt(250) == 0) Effects.add(entity, MobEffects.POISON, 100, 2);
        } else if (total > 0.65D) {
            Effects.add(entity, MobEffects.WEAKNESS, 100, 2);
            Effects.add(entity, MobEffects.SLOWNESS, 100, 2);
            if (entity.getRandom().nextInt(500) == 0) Effects.add(entity, MobEffects.POISON, 100, 0);
        } else if (total > 0.45D) {
            Effects.add(entity, MobEffects.WEAKNESS, 100, 1);
            Effects.add(entity, MobEffects.SLOWNESS, 100, 1);
        } else if (total > 0.25D) {
            Effects.add(entity, MobEffects.SLOWNESS, 100, 0);
        }

        // Coughing-blood and coughing-coal particle bursts stay deferred with the particle system.
        Level level = entity.level();
        int freq = Math.max((int) (1000 - 950 * total), 20);
        if (level instanceof ServerLevel && level.getGameTime() % freq == entity.getId() % freq) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.COUGH.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void syncToPlayer(ServerPlayer player) {
        EntityHbmProps props = HbmLivingProps.getData(player);
        ByteBuf buf = Unpooled.buffer();
        props.serialize(buf);
        byte[] data = ByteBufUtil.getBytes(buf);
        buf.release();
        GolemNetwork.sendToPlayer(player, EntityPropsSyncPayload.CODEC, new EntityPropsSyncPayload(data));
    }
}
