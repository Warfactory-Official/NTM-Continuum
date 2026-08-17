package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The gun state machine, ported from 1.12.2 {@code GunStateDecider}.
 *
 * <p>It runs on the tick a gun's timer runs out — that is the whole design: every action sets a state
 * and a delay, and when the delay expires the decider works out what happens next. Draws finish, jams
 * clear, reloads either cycle again or end, and a held trigger on an automatic refires.
 */
public class GunStateDecider {

    /**
     * The standard decider: handles an automatic primary receiver plus one receiver's reload cycle,
     * with draw delays and both semi and full auto.
     */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_DECIDER = (stack, ctx) -> {
        int index = ctx.configIndex;
        GunState lastState = ItemGunBaseNT.getState(stack, index);
        deciderStandardFinishDraw(stack, lastState, index);
        deciderStandardClearJam(stack, lastState, index);
        deciderStandardReload(stack, ctx, lastState, 0, index);
        deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getPrimary(stack, index) && ItemGunBaseNT.getMode(stack, index) == 0);
    };

    /** DRAWING to IDLE. */
    public static void deciderStandardFinishDraw(ItemStack stack, GunState lastState, int index) {
        if (lastState == GunState.DRAWING) {
            ItemGunBaseNT.setState(stack, index, GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    /** JAMMED to IDLE. */
    public static void deciderStandardClearJam(ItemStack stack, GunState lastState, int index) {
        if (lastState == GunState.JAMMED) {
            ItemGunBaseNT.setState(stack, index, GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    /**
     * Performs one reload cycle. If more can still be loaded — a shotgun tube — it stays in RELOADING
     * and goes round again; otherwise the gun either jams or finishes the reload.
     */
    public static void deciderStandardReload(ItemStack stack, LambdaContext ctx, GunState lastState,
            int recIndex, int gunIndex) {
        if (lastState != GunState.RELOADING) return;

        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        GunConfig cfg = ctx.config;
        Receiver rec = cfg.getReceivers(stack)[recIndex];
        IMagazine<BulletConfig> mag = rec.getMagazine(stack);

        mag.reloadAction(stack, ctx.inventory);
        boolean cancel = ItemGunBaseNT.getReloadCancel(stack);

        if (!cancel && mag.canReload(stack, ctx.inventory)) {
            ItemGunBaseNT.setState(stack, gunIndex, GunState.RELOADING);
            ItemGunBaseNT.setTimer(stack, gunIndex, rec.getReloadCycleDuration(stack));
            ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD_CYCLE, gunIndex);
        } else {
            // A worn gun is most likely to bind up exactly when you finish reloading it.
            if (getStandardJamChance(stack, cfg, gunIndex) > entity.getRandom().nextFloat()) {
                ItemGunBaseNT.setState(stack, gunIndex, GunState.JAMMED);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getJamDuration(stack));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.JAMMED, gunIndex);
            } else {
                ItemGunBaseNT.setState(stack, gunIndex, GunState.DRAWING);
                int duration = rec.getReloadEndDuration(stack)
                        + (mag.getAmountBeforeReload(stack) <= 0 ? rec.getReloadCockOnEmptyPost(stack) : 0);
                ItemGunBaseNT.setTimer(stack, gunIndex, duration);
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD_END, gunIndex);
            }

            ItemGunBaseNT.setReloadCancel(stack, false);
        }

        mag.setAmountAfterReload(stack, mag.getAmount(stack, ctx.inventory));
    }

    /** Jam chance: none below two-thirds worn, then climbing to certainty by the end of its life. */
    public static float getStandardJamChance(ItemStack stack, GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.66F) return 0F;
        return Math.min((percent - 0.66F) * 4F, 1F);
    }

    /**
     * Resolves the cooldown after a shot: refire if the gun is automatic and the trigger is still held,
     * otherwise fall back to idle — or start a reload, for guns that reload themselves when empty.
     */
    public static void deciderAutoRefire(ItemStack stack, LambdaContext ctx, GunState lastState,
            int recIndex, int gunIndex, BooleanSupplier refireCondition) {
        if (lastState != GunState.COOLDOWN) return;

        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        GunConfig cfg = ctx.config;
        Receiver rec = cfg.getReceivers(stack)[recIndex];

        if (rec.getRefireOnHold(stack) && refireCondition.getAsBoolean()) {
            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);
                ItemGunBaseNT.setState(stack, gunIndex, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterFire(stack));
                Lego.playFireSound(entity, rec, stack);

                int remaining = rec.getRoundsPerCycle(stack) - 1;
                for (int i = 0; i < remaining; i++) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);
                }
            } else if (rec.getDoesDryFireAfterAuto(stack)) {
                ItemGunBaseNT.setState(stack, gunIndex,
                        rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterDryFire(stack));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, gunIndex);
            } else {
                idle(stack, gunIndex);
            }
            return;
        }

        // Reload-on-empty, deliberately only for guns that do not refire on hold.
        if (rec.getReloadOnEmpty(stack) && rec.getMagazine(stack).getAmount(stack, ctx.inventory) <= 0) {
            ItemGunBaseNT.setIsAiming(stack, false);
            IMagazine<BulletConfig> mag = rec.getMagazine(stack);

            if (mag.canReload(stack, ctx.inventory)) {
                int loaded = mag.getAmount(stack, ctx.inventory);
                mag.setAmountBeforeReload(stack, loaded);
                ItemGunBaseNT.setState(stack, gunIndex, GunState.RELOADING);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getReloadBeginDuration(stack)
                        + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD, gunIndex);
            } else {
                idle(stack, gunIndex);
            }
            return;
        }

        idle(stack, gunIndex);
    }

    private static void idle(ItemStack stack, int gunIndex) {
        ItemGunBaseNT.setState(stack, gunIndex, GunState.IDLE);
        ItemGunBaseNT.setTimer(stack, gunIndex, 0);
    }
}
