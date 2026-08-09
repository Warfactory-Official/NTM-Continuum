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

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }

    public static DamageSource radiation(Level level) { return source(level, RADIATION); }
    public static DamageSource digamma(Level level) { return source(level, DIGAMMA); }
    public static DamageSource monoxide(Level level) { return source(level, MONOXIDE); }
    public static DamageSource asbestos(Level level) { return source(level, ASBESTOS); }
    public static DamageSource blacklung(Level level) { return source(level, BLACKLUNG); }
}
