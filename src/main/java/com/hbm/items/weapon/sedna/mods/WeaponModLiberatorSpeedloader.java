package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.sedna.GunAnimation;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

/** Converts the Liberator's four round-by-round loader to a four-round full reload. */
public class WeaponModLiberatorSpeedloader extends WeaponModBase {
    private static final MagazineFullReload MAGAZINE = new MagazineFullReload(0, 4);
    private static final BiFunction<ItemStack, GunAnimation, BusAnimation> ANIMS = (stack, type) -> {
        switch (type) {
            case RELOAD -> {
                return new BusAnimation().addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
                        .addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100)
                                .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                        .addBus("SHELL1", reloadShell()).addBus("SHELL2", reloadShell())
                        .addBus("SHELL3", reloadShell()).addBus("SHELL4", reloadShell());
            }
            case RELOAD_END -> {
                return new BusAnimation().addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0)
                                .addPos(15, 0, 0, 250).addPos(0, 0, 0, 50))
                        .addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0)
                                .addPos(0, 0, 0, 250, IType.SIN_UP));
            }
            case JAMMED -> {
                return new BusAnimation().addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0)
                                .addPos(15, 0, 0, 250).addPos(0, 0, 0, 50).addPos(0, 0, 0, 550)
                                .addPos(15, 0, 0, 100).addPos(15, 0, 0, 600).addPos(0, 0, 0, 50))
                        .addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0)
                                .addPos(0, 0, 0, 250, IType.SIN_UP).addPos(0, 0, 0, 600)
                                .addPos(45, 0, 0, 250, IType.SIN_DOWN).addPos(45, 0, 0, 300)
                                .addPos(0, 0, 0, 150, IType.SIN_UP));
            }
            default -> { }
        }
        return null;
    };

    public WeaponModLiberatorSpeedloader(int id) { super(id, "SPEEDLOADER"); }

    private static BusAnimationSequence reloadShell() {
        return new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400)
                .addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP);
    }

    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (GunConfig.FUN_ANIMNATIONS.equals(key)) {
            @SuppressWarnings("unchecked") BiFunction<ItemStack, GunAnimation, BusAnimation> original =
                    (BiFunction<ItemStack, GunAnimation, BusAnimation>) base;
            BiFunction<ItemStack, GunAnimation, BusAnimation> replacement = (stack, type) -> {
                BusAnimation source = ANIMS.apply(stack, type);
                return source != null ? source : original == null ? new BusAnimation() : original.apply(stack, type);
            };
            return cast(replacement, base);
        }
        if (Receiver.O_MAGAZINE.equals(key) && base instanceof MagazineSingleReload original) {
            if (MAGAZINE.acceptedBullets.isEmpty()) MAGAZINE.acceptedBullets.addAll(original.acceptedBullets);
            MAGAZINE.index = original.index;
            return cast(MAGAZINE, base);
        }
        return base;
    }
}
