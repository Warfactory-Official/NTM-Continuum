package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import dev.golem.core.render.anim.BusAnimation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmorNCRPARanged implements IPARanged {
    private final MagazineBelt steer = new MagazineBelt();
    private final MagazineBelt dumb = new MagazineBelt();

    @Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { fire(stack, ctx, true); }
    @Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { fire(stack, ctx, false); }

    private void fire(ItemStack stack, LambdaContext ctx, boolean guided) {
        Player player = ctx.getPlayer();
        if (player == null || ItemGunBaseNT.getState(stack, 0) != GunState.IDLE) return;
        MagazineBelt magazine = guided ? steer : dumb;
        if (magazine.acceptedBullets.isEmpty()) magazine.addConfigs(guided ? XFactoryRocket.rocket_ncrpa_steer : XFactoryRocket.rocket_ncrpa);
        BulletConfig config = magazine.getType(stack, player.getInventory());
        if (magazine.getAmount(stack, player.getInventory()) <= 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE_CLICK.get(), SoundSource.PLAYERS, 1F, 1F);
            return;
        }
        magazine.useUpAmmo(stack, player.getInventory(), 1);
        float side = player.getRandom().nextBoolean() ? -0.25F : 0.25F;
        player.level().addFreshEntity(new com.hbm.entity.projectile.EntityBulletBaseMK4(player, config, 25F, 0F, side, 0D, 0D));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModGunSounds.FIRE_ROCKET.get(), SoundSource.PLAYERS, 0.5F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }
}
