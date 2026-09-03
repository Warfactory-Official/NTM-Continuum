package com.hbm.entity.missile;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/**
 * Entity types for the missiles, under their 1.12.2 registry ids.
 *
 * <p>Two registration details are load-bearing:
 *
 * <ul>
 *   <li><b>{@code clientTrackingRange(64)}.</b> Upstream asks for 1000 blocks so a launch is visible
 *       across a base. The server clamps tracking to its view distance regardless, so 64 chunks is the
 *       honest spelling of "as far as the server will send it" — the same clamp the Torex cloud took.
 *   <li><b>{@code updateInterval(1)}.</b> The client does not simulate the flight (see
 *       {@link EntityMissileBaseNT}), so it needs a position every tick for vanilla's interpolation to
 *       draw a smooth arc. This is the setting that replaces upstream's hand-written interpolation and
 *       its tracker rotation hack.
 * </ul>
 *
 * <p>Only the missiles whose warheads are ported are registered. A missile item with no entry here is
 * simply not launchable — see {@code ModMissileItems} and IMPLEMENTATION.md §20 for what each of the
 * remaining ones is waiting on.
 */
public final class ModMissileEntities {

    private ModMissileEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Constants.MOD_ID);

    // --- tier 0 ---

    public static final RegistryObject<EntityType<EntityMissileTier0.EntityMissileMicro>> MISSILE_MICRO =
            missile("entity_missile_micro", EntityMissileTier0.EntityMissileMicro::new);
    public static final RegistryObject<EntityType<EntityMissileTier0.EntityMissileTaint>> MISSILE_TAINT =
            missile("entity_missile_taint", EntityMissileTier0.EntityMissileTaint::new);

    // --- tier 1 ---

    public static final RegistryObject<EntityType<EntityMissileTier1.EntityMissileGeneric>> MISSILE_GENERIC =
            missile("entity_missile_generic", EntityMissileTier1.EntityMissileGeneric::new);
    public static final RegistryObject<EntityType<EntityMissileTier1.EntityMissileDecoy>> MISSILE_DECOY =
            missile("entity_missile_decoy", EntityMissileTier1.EntityMissileDecoy::new);
    public static final RegistryObject<EntityType<EntityMissileTier1.EntityMissileIncendiary>> MISSILE_INCENDIARY =
            missile("entity_missile_incendiary", EntityMissileTier1.EntityMissileIncendiary::new);
    public static final RegistryObject<EntityType<EntityMissileTier1.EntityMissileBunkerBuster>> MISSILE_BUSTER =
            missile("entity_missile_bunker_buster", EntityMissileTier1.EntityMissileBunkerBuster::new);

    // --- tier 2 ---

    public static final RegistryObject<EntityType<EntityMissileTier2.EntityMissileStrong>> MISSILE_STRONG =
            missile("entity_missile_strong", EntityMissileTier2.EntityMissileStrong::new);
    public static final RegistryObject<EntityType<EntityMissileTier2.EntityMissileIncendiaryStrong>> MISSILE_INCENDIARY_STRONG =
            missile("entity_missile_incendiary_strong", EntityMissileTier2.EntityMissileIncendiaryStrong::new);
    public static final RegistryObject<EntityType<EntityMissileTier2.EntityMissileBusterStrong>> MISSILE_BUSTER_STRONG =
            missile("entity_missile_buster_strong", EntityMissileTier2.EntityMissileBusterStrong::new);
    public static final RegistryObject<EntityType<EntityMissileTier2.EntityMissileEMPStrong>> MISSILE_EMP_STRONG =
            missile("entity_missile_emp_strong", EntityMissileTier2.EntityMissileEMPStrong::new);

    // --- tier 3 ---

    public static final RegistryObject<EntityType<EntityMissileTier3.EntityMissileBurst>> MISSILE_BURST =
            missile("entity_missile_burst", EntityMissileTier3.EntityMissileBurst::new);
    public static final RegistryObject<EntityType<EntityMissileTier3.EntityMissileInferno>> MISSILE_INFERNO =
            missile("entity_missile_inferno", EntityMissileTier3.EntityMissileInferno::new);

    // --- tier 4 ---

    public static final RegistryObject<EntityType<EntityMissileTier4.EntityMissileNuclear>> MISSILE_NUCLEAR =
            missile("entity_missile_nuclear", EntityMissileTier4.EntityMissileNuclear::new);
    public static final RegistryObject<EntityType<EntityMissileTier4.EntityMissileMirv>> MISSILE_MIRV =
            missile("entity_missile_mirv", EntityMissileTier4.EntityMissileMirv::new);
    public static final RegistryObject<EntityType<EntityMissileTier4.EntityMissileDoomsday>> MISSILE_DOOMSDAY =
            missile("entity_missile_doomsday", EntityMissileTier4.EntityMissileDoomsday::new);
    public static final RegistryObject<EntityType<EntityMissileTier4.EntityMissileDoomsdayRusted>> MISSILE_DOOMSDAY_RUSTED =
            missile("entity_missile_doomsday_rusted", EntityMissileTier4.EntityMissileDoomsdayRusted::new);
    public static final RegistryObject<EntityType<EntityMissileTier4.EntityMissileN2>> MISSILE_N2 =
            missile("entity_missile_n2", EntityMissileTier4.EntityMissileN2::new);

    // --- specials ---

    public static final RegistryObject<EntityType<EntityMissileStealth>> MISSILE_STEALTH =
            missile("entity_missile_stealth", EntityMissileStealth::new);
    /** Taller and thinner than the rest: upstream sizes the interceptor 1 × 8. */
    public static final RegistryObject<EntityType<EntityMissileAntiBallistic>> MISSILE_ANTI_BALLISTIC =
            ENTITY_TYPES.register("entity_missile_ab", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.<EntityMissileAntiBallistic>of(EntityMissileAntiBallistic::new, MobCategory.MISC)
                            .sized(1.0F, 8.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1), key));

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> missile(
            String id, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(id, key -> dev.golem.core.registry.Entities.build(
                EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(1.5F, 1.5F)
                        .clientTrackingRange(64)
                        .updateInterval(1), key));
    }

    public static void init() {}
}
