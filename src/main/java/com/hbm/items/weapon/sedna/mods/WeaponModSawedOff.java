package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunAnimation;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
/** Shared stat portion of the sawed-off modifier; weapon-specific animation replacement is client-owned. */
public class WeaponModSawedOff extends WeaponModBase {
    public WeaponModSawedOff(int id) { super(id, "BARREL"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.F_SPREADINNATE.equals(key)) return cast(Math.max(0.025F, (Float) base), base);
        if (Receiver.F_SPREADAMMO.equals(key)) return cast((Float) base * 1.5F, base);
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base * 1.35F, base);
        if (gun.is(ModGunItems.gun_maresleg.get())) {
            if (GunConfig.FUN_ANIMNATIONS.equals(key)) {
                @SuppressWarnings("unchecked") BiFunction<ItemStack, GunAnimation, BusAnimation> original =
                        (BiFunction<ItemStack, GunAnimation, BusAnimation>) base;
                BiFunction<ItemStack, GunAnimation, BusAnimation> replacement = (stack, type) -> {
                    BusAnimation animation = shortAnimation(type);
                    return animation != null ? animation : original == null ? new BusAnimation() : original.apply(stack, type);
                };
                return cast(replacement, base);
            }
            if (GunConfig.I_DRAWDURATION.equals(key)) return cast(5, base);
        }
        return base;
    }

    private static BusAnimation shortAnimation(GunAnimation type) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                    .addPos(-60, 0, 0, 0).addPos(0, 0, -3, 250, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                    .addBus("SIGHT", new BusAnimationSequence().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
                    .addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
                    .addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
                    .addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
                    .addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                    .addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 650)
                            .addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
            default -> null;
        };
    }
}
