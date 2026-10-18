package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.Orchestras;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import dev.golem.core.render.anim.BusAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

public final class WeaponModCarbineBayonet extends WeaponModBayonet {
    public WeaponModCarbineBayonet(int id) { super(id); }
    @Override float stabDamage() { return XFactory762mm.GUN_CARBINE_BAYONET_DAMAGE.get(); }
    @Override BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> normalOrchestra() { return Orchestras.ORCHESTRA_CARBINE; }
    @Override BiFunction<ItemStack, GunAnimation, BusAnimation> inspectionAnimation() { return (stack, type) -> stabAnimation(); }
}
