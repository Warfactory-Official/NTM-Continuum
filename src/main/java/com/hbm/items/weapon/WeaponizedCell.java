package com.hbm.items.weapon;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code WeaponizedCell}, the rigged star blaster cell: dropped on the floor it smokes, reddens and spits
 * lava for 50 seconds (or until it burns), then goes off as a 100-block F.L.E.I.J.A. with its rainbow cloud.
 *
 * <p>{@code onEntityItemUpdate} is {@link #tickDropped}, driven by the loaders' item-entity tick like the
 * singularities. 1.7 spawned the particles on the client's copy of the item; the hook runs on the server,
 * so each is sent as one server particle at the same random offset instead.
 */
public class WeaponizedCell extends CompatItem {

    public WeaponizedCell(Properties properties) {
        super(properties);
    }

    public static void tickDropped(ItemEntity item) {
        if (!(item.getItem().getItem() instanceof WeaponizedCell)) return;
        if (!(item.level() instanceof ServerLevel world)) return;

        if (item.tickCount > 50 * 20 || item.isOnFire()) {
            if (WeaponConfig.dropStar) {
                EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(world, item.getX(), item.getY(), item.getZ(), 100);
                if (!ex.isRemoved()) {
                    world.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.BLOCKS, 100.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
                    world.addFreshEntity(ex);
                    world.addFreshEntity(EntityCloudFleijaRainbow.statFac(world, 100, item.getX(), item.getY(), item.getZ()));
                }
            }
            item.discard();
            return;
        }

        RandomSource rand = world.getRandom();
        int randy = (50 * 20) - item.tickCount;
        if (randy < 1) randy = 1;

        if (rand.nextInt(50 * 20) >= randy) particle(world, item, Compat.dust(0xFF0000, 1.0F));
        else particle(world, item, ParticleTypes.SMOKE);

        if (randy < 100) particle(world, item, ParticleTypes.LAVA);
    }

    private static void particle(ServerLevel world, ItemEntity item, ParticleOptions type) {
        RandomSource rand = world.getRandom();
        world.sendParticles(type,
                item.getX() + rand.nextGaussian() * item.getBbWidth() / 2,
                item.getY() + rand.nextGaussian() * item.getBbHeight(),
                item.getZ() + rand.nextGaussian() * item.getBbWidth() / 2,
                1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("A charged energy cell, rigged to explode"));
        list.add(Component.literal("when left on the floor for too long."));
    }
}
