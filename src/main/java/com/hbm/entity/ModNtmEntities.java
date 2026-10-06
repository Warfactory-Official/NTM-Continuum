package com.hbm.entity;

import com.hbm.Constants;
import com.hbm.entity.cart.EntityMinecartCrate;
import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.entity.cart.EntityMinecartOre;
import com.hbm.entity.cart.EntityMinecartPowder;
import com.hbm.entity.cart.EntityMinecartSemtex;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.entity.grenade.EntityGrenadeBouncyGeneric;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.grenade.EntityWastePearl;
import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.item.EntityFallingMultiblock;
import com.hbm.entity.item.EntityFireworks;
import com.hbm.entity.item.EntityItemBuoyant;
import com.hbm.entity.item.EntityItemWaste;
import com.hbm.entity.item.EntityMinecartTest;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.entity.logic.EntityC130;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityTomBlast;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.entity.missile.EntityMinerRocket;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.entity.missile.EntityMissileShuttle;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileBHole;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileEMP;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileSchrabidium;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileTest;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileCluster;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileClusterStrong;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileDrill;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileRain;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileVolcano;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.entity.mob.EntityCreeperGold;
import com.hbm.entity.mob.EntityCreeperNuclear;
import com.hbm.entity.mob.EntityCreeperPhosgene;
import com.hbm.entity.mob.EntityCreeperTainted;
import com.hbm.entity.mob.EntityCreeperVolatile;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.EntityPigeon;
import com.hbm.entity.mob.EntityPlasticBag;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBody;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrenda;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityAcidBomb;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityBuilding;
import com.hbm.entity.projectile.EntityBullet;
import com.hbm.entity.projectile.EntityBurningFOEQ;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.entity.projectile.EntityCog;
import com.hbm.entity.projectile.EntityDuchessGambit;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.entity.projectile.EntityTorpedo;
import com.hbm.entity.train.EntityRailCarBase.BoundingBoxDummyEntity;
import com.hbm.entity.train.EntityRailCarRidable.SeatDummyEntity;
import com.hbm.entity.train.TrainCargoTram;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.LivingAttributes;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Entity types brought over from ntm-next's {@code ModEntities} (ids, hitboxes, tracking ranges and
 * attribute sets as written there, themselves the 1.7 {@code EntityMappings} values). Entities the
 * port had already registered before this wave keep their existing registry classes and ids.
 *
 * <p>The seven glyphid/waypoint/acid-bomb classes that registered themselves in ntm-next keep a
 * {@code TYPE} field on the class, registered through {@link #ENTITY_TYPES}, so their callers stay as
 * copied. The entity types register through {@link Reg} over this class's own register (so the order is
 * unchanged); they carry no declarations, so the class is not a Dejson declaration class.
 */
public final class ModNtmEntities {

    private ModNtmEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.createEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ENTITY_TYPES);

    public static final RegistryObject<EntityType<EntityUndeadSoldier>> UNDEAD_SOLDIER =
            REG.entity("entity_undead_soldier", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityUndeadSoldier>of(EntityUndeadSoldier::new, MobCategory.MONSTER)).sized(.6F, 1.95F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityFBI>> FBI =
            REG.entity("entity_fbi", key -> Entities.build(
                    EntityType.Builder.<EntityFBI>of(EntityFBI::new, MobCategory.MONSTER).sized(0.6F, 1.8F).fireImmune().clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCreeperGold>> CREEPER_GOLD =
            REG.entity("entity_mob_gold_creeper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCreeperGold>of(EntityCreeperGold::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCreeperVolatile>> CREEPER_VOLATILE =
            REG.entity("entity_mob_volatile_creeper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCreeperVolatile>of(EntityCreeperVolatile::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCreeperPhosgene>> CREEPER_PHOSGENE =
            REG.entity("entity_mob_phosgene_creeper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCreeperPhosgene>of(EntityCreeperPhosgene::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCreeperTainted>> CREEPER_TAINTED =
            REG.entity("entity_mob_tainted_creeper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCreeperTainted>of(EntityCreeperTainted::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCreeperNuclear>> CREEPER_NUCLEAR =
            REG.entity("entity_mob_nuclear_creeper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCreeperNuclear>of(EntityCreeperNuclear::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityCyberCrab>> CYBER_CRAB =
            REG.entity("entity_cyber_crab", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCyberCrab>of(EntityCyberCrab::new, MobCategory.MONSTER)).sized(0.75F, 0.35F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityTeslaCrab>> TESLA_CRAB =
            REG.entity("entity_tesla_crab", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityTeslaCrab>of(EntityTeslaCrab::new, MobCategory.MONSTER)).sized(0.75F, 1.25F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityTaintCrab>> TAINT_CRAB =
            REG.entity("entity_taint_crab", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityTaintCrab>of(EntityTaintCrab::new, MobCategory.MONSTER)).sized(1.25F, 1.25F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityDuck>> DUCK =
            REG.entity("entity_fucc_a_ducc", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDuck>of(EntityDuck::new, MobCategory.CREATURE)).sized(0.3F, 0.7F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGhost>> GHOST =
            REG.entity("entity_ghost", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGhost>of(EntityGhost::new, MobCategory.MONSTER)).sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityDummy>> TEST_DUMMY =
            REG.entity("entity_test_dummy", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDummy>of(EntityDummy::new, MobCategory.CREATURE)).sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityBlockSpider>> BLOCK_SPIDER =
            REG.entity("entity_taintcrawler", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBlockSpider>of(EntityBlockSpider::new, MobCategory.MONSTER)).sized(0.95F, 1.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityQuackos>> QUACKOS =
            REG.entity("entity_elder_one", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityQuackos>of(EntityQuackos::new, MobCategory.CREATURE)).sized(7.5F, 17.5F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityPlasticBag>> PLASTIC_BAG =
            REG.entity("entity_plastic_bag", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityPlasticBag>of(EntityPlasticBag::new, MobCategory.WATER_CREATURE)).sized(0.45F, 0.45F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityPigeon>> PIGEON =
            REG.entity("entity_pigeon", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityPigeon>of(EntityPigeon::new, MobCategory.CREATURE)).sized(0.5F, 1.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGlyphid>> GLYPHID =
            REG.entity("entity_glyphid", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphid>of(EntityGlyphid::new, MobCategory.MONSTER)).sized(1.75F, 1F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGlyphidBehemoth>> GLYPHID_BEHEMOTH =
            REG.entity("entity_glyphid_behemoth", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphidBehemoth>of(EntityGlyphidBehemoth::new, MobCategory.MONSTER)).sized(2.5F, 1.5F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGlyphidBrenda>> GLYPHID_BRENDA =
            REG.entity("entity_glyphid_brenda", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphidBrenda>of(EntityGlyphidBrenda::new, MobCategory.MONSTER)).sized(2.5F, 1.75F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGlyphidScout>> GLYPHID_SCOUT =
            REG.entity("entity_glyphid_scout", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphidScout>of(EntityGlyphidScout::new, MobCategory.MONSTER)).sized(1.25F, 0.75F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityGlyphidNuclear>> GLYPHID_NUCLEAR =
            REG.entity("entity_glyphid_nuclear", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphidNuclear>of(EntityGlyphidNuclear::new, MobCategory.MONSTER)).sized(2.5F, 1.75F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityMeteor>> METEOR =
            REG.entity("entity_meteor", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMeteor>of(EntityMeteor::new, MobCategory.MISC)).fireImmune().sized(4F, 4F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityCog>> COG =
            REG.entity("entity_stray_cog", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCog>of(EntityCog::new, MobCategory.MISC)).sized(1.0F, 1.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityItemWaste>> WASTE_ITEM =
            REG.entity("waste_item", key -> Entities.build(
                    EntityType.Builder.<EntityItemWaste>of(EntityItemWaste::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(7).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityOrangeFX>> AGENT_ORANGE =
            REG.entity("entity_agent_orange", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityOrangeFX>of(EntityOrangeFX::new, MobCategory.MISC)).noSave().sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityChlorineFX>> CHLORINE_FX =
            REG.entity("entity_chlorine_fx", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityChlorineFX>of(EntityChlorineFX::new, MobCategory.MISC)).noSave().sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityCloudFX>> CLOUD_FX =
            REG.entity("entity_cloud_fx", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCloudFX>of(EntityCloudFX::new, MobCategory.MISC)).noSave().sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityPinkCloudFX>> PINK_CLOUD_FX =
            REG.entity("entity_pink_cloud_fx", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityPinkCloudFX>of(EntityPinkCloudFX::new, MobCategory.MISC)).noSave().sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityFireworks>> FIREWORK_BALL =
            REG.entity("entity_firework_ball", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityFireworks>of(EntityFireworks::new, MobCategory.MISC)).sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartOre>> CART_ORE =
            REG.entity("entity_cart_ore", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartOre>of(EntityMinecartOre::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartPowder>> CART_POWDER =
            REG.entity("entity_cart_powder", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartPowder>of(EntityMinecartPowder::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartSemtex>> CART_SEMTEX =
            REG.entity("entity_cart_semtex", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartSemtex>of(EntityMinecartSemtex::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartCrate>> CART_CRATE =
            REG.entity("entity_cart_crate", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartCrate>of(EntityMinecartCrate::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartDestroyer>> CART_DESTROYER =
            REG.entity("entity_cart_destroyer", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartDestroyer>of(EntityMinecartDestroyer::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinecartTest>> CART_TEST =
            REG.entity("entity_minecart_test", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinecartTest>of(EntityMinecartTest::new, MobCategory.MISC)).sized(0.98F, 0.7F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBoatRubber>> BOAT_RUBBER =
            REG.entity("entity_rubber_boat", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBoatRubber>of(EntityBoatRubber::new, MobCategory.MISC)).sized(1.5F, 0.6F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<TrainCargoTram>> CARGO_TRAM =
            REG.entity("entity_cargo_tram", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<TrainCargoTram>of(TrainCargoTram::new, MobCategory.MISC)).sized(5F, 2F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<TrainCargoTramTrailer>> CARGO_TRAM_TRAILER =
            REG.entity("entity_cargo_tram_trailer", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<TrainCargoTramTrailer>of(TrainCargoTramTrailer::new, MobCategory.MISC)).sized(5F, 2F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<BoundingBoxDummyEntity>> BOUNDING_DUMMY =
            REG.entity("entity_bounding_dummy", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<BoundingBoxDummyEntity>of(BoundingBoxDummyEntity::new, MobCategory.MISC)).sized(1F, 1F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<SeatDummyEntity>> SEAT_DUMMY =
            REG.entity("entity_seat_dummy", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<SeatDummyEntity>of(SeatDummyEntity::new, MobCategory.MISC)).sized(0.5F, 0.1F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMovingItem>> MOVING_ITEM =
            REG.entity("entity_c_item", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMovingItem>of(EntityMovingItem::new, MobCategory.MISC)).sized(0.375F, 0.375F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMovingPackage>> MOVING_PACKAGE =
            REG.entity("entity_c_package", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMovingPackage>of(EntityMovingPackage::new, MobCategory.MISC)).sized(0.5F, 0.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityDeliveryDrone>> DELIVERY_DRONE =
            REG.entity("entity_delivery_drone", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDeliveryDrone>of(EntityDeliveryDrone::new, MobCategory.MISC)).sized(0.75F, 0.75F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityRequestDrone>> REQUEST_DRONE =
            REG.entity("entity_request_drone", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityRequestDrone>of(EntityRequestDrone::new, MobCategory.MISC)).sized(0.75F, 0.75F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityFBIDrone>> FBI_DRONE =
            REG.entity("entity_fbi_drone", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityFBIDrone>of(EntityFBIDrone::new, MobCategory.MONSTER)).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityRADBeast>> RAD_BEAST =
            REG.entity("entity_radiation_blaze", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityRADBeast>of(EntityRADBeast::new, MobCategory.MONSTER)).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityMaskMan>> MASK_MAN =
            REG.entity("entity_mob_mask_man", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMaskMan>of(EntityMaskMan::new, MobCategory.MONSTER)).fireImmune().sized(2.0F, 5.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityUFO>> UFO =
            REG.entity("entity_ufo", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityUFO>of(EntityUFO::new, MobCategory.MONSTER)).fireImmune().sized(15.0F, 4.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBOTPrimeHead>> BALLS_O_TRON =
            REG.entity("entity_balls_o_tron", key -> Entities.build(
                    Entities.eyeHeight(Entities.noLoot(EntityType.Builder.<EntityBOTPrimeHead>of(EntityBOTPrimeHead::new, MobCategory.MONSTER)).fireImmune().sized(3.0F, 3.0F), 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBOTPrimeBody>> BALLS_O_TRON_SEG =
            REG.entity("entity_balls_o_tron_seg", key -> Entities.build(
                    Entities.eyeHeight(Entities.noLoot(EntityType.Builder.<EntityBOTPrimeBody>of(EntityBOTPrimeBody::new, MobCategory.MONSTER)).fireImmune().sized(2.0F, 2.0F), 1.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityHunterChopper>> HUNTER_CHOPPER =
            REG.entity("entity_mob_hunter_chopper", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityHunterChopper>of(EntityHunterChopper::new, MobCategory.MONSTER)).fireImmune().sized(8.25F, 3.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final RegistryObject<EntityType<EntityTntNtm>> TNT_NTM =
            REG.entity("tnt", key -> Entities.build(
                    Entities.eyeHeight(Entities.noLoot(EntityType.Builder.<EntityTntNtm>of(EntityTntNtm::new, MobCategory.MISC)).fireImmune().sized(0.98F, 0.98F), 0.15F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityFallingBlockNT>> FALLING_BLOCK_NT =
            REG.entity("entity_falling_block", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityFallingBlockNT>of(EntityFallingBlockNT::new, MobCategory.MISC)).sized(0.98F, 0.98F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityFallingMultiblock>> FALLING_MULTIBLOCK =
            REG.entity("entity_falling_multiblock", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityFallingMultiblock>of(EntityFallingMultiblock::new, MobCategory.MISC)).sized(0.98F, 0.98F).clientTrackingRange(64).updateInterval(20), key));
    public static final RegistryObject<EntityType<EntityC130>> C130 =
            REG.entity("c130", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityC130>of(EntityC130::new, MobCategory.MISC)).sized(8.0F, 4.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBomber>> BOMBER =
            REG.entity("entity_bomber", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBomber>of(EntityBomber::new, MobCategory.MISC)).sized(8.0F, 4.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBombletZeta>> BOMBLET_ZETA =
            REG.entity("entity_zeta", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBombletZeta>of(EntityBombletZeta::new, MobCategory.MISC)).noSave().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityGrenadeUniversal>> GRENADE_UNIVERSAL =
            REG.entity("grenade_universal", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGrenadeUniversal>of(EntityGrenadeUniversal::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityWastePearl>> WASTE_PEARL =
            REG.entity("entity_waste_pearl", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityWastePearl>of(EntityWastePearl::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityGrenadeBouncyGeneric>> GRENADE_BOUNCY_GENERIC =
            REG.entity("grenade_bouncy_generic", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGrenadeBouncyGeneric>of(EntityGrenadeBouncyGeneric::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityDisperserCanister>> DISPERSER_CANISTER =
            REG.entity("disperser_canister", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDisperserCanister>of(EntityDisperserCanister::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityItemBuoyant>> ITEM_BUOYANT =
            REG.entity("item_buoyant", key -> Entities.build(
                    EntityType.Builder.<EntityItemBuoyant>of(EntityItemBuoyant::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(7).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityNukeExplosionMK3>> NUKE_EXPLOSION_MK3 =
            REG.entity("nuke_mk3", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityNukeExplosionMK3>of(EntityNukeExplosionMK3::new, MobCategory.MISC)).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(0).updateInterval(Integer.MAX_VALUE), key));
    public static final RegistryObject<EntityType<EntityCloudFleija>> CLOUD_FLEIJA =
            REG.entity("cloud_fleija", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCloudFleija>of(EntityCloudFleija::new, MobCategory.MISC)).fireImmune().sized(20.0F, 40.0F).clientTrackingRange(32).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityCloudSolinium>> CLOUD_SOLINIUM =
            REG.entity("cloud_solinium", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCloudSolinium>of(EntityCloudSolinium::new, MobCategory.MISC)).fireImmune().sized(20.0F, 40.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityCloudFleijaRainbow>> CLOUD_RAINBOW =
            REG.entity("cloud_rainbow", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCloudFleijaRainbow>of(EntityCloudFleijaRainbow::new, MobCategory.MISC)).fireImmune().sized(20.0F, 40.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityParachuteCrate>> PARACHUTE_CRATE =
            REG.entity("parachute_crate", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityParachuteCrate>of(EntityParachuteCrate::new, MobCategory.MISC)).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBoxcar>> BOXCAR =
            REG.entity("boxcar", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBoxcar>of(EntityBoxcar::new, MobCategory.MISC)).fireImmune().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityTorpedo>> TORPEDO =
            REG.entity("torpedo", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityTorpedo>of(EntityTorpedo::new, MobCategory.MISC)).fireImmune().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityDuchessGambit>> DUCHESS_GAMBIT =
            REG.entity("duchessgambit", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDuchessGambit>of(EntityDuchessGambit::new, MobCategory.MISC)).fireImmune().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBuilding>> FALLING_BUILDING =
            REG.entity("falling_building", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBuilding>of(EntityBuilding::new, MobCategory.MISC)).fireImmune().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBobmazon>> BOBMAZON_DELIVERY =
            REG.entity("entity_bobmazon_delivery", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBobmazon>of(EntityBobmazon::new, MobCategory.MISC)).sized(1.0F, 3.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMinerRocket>> MINER_LANDER =
            REG.entity("entity_miner_lander", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMinerRocket>of(EntityMinerRocket::new, MobCategory.MISC)).sized(1.0F, 3.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityQuasar>> DIGAMMA_QUASAR =
            REG.entity("entity_digamma_quasar", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityQuasar>of(EntityQuasar::new, MobCategory.MISC)).sized(0.6F, 1.8F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityEMPBlast>> EMP_BLAST =
            REG.entity("entity_emp_blast", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityEMPBlast>of(EntityEMPBlast::new, MobCategory.MISC)).sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBalefire>> BALEFIRE =
            REG.entity("entity_balefire", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBalefire>of(EntityBalefire::new, MobCategory.MISC)).sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityDeathBlast>> LASER_BLAST =
            REG.entity("entity_laser_blast", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityDeathBlast>of(EntityDeathBlast::new, MobCategory.MISC)).sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityTomBlast>> TOM_BUST =
            REG.entity("entity_tom_bust", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityTomBlast>of(EntityTomBlast::new, MobCategory.MISC)).sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityCloudTom>> MOONSTONE_BLAST =
            REG.entity("entity_moonstone_blast", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityCloudTom>of(EntityCloudTom::new, MobCategory.MISC)).fireImmune().sized(1.0F, 4.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBullet>> BULLET =
            REG.entity("entity_bullet", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBullet>of(EntityBullet::new, MobCategory.MISC)).sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityBurningFOEQ>> BURNING_FOEQ =
            REG.entity("entity_burning_foeq", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityBurningFOEQ>of(EntityBurningFOEQ::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityFallingNuke>> FALLING_BOMB =
            REG.entity("entity_falling_bomb", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityFallingNuke>of(EntityFallingNuke::new, MobCategory.MISC)).sized(0.98F, 0.98F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntitySiegeLaser>> SIEGE_LASER =
            REG.entity("entity_siege_laser", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntitySiegeLaser>of(EntitySiegeLaser::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityTom>> TOM_THE_MOONSTONE =
            REG.entity("entity_tom_the_moonstone", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityTom>of(EntityTom::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntitySoyuz>> SOYUZ =
            REG.entity("entity_soyuz", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntitySoyuz>of(EntitySoyuz::new, MobCategory.MISC)).fireImmune().sized(5.0F, 50.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntitySoyuzCapsule>> SOYUZ_CAPSULE =
            REG.entity("entity_soyuz_capsule", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntitySoyuzCapsule>of(EntitySoyuzCapsule::new, MobCategory.MISC)).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityChopperMine>> CHOPPER_MINE =
            REG.entity("entity_chopper_mine", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityChopperMine>of(EntityChopperMine::new, MobCategory.MISC)).fireImmune().sized(12.0F, 12.0F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileTest>> MISSILE_TEST =
            REG.entity("missile_test", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileTest>of(EntityMissileTest::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileSchrabidium>> MISSILE_SCHRABIDIUM =
            REG.entity("missile_schrabidium", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileSchrabidium>of(EntityMissileSchrabidium::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileBHole>> MISSILE_BHOLE =
            REG.entity("missile_bhole", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileBHole>of(EntityMissileBHole::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileEMP>> MISSILE_EMP =
            REG.entity("missile_emp", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileEMP>of(EntityMissileEMP::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileCluster>> MISSILE_CLUSTER =
            REG.entity("missile_cluster", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileCluster>of(EntityMissileCluster::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileClusterStrong>> MISSILE_CLUSTER_STRONG =
            REG.entity("missile_cluster_strong", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileClusterStrong>of(EntityMissileClusterStrong::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileRain>> MISSILE_RAIN =
            REG.entity("missile_rain", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileRain>of(EntityMissileRain::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileDrill>> MISSILE_DRILL =
            REG.entity("missile_drill", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileDrill>of(EntityMissileDrill::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileVolcano>> MISSILE_VOLCANO =
            REG.entity("missile_volcano", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileVolcano>of(EntityMissileVolcano::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileCustom>> MISSILE_CUSTOM =
            REG.entity("entity_custom_missile", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileCustom>of(EntityMissileCustom::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));
    public static final RegistryObject<EntityType<EntityMissileShuttle>> MISSILE_SHUTTLE =
            REG.entity("entity_missile_shuttle", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityMissileShuttle>of(EntityMissileShuttle::new, MobCategory.MISC)).fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1), key));

    static {
        LivingAttributes.register(UNDEAD_SOLDIER, EntityUndeadSoldier::createAttributes);
        LivingAttributes.register(FBI, EntityFBI::createAttributes);
        LivingAttributes.register(CREEPER_GOLD, Creeper::createAttributes);
        LivingAttributes.register(CREEPER_VOLATILE, Creeper::createAttributes);
        LivingAttributes.register(CREEPER_PHOSGENE, Creeper::createAttributes);
        LivingAttributes.register(CREEPER_TAINTED, EntityCreeperTainted::createAttributes);
        LivingAttributes.register(CREEPER_NUCLEAR, EntityCreeperNuclear::createAttributes);
        LivingAttributes.register(CYBER_CRAB, EntityCyberCrab::createAttributes);
        LivingAttributes.register(TESLA_CRAB, EntityTeslaCrab::createAttributes);
        LivingAttributes.register(TAINT_CRAB, EntityTaintCrab::createAttributes);
        LivingAttributes.register(DUCK, Chicken::createAttributes);
        LivingAttributes.register(GHOST, EntityGhost::createAttributes);
        LivingAttributes.register(TEST_DUMMY, EntityDummy::createAttributes);
        LivingAttributes.register(BLOCK_SPIDER, EntityBlockSpider::createAttributes);
        LivingAttributes.register(QUACKOS, Chicken::createAttributes);
        LivingAttributes.register(PLASTIC_BAG, EntityPlasticBag::createAttributes);
        LivingAttributes.register(PIGEON, EntityPigeon::createAttributes);
        LivingAttributes.register(GLYPHID, EntityGlyphid::createAttributes);
        LivingAttributes.register(GLYPHID_BEHEMOTH, EntityGlyphidBehemoth::createAttributes);
        LivingAttributes.register(GLYPHID_BRENDA, EntityGlyphidBrenda::createAttributes);
        LivingAttributes.register(GLYPHID_SCOUT, EntityGlyphidScout::createAttributes);
        LivingAttributes.register(GLYPHID_NUCLEAR, EntityGlyphidNuclear::createAttributes);
        LivingAttributes.register(FBI_DRONE, EntityFBIDrone::createAttributes);
        LivingAttributes.register(RAD_BEAST, EntityRADBeast::createAttributes);
        LivingAttributes.register(MASK_MAN, EntityMaskMan::createAttributes);
        LivingAttributes.register(UFO, EntityUFO::createAttributes);
        LivingAttributes.register(BALLS_O_TRON, EntityBOTPrimeHead::createAttributes);
        LivingAttributes.register(BALLS_O_TRON_SEG, EntityBOTPrimeBody::createAttributes);
        LivingAttributes.register(HUNTER_CHOPPER, EntityHunterChopper::createAttributes);
    }

    /** Touches every self-registering entity class so its {@code TYPE} lands before the registry flush. */
    public static void init() {
        java.util.Objects.requireNonNull(EntityWaypoint.TYPE);
        java.util.Objects.requireNonNull(EntityParasiteMaggot.TYPE);
        java.util.Objects.requireNonNull(EntityGlyphidBlaster.TYPE);
        java.util.Objects.requireNonNull(EntityGlyphidBombardier.TYPE);
        java.util.Objects.requireNonNull(EntityGlyphidBrawler.TYPE);
        java.util.Objects.requireNonNull(EntityGlyphidDigger.TYPE);
        java.util.Objects.requireNonNull(EntityAcidBomb.TYPE);
    }
}
