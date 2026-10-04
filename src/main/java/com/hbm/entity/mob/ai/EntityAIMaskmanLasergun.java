package com.hbm.entity.mob.ai;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.Constants;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;



public class EntityAIMaskmanLasergun extends Goal {

    /**
     * 1.7 played "hbm:weapon.teslaShoot" (sounds/weapon/teslaShoot.ogg), not the sedna "weapon.fire.tesla" shot.
     * sounds.json carries the event as weapon.teslashoot, but nothing registers it, so it plays as a direct event.
     */
    private static final SoundEvent TESLA_SHOOT =
            SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, "weapon.teslashoot"));

    private final PathfinderMob owner;
    private Attack attack;
    private int timer;
    private int attackCount;

    public EntityAIMaskmanLasergun(PathfinderMob owner) {
        this.owner = owner;
        this.attack = Attack.values()[owner.getRandom().nextInt(3)];
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.owner.getTarget();
        // 1.7 checked only for null here - a dead target still counts, unlike the minigun's goal
        return target != null && Math.sqrt(this.owner.distanceToSqr(target)) > 10D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.owner.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {

        LivingEntity target = this.owner.getTarget();
        if (target == null) return;

        this.timer--;

        if (this.timer <= 0) {
            this.timer = this.attack.delay;
            fire(target);
            this.attackCount++;

            if (this.attackCount >= this.attack.amount) {
                this.attackCount = 0;
                int next = this.attack.ordinal() + this.owner.getRandom().nextInt(Attack.values().length - 1);
                this.attack = Attack.values()[next % Attack.values().length];
            }
        }
    }

    private void fire(LivingEntity target) {

        switch (this.attack) {

            case ORB -> {
                EntityBulletBaseMK4 orb = XFactoryNPC.aimed(this.owner, XFactoryNPC.maskman_orb,
                        XFactoryNPC.MASKMAN_ORB_DAMAGE, target, 0F);
                if (orb == null) return;
                orb.setDeltaMovement(orb.getDeltaMovement().add(0D, 0.5D, 0D));
                this.owner.level().addFreshEntity(orb);
                play(TESLA_SHOOT);
            }

            case MISSILE -> {
                EntityBulletBaseMK4 missile = XFactoryNPC.aimed(this.owner, XFactoryNPC.maskman_rocket,
                        XFactoryNPC.MASKMAN_ROCKET_DAMAGE, target, 0F);
                if (missile == null) return;
                // 1.7: the rocket is thrown up and gravity walks it in, overwriting the aimed heading outright
                Vec3 flat = new Vec3(target.getX() - this.owner.getX(), 0D, target.getZ() - this.owner.getZ());
                missile.setDeltaMovement(flat.x * 0.05D,
                        0.5D + this.owner.getRandom().nextDouble() * 0.5D, flat.z * 0.05D);
                this.owner.level().addFreshEntity(missile);
                play(ModSounds.GUN_UNDERBARREL_FIRE.get());
            }

            case SPLASH -> {
                for (int i = 0; i < 5; i++) {
                    EntityBulletBaseMK4 tracer = XFactoryNPC.aimed(this.owner, XFactoryNPC.maskman_tracer,
                            XFactoryNPC.MASKMAN_TRACER_DAMAGE, target, 0.05F);
                    if (tracer != null) this.owner.level().addFreshEntity(tracer);
                }
            }
        }
    }

    private void play(SoundEvent sound) {
        this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                sound, SoundSource.HOSTILE, 1F, 1F);
    }

    private enum Attack {

        ORB(60, 5),
        MISSILE(10, 10),
        SPLASH(40, 3);

        final int delay;
        final int amount;

        Attack(int delay, int amount) {
            this.delay = delay;
            this.amount = amount;
        }
    }
}
