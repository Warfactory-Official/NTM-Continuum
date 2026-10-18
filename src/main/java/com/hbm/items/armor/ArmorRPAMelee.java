package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.factory.XFactoryPA;
import com.hbm.sound.ModSounds;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public final class ArmorRPAMelee extends ArmorPAMeleeBase {
    @Override protected float lightDamage() { return XFactoryPA.MELEE_RPA_SWINGS_DAMAGE.get(); }
    @Override protected float heavyDamage() { return XFactoryPA.MELEE_RPA_SLAP_DAMAGE.get(); }
    @Override protected float largeMult() { return XFactoryPA.MELEE_RPA_LARGE_MULT.get(); }
    @Override protected int lightCooldown() { return 14; }
    @Override protected int heavyCooldown() { return 20; }
    @Override protected boolean isLightHit(int timer) { return timer == 3 || timer == 9; }
    @Override protected boolean isHeavyHit(int timer) { return timer == 8; }
    @Override protected boolean refiresOnHold() { return true; }
    @Override protected SoundEvent hitSound() { return ModSounds.GUN_SMACK.get(); }

    @Override public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
        if (type == GunAnimation.EQUIP) return new BusAnimation().addBus("EQUIP",
                new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 250, IType.SIN_DOWN));
        if (type == GunAnimation.CYCLE) return new BusAnimation()
                .addBus("SWINGRIGHT", new BusAnimationSequence().addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                .addBus("SWINGLEFT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
        if (type == GunAnimation.ALT_CYCLE) return new BusAnimation()
                .addBus("SLAPTURN", new BusAnimationSequence().addPos(1, 0, 0, 250, IType.LINEAR).hold(150).addPos(0, 0, 0, 350, IType.LINEAR))
                .addBus("SLAP", new BusAnimationSequence().hold(250).addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL));
        return null;
    }
}
