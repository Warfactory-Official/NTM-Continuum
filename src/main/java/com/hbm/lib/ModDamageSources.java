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
    /**
     * The shockwave a power-armour wearer makes on landing hard (old {@code ArmorFSB.handleFall}).
     * Upstream built this as {@code causePlayerDamage(player).setDamageBypassesArmor()}, so it is
     * attributed to the wearer and carries the {@code bypasses_armor} tag.
     */
    public static final ResourceKey<DamageType> HARD_LANDING = key("hard_landing");
    public static final ResourceKey<DamageType> TAINT = key("taint");
    public static final ResourceKey<DamageType> BANG = key("bang");
    public static final ResourceKey<DamageType> LEAD = key("lead");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF = key("euthanized_self");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF2 = key("euthanized_self2");
    public static final ResourceKey<DamageType> OVERDOSE = key("overdose");

    /**
     * The damage categories a bullet can belong to, ported from 1.12.2's
     * {@code DamageResistanceHandler.DamageClass}. Upstream turns the enum's name into a damage-source
     * string at hit time and then sets projectile/fire/explosion flags on it; here each class is a real
     * {@code hbm:bullet_<name>} damage type, and those three flags are the vanilla {@code is_projectile}
     * / {@code is_fire} / {@code is_explosion} type tags.
     *
     * <p>The classes carry no resistance behaviour yet — the damage-resistance system they exist to feed
     * is not ported — so today they differ only in death message and vanilla flags.
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
    public static DamageSource taint(Level level) { return source(level, TAINT); }
    public static DamageSource bang(Level level) { return source(level, BANG); }
    public static DamageSource lead(Level level) { return source(level, LEAD); }
    public static DamageSource euthanizedSelf(Level level) { return source(level, EUTHANIZED_SELF); }
    public static DamageSource euthanizedSelf2(Level level) { return source(level, EUTHANIZED_SELF2); }
    public static DamageSource overdose(Level level) { return source(level, OVERDOSE); }
}
