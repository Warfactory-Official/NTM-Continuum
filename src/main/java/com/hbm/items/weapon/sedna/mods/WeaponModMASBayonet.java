package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.Orchestras;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import dev.golem.core.render.anim.BusAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

public final class WeaponModMASBayonet extends WeaponModBayonet {
    public WeaponModMASBayonet(int id) { super(id); }
    @Override float stabDamage() { return XFactory762mm.GUN_SOUTH_STAR_BAYONET_DAMAGE.get(); }
    @Override BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> normalOrchestra() { return Orchestras.ORCHESTRA_MAS36; }
    @Override BiFunction<ItemStack, GunAnimation, BusAnimation> inspectionAnimation() { return (stack, type) -> stabAnimation(); }
}
