package com.hbm.handler;

import dev.golem.core.hook.Hook;
import java.util.function.BiConsumer;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.armor.ArmorLiquidator;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ItemModRevive;
import com.hbm.items.armor.ItemModShackles;
import com.hbm.items.armor.ItemModShield;
import com.hbm.items.armor.ItemModTwoKick;
import com.hbm.items.weapon.MobSlicer;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.sound.ModGunSounds;
import com.hbm.util.InventoryUtil;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Armors;
import dev.golem.core.world.Attacks;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Compat;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Deaths;
import dev.golem.core.world.Durability;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Gliders;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The armor-module halves of 1.7 {@code ModEventHandler}: the living update (module ticks, module
 * hazards, module attribute modifiers), the player shield, the revive modules on death, the ballistic
 * gauntlet on a punch and the module items' rare mob drops.
 *
 * <p>Loader routes: the living update is {@code EntityEffectHandler.onUpdate} (the living-tick mixin),
 * the shield sits in {@code DamageResistanceHandler}'s hurt handler ahead of the module damage hooks as
 * in 1.7, and death/punch come from Golem Core's {@link Deaths} and {@link Attacks}.
 */
public final class ArmorModEvents {

    private ArmorModEvents() {}

    /** 1.7 {@code getEquipmentInSlot(1..4)}: boots, legs, chest, helmet. */
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD};

    /** After the existing real-player, mob-loot and module-drop rules have accepted this death. */
    public static final Hook<BiConsumer<LivingEntity, Player>> KILLED_BY_PLAYER =
            Hook.notifying();

    private static boolean installed;

    public static void init() {
        if (installed) return;
        installed = true;
        Deaths.install();
        Attacks.install();
        Deaths.onAllowDeath(ArmorModEvents::onAllowDeath);
        Deaths.onDeath(ArmorModEvents::onDeath);
        Attacks.onPlayerAttack(ArmorModEvents::onPlayerAttack);
        // port: an elytra installed in a chestplate glides like a worn one (ArmorModHandler.isElytra)
        Gliders.register(new Gliders.Source() {
            @Override
            public boolean canGlide(LivingEntity entity) {
                return Gliders.hasFlightLeft(
                        ArmorModHandler.installedElytra(entity.getItemBySlot(EquipmentSlot.CHEST)));
            }

            @Override
            public void wear(LivingEntity entity) {
                ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
                ItemStack elytra = ArmorModHandler.installedElytra(chest);
                if (elytra.isEmpty()) return;
                Durability.hurt(elytra, 1, entity, EquipmentSlot.CHEST);
                if (!elytra.isEmpty()) ArmorModHandler.applyMod(chest, elytra);
            }
        });
        Gliders.registerWings(chest -> ArmorModHandler.installedElytra(chest));
    }


    // Living update

    /** 1.7 {@code onLivingUpdate}'s armor-module loop plus the shield regeneration. Both sides. */
    public static void onLivingUpdate(LivingEntity entity) {
        boolean server = !entity.level().isClientSide();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            // Runs for every living entity every tick on both sides: the lists are only allocated when a
            // modded or self-modifying suit can actually fill them (reconcileModifiers never keeps them).
            List<ItemArmorMod.Modifier> wanted = List.of();
            ArmorSlot armorSlot = Armors.slotOf(armor); // 1.7: any ItemArmor

            if (armorSlot != null && ArmorModHandler.hasMods(armor)) {
                if (server) wanted = new ArrayList<>();
                for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                    if (!(mod.getItem() instanceof ItemArmorMod armorMod)) continue;
                    if (server && armorMod.ticksServer()) armorMod.modUpdate(entity, armor);
                    else if (!server && armorMod.ticksClient()) armorMod.modUpdateClient(entity, armor);
                    if (server) {
                        HazardSystem.applyHazards(mod, entity);
                        if (armorMod.hasAttributes()) armorMod.collectModifiers(armor, armorSlot, wanted);
                    }
                }
            }
            if (server) {
                reconcileModifiers(entity, "hbm:armor_mod_" + slot.getName() + "_", wanted);
                // 1.7 kept the suit's own item attributes under separate fixedUUIDs.
                List<ItemArmorMod.Modifier> own = List.of();
                if (armor.getItem() instanceof ArmorLiquidator liquidator) {
                    own = new ArrayList<>();
                    liquidator.collectModifiers(own);
                }
                reconcileModifiers(entity, "hbm:armor_fixed_" + slot.getName() + "_", own);
            }
        }
        if (server && entity instanceof Player player) regenerateShield(player);
    }

    /** Modifier ids this handler has put on each entity, so unmodded entities cost no attribute work. */
    private static final Map<LivingEntity, Set<String>> APPLIED = new WeakHashMap<>();

    /**
     * Keeps exactly the wanted modifiers under one id prefix. 1.7 swapped modifier maps when the worn stack
     * changed; asserting the wanted set every tick reaches the same state, and {@link Attributes#setTransient}
     * leaves an unchanged modifier alone so nothing resyncs. Server thread only.
     */
    private static void reconcileModifiers(LivingEntity entity, String prefix, List<ItemArmorMod.Modifier> wanted) {
        Set<String> applied = APPLIED.get(entity);
        if (wanted.isEmpty() && applied == null) return;
        if (applied == null) APPLIED.put(entity, applied = new HashSet<>());

        Set<String> keep = new HashSet<>();
        for (ItemArmorMod.Modifier modifier : wanted) {
            AttributeInstance attribute = attribute(entity, modifier.attribute());
            if (attribute == null) continue;
            String id = prefix + modifier.attribute();
            Attributes.setTransient(attribute, id, modifier.amount(), modifier.op());
            keep.add(id);
            applied.add(id);
        }
        for (Iterator<String> it = applied.iterator(); it.hasNext(); ) {
            String id = it.next();
            if (!id.startsWith(prefix) || keep.contains(id)) continue;
            String name = id.substring(prefix.length());
            Attributes.removeTransient(attribute(entity, name), id);
            it.remove();
            if (ItemArmorMod.Modifier.MAX_HEALTH.equals(name) && entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
        if (applied.isEmpty()) APPLIED.remove(entity);
    }

    private static AttributeInstance attribute(LivingEntity entity, String name) {
        return switch (name) {
            case ItemArmorMod.Modifier.MAX_HEALTH ->
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            case ItemArmorMod.Modifier.KNOCKBACK_RESISTANCE ->
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            case ItemArmorMod.Modifier.ATTACK_DAMAGE ->
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            case ItemArmorMod.Modifier.MOVEMENT_SPEED ->
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            default -> null;
        };
    }

    // Shield

    /** 1.7 {@code HbmPlayerProps.getEffectiveMaxShield}: base shield plus a Life Extender insert. */
    public static float effectiveMaxShield(Player player) {
        float max = HbmLivingProps.getData(player).maxShield;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            ItemStack insert = ArmorModHandler.pryMod(chest, ArmorModHandler.kevlar);
            if (insert.getItem() instanceof ItemModShield shield) max += shield.shield;
        }
        return max;
    }

    /** 1.7 {@code EntityEffectHandler}: regenerates after 60 quiet ticks, ramping 0.005 per tick waited. */
    private static void regenerateShield(Player player) {
        EntityHbmProps props = HbmLivingProps.getData(player);
        float max = effectiveMaxShield(player);
        if (props.shield < max && player.tickCount > props.lastDamage + 60) {
            int tsd = player.tickCount - (props.lastDamage + 60);
            props.shield += Math.min(max - props.shield, 0.005F * tsd);
        }
        if (props.shield > max) props.shield = max;
    }

    /** 1.7 {@code onEntityDamaged}'s first block: the shield soaks damage and resets the regen delay. */
    public static void absorbShield(DamageEvents.DamageEvent event) {
        if (!(event.entity() instanceof Player player)) return;
        event.setAmount(PlayerShield.absorb(player, event.amount()));
    }

    // Sounds

    public static final int PLINK_COOLDOWN_TICKS = 10;
    private static final Map<Player, Long> PLINK = new WeakHashMap<>();

    /** Remaining time in the same sound throttle used by actual armor interception. */
    public static int plinkCooldown(Player player) {
        var last = PLINK.get(player);
        return last == null ? 0 : (int) Math.max(0L, PLINK_COOLDOWN_TICKS - (player.level().getGameTime() - last));
    }

    /** 1.7 {@code HbmPlayerProps.plink}: a sound with a 10-tick per-player cooldown. */
    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        long now = player.level().getGameTime();
        Long last = PLINK.get(player);
        if (last == null || now - last >= PLINK_COOLDOWN_TICKS) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                    volume, pitch);
            PLINK.put(player, now);
        }
    }

    // Death

    /** 1.7 {@code onEntityDeathFirst} (highest priority): scrumpy/whiskey and shackle revives. */
    private static boolean onAllowDeath(LivingEntity entity, DamageSource source) {
        // 1.12.2: "if a person with shackles is killed by crucible, he will be dead AND alive at the same time"
        if (MobSlicer.killedByMobSlicer(entity)) return true;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!Armors.isArmor(stack) || !ArmorModHandler.hasMods(stack)) continue;

            ItemStack revive = ArmorModHandler.pryMod(stack, ArmorModHandler.extra);
            if (revive.isEmpty()) continue;

            if (revive.getItem() instanceof ItemModRevive) {
                revive.setDamageValue(revive.getDamageValue() + 1);
                if (revive.getDamageValue() >= revive.getMaxDamage()) {
                    ArmorModHandler.removeMod(stack, ArmorModHandler.extra);
                } else {
                    ArmorModHandler.applyMod(stack, revive);
                }
                entity.setHealth(entity.getMaxHealth());
                Effects.add(entity, Effects.resistance(), 60, 99);
                return false;
            }

            if (revive.getItem() instanceof ItemModShackles && HbmLivingProps.getRadiation(entity) < 1000F) {
                int dmg = ItemModShackles.revives(revive) + 1;
                ItemModShackles.setRevives(revive, dmg);
                ArmorModHandler.applyMod(stack, revive);
                entity.setHealth(entity.getMaxHealth());
                HbmLivingProps.incrementRadiation(entity, dmg * dmg);
                return false;
            }
        }
        return true;
    }

    /** The module items' share of 1.7 {@code onEntityDeath}'s player-kill mob loot. */
    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !Compat.mobLoot(level)) return;
        // 1.7 excludes Forge's FakePlayer
        if (!(source.getEntity() instanceof Player killer) || Platform.isFakePlayer(killer)) return;
        RandomSource rng = entity.getRandom();

        // 1.7's cave spider extended its spider, so both roll the milk. Compared by entity type because
        // 26.1 moved both spider classes into a subpackage.
        boolean caveSpider = entity.getType() == EntityType.CAVE_SPIDER;
        if ((entity.getType() == EntityType.SPIDER || caveSpider) && rng.nextInt(500) == 0)
            drop(entity, level, ModArmorModItems.spider_milk.get());
        if (caveSpider && rng.nextInt(100) == 0) drop(entity, level, ModArmorModItems.serum.get());
        if (entity instanceof Animal && rng.nextInt(500) == 0) drop(entity, level, ModArmorModItems.bandaid.get());
        if (entity instanceof Enemy && rng.nextInt(1000) == 0) drop(entity, level, ModArmorModItems.heart_piece.get());
        if (entity instanceof EntityCyberCrab && rng.nextInt(500) == 0) drop(entity, level, ModArmorModItems.wd40.get());
        for (var listener : KILLED_BY_PLAYER) listener.accept(entity, killer);
    }

    private static void drop(LivingEntity entity, ServerLevel level, Item item) {
        Compat.spawnAtLocation(entity, level, new ItemStack(item));
    }

    // Ballistic gauntlet

    /** 1.7 {@code onPlayerPunch}: a bare-handed punch fires the first 12 gauge round found. */
    private static void onPlayerAttack(Player player, Entity target) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty() || !ArmorModHandler.hasMods(chestplate)) return;
        if (ItemStacks.hasAttackDamageModifier(player.getMainHandItem())) return;

        ItemStack servo = ArmorModHandler.pryMod(chestplate, ArmorModHandler.servos);
        if (!(servo.getItem() instanceof ItemModTwoKick)) return;

        BulletConfig fired = null;
        BulletConfig[] configs = {XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum, XFactory12ga.g12_bp_slug,
                XFactory12ga.g12, XFactory12ga.g12_slug, XFactory12ga.g12_flechette, XFactory12ga.g12_magnum,
                XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus};
        for (BulletConfig config : configs) {
            if (config == null || config.ammoItem() == null) continue;
            if (InventoryUtil.doesPlayerHaveAStacks(player, List.of(new ComparableStack(config.ammoItem(), 1)), true)) {
                fired = config;
                break;
            }
        }
        if (fired == null) return;

        int bullets = fired.projectilesMin;
        if (fired.projectilesMax > fired.projectilesMin) {
            bullets += player.getRandom().nextInt(fired.projectilesMax - fired.projectilesMin);
        }
        for (int i = 0; i < bullets; i++) {
            EntityBulletBaseMK4 mk4 = new EntityBulletBaseMK4(player, fired, 15F, 0F, -0.1875, -0.0625, 0.5);
            player.level().addFreshEntity(mk4);
            if (i == 0 && fired.blackPowder) {
                var motion = mk4.getDeltaMovement();
                ParticleEffects.blackPowder(player.level(), mk4.getX(), mk4.getY(), mk4.getZ(),
                        motion.x, motion.y, motion.z, 10, 0.25F, 0.5F, 10, 0.25F);
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModGunSounds.FIRE_SPAS.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
