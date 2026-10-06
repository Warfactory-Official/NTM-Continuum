package com.hbm.client.render.entity;

import com.hbm.client.render.BlackHoleRenderer;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers.Sink;
import com.hbm.client.render.MissileRenderer;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.missile.IMissilePitch;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityAcidBomb;
import com.hbm.main.ResourceManager;
import dev.golem.core.render.VanillaRenderers;
import dev.golem.core.render.living.CompatChickenRenderer;
import dev.golem.core.render.living.CompatCreeperRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Renderer bindings for the ntm-next entity port, one line per {@link ModNtmEntities} handle plus the seven
 * self-registering types. Every ported type is bound here, including the logic entities that draw nothing:
 * a visible entity with no renderer takes the render thread down the frame it enters view, so
 * {@link HbmEntityRenderers#verifyCoverage} checks the whole registry after registration.
 *
 * <p>Vanilla bodies (creepers, chickens, minecarts, TNT, dropped items) go through Golem's facades and factories
 * so the same line compiles on every node; the rest are this package's renderers.
 */
public final class NtmEntityRenderers {

    private NtmEntityRenderers() {}

    public static void registerAll(Sink sink) {
        // 1.7: invisible logic and effect entities.
        sink.register(ModNtmEntities.BALEFIRE.get(), VanillaRenderers.noop());
        sink.register(ModNtmEntities.TOM_BUST.get(), VanillaRenderers.noop());
        sink.register(ModNtmEntities.NUKE_EXPLOSION_MK3.get(), VanillaRenderers.noop());
        sink.register(ModNtmEntities.WASTE_PEARL.get(), VanillaRenderers.noop());
        sink.register(ModNtmEntities.BOUNDING_DUMMY.get(), VanillaRenderers.noop());
        sink.register(ModNtmEntities.SEAT_DUMMY.get(), VanillaRenderers.noop());
        sink.register(EntityWaypoint.TYPE.get(), VanillaRenderers.noop());

        // Vanilla bodies.
        sink.register(ModNtmEntities.WASTE_ITEM.get(), VanillaRenderers.item());
        sink.register(ModNtmEntities.ITEM_BUOYANT.get(), VanillaRenderers.item());
        // A visible entity with no renderer crashes the render thread; the vanilla TNT block reads the fuse.
        sink.register(ModNtmEntities.TNT_NTM.get(), VanillaRenderers.tnt());
        // 1.7's RenderNeoCart drew the NTM chassis mesh with a per-material skin; that renderer needs the
        // rail-following pose 26.1 keeps private, so the whole roster rides vanilla minecart geometry.
        sink.register(ModNtmEntities.CART_ORE.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.CART_POWDER.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.CART_SEMTEX.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.CART_CRATE.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.CART_DESTROYER.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.CART_TEST.get(), VanillaRenderers.minecart());
        sink.register(ModNtmEntities.BOAT_RUBBER.get(), RenderBoatRubber::new);

        // Mobs on vanilla bodies.
        sink.register(ModNtmEntities.CREEPER_NUCLEAR.get(),
                ctx -> new CompatCreeperRenderer(ctx, ResourceManager.creeper_nuclear_tex, 5F, ResourceManager.creeper_armor_tex));
        sink.register(ModNtmEntities.CREEPER_TAINTED.get(),
                ctx -> new CompatCreeperRenderer(ctx, ResourceManager.creeper_tainted_tex, 1F, ResourceManager.creeper_armor_taint_tex));
        sink.register(ModNtmEntities.CREEPER_PHOSGENE.get(),
                ctx -> new CompatCreeperRenderer(ctx, ResourceManager.creeper_phosgene_tex, 1F));
        sink.register(ModNtmEntities.CREEPER_VOLATILE.get(),
                ctx -> new CompatCreeperRenderer(ctx, ResourceManager.creeper_volatile_tex, 1F));
        sink.register(ModNtmEntities.CREEPER_GOLD.get(),
                ctx -> new CompatCreeperRenderer(ctx, ResourceManager.creeper_gold_tex, 1F));
        // 1.7 RenderDuck and RenderQuacc: vanilla chicken geometry, NTM skin, Quackos at 25x.
        sink.register(ModNtmEntities.DUCK.get(), ctx -> new CompatChickenRenderer(ctx, ResourceManager.duck_tex, 0.3F, 1F));
        sink.register(ModNtmEntities.QUACKOS.get(), ctx -> new CompatChickenRenderer(ctx, ResourceManager.duck_tex, 7.5F, 25F) {
            // 1.7 RenderQuacc pushed BossStatus from doRender, which ran only for an entity in range and in the frustum
            @Override
            public boolean shouldRender(net.minecraft.world.entity.animal.chicken.Chicken entity,
                                        net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
                boolean render = super.shouldRender(entity, frustum, camX, camY, camZ);
                if (render) com.hbm.client.BossStatus.set(entity, net.minecraft.world.BossEvent.BossBarColor.YELLOW);
                return render;
            }
        });
        sink.register(ModNtmEntities.GHOST.get(), RenderHumanoidMob::ghost);
        sink.register(ModNtmEntities.TEST_DUMMY.get(), RenderHumanoidMob::dummy);
        sink.register(ModNtmEntities.FBI.get(), RenderHumanoidMob::fbi);
        sink.register(ModNtmEntities.UNDEAD_SOLDIER.get(), RenderUndeadSoldier::new);
        sink.register(EntityParasiteMaggot.TYPE.get(), RenderParasiteMaggot::new);
        sink.register(ModNtmEntities.RAD_BEAST.get(), RenderRADBeast::new);
        sink.register(ModNtmEntities.CYBER_CRAB.get(), RenderCyberCrab::new);
        sink.register(ModNtmEntities.PIGEON.get(), RenderPigeon::new);

        // Mobs on OBJ meshes.
        sink.register(ModNtmEntities.TESLA_CRAB.get(), RenderTeslaCrab::tesla);
        sink.register(ModNtmEntities.TAINT_CRAB.get(), RenderTeslaCrab::taint);
        sink.register(ModNtmEntities.BLOCK_SPIDER.get(), RenderBlockSpider::new);
        sink.register(ModNtmEntities.PLASTIC_BAG.get(), RenderPlasticBag::new);
        sink.register(ModNtmEntities.GLYPHID.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(EntityGlyphidBrawler.TYPE.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(ModNtmEntities.GLYPHID_BEHEMOTH.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(ModNtmEntities.GLYPHID_BRENDA.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(EntityGlyphidBombardier.TYPE.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(EntityGlyphidBlaster.TYPE.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(ModNtmEntities.GLYPHID_SCOUT.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(EntityGlyphidDigger.TYPE.get(), ctx -> new RenderGlyphid<>(ctx));
        sink.register(ModNtmEntities.GLYPHID_NUCLEAR.get(), RenderGlyphidNuclear::new);
        sink.register(EntityAcidBomb.TYPE.get(), RenderAcidBomb::new);
        sink.register(ModNtmEntities.FBI_DRONE.get(), RenderFBIDrone::new);
        sink.register(ModNtmEntities.MASK_MAN.get(), RenderMaskMan::new);
        sink.register(ModNtmEntities.UFO.get(), RenderUFO::new);
        sink.register(ModNtmEntities.BALLS_O_TRON.get(), RenderBOTPrime::head);
        sink.register(ModNtmEntities.BALLS_O_TRON_SEG.get(), RenderBOTPrime::body);
        sink.register(ModNtmEntities.HUNTER_CHOPPER.get(), RenderHunterChopper::new);

        // Vehicles, planes and props.
        sink.register(ModNtmEntities.CARGO_TRAM.get(), ctx -> new RenderTrainCargoTram<>(ctx));
        sink.register(ModNtmEntities.CARGO_TRAM_TRAILER.get(), RenderTrainCargoTramTrailer::new);
        sink.register(ModNtmEntities.C130.get(), RenderC130::new);
        sink.register(ModNtmEntities.BOMBER.get(), RenderBomber::new);
        sink.register(ModNtmEntities.BOMBLET_ZETA.get(), RenderBombletZeta::new);
        sink.register(ModNtmEntities.PARACHUTE_CRATE.get(), RenderParachuteCrate::new);
        sink.register(ModNtmEntities.BOXCAR.get(), ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.BOXCAR));
        sink.register(ModNtmEntities.TORPEDO.get(), ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.TORPEDO));
        sink.register(ModNtmEntities.DUCHESS_GAMBIT.get(), ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.DUCHESS_GAMBIT));
        sink.register(ModNtmEntities.FALLING_BUILDING.get(), ctx -> new RenderBoxcar<>(ctx, RenderBoxcar.Kind.BUILDING));
        sink.register(ModNtmEntities.MINER_LANDER.get(), RenderMinerRocket::lander);
        sink.register(ModNtmEntities.BOBMAZON_DELIVERY.get(), RenderMinerRocket::bobmazon);
        sink.register(ModNtmEntities.SOYUZ.get(), RenderSoyuz::new);
        sink.register(ModNtmEntities.SOYUZ_CAPSULE.get(), RenderSoyuzCapsule::new);
        sink.register(ModNtmEntities.BURNING_FOEQ.get(), RenderFOEQ::new);
        sink.register(ModNtmEntities.FALLING_BOMB.get(), RenderFallingNuke::new);
        sink.register(ModNtmEntities.TOM_THE_MOONSTONE.get(), RenderTom::new);

        // Items in flight.
        sink.register(ModNtmEntities.MOVING_ITEM.get(), RenderMovingItem::new);
        sink.register(ModNtmEntities.MOVING_PACKAGE.get(), RenderMovingPackage::new);
        sink.register(ModNtmEntities.DELIVERY_DRONE.get(), ctx -> new RenderDeliveryDrone<>(ctx));
        sink.register(ModNtmEntities.REQUEST_DRONE.get(), ctx -> new RenderDeliveryDrone<>(ctx));
        sink.register(ModNtmEntities.FALLING_BLOCK_NT.get(), RenderFallingBlockNT::new);
        sink.register(ModNtmEntities.FALLING_MULTIBLOCK.get(), RenderFallingMultiblock::new);
        sink.register(ModNtmEntities.GRENADE_UNIVERSAL.get(), RenderGrenadeUniversal::new);
        sink.register(ModNtmEntities.GRENADE_BOUNCY_GENERIC.get(), ctx -> new RenderGenericGrenade<>(ctx));
        sink.register(ModNtmEntities.DISPERSER_CANISTER.get(), ctx -> new RenderGenericGrenade<>(ctx));

        // Projectiles.
        sink.register(ModNtmEntities.METEOR.get(), RenderMeteor::new);
        sink.register(ModNtmEntities.COG.get(), RenderCog::new);
        sink.register(ModNtmEntities.FIREWORK_BALL.get(), ctx -> new RenderShrapnel<>(ctx));
        sink.register(ModEntities.SHRAPNEL.get(), ctx -> new RenderShrapnel<>(ctx));
        sink.register(ModNtmEntities.BULLET.get(), RenderBullet::new);
        sink.register(ModNtmEntities.CHOPPER_MINE.get(), RenderChopperMine::new);
        sink.register(ModNtmEntities.SIEGE_LASER.get(), RenderSiegeLaser::new);
        sink.register(ModNtmEntities.LASER_BLAST.get(), RenderDeathBlast::new);
        sink.register(ModNtmEntities.EMP_BLAST.get(), RenderEMPBlast::new);
        sink.register(ModNtmEntities.DIGAMMA_QUASAR.get(), ctx -> {
            BlackHoleRenderer renderer = new BlackHoleRenderer(ctx);
            renderer.gem();
            return renderer;
        });

        // Clouds.
        sink.register(ModNtmEntities.CLOUD_FLEIJA.get(), RenderCloudFleija::new);
        sink.register(ModNtmEntities.CLOUD_SOLINIUM.get(), RenderCloudSolinium::new);
        sink.register(ModNtmEntities.CLOUD_RAINBOW.get(), RenderCloudRainbow::new);
        sink.register(ModNtmEntities.MOONSTONE_BLAST.get(), RenderCloudTom::new);
        sink.register(ModNtmEntities.AGENT_ORANGE.get(), ctx -> new RenderMultiCloud<EntityOrangeFX>(ctx, "orange",
                EntityOrangeFX::particleAge, EntityOrangeFX::maxAge));
        sink.register(ModNtmEntities.CHLORINE_FX.get(), ctx -> new RenderMultiCloud<EntityChlorineFX>(ctx, "chlorine",
                EntityChlorineFX::particleAge, EntityChlorineFX::maxAge));
        sink.register(ModNtmEntities.CLOUD_FX.get(), ctx -> new RenderMultiCloud<EntityCloudFX>(ctx, "cloud",
                EntityCloudFX::particleAge, EntityCloudFX::maxAge));
        sink.register(ModNtmEntities.PINK_CLOUD_FX.get(), ctx -> new RenderMultiCloud<EntityPinkCloudFX>(ctx, "pc",
                EntityPinkCloudFX::particleAge, EntityPinkCloudFX::maxAge));

        // Missiles: the micro body is the old missileTaint OBJ at 2x, as the pre-existing tier 0 pair; the rest
        // reuse the tier bodies the missile wave already loads.
        missile(sink, ModNtmEntities.MISSILE_TEST.get(), "missile_micro.obj", "missile_test.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_SCHRABIDIUM.get(), "missile_micro.obj", "missile_micro_schrab.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_BHOLE.get(), "missile_micro.obj", "missile_micro_bhole.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_EMP.get(), "missile_micro.obj", "missile_micro_emp.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_CLUSTER.get(), "missile_v2.obj", "missile_v2_cl.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_CLUSTER_STRONG.get(), "missile_strong.obj", "missile_strong_cl.png", 1.0F);
        missile(sink, ModNtmEntities.MISSILE_RAIN.get(), "missile_huge.obj", "missile_huge_cl.png", 0.8F);
        missile(sink, ModNtmEntities.MISSILE_DRILL.get(), "missile_huge.obj", "missile_huge_bu.png", 0.8F);
        missile(sink, ModNtmEntities.MISSILE_VOLCANO.get(), "missile_atlas.obj", "missile_atlas_tectonic.png", 1.5F);
        missile(sink, ModNtmEntities.MISSILE_SHUTTLE.get(), "missile_shuttle.obj", "missile_shuttle.png", 1.0F);
        sink.register(ModNtmEntities.MISSILE_CUSTOM.get(), RenderMissileCustom::new);
    }

    private static <T extends Entity & IMissilePitch> void missile(Sink sink, EntityType<T> type, String model,
                                                                 String texture, float scale) {
        HbmEntityRenderers.regGem(sink, type, context -> new MissileRenderer<>(context, model, texture, scale));
    }
}
