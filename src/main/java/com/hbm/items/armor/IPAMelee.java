package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.GunAnimation;
import dev.golem.core.render.anim.BusAnimation;
import net.minecraft.world.item.ItemStack;

public interface IPAMelee {
    BusAnimation playAnim(ItemStack stack, GunAnimation type);
    void orchestra(ItemStack stack, LambdaContext ctx);
    void clickPrimary(ItemStack stack, LambdaContext ctx);
    void clickSecondary(ItemStack stack, LambdaContext ctx);
}
