package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

/** G3 polymer furniture recoil callback, kept independent from the client animation implementation. */
public class WeaponModPolymerFurniture extends WeaponModBase {
    public WeaponModPolymerFurniture(int id) { super(id, "FURNITURE"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return Receiver.CON_ONRECOIL.equals(key) ? cast(LAMBDA_RECOIL_G3, base) : base;
    }
    public static final java.util.function.BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> {
                if (ctx.getPlayer() != null) ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
            };
}
