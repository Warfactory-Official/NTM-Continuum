package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.entity.projectile.GunProjectilePayloads;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.IRepairable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Fire-extinguisher, hook and mortar round declarations. */
public final class XFactoryTool {
    public static BulletConfig fext_water;
    public static BulletConfig fext_foam;
    public static BulletConfig fext_sand;
    public static BulletConfig ct_hook;
    public static BulletConfig ct_mortar;
    public static BulletConfig ct_mortar_charge;

    private XFactoryTool() {}

    /** Water removes both ordinary fire and the two foam forms in the same 3x3x3 volume as 1.12.2. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_WATER_HIT = XFactoryTool::waterHit;
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_FOAM_HIT = XFactoryTool::foamHit;
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_SAND_HIT = XFactoryTool::sandHit;

    public static final Consumer<net.minecraft.world.entity.Entity> LAMBDA_SET_HOOK = entity -> {
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
        if (!bullet.level().isClientSide() && bullet.tickCount < 2
                && bullet.getThrower() instanceof net.minecraft.world.entity.player.Player player
                && player.getMainHandItem().getItem() instanceof ItemGunChargeThrower) {
            ItemGunChargeThrower.setLastHook(player.getMainHandItem(), bullet.getId());
        }
        bullet.ignoreFrustum = true;
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_HOOK = (bullet, hit) -> {
        if (hit instanceof BlockHitResult blockHit) {
            Vec3 offset = bullet.getDeltaMovement().scale(-1D).normalize().scale(0.05D);
            Vec3 location = hit.getLocation();
            bullet.setPos(location.x + offset.x, location.y + offset.y, location.z + offset.z);
            bullet.getStuck(blockHit.getBlockPos(), blockHit.getDirection().get3DDataValue());
        }
    };

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_CT = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CT_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                        .addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD:
                return new BusAnimation().addBus("RAISE", new BusAnimationSequence()
                                .addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(2000)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("AMMO", new BusAnimationSequence().setPos(0, -10, -5).hold(500)
                                .addPos(0, 0, 5, 750, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP)
                                .hold(4000))
                        .addBus("TWIST", new BusAnimationSequence().setPos(0, 0, 25).hold(2000)
                                .addPos(0, 0, 0, 150));
            case INSPECT:
                return new BusAnimation().addBus("TURN", new BusAnimationSequence()
                                .addPos(0, 60, 0, 500, IType.SIN_FULL).hold(1750)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("ROLL", new BusAnimationSequence().hold(750)
                                .addPos(0, 0, -90, 500, IType.SIN_FULL).hold(1000)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL));
            default:
                return null;
        }
    };

    private static void waterHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult blockHit) || bullet.level().isClientSide()) return;
        Level level = bullet.level();
        BlockPos centre = blockHit.getBlockPos();
        boolean fizz = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(ModRBMKBlocks.foam_layer.get()) || state.is(ModBlocks.block_foam.get())) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                fizz = true;
            }
        }
        extinguishCore(level, centre, IRepairable.EnumExtinguishType.WATER);
        if (fizz) hiss(level, bullet);
        bullet.discard();
    }

    private static void foamHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult blockHit) || bullet.level().isClientSide()) return;
        Level level = bullet.level();
        BlockPos base = blockHit.getBlockPos();
        boolean fizz = removeFire(level, base);
        if (extinguishCore(level, base, IRepairable.EnumExtinguishType.FOAM)) return;
        if (bullet.randomSource().nextBoolean()) base = base.relative(blockHit.getDirection());
        placeLayer(level, base, ModRBMKBlocks.foam_layer.get(), ModBlocks.block_foam.get());
        if (fizz) hiss(level, bullet);
    }

    private static void sandHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult blockHit) || bullet.level().isClientSide()) return;
        Level level = bullet.level();
        BlockPos base = blockHit.getBlockPos();
        BlockState before = level.getBlockState(base);
        boolean fizz = before.is(Blocks.FIRE);
        if (extinguishCore(level, base, IRepairable.EnumExtinguishType.SAND)) return;
        if (bullet.randomSource().nextBoolean()) base = base.relative(blockHit.getDirection());
        if (placeLayer(level, base, ModRBMKBlocks.sand_boron_layer.get(), ModFallingBlocks.sand_boron.get()) && fizz) {
            hiss(level, bullet);
        }
    }

    private static boolean extinguishCore(Level level, BlockPos pos, IRepairable.EnumExtinguishType type) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return false;
        BlockEntity core = MultiblockSurface.coreEntity(server, pos);
        if (!(core instanceof IRepairable repairable)) return false;
        repairable.tryExtinguish(server, pos, type);
        return true;
    }

    private static boolean removeFire(Level level, BlockPos centre) {
        boolean removed = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            if (level.getBlockState(pos).is(Blocks.FIRE)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                removed = true;
            }
        }
        return removed;
    }

    private static boolean placeLayer(Level level, BlockPos pos, net.minecraft.world.level.block.Block layer,
            net.minecraft.world.level.block.Block full) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(layer) && !state.canBeReplaced()) return false;
        if (!layer.defaultBlockState().canSurvive(level, pos)) return false;
        if (!state.is(layer)) {
            level.setBlockAndUpdate(pos, layer.defaultBlockState());
        } else {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers < 7) level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
            else level.setBlockAndUpdate(pos, full.defaultBlockState());
        }
        return true;
    }

    private static void hiss(Level level, EntityBulletBaseMK4 bullet) {
        level.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(),
                net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS,
                1F, 1.5F + level.getRandom().nextFloat() * 0.5F);
    }

    private static Consumer<net.minecraft.world.entity.Entity> blockDust(
            java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block,
            double spread) {
        return entity -> {
            if (!entity.level().isClientSide()) return;
            CompoundTag data = new CompoundTag();
            data.putInt("block", BuiltInRegistries.BLOCK.getId(block.get()));
            net.minecraft.util.RandomSource random = entity.level().getRandom();
            data.putDouble("mX", entity.getDeltaMovement().x + random.nextGaussian() * spread);
            data.putDouble("mY", entity.getDeltaMovement().y - 0.2D + random.nextGaussian() * spread);
            data.putDouble("mZ", entity.getDeltaMovement().z + random.nextGaussian() * spread);
            HbmEffectNT.VanillaExt_BlockDust.summonParticle(entity.level(), entity.getX(), entity.getY(),
                    entity.getZ(), data);
        };
    }

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_MORTAR = (bullet, hit) -> {
        if (hit instanceof net.minecraft.world.phys.EntityHitResult && bullet.tickCount < 3
                && ((net.minecraft.world.phys.EntityHitResult) hit).getEntity() == bullet.getThrower()) return;
        net.minecraft.world.phys.Vec3 location = hit.getLocation();
        ExplosionVNT explosion = new ExplosionVNT(bullet.level(), location.x, location.y, location.z,
                5F, bullet.getThrower());
        explosion.setBlockAllocator(new BlockAllocatorBulkie(60, 8));
        explosion.setBlockProcessor(new BlockProcessorStandard());
        explosion.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        explosion.setPlayerProcessor(new PlayerProcessorStandard());
        explosion.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        explosion.explode();
        bullet.discard();
    };

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_MORTAR_CHARGE = (bullet, hit) -> {
        if (hit instanceof net.minecraft.world.phys.EntityHitResult && bullet.tickCount < 3
                && ((net.minecraft.world.phys.EntityHitResult) hit).getEntity() == bullet.getThrower()) return;
        net.minecraft.world.phys.Vec3 location = hit.getLocation();
        ExplosionVNT explosion = new ExplosionVNT(bullet.level(), location.x, location.y, location.z,
                15F, bullet.getThrower());
        explosion.setBlockAllocator(new BlockAllocatorStandard());
        explosion.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                .withBlockEffect(new BlockMutatorDebris(() -> ModBlocks.block_slag.get().defaultBlockState())));
        explosion.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        explosion.setPlayerProcessor(new PlayerProcessorStandard());
        explosion.explode();
        com.hbm.particle.helper.ExplosionCreator.composeEffectSmall(bullet.level(), location.x,
                location.y + 0.5D, location.z);
        bullet.discard();
    };

    public static void init() {
        fext_water = BulletConfigs.register("fext_water", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("fext_water")).setReloadCount(300).setLife(100)
                .setVel(0.75F).setGrav(0.04F).setSpread(0.025F)
                .setOnUpdate(blockDust(() -> Blocks.WATER, 0.05D)).setOnRicochet(LAMBDA_WATER_HIT)
                .setOnEntityHit(GunProjectilePayloads::extinguishEntity));
        fext_foam = BulletConfigs.register("fext_foam", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("fext_foam")).setReloadCount(300).setLife(100)
                .setVel(0.75F).setGrav(0.04F).setSpread(0.05F)
                .setOnUpdate(blockDust(ModBlocks.block_foam, 0.1D)).setOnRicochet(LAMBDA_FOAM_HIT)
                .setOnEntityHit(GunProjectilePayloads::extinguishEntity));
        fext_sand = BulletConfigs.register("fext_sand", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("fext_sand")).setReloadCount(300).setLife(100)
                .setVel(0.75F).setGrav(0.04F).setSpread(0.05F)
                .setOnUpdate(blockDust(ModFallingBlocks.sand_boron, 0.1D)).setOnRicochet(LAMBDA_SAND_HIT)
                .setOnEntityHit(GunProjectilePayloads::extinguishEntity));
        ct_hook = BulletConfigs.register("ct_hook", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("ct_hook")).setRenderRotations(false).setLife(6000).setVel(3F)
                .setGrav(0.035F).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setOnUpdate(LAMBDA_SET_HOOK).setOnImpact(LAMBDA_HOOK));
        ct_mortar = BulletConfigs.register("ct_mortar", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("ct_mortar")).setDamage(2.5F).setLife(200)
                .setVel(3F).setGrav(0.035F).setOnImpact(LAMBDA_MORTAR));
        ct_mortar_charge = BulletConfigs.register("ct_mortar_charge", new BulletConfig()
                .setItem(NtmToolGunCatalog.standardAmmo("ct_mortar_charge")).setDamage(5F).setLife(200)
                .setVel(3F).setGrav(0.035F).setOnImpact(LAMBDA_MORTAR_CHARGE));

        NtmToolGunCatalog.gun_fireext = NtmToolGunCatalog.weapon("gun_fireext",
                props -> new com.hbm.items.weapon.sedna.ItemGunBaseNT(props, WeaponQuality.UTILITY,
                        new GunConfig().dura(5_000).draw(10).inspect(55).reloadChangeType(true)
                                .hideCrosshair(false).crosshair(Crosshair.L_CIRCLE)
                                .rec(new Receiver(0).dmg(0F).delay(1).dry(0).auto(true).spread(0F)
                                        .spreadHipfire(0F).reload(20).jam(0)
                                        .mag(new MagazineFullReload(0, 300).addConfigs(fext_water, fext_foam, fext_sand))
                                        .offset(1, -0.0625 * 2.5, -0.25D)
                                        .sound(com.hbm.sound.ModGunSounds.FIRE_EXTINGUISHER::get, 1F, 1F)
                                        .setupStandardFire())
                                .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_FIREEXT)));

        NtmToolGunCatalog.gun_charge_thrower = NtmToolGunCatalog.weapon("gun_charge_thrower",
                props -> (ItemGunChargeThrower) new ItemGunChargeThrower(props, WeaponQuality.UTILITY,
                        new GunConfig().dura(3_000).draw(10).inspect(55).reloadChangeType(true)
                                .hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                                .rec(new Receiver(0).dmg(10F).delay(4).dry(10).auto(true).spread(0F).spreadHipfire(0F).reload(60).jam(0)
                                        .sound(com.hbm.sound.ModGunSounds.FIRE_CHARGE::get, 1F, 1F)
                                        .mag(new MagazineFullReload(0, 1).addConfigs(ct_hook, ct_mortar, ct_mortar_charge))
                                        .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                                        .recoil(LAMBDA_RECOIL_CT))
                                .setupStandardConfiguration().anim(LAMBDA_CT_ANIMS)
                                .orchestra(Orchestras.ORCHESTRA_CHARGE_THROWER)).setDefaultAmmo(
                         ModAmmoItems.ct_mortar, 3));
    }

    public static GunConfig chargeThrowerConfig() {
        return new GunConfig().dura(3000).draw(10).inspect(55).crosshair(Crosshair.L_CIRCUMFLEX)
                .rec(new Receiver(0).dmg(10F).delay(4).dry(10).auto(true).reload(60).jam(0)
                        .sound(com.hbm.sound.ModGunSounds.FIRE_CHARGE::get, 1F, 1F)
                        .mag(new MagazineFullReload(0, 1).addConfigs(ct_hook, ct_mortar, ct_mortar_charge))
                        .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire())
                .setupStandardConfiguration();
    }

}
