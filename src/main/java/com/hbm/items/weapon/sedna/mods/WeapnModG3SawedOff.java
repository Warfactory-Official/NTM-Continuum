package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

/** Source-spelled G3 stock-removal modifier and its shortened equip animation. */
public final class WeapnModG3SawedOff extends WeaponModBase {
    private static final BiFunction<ItemStack, GunAnimation, BusAnimation> ANIMS = (stack, type) ->
            type == GunAnimation.EQUIP
                    ? new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    : new BusAnimation();

    public WeapnModG3SawedOff(int id) { super(id, "SHIELD"); }

    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.I_DRAWDURATION) return cast(5, base);
        if (key == GunConfig.FUN_ANIMNATIONS) {
            @SuppressWarnings("unchecked") BiFunction<ItemStack, GunAnimation, BusAnimation> original =
                    (BiFunction<ItemStack, GunAnimation, BusAnimation>) base;
            BiFunction<ItemStack, GunAnimation, BusAnimation> replacement = (stack, type) -> type == GunAnimation.EQUIP
                    ? ANIMS.apply(stack, type)
                    : original == null ? null : original.apply(stack, type);
            return cast(replacement, base);
        }
        return base;
    }
}
