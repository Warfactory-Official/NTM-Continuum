package com.hbm.handler;

import dev.golem.core.world.Explosions;
import com.hbm.Constants;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.data.RadiationDataMaps;
import com.hbm.api.data.RadiationDataMaps.BiomeRadiation;
import com.hbm.api.data.RadiationDataMaps.Transformation;
import com.hbm.api.environment.Environment;
import com.hbm.config.ServerConfig;
import com.hbm.config.WorldConfig;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.particle.helper.LivingEffects;
import com.hbm.potion.ModEffects;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;
import com.hbm.util.ArmorRegistry;
import com.hbm.world.biome.ModBiomes;
import dev.golem.core.Ids;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Effects;

import dev.golem.core.world.Compat;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

/**
 * Per-living-entity tick logic (radiation sickness, contamination effects, digamma, lung disease, oil,
 * pollution, HBM's own burns, the time bomb), ported from 1.7.10's {@code EntityEffectHandler}. Driven by the
 * {@code MixinLivingEntity} tick injection, the cross-loader stand-in for Forge's {@code LivingUpdateEvent}
 * (runs on both sides).
 *
 * <p>The vomit / sweat / radiation-flash / cough / oil-drip / digamma particle bursts
 * are sent as 1.7 sent them, through {@code LivingEffects} and the {@code HbmEffectNT} types {@code Sweat},
 * {@code Vomit} and {@code Radiation} (client: {@code LivingEffectParticles}, 150..199-tick vanilla block dust as in
 * 1.7). The nuclear creeper and Quackos transforms and the crater-biome and nether ambient radiation
 * are handled here too.
 */
public class EntityEffectHandler {

    private static final int SYNC_INTERVAL = 5;
    private static final Set<String> BAD_RESULTS = ConcurrentHashMap.newKeySet();

    public static void onUpdate(LivingEntity entity) {

        // 1.7 ran the armor-module loop in the same living update, just before EntityEffectHandler.
        ArmorModEvents.onLivingUpdate(entity);

        // Placed first to prevent picking up transient radiation from the handlers below.
        handleRadiationEffect(entity);

        // Full-set armour. Runs on both sides because the footstep sound is client-side and the charge
        // drain is server-side; this is the port's stand-in for 1.12's PlayerTickEvent + onArmorTick.
        if (entity instanceof Player player) handleArmor(player);

        if (entity.level().isClientSide() && entity instanceof Player player && player.isLocalPlayer()) {
            craterAura(player);
            radiationFlash(player);
        }

        if (!entity.level().isClientSide()) {

            if (entity.tickCount % 20 == 0) {
                HbmLivingProps.setRadBuf(entity, HbmLivingProps.getRadEnv(entity));
                HbmLivingProps.setRadEnv(entity, 0);
            }

            // 1.7 X528_enable528NetherBurn (forced off without 528 mode): players burn in the nether, 1.7 setFire(5)
            if (GeneralConfig.enable528NetherBurn && entity instanceof Player
                    && !entity.fireImmune() && entity.level().dimension() == Level.NETHER) {
                Effects.ignite(entity, 5);
            }

            craterBiomeRadiation(entity);

            // hbm-space ran handleOxy and handleCorrosion here, after the biome radiation: addons that give entities an
            // atmosphere to breathe listen on com.hbm.api.environment.Environment.LIVING_TICK
            Environment.livingTick(entity);

            HazardSystem.tickEntity(entity);

            handleContamination(entity);
            handleRadiation(entity);
            handleDigamma(entity);
            handleLungDisease(entity);
            handleContagion(entity);
            handleOil(entity);
            handlePollution(entity);
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
     * {@code Item.onArmorTick} (per piece). Neither has a cross-loader equivalent here, since 26.1's
     * {@code inventoryTick} is server-only, so both ride the {@code MixinLivingEntity} tick, which runs
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
        ModArmorSetBItems.tickPlayer(player); // steam/diesel modifiers, mining helmet, beta
        ArmorDashHandler.tick(player); // 1.7 handleDashing: bismuth suit / dash modules
        SpecialPlayers.tick(player); // 1.7 onWingFlop + PU RADIATION (ShadyUtil players)
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
        if (!entity.isAlive()) return; // 1.7 handleTemperature: isEntityAlive

        EntityHbmProps props = HbmLivingProps.getData(entity);
        if (entity.fireImmune()) {
            props.setFire(0);
            props.setPhosphorus(0);
        }
        if (entity.isInWaterOrRain()) props.setFire(0);

        DamageSource source = server.damageSources().onFire();
        int stagger = entity.tickCount + entity.getId();
        if (props.getFire() > 0) {
            props.setFire(props.getFire() - 1);
            if (stagger % 15 == 0) fizz(server, entity);
            if (stagger % 40 == 0) Compat.hurt(entity, server, source, 2F);
            flame(server, entity, FlameCreator.META_FIRE);
        }
        if (props.getPhosphorus() > 0) {
            props.setPhosphorus(props.getPhosphorus() - 1);
            if (stagger % 15 == 0) fizz(server, entity);
            if (stagger % 40 == 0) Compat.hurt(entity, server, source, 5F);
            flame(server, entity, FlameCreator.META_FIRE);
        }
        if (props.getBalefire() > 0) {
            props.setBalefire(props.getBalefire() - 1);
            if (stagger % 15 == 0) fizz(server, entity);
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    ContaminationType.CREATIVE, 5F);
            if (stagger % 20 == 0) Compat.hurt(entity, server, source, 5F);
            flame(server, entity, FlameCreator.META_BALEFIRE);
        }
        // 1.7 blackFire (the 358.00 "BL" black flame): 5 RAD a tick and 10 fire damage every 10 ticks
        if (props.getBlackFire() > 0) {
            props.setBlackFire(props.getBlackFire() - 1);
            if (stagger % 10 == 0) fizz(server, entity);
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    ContaminationType.CREATIVE, 5F);
            if (stagger % 10 == 0) Compat.hurt(entity, server, source, 10F);
            flame(server, entity, FlameCreator.META_BLACK);
        }
        if ((props.getFire() > 0 || props.getPhosphorus() > 0 || props.getBalefire() > 0 || props.getBlackFire() > 0)
                && !entity.isAlive()) {
            ConfettiUtil.decideConfetti(entity, source);
        }
    }

    /** 1.7 "random.fizz" at the entity's middle, pitch 1.5..2. */
    private static void fizz(ServerLevel level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1F,
                1.5F + entity.getRandom().nextFloat() * 0.5F);
    }

    /** 1.7 {@code FlameCreator.composeEffect} at a random point inside the entity's box. */
    private static void flame(ServerLevel level, LivingEntity entity, int meta) {
        RandomSource rand = entity.getRandom();
        float width = entity.getBbWidth();
        FlameCreator.composeEffect(level,
                entity.getX() - width / 2 + width * rand.nextDouble(),
                entity.getY() + rand.nextDouble() * entity.getBbHeight(),
                entity.getZ() - width / 2 + width * rand.nextDouble(), meta);
    }

    /**
     * 1.7 {@code handleOil}: a doused entity counts the oil down, and if it catches fire the oil goes up in a
     * strength-3 block-breaking explosion at its middle. Every fifth tick of its life it drips one coal-block dust
     * particle (1.7 {@code sweat}, 25 blocks).
     */
    private static void handleOil(LivingEntity entity) {
        int oil = HbmLivingProps.getOil(entity);
        if (oil <= 0) return;
        if (entity.isOnFire()) {
            HbmLivingProps.setOil(entity, 0);
            Explosions.explode(entity.level(), null, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 3F,
                    false, Level.ExplosionInteraction.TNT);
        } else {
            HbmLivingProps.setOil(entity, oil - 1);
        }
        if (entity.tickCount % 5 == 0) {
            LivingEffects.sweat(entity, 1, Blocks.COAL_BLOCK, 25D);
        }
    }

    /**
     * 1.7 {@code handlePollution}, every 60 ticks at eye level: poison above 10 (poison I below 25, II below 50,
     * wither III above) unless the helmet stops blistering gas; heavy metal above 25 gives the lead effect (I below
     * 50, III above; 1.7 gives III twice) unless it stops fine particles.
     */
    private static void handlePollution(LivingEntity entity) {
        if (!RadiationConfig.enablePollution) return;
        if (entity.tickCount % 60 != 0) return;
        BlockPos eye = BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());

        if (RadiationConfig.enablePoison && !ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD,
                ArmorRegistry.HazardClass.GAS_BLISTERING)) {
            float poison = PollutionHandler.getPollution(entity.level(), eye,
                    PollutionType.POISON);
            if (poison > 10) {
                if (poison < 25) Effects.add(entity, MobEffects.POISON, 100, 0);
                else if (poison < 50) Effects.add(entity, MobEffects.POISON, 100, 1);
                else Effects.add(entity, MobEffects.WITHER, 100, 2);
            }
        }

        if (RadiationConfig.enableLeadPoisoning && !ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD,
                ArmorRegistry.HazardClass.PARTICLE_FINE)) {
            float metal = PollutionHandler.getPollution(entity.level(), eye,
                    PollutionType.HEAVYMETAL);
            if (metal > 25) {
                Effects.addRegistered(entity, ModEffects.LEAD.get(), 100, metal < 50 ? 0 : 2);
            }
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

    /**
     * 1.7 {@code onUpdate}: an entity standing in a crater biome takes the biome's ambient radiation (outer 0.5,
     * crater 5, inner 25 RAD/s; times 5 when wet) as {@code CREATIVE} contamination, so immune and creative
     * entities are skipped by {@code contaminate} itself. A direct dose, not a chunk-radiation source, as in 1.7.
     * 1.7 read the 2D biome at the feet; the port reads the biome at the entity's block.
     */
    private static void craterBiomeRadiation(LivingEntity entity) {
        float radiation = craterBiomeRad(entity.level().getBiome(entity.blockPosition()), entity.isInWaterOrRain()); // 1.7 isWet

        if (radiation > 0) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, radiation / 20F);
        }
    }

    /** The biome's ambient RAD/s: its {@code hbm:ambient_radiation} entry, else 1.7's crater config values, else 0. */
    public static float craterBiomeRad(Holder<Biome> biome, boolean wet) {
        BiomeRadiation data = RadiationDataMaps.BIOME_RADIATION.get(biome);
        if (data != null) return wet ? data.radiation() * data.wetMultiplier() : data.radiation();
        float radiation = 0;
        if (biome.is(ModBiomes.crater_outer)) radiation = WorldConfig.craterBiomeOuterRad;
        if (biome.is(ModBiomes.crater)) radiation = WorldConfig.craterBiomeRad;
        if (biome.is(ModBiomes.crater_inner)) radiation = WorldConfig.craterBiomeInnerRad;
        if (wet) radiation *= WorldConfig.craterBiomeWaterMult;
        return radiation;
    }

    /** 1.7 {@code onUpdate}, client: three {@code townaura} (mycelium) motes a tick around the local player in a crater or inner crater. */
    private static void craterAura(Player player) {
        var biome = player.level().getBiome(player.blockPosition());
        if (!biome.is(ModBiomes.crater) && !biome.is(ModBiomes.crater_inner)) return;
        var rand = player.getRandom();
        for (int i = 0; i < 3; i++) {
            player.level().addParticle(ParticleTypes.MYCELIUM,
                    player.getX() + rand.nextGaussian() * 3, player.getY() + rand.nextGaussian() * 2,
                    player.getZ() + rand.nextGaussian() * 3, 0, 0, 0);
        }
    }

    /**
     * 1.7 {@code handleRadiationFX}, client branch: a player above 600 RAD sees 1 (above 800: 2, above 900: 4) cyan
     * flashes a tick ({@code effectNT} type {@code radiation}, played locally). The port syncs radiation only to the
     * owning client, as 1.7 did, so this runs for the local player.
     */
    private static void radiationFlash(Player player) {
        float radiation = (float) HbmLivingProps.getRadiation(player);
        if (radiation <= 600) return;
        CompoundTag data = new CompoundTag();
        data.putInt("count", radiation > 900 ? 4 : radiation > 800 ? 2 : 1);
        HbmEffectNT.Radiation.summonParticle(player.level(), player.getX(), player.getY(),
                player.getZ(), data);
    }

    private static void handleRadiation(LivingEntity entity) {

        if (ContaminationUtil.isRadImmune(entity))
            return;

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        double offset = ChunkRadiationManager.proxy.getRadiation(level, pos);
        // 1.7 handleRadiationFX: the nether's ambient floor (hellRad, 0.1 RAD/s) under the chunk value
        if (level.dimension() == Level.NETHER && RadiationConfig.hellRad > 0 && offset < RadiationConfig.hellRad)
            offset = RadiationConfig.hellRad;
        Double dimension = RadiationDataMaps.DIMENSION_RADIATION.byId().get(Ids.keyId(level.dimension()).toString());
        if (dimension != null && offset < dimension) offset = dimension;
        // hbm-space: unshielded sunlight on its planets and in orbit raises the floor further
        // (com.hbm.api.environment.Environment.AMBIENT_RADIATION; none on plain HBM)
        float floor = Environment.ambientRadiationFloor(level, pos, entity);
        if (floor > 0F && floor > offset) offset = floor;
        double radD = Math.max(0D, offset);
        if (radD > 0D) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, radD / 20D);
        }

        // Fallout rain radiation is deferred with the fallout system.

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        // 1.7 handleRadiationFX: above 600 RAD a 20-tick blood-vomit burst every 600 ticks, else above 200 a normal
        // one every 1200 (1.7 nests the 600 window inside "> 600", so a >600 entity never takes the 1200 branch);
        // above 900 a redstone sweat particle on about one tick in ten. Hunger and the sound on the window's tick 1.
        Random rand = new Random(entity.getId());
        int r600 = rand.nextInt(600);
        int r1200 = rand.nextInt(1200);
        long time = level.getGameTime();

        if (HbmLivingProps.getRadiation(entity) > 600) {
            if ((time + r600) % 600 < 20 && canVomit(entity)) {
                LivingEffects.vomit(entity, "blood", 25);
                if ((time + r600) % 600 == 1 && level instanceof ServerLevel) {
                    level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    Effects.add(entity, MobEffects.HUNGER, 60, 19);
                }
            }
        } else if (HbmLivingProps.getRadiation(entity) > 200 && (time + r1200) % 1200 < 20 && canVomit(entity)) {
            LivingEffects.vomit(entity, "normal", 15);
            if ((time + r1200) % 1200 == 1 && level instanceof ServerLevel) {
                level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                Effects.add(entity, MobEffects.HUNGER, 60, 19);
            }
        }

        if (HbmLivingProps.getRadiation(entity) > 900 && (time + rand.nextInt(10)) % 10 == 0) {
            LivingEffects.sweat(entity, 1,
                    Blocks.REDSTONE_BLOCK, 25D);
        }
    }

    /** 1.7 {@code canVomit}: everything but water creatures. */
    private static boolean canVomit(LivingEntity entity) {
        return entity.getType().getCategory() != MobCategory.WATER_CREATURE;
    }

    private static void handleRadiationEffect(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!GeneralConfig.enableRads) return;
        if (!entity.isAlive()) return;
        // 1.7: creative players are skipped entirely (the port also skips spectators)
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        double eRad = HbmLivingProps.getRadiation(entity);
        if (eRad <= 0) return;
        DamageSource radiation = ModDamageSources.radiation(serverLevel);
        RandomSource rand = level.getRandom();

        // read before 1.7's 50 RAD cut-off, which every 1.7 threshold is above
        Transformation transformation = RadiationDataMaps.TRANSFORMATIONS.get(entity.getType());
        if (transformation != null && eRad >= transformation.threshold() && entity instanceof Mob mob) {
            if (transformation.chance() <= 1 || rand.nextInt(transformation.chance()) == 0) {
                convert(mob, transformation.result(), serverLevel);
            } else if (transformation.fallbackDamage() > 0F) {
                Compat.hurt(entity, serverLevel, radiation, transformation.fallbackDamage());
            }
            return;
        }

        if (eRad < 200 || ContaminationUtil.isRadImmune(entity)) return;
        // 1.7 caps a sick entity at 2500 (the 1.12.2 fork had raised this to 2,500,000)
        if (eRad > 2500) HbmLivingProps.setRadiation(entity, 2500);

        // 1.7 EntityEffectHandler:234-269: an independent world.rand roll per effect, and no hunger or mining
        // fatigue (the 1.12.2 fork had added both to every band).
        if (eRad >= 1000) {
            Compat.hurt(entity, serverLevel, radiation, 1000F);
            HbmLivingProps.setRadiation(entity, 0);
            // a player survives this only through a death-preventing item (a totem), which must keep working
            if (entity.getHealth() > 0 && !(entity instanceof Player)) {
                entity.setHealth(0);
                entity.die(radiation);
            }
            // 1.7 achRadDeath
            if (entity instanceof ServerPlayer player && entity.getHealth() <= 0) {
                HbmCriteria.RADIATION.fire(player, "death");
            }
        } else if (eRad >= 800) {
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rand.nextInt(500) == 0) Effects.add(entity, MobEffects.POISON, 3 * 20, 2);
            if (rand.nextInt(700) == 0) Effects.add(entity, MobEffects.WITHER, 3 * 20, 1);
        } else if (eRad >= 600) {
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rand.nextInt(500) == 0) Effects.add(entity, MobEffects.POISON, 3 * 20, 1);
        } else if (eRad >= 400) {
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rand.nextInt(500) == 0) Effects.add(entity, MobEffects.SLOWNESS, 5 * 20, 0);
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.WEAKNESS, 5 * 20, 1);
        } else {
            if (rand.nextInt(300) == 0) Effects.add(entity, MobEffects.NAUSEA, 5 * 20, 0);
            if (rand.nextInt(500) == 0) Effects.add(entity, MobEffects.WEAKNESS, 5 * 20, 0);
            // 1.7 achRadPoison: only the 200-400 band grants it, exactly as 1.7's else-if chain did
            if (entity instanceof ServerPlayer player) HbmCriteria.RADIATION.fire(player, "poison");
        }
    }

    private static void convert(Mob from, String toType, ServerLevel level) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Ids.parse(toType)).orElse(null);
        if (type == null || !(Entities.createForConversion(type, level) instanceof Mob to)) {
            if (BAD_RESULTS.add(toType)) Constants.LOG.warn("Radiation transformation: {} is not a mob type", toType);
            return;
        }
        to.snapTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        if (from.isAlive() && level.addFreshEntity(to)) {
            from.discard();
        }
    }

    private static void handleDigamma(LivingEntity entity) {
        double digamma = HbmLivingProps.getDigamma(entity);
        if (digamma < 0.01F) return;
        // 1.7 handleDigamma: one soul-sand sweat particle, on every tick from 9 digamma up, else 1 in (10 - digamma)
        int chance = Math.max(10 - (int) digamma, 1);
        if (chance == 1 || entity.getRandom().nextInt(chance) == 0) {
            LivingEffects.sweat(entity, 1, Blocks.SOUL_SAND, 25D);
        }
    }

    /**
     * 1.7 {@code handleContagion}, the MKU infection, only while {@code /ntmserver ENABLE_MKU} is on: an infected player
     * marks a random unstackable item (1 in 100 an armour piece) {@code ntm_contagion}, a healthy one holding a marked
     * item catches it (3 hours) unless wearing hazmat 2 with a bacteria-proof helmet; from 2:55 left the infected
     * spread it once a second to every living within 2 blocks (16 when wet) and mark dropped items; from 2:00 rare
     * nausea, from 1:00 nausea and weakness V, from 0:30 1 damage (1 in 400), from 0:05 2 damage (1 in 100), and at
     * 0 a 1000-damage death. From 0:30 a 20-tick blood-vomit burst every 200 ticks, with its sound on the last tick.
     */
    private static void handleContagion(LivingEntity entity) {
        if (!ServerConfig.ENABLE_MKU.get()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        RandomSource rand = entity.getRandom();
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
                    TileEntityMachineRadiolysis.CONTAGION.set(stack, true);
                } else if (TileEntityMachineRadiolysis.CONTAGION.getOr(stack, false)) {
                    if (!mkuProof(player)) HbmLivingProps.setContagion(player, 3 * hour);
                }
            }
        }

        if (contagion > 0) {
            HbmLivingProps.setContagion(entity, contagion - 1);

            // aerial transmission only happens once a second 5 minutes into the contagion
            if (contagion < (2 * hour + 55 * minute) && contagion % 20 == 0) {
                double range = entity.isInWaterOrRain() ? 16D : 2D; // avoid rain, just avoid it
                for (Entity ent : level.getEntities(entity, entity.getBoundingBox().inflate(range))) {
                    if (ent instanceof LivingEntity living) {
                        if (HbmLivingProps.getContagion(living) <= 0 && !mkuProof(living)) {
                            HbmLivingProps.setContagion(living, 3 * hour);
                        }
                    }
                    if (ent instanceof ItemEntity item) {
                        ItemStack stack = item.getItem().copy();
                        TileEntityMachineRadiolysis.CONTAGION.set(stack, true);
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
            if (contagion < 30 * minute && (contagion + entity.getId()) % 200 < 20 && canVomit(entity)) {
                LivingEffects.vomit(entity, "blood", 25);
                if ((contagion + entity.getId()) % 200 == 19)
                    level.playSound(null, entity.blockPosition(), ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            // end of contagion, drop dead
            if (contagion == 0) {
                Compat.hurt(entity, level, ModDamageSources.mku(level), 1000F);
            }
        }
    }

    private static final EquipmentSlot[] MKU_ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    /** 1.7: hazmat 2 and a bacteria-proof helmet keep the MKU out ("liable to change to hazmat 1 at bob's pleasure"). */
    private static boolean mkuProof(LivingEntity living) {
        return ArmorUtil.checkForHaz2(living) && ArmorRegistry.hasProtection(living,
                EquipmentSlot.HEAD, ArmorRegistry.HazardClass.BACTERIA);
    }

    /**
     * 1.7 {@code handleLungDisease}. Black lung below half the maximum heals a tick at a time; the effects follow the
     * combined lung damage (weakness III above 75%, nausea above 95%), and the cough frequency also follows a
     * player's soot at eye level (above 30 soot a player coughs even with healthy lungs) unless the helmet stops
     * coarse particles. Each cough also sends 1.7's coughed-blood and coughed-coal-smoke bursts.
     */
    private static void handleLungDisease(LivingEntity entity) {

        if (entity instanceof Player player && player.isCreative()) {
            HbmLivingProps.setBlackLung(entity, 0);
            HbmLivingProps.setAsbestos(entity, 0);
            return;
        }

        int bl = HbmLivingProps.getBlackLung(entity);
        if (bl > 0 && bl < EntityHbmProps.MAX_BLACKLUNG * 0.5)
            HbmLivingProps.setBlackLung(entity, HbmLivingProps.getBlackLung(entity) - 1);

        double blacklung = Math.min(HbmLivingProps.getBlackLung(entity), EntityHbmProps.MAX_BLACKLUNG);
        double asbestos = Math.min(HbmLivingProps.getAsbestos(entity), EntityHbmProps.MAX_ASBESTOS);
        double soot = 0;
        if (entity instanceof Player && !ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD,
                ArmorRegistry.HazardClass.PARTICLE_COARSE)) {
            soot = PollutionHandler.getPollution(entity.level(),
                    BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()),
                    PollutionType.SOOT);
        }

        boolean coughs = blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.25D || asbestos / EntityHbmProps.MAX_ASBESTOS > 0.25D
                || soot > 30;
        if (!coughs) return;

        double blacklungDelta = 1D - (blacklung / (double) EntityHbmProps.MAX_BLACKLUNG);
        double asbestosDelta = 1D - (asbestos / (double) EntityHbmProps.MAX_ASBESTOS);
        double sootDelta = 1D - Math.min(soot / 100, 1D);
        double total = 1 - (blacklungDelta * asbestosDelta);

        if (total > 0.75D) Effects.add(entity, MobEffects.WEAKNESS, 100, 2);
        if (total > 0.95D) Effects.add(entity, MobEffects.NAUSEA, 100, 0);

        total = 1 - (blacklungDelta * asbestosDelta * sootDelta);
        Level level = entity.level();
        int freq = Math.max((int) (1000 - 950 * total), 20);
        if (level instanceof ServerLevel && level.getGameTime() % freq == entity.getId() % freq) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.COUGH.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            // 1.7: blood above 75% asbestos or black lung, coal smoke above 50% black lung (50 puffs above 80%)
            if (asbestos / EntityHbmProps.MAX_ASBESTOS > 0.75D || blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.75D) {
                LivingEffects.vomit(entity, "blood", 5);
            }
            if (blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.5D) {
                LivingEffects.vomit(entity, "smoke",
                        blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.8D ? 50 : 10);
            }
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
