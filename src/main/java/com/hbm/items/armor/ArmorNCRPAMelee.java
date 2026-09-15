package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.sound.ModSounds;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public final class ArmorNCRPAMelee extends ArmorPAMeleeBase {
    @Override protected int lightCooldown() { return 25; }
    @Override protected int heavyCooldown() { return 30; }
    @Override protected boolean isLightHit(int timer) { return timer == 5 || timer == 15; }
    @Override protected boolean isHeavyHit(int timer) { return timer == 5; }
    @Override protected SoundEvent hitSound() { return ModSounds.GUN_STAB.get(); }

    @Override public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
        if (type == GunAnimation.EQUIP) return new BusAnimation().addBus("EQUIP",
                new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 750, IType.SIN_DOWN));
        if (type == GunAnimation.CYCLE) return new BusAnimation()
                .addBus("SWINGRIGHT", new BusAnimationSequence().addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("SWINGLEFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
        if (type == GunAnimation.ALT_CYCLE) return new BusAnimation()
                .addBus("SWEEPTURN", new BusAnimationSequence().addPos(1, 0, 0, 100, IType.LINEAR).hold(350).addPos(0, 0, 0, 500, IType.LINEAR))
                .addBus("SWEEPCUT", new BusAnimationSequence().hold(100).addPos(1, 0, 0, 250, IType.SIN_DOWN).hold(100).addPos(0, 0, 0, 500, IType.SIN_FULL));
        return null;
    }
}
