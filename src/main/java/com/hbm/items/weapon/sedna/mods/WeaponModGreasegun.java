package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.factory.Orchestras;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import net.minecraft.world.item.ItemStack;
/** Grease-gun furniture stats; the old casing/orchestra presentation is intentionally separate. */
public class WeaponModGreasegun extends WeaponModBase {
    public WeaponModGreasegun(int id) { super(id, "FURNITURE"); setPriority(PRIORITY_ADDITIVE); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (GunConfig.F_DURABILITY.equals(key)) return cast((Float) base * 3F, base);
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base + 2F, base);
        if (Receiver.F_SPREADINNATE.equals(key)) return cast(0F, base);
        if (Receiver.I_DELAYAFTERFIRE.equals(key)) return cast((Integer) base / 2, base);
        if (GunConfig.CON_ORCHESTRA.equals(key)) return cast(ORCHESTRA_GREASEGUN, base);
        return base;
    }

    public static final java.util.function.BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_GREASEGUN = (stack, ctx) -> {
        var entity = ctx.entity;
        if (entity.level().isClientSide()) return;
        GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
        boolean aiming = ItemGunBaseNT.getIsAiming(stack);
        if (type == GunAnimation.CYCLE) {
            if (timer == 1) {
                SpentCasing casing = CasingCreator.getCasing(ctx.config.getReceivers(stack)[0].getMagazine(stack), stack, ctx.inventory);
                if (casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.55, aiming ? 0 : -0.125,
                        aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01,
                        -7.5F + (float) entity.getRandom().nextGaussian() * 5F,
                        12F + (float) entity.getRandom().nextGaussian() * 5F, casing.getName());
            }
            return;
        }
        Orchestras.ORCHESTRA_GREASEGUN.accept(stack, ctx);
    };
}
