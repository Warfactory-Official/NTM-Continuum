package com.hbm.lib;

import com.hbm.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * HBM damage sources, ported from 1.12.2's {@code ModDamageSource}. Damage types are data-driven in
 * 26.1: each key here has a matching {@code data/hbm/damage_type/<id>.json}, and the old builder flags
 * ({@code setDamageBypassesArmor()} / {@code setDamageIsAbsolute()} / creative) are vanilla damage-type
 * <b>tags</b> ({@code minecraft:bypasses_armor}, {@code bypasses_effects}, {@code bypasses_invulnerability})
 * in {@code data/minecraft/tags/damage_type/}. Death messages: {@code death.attack.<message_id>} lang keys.
 *
 * <p>{@code DamageSources.source(...)} is private in 26.1, so sources are built from the public
 * {@link DamageSource} constructor via the registry holder.
 */
public final class ModDamageSources {

    private ModDamageSources() {}

    /** bypasses armor (old: {@code setDamageBypassesArmor()}) */
    public static final ResourceKey<DamageType> RADIATION = key("radiation");
    /** absolute + bypasses armor + hits creative (old digamma) */
    public static final ResourceKey<DamageType> DIGAMMA = key("digamma");
    /** absolute + bypasses armor */
    public static final ResourceKey<DamageType> MONOXIDE = key("monoxide");
    /** absolute + bypasses armor */
    public static final ResourceKey<DamageType> ASBESTOS = key("asbestos");
    /** absolute + bypasses armor */
    public static final ResourceKey<DamageType> BLACKLUNG = key("blacklung");
    /** The BJ helmet's self-ejection failure (old {@code ModDamageSource.lunar}). */
    public static final ResourceKey<DamageType> LUNAR = key("lunar");
    /** Absolute, armour-bypassing pull damage from an active black hole. */
    public static final ResourceKey<DamageType> BLACKHOLE = key("blackhole");
    /** Projectile damage from a tumbling block fragment. */
    public static final ResourceKey<DamageType> RUBBLE = key("rubble");
    /** Projectile damage from the old shrapnel entity. */
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    /** Impaled on spikes (old {@code ModDamageSource.spikes}: bypasses armour). */
    public static final ResourceKey<DamageType> SPIKES = key("spikes");
    /** The sawmill's operating blade: absolute and bypasses armor (old damage id: {@code blender}). */
    public static final ResourceKey<DamageType> TURBOFAN = key("blender");
    /**
     * The shockwave a power-armour wearer makes on landing hard (old {@code ArmorFSB.handleFall}).
     * Upstream built this as {@code causePlayerDamage(player).setDamageBypassesArmor()}, so it is
     * attributed to the wearer and carries the {@code bypasses_armor} tag.
     */
    public static final ResourceKey<DamageType> HARD_LANDING = key("hard_landing");
    public static final ResourceKey<DamageType> TAINT = key("taint");
    public static final ResourceKey<DamageType> BANG = key("bang");
    /** absolute + bypasses armor (old: {@code setDamageIsAbsolute().setDamageBypassesArmor()}) */
    public static final ResourceKey<DamageType> ELECTRICITY = key("electricity");
    /** absolute + bypasses armor — the MASER turret's emitter */
    public static final ResourceKey<DamageType> MICROWAVE = key("microwave");
    public static final ResourceKey<DamageType> LEAD = key("lead");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF = key("euthanized_self");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF2 = key("euthanized_self2");
    public static final ResourceKey<DamageType> OVERDOSE = key("overdose");
    /** Scalded by a hot fluid — a mist cloud above 100 °C. Carries {@code is_fire}. */
    public static final ResourceKey<DamageType> BOIL = key("boil");
    /** Frozen by a cryogenic fluid — a mist cloud below −20 °C. */
    public static final ResourceKey<DamageType> ICE = key("ice");
    /** Eaten by a corrosive fluid. Absolute, and it damages the whole suit on the way through. */
    public static final ResourceKey<DamageType> ACID = key("acid");

    // --- ntm-next entity port (gen_entity_damage_types.py) ---
    /** ntm-next cloud: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> CLOUD = key("cloud");
    /** ntm-next pink_cloud: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> PINK_CLOUD = key("pink_cloud");

    // --- ntm-next entity port (gen_entity_damage_types.py) ---
    /** ntm-next acid_player: no tags. */
    public static final ResourceKey<DamageType> ACID_PLAYER = key("acid_player");
    /** ntm-next boat: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> BOAT = key("boat");
    /** ntm-next boxcar: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> BOXCAR = key("boxcar");
    /** ntm-next building: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> BUILDING = key("building");
    /** ntm-next combine: bypasses_armor, is_projectile. */
    public static final ResourceKey<DamageType> COMBINE = key("combine");
    /** ntm-next electric: no tags. */
    public static final ResourceKey<DamageType> ELECTRIC = key("electric");
    /** ntm-next emplacer: is_projectile. */
    public static final ResourceKey<DamageType> EMPLACER = key("emplacer");
    /** ntm-next exhaust: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> EXHAUST = key("exhaust");
    /** ntm-next explosive: is_explosion. */
    public static final ResourceKey<DamageType> EXPLOSIVE = key("explosive");
    /** ntm-next fire: is_fire. */
    public static final ResourceKey<DamageType> FIRE = key("fire");
    /** ntm-next laser: no tags. */
    public static final ResourceKey<DamageType> LASER = key("laser");
    /** ntm-next meteorite: bypasses_armor, bypasses_effects. */
    public static final ResourceKey<DamageType> METEORITE = key("meteorite");
    /** ntm-next mud_poisoning: bypasses_armor. */
    public static final ResourceKey<DamageType> MUD_POISONING = key("mud_poisoning");
    /** ntm-next nuclear_blast: is_explosion. */
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
    /** ntm-next revolver_bullet: is_projectile. */
    public static final ResourceKey<DamageType> REVOLVER_BULLET = key("revolver_bullet");
    /** ntm-next subatomic: no tags. */
    public static final ResourceKey<DamageType> SUBATOMIC = key("subatomic");
    /** ntm-next tau: bypasses_armor, is_projectile. */
    public static final ResourceKey<DamageType> TAU = key("tau");

    /**
     * The damage categories a bullet can belong to, ported from 1.12.2's
     * {@code DamageResistanceHandler.DamageClass}. Upstream turns the enum's name into a damage-source
     * string at hit time and then sets projectile/fire/explosion flags on it; here each class is a real
     * {@code hbm:bullet_<name>} damage type, and those three flags are the vanilla {@code is_projectile}
     * / {@code is_fire} / {@code is_explosion} type tags.
     *
     * <p>The classes feed {@code DamageResistanceHandler}'s source/category precedence. Their vanilla
     * projectile/fire/explosion tags remain authoritative for ordinary armor calculations.
     */
    public enum DamageClass {
        PHYSICAL, FIRE, EXPLOSIVE, ELECTRIC, PLASMA, LASER, MICROWAVE, SUBATOMIC, OTHER;

        private final ResourceKey<DamageType> type = key("bullet_" + name().toLowerCase(java.util.Locale.ROOT));

        public ResourceKey<DamageType> type() { return type; }
    }

    /**
     * The armour-bypassing counterpart used for the armour-piercing fraction of a bullet's damage.
     * A round with {@code armorPiercingPercent} of 0.4 lands 40% of its damage through this type and the
     * rest through its own class, which is what "40% of the damage ignores armour" means once there is
     * no damage-resistance layer to express it in.
     */
    public static final ResourceKey<DamageType> BULLET_PIERCING = key("bullet_piercing");

    /** A bullet hit: {@code direct} is the projectile, {@code attacker} whoever fired it (may be null). */
    public static DamageSource bullet(Level level, ResourceKey<DamageType> key,
            net.minecraft.world.entity.Entity direct, net.minecraft.world.entity.Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key),
                direct, attacker);
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, dev.golem.core.Ids.of(Constants.MOD_ID, name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }

    /** A typed source with a direct (projectile/beam) and a causing entity, the 3-arg {@code DamageSources.source} shape. */
    public static DamageSource source(Level level, ResourceKey<DamageType> key,
                                      net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity direct,
                                      net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity causing) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key), direct, causing);
    }

    /** The hard-landing shockwave, credited to the wearer whose suit made it. */
    public static DamageSource hardLanding(Level level, net.minecraft.world.entity.Entity wearer) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(HARD_LANDING), null, wearer);
    }

    public static DamageSource radiation(Level level) { return source(level, RADIATION); }
    public static DamageSource digamma(Level level) { return source(level, DIGAMMA); }
    public static DamageSource monoxide(Level level) { return source(level, MONOXIDE); }
    public static DamageSource asbestos(Level level) { return source(level, ASBESTOS); }
    public static DamageSource blacklung(Level level) { return source(level, BLACKLUNG); }
    public static DamageSource lunar(Level level) { return source(level, LUNAR); }
    public static DamageSource blackhole(Level level) { return source(level, BLACKHOLE); }
    public static DamageSource rubble(Level level) { return source(level, RUBBLE); }
    public static DamageSource turbofan(Level level) { return source(level, TURBOFAN); }
    public static DamageSource taint(Level level) { return source(level, TAINT); }
    public static DamageSource bang(Level level) { return source(level, BANG); }
    public static DamageSource shrapnel(Level level) { return source(level, SHRAPNEL); }
    public static DamageSource electricity(Level level) { return source(level, ELECTRICITY); }
    public static DamageSource microwave(Level level) { return source(level, MICROWAVE); }
    public static DamageSource lead(Level level) { return source(level, LEAD); }
    public static DamageSource euthanizedSelf(Level level) { return source(level, EUTHANIZED_SELF); }
    public static DamageSource euthanizedSelf2(Level level) { return source(level, EUTHANIZED_SELF2); }
    public static DamageSource overdose(Level level) { return source(level, OVERDOSE); }
}
