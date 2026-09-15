package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.packet.HbmPackets;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Original-hbm's tick-indexed Sedna sound timelines.
 *
 * <p>Positional one-shots use the seeded packet path. The source casing callbacks are kept beside their
 * authored timelines below; client loop state lives in {@code GunEffectState}.
 */
public final class Orchestras {

    private Orchestras() {}

    private record Cue(GunAnimation[] types, int timer, Supplier<SoundEvent> sound, float volume, float pitch) {}

    private static Cue cue(GunAnimation type, int timer, Supplier<SoundEvent> sound, float volume, float pitch) {
        return new Cue(new GunAnimation[] {type}, timer, sound, volume, pitch);
    }

    private static Cue cue(int timer, Supplier<SoundEvent> sound, float volume, float pitch,
            GunAnimation... types) {
        return new Cue(types, timer, sound, volume, pitch);
    }

    private enum Muzzle { NONE, CYCLE, CYCLE_OR_ALT, NI4NI_CYCLE }

    @FunctionalInterface
    private interface CasingCallback {
        void accept(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, GunAnimation type, int timer);
    }

    private static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> timeline(Cue... cues) {
        return timeline(Muzzle.NONE, null, cues);
    }

    /**
     * Shared server tick plumbing only. Casing callbacks are supplied by each source orchestra;
     * there is deliberately no fallback casing or universal CYCLE emission here.
     */
    private static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> timeline(Muzzle muzzle,
            CasingCallback casing, Cue... cues) {
        return (stack, ctx) -> {
            if (ctx.entity.level().isClientSide()) return;
            GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
            int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
            if (((muzzle == Muzzle.CYCLE || (muzzle == Muzzle.NI4NI_CYCLE
                    && stack.getItem() == com.hbm.items.ModGunItems.gun_n_i_4_n_i.get()))
                    && type == GunAnimation.CYCLE && timer == 0)
                    || (muzzle == Muzzle.CYCLE_OR_ALT
                    && (type == GunAnimation.CYCLE || type == GunAnimation.ALT_CYCLE) && timer == 0))
                HbmPackets.sendMuzzleFlash(ctx.entity);
            if (casing != null) casing.accept(stack, ctx, type, timer);
            for (Cue cue : cues) {
                if (cue.timer() != timer || !contains(cue.types(), type)) continue;
                play(ctx, cue.sound(), cue.volume(), cue.pitch());
            }
        };
    }

    private static boolean contains(GunAnimation[] types, GunAnimation wanted) {
        for (GunAnimation type : types) if (type == wanted) return true;
        return false;
    }

    private static void play(ItemGunBaseNT.LambdaContext ctx, Supplier<SoundEvent> sound,
            float volume, float pitch) {
        ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                sound.get(), SoundSource.PLAYERS, volume, pitch);
    }

    private static IMagazine<?> magazine(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, int rail) {
        Receiver[] receivers = ctx.config.getReceivers(stack);
        return rail >= 0 && rail < receivers.length ? receivers[rail].getMagazine(stack) : null;
    }

    private static SpentCasing casing(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, int rail) {
        return CasingCreator.getCasing(magazine(stack, ctx, rail), stack, ctx.inventory);
    }

    private static final CasingCallback DEBUG_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.RELOAD || timer != 16) return;
        IMagazine<?> mag = magazine(stack, ctx, 0);
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null && mag != null) for (int i = 0; i < mag.getCapacity(stack); i++)
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.25D, -0.125D, -0.125D,
                    -0.05D, 0D, 0D, 0.01D, casing.getName());
    };

    private static final CasingCallback CARBINE_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, 0.21D, -0.06D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 2.5F,
                    2.5F + (float) ctx.entity.getRandom().nextGaussian() * 2F, casing.getName(), true, 60,
                    0.5D, 20);
        }
    };

    private static final CasingCallback HENRY_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 14) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.5D, -0.125D,
                    aiming ? -0.125D : -0.375D, 0D, 0.12D, -0.12D, 0.01D,
                    -7.5F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 1.5F, casing.getName(), true, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback NOPIP_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.RELOAD || timer != 16) return;
        IMagazine<?> mag = magazine(stack, ctx, 0);
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null && mag != null) for (int i = 0; i < mag.getCapacity(stack); i++)
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.25D, -0.125D, -0.125D,
                    -0.05D, 0D, 0D, 0.01D,
                    -6.5F + (float) ctx.entity.getRandom().nextGaussian() * 3F,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName());
    };

    private static final CasingCallback HANGMAN_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.RELOAD || timer != 10) return;
        IMagazine<?> mag = magazine(stack, ctx, 0);
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null && mag != null) for (int i = 0; i < mag.getCapacity(stack); i++)
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.25D, -0.25D, -0.125D,
                    -0.05D, 0D, 0D, 0.01D,
                    -6.5F + (float) ctx.entity.getRandom().nextGaussian() * 3F,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName());
    };

    private static final CasingCallback MARESLEG_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 14) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D, -0.125D,
                    aiming ? -0.125D : -0.375D, 0D, 0.18D, -0.12D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback MARESLEG_SHORT_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 14) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D, -0.125D,
                    aiming ? -0.125D : -0.375D, 0D, -0.08D, 0D, 0.01D,
                    -15F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback MARESLEG_AKIMBO_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 14) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            int offset = ctx.configIndex == 0 ? -1 : 1;
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D, -0.125D,
                    aiming ? -0.125D * offset : -0.375D * offset, 0D, -0.08D, 0D, 0.01D,
                    -15F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback GREASEGUN_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 2) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.55D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -7.5F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    12F + (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName());
        }
    };

    private static final CasingCallback UZI_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -2.5F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    10F + ctx.entity.getRandom().nextFloat() * 15F, casing.getName());
        }
    };

    private static final CasingCallback UZI_AKIMBO_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            int mult = ctx.configIndex == 0 ? -1 : 1;
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D, -0.125D,
                    -0.375D * mult, 0D, 0.18D, -0.12D * mult, 0.01D,
                    -2.5F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (10F + ctx.entity.getRandom().nextFloat() * 15F) * mult, casing.getName());
        }
    };

    private static final CasingCallback DOUBLE_BARREL_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.RELOAD || timer != 12) return;
        IMagazine<?> mag = magazine(stack, ctx, 0);
        int toEject = mag == null ? 0 : mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) for (int i = 0; i < toEject; i++)
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0D, -0.1875D, -0.375D,
                    -0.24D, 0.18D, 0D, 0.01D,
                    -20F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
    };

    private static final CasingCallback MINIGUN_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        boolean aiming = ItemGunBaseNT.getIsAiming(stack);
        int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XWeaponModManager.ID_MINIGUN_SPEED) ? 3 : 1;
        for (int i = 0; i < rounds; i++) {
            SpentCasing casing = casing(stack, ctx, 0);
            if (casing != null)
                CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, aiming ? 0.125D : 0.5D,
                        aiming ? -0.125D : -0.25D, aiming ? -0.25D : -0.5D, 0D, 0.18D, -0.12D, 0.01D,
                        (float) ctx.entity.getRandom().nextGaussian() * 15F,
                        (float) ctx.entity.getRandom().nextGaussian() * 15F, casing.getName());
        }
    };

    private static final CasingCallback MINIGUN_DUAL_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        int index = ctx.configIndex == 0 ? -1 : 1;
        int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XWeaponModManager.ID_MINIGUN_SPEED) ? 3 : 1;
        for (int i = 0; i < rounds; i++) {
            SpentCasing casing = casing(stack, ctx, 0);
            if (casing != null)
                CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.25D, -0.25D, -0.5D * index,
                        0D, 0.18D, -0.12D * index, 0.01D,
                        (float) ctx.entity.getRandom().nextGaussian() * 15F,
                        (float) ctx.entity.getRandom().nextGaussian() * 15F, casing.getName());
        }
    };

    private static final CasingCallback MAS36_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 12) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack)
                    && !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, -0.05D, 0.2D, -0.025D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 10F,
                    (float) ctx.entity.getRandom().nextGaussian() * 12.5F, casing.getName(), true, 60, 0.5D, 10);
        }
    };

    private static final CasingCallback LIBERATOR_CASINGS = (stack, ctx, type, timer) -> {
        IMagazine<?> mag = magazine(stack, ctx, 0);
        if (mag == null) return;
        int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
        if (type == GunAnimation.RELOAD && timer == 4) {
            SpentCasing casing = casing(stack, ctx, 0);
            if (casing != null) for (int i = 0; i < toEject; i++)
                CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.625D, -0.1875D, -0.375D,
                        -0.12D, 0.18D, 0D, 0.01D,
                        -15F + (float) ctx.entity.getRandom().nextGaussian() * 7.5F,
                        (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
        }
        if (type == GunAnimation.INSPECT && timer == 4 && toEject > 0) {
            SpentCasing casing = casing(stack, ctx, 0);
            if (casing != null) for (int i = 0; i < toEject; i++)
                CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.625D, -0.1875D, -0.375D,
                        -0.12D, 0.18D, 0D, 0.01D,
                        -15F * (float) ctx.entity.getRandom().nextGaussian() * 7.5F,
                        (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
            mag.setAmountAfterReload(stack, 0);
        }
    };

    private static final CasingCallback SPAS_CASINGS = (stack, ctx, type, timer) -> {
        if ((type != GunAnimation.CYCLE && type != GunAnimation.ALT_CYCLE) || timer != 10) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -3F + (float) ctx.entity.getRandom().nextGaussian() * 2.5F,
                    -15F + ctx.entity.getRandom().nextFloat() * -5F, casing.getName());
        }
    };

    private static final CasingCallback SHREDDER_SEXY_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 2) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? -0.0625D : -0.125D, aiming ? -0.125D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 2.5F,
                    (float) ctx.entity.getRandom().nextGaussian() * -20F + 15F, casing.getName(), false, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback BOLTER_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.5D,
                    aiming ? 0D : -0.125D, aiming ? -0.0625D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    10F + ctx.entity.getRandom().nextFloat() * 10F, casing.getName());
        }
    };

    private static final CasingCallback AM180_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.4375D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, -0.06D, 0D, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 10F,
                    (float) ctx.entity.getRandom().nextGaussian() * 10F, casing.getName());
        }
    };

    private static final CasingCallback STAR_F_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.1875D, 0D, 0.18D, -0.12D, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    12.5F + ctx.entity.getRandom().nextFloat() * 5F, casing.getName());
        }
    };

    private static final CasingCallback STAR_F_AKIMBO_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            int side = ctx.configIndex == 0 ? -1 : 1;
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.3125D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.1875D * side, 0D, 0.18D, -0.12D * side, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    12.5F + ctx.entity.getRandom().nextFloat() * 5F, casing.getName());
        }
    };

    private static final CasingCallback AMAT_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 12) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.125D, -0.25D, -0.05D, 0.2D, -0.025D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 10F,
                    (float) ctx.entity.getRandom().nextGaussian() * 12.5F, casing.getName(), true, 60, 0.5D, 10);
        }
    };

    private static final CasingCallback M2_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.3125D, 0D, 0.06D, -0.18D, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 20F,
                    12.5F + (float) ctx.entity.getRandom().nextGaussian() * 7.5F, casing.getName());
        }
    };

    private static final CasingCallback G3_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack)
                    && !(stack.getItem() == com.hbm.items.ModGunItems.gun_g3_zebra.get()
                    || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE));
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.5D,
                    aiming ? 0D : -0.125D, aiming ? 0D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    12.5F + ctx.entity.getRandom().nextFloat() * 5F, casing.getName());
        }
    };

    private static final CasingCallback STG77_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, aiming ? 0.125D : 0.25D,
                    aiming ? -0.125D : -0.25D, aiming ? -0.125D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    7.5F + ctx.entity.getRandom().nextFloat() * 5F, casing.getName(), false, 0, 0D, 0, 0.125D);
        }
    };

    private static final CasingCallback LAG_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.375D,
                    aiming ? 0D : -0.0625D, aiming ? 0D : -0.25D, 0D, 0.18D, -0.12D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 5F,
                    10F + ctx.entity.getRandom().nextFloat() * 10F, casing.getName());
        }
    };

    private static final CasingCallback FLAREGUN_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.RELOAD || timer != 4) return;
        IMagazine<?> mag = magazine(stack, ctx, 0);
        if (mag == null || mag.getAmountAfterReload(stack) <= 0) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.625D, -0.125D,
                    aiming ? -0.125D : -0.375D, -0.12D, 0.18D, 0D, 0.01D,
                    -15F + (float) ctx.entity.getRandom().nextGaussian() * 7.5F,
                    (float) ctx.entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
        }
        mag.setAmountBeforeReload(stack, 0);
    };

    private static final CasingCallback CONGOLAKE_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 15) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.625D,
                    aiming ? -0.0625D : -0.25D, aiming ? 0D : -0.375D, 0D, 0.18D, 0.12D, 0.01D,
                    -5F + (float) ctx.entity.getRandom().nextGaussian() * 3.5F,
                    -10F + ctx.entity.getRandom().nextFloat() * 5F, casing.getName(), true, 60, 0.5D, 20);
        }
    };

    private static final CasingCallback MK108_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 2) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.5D,
                    aiming ? -0.125D : -0.3125D, aiming ? -0.375D : -0.3125D, 0D, 0.18D, -0.12D, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 2.5F,
                    (float) ctx.entity.getRandom().nextGaussian() * -20F + 15F, casing.getName(), true, 60, 0.5D, 10);
        }
    };

    private static final CasingCallback ABERRATOR_CASINGS = (stack, ctx, type, timer) -> {
        if (type != GunAnimation.CYCLE || timer != 1) return;
        SpentCasing casing = casing(stack, ctx, 0);
        if (casing != null) {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            int cba = (stack.getItem() == com.hbm.items.ModGunItems.gun_aberrator_eott.get()
                    && ctx.configIndex == 0) ? -1 : 1;
            CasingCreator.composeEffect(ctx.entity.level(), ctx.entity, 0.5D,
                    aiming ? 0D : -0.125D, aiming ? -0.0625D : -0.25D * cba,
                    -0.05D, 0.25D, -0.05D * cba, 0.01D,
                    -10F + (float) ctx.entity.getRandom().nextGaussian() * 10F,
                    (float) ctx.entity.getRandom().nextGaussian() * 12.5F, casing.getName());
        }
    };

    /** Source's explicit legacy/modern packet path; ordinary cues stay on vanilla's seeded sound packet. */
    private static void play(ItemGunBaseNT.LambdaContext ctx, Supplier<SoundEvent> sound,
            float volume, float pitch, boolean legacy) {
        ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                sound.get(), SoundSource.PLAYERS, volume, pitch);
    }

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> DEBUG_ORCHESTRA = timeline(Muzzle.NONE,
            DEBUG_CASINGS,
            cue(GunAnimation.RELOAD, 3, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 10, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 34, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.CYCLE, 11, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 11, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.INSPECT, 3, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.INSPECT, 16, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_PEPPERBOX = timeline(Muzzle.CYCLE,
            null,
            cue(GunAnimation.RELOAD, 24, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 55, ModGunSounds.REVOLVER_SPIN, 1F, 1F),
            cue(GunAnimation.CYCLE, 21, ModGunSounds.REVOLVER_COCK, 1F, 0.6F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 11, ModGunSounds.REVOLVER_COCK, 1F, 0.6F),
            cue(GunAnimation.INSPECT, 3, ModGunSounds.REVOLVER_SPIN, 1F, 1F),
            cue(GunAnimation.JAMMED, 28, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.JAMMED, 45, ModSounds.DRY_FIRE_CLICK, 1F, 0.6F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_ATLAS = timeline(Muzzle.CYCLE,
            null,
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 44, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.CYCLE, 5, ModGunSounds.REVOLVER_COCK, 1F, 0.9F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 5, ModGunSounds.REVOLVER_COCK, 1F, 0.9F),
            cue(GunAnimation.INSPECT, 2, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 24, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 12, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.JAMMED, 34, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_DANI = ORCHESTRA_ATLAS;

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> HENRY_TIMELINE = timeline(Muzzle.CYCLE,
            HENRY_CASINGS,
            cue(GunAnimation.RELOAD, 8, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD_CYCLE, 0, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 0, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 0, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 12, ModGunSounds.LEVER_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 36, ModGunSounds.LEVER_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 44, ModGunSounds.LEVER_COCK, 1F, 1F),
            cue(GunAnimation.CYCLE, 12, ModGunSounds.LEVER_COCK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 12, ModGunSounds.LEVER_COCK, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_HENRY = (stack, ctx) -> {
        HENRY_TIMELINE.accept(stack, ctx);
        if (!ctx.entity.level().isClientSide() && ItemGunBaseNT.getLastAnim(stack, ctx.configIndex)
                == GunAnimation.RELOAD_END && ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex) == 12
                && ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmountBeforeReload(stack) <= 0) {
            play(ctx, ModGunSounds.LEVER_COCK, 1F, 1F);
        }
    };

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_GREASEGUN = timeline(Muzzle.CYCLE,
            GREASEGUN_CASINGS,
            cue(GunAnimation.EQUIP, 5, ModGunSounds.LATCH_OPEN, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 11, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 24, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.INSPECT, 5, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.INSPECT, 26, ModGunSounds.MAG_SMALL_INSERT, 1F, 1.25F),
            cue(GunAnimation.JAMMED, 11, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 26, ModGunSounds.PISTOL_COCK, 1F, 0.8F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MARESLEG = timeline(Muzzle.CYCLE,
            MARESLEG_CASINGS,
            cue(GunAnimation.RELOAD, 8, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_CYCLE, 0, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 17, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 29, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MARESLEG_SHORT = timeline(Muzzle.CYCLE,
            MARESLEG_SHORT_CASINGS,
            cue(GunAnimation.RELOAD, 8, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_CYCLE, 0, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 17, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 29, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MARESLEG_AKIMBO = timeline(Muzzle.CYCLE,
            MARESLEG_AKIMBO_CASINGS,
            cue(GunAnimation.RELOAD, 8, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_CYCLE, 0, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 2, ModGunSounds.REVOLVER_COCK, 1F, 0.7F),
            cue(GunAnimation.JAMMED, 17, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 29, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.LEVER_COCK, 1F, 0.8F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FLAREGUN = timeline(Muzzle.NONE,
            FLAREGUN_CASINGS,
            cue(GunAnimation.RELOAD, 0, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.CANISTER_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 24, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.JAMMED, 10, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 29, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.CYCLE, 12, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 12, ModGunSounds.REVOLVER_COCK, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_NOPIP = timeline(Muzzle.CYCLE,
            NOPIP_CASINGS,
            cue(GunAnimation.RELOAD, 3, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 10, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 34, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.CYCLE, 11, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 11, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.INSPECT, 3, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.INSPECT, 16, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_CARBINE = timeline(Muzzle.CYCLE,
            CARBINE_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 2, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 2, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 31, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.INSPECT, 6, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_AM180 = timeline(Muzzle.CYCLE,
            AM180_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 6, ModGunSounds.PISTOL_COCK, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 6, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.IMPACT, 0.25F, 1F),
            cue(GunAnimation.RELOAD, 48, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 54, ModGunSounds.PISTOL_COCK, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 6, ModGunSounds.PISTOL_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 20, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.INSPECT, 6, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 53, ModGunSounds.MAG_INSERT, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_LIBERATOR = timeline(Muzzle.CYCLE,
            LIBERATOR_CASINGS,
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_COCK, 1F, 0.75F),
            cue(GunAnimation.RELOAD_CYCLE, 5, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD_END, 2, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 2, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 12, ModGunSounds.REVOLVER_COCK, 1F, 0.75F),
            cue(GunAnimation.JAMMED, 26, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_CONGOLAKE = timeline(Muzzle.CYCLE,
            CONGOLAKE_CASINGS,
            cue(0, ModGunSounds.GRENADE_RELOAD, 1F, 1F, GunAnimation.RELOAD, GunAnimation.RELOAD_CYCLE),
            cue(GunAnimation.INSPECT, 9, ModGunSounds.GRENADE_OPEN, 1F, 1F),
            cue(GunAnimation.INSPECT, 27, ModGunSounds.GRENADE_CLOSE, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FLAMER = timeline(
            cue(GunAnimation.RELOAD, 15, ModGunSounds.LATCH_OPEN, 1F, 1F),
            cue(GunAnimation.RELOAD, 35, ModGunSounds.IMPACT, 0.5F, 1F),
            cue(GunAnimation.RELOAD, 60, ModGunSounds.REVOLVER_CLOSE, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 70, ModGunSounds.CANISTER_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 85, ModGunSounds.VALVE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FLAMER_DAYBREAKER = ORCHESTRA_FLAMER;
    // The source chemthrower owns only its client loop; it has no reload foley callback.
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_CHEMTHROWER = timeline();

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_LAG = timeline(Muzzle.CYCLE,
            LAG_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 8, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 8, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.JAMMED, 20, ModGunSounds.IMPACT, 0.5F, 1.6F),
            cue(GunAnimation.JAMMED, 36, ModGunSounds.MAG_INSERT, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_UZI = timeline(Muzzle.CYCLE,
            UZI_CASINGS,
            cue(GunAnimation.EQUIP, 8, ModGunSounds.LATCH_OPEN, 1F, 1.25F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 4, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 17, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 31, ModGunSounds.PISTOL_COCK, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_UZI_AKIMBO = timeline(Muzzle.CYCLE,
            UZI_AKIMBO_CASINGS,
            cue(GunAnimation.EQUIP, 8, ModGunSounds.LATCH_OPEN, 1F, 1.25F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 4, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 17, ModGunSounds.PISTOL_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 31, ModGunSounds.PISTOL_COCK, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_SPAS = timeline(Muzzle.CYCLE_OR_ALT,
            SPAS_CASINGS,
            cue(8, ModGunSounds.SHOTGUN_COCK, 1F, 1F, GunAnimation.CYCLE, GunAnimation.ALT_CYCLE),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 8, ModGunSounds.SHOTGUN_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD_CYCLE, 5, ModGunSounds.SHOTGUN_LOAD, 1F, 1F),
            cue(GunAnimation.INSPECT, 5, ModGunSounds.SHOTGUN_OPEN, 1F, 1F),
            cue(GunAnimation.INSPECT, 18, ModGunSounds.SHOTGUN_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 18, ModGunSounds.WHACK, 1F, 1F),
            cue(GunAnimation.JAMMED, 25, ModGunSounds.WHACK, 1F, 1F),
            cue(GunAnimation.JAMMED, 29, ModGunSounds.SHOTGUN_CLOSE, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_PANERSCHRECK = timeline(Muzzle.CYCLE,
            null,
            cue(GunAnimation.RELOAD, 30, ModGunSounds.CANISTER_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_STAR_F = timeline(Muzzle.CYCLE,
            STAR_F_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.9F),
            cue(GunAnimation.CYCLE_DRY, 5, ModGunSounds.PISTOL_COCK, 1F, 1.1F),
            cue(GunAnimation.RELOAD, 5, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.RELOAD, 22, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.JAMMED, 15, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 19, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.JAMMED, 23, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 27, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.INSPECT, 7, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_STAR_F_AKIMBO = timeline(Muzzle.CYCLE,
            STAR_F_AKIMBO_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.9F),
            cue(GunAnimation.CYCLE_DRY, 5, ModGunSounds.PISTOL_COCK, 1F, 1.1F),
            cue(GunAnimation.RELOAD, 5, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.RELOAD, 22, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.JAMMED, 15, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 19, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.JAMMED, 23, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 27, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F),
            cue(GunAnimation.INSPECT, 7, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 1.1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_G3 = timeline(Muzzle.CYCLE,
            G3_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 5, ModGunSounds.PISTOL_COCK, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 4, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 28, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.JAMMED, 16, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 20, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 26, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 30, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_STINGER =
            timeline(Muzzle.CYCLE, null, cue(GunAnimation.RELOAD, 30, ModGunSounds.CANISTER_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MK108 = timeline(Muzzle.CYCLE,
            MK108_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_CLOSE, 1F, 0.65F),
            cue(GunAnimation.RELOAD, 10, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.MAG_REMOVE, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 60, ModGunSounds.IMPACT, 0.5F, 1F),
            cue(GunAnimation.RELOAD, 90, ModGunSounds.MAG_INSERT, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 100, ModGunSounds.MAG_SMALL_INSERT, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 125, ModGunSounds.REVOLVER_CLOSE, 1F, 0.65F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_AMAT = timeline(Muzzle.CYCLE,
            AMAT_CASINGS,
            cue(GunAnimation.EQUIP, 10, ModGunSounds.REVOLVER_COCK, 0.5F, 1.25F),
            cue(GunAnimation.EQUIP, 15, ModGunSounds.REVOLVER_CLOSE, 0.5F, 1.25F),
            cue(GunAnimation.CYCLE, 7, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.CYCLE, 16, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 20, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.RELOAD, 41, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 5, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 12, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 16, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 23, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.REVOLVER_COCK, 0.5F, 1F),
            cue(GunAnimation.INSPECT, 45, ModGunSounds.REVOLVER_CLOSE, 0.5F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_M2 = timeline(Muzzle.CYCLE,
            M2_CASINGS,
            cue(GunAnimation.EQUIP, 0, ModSounds.TURRET_HOWARD_RELOAD, 1F, 1F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_SHREDDER = timeline(Muzzle.CYCLE,
            null,
            cue(GunAnimation.CYCLE, 2, ModGunSounds.SHREDDER_CYCLE, 0.25F, 1.5F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModGunSounds.SHREDDER_CYCLE, 0.25F, 1.5F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.INSPECT, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 28, ModGunSounds.MAG_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_SHREDDER_SEXY = timeline(Muzzle.CYCLE,
            SHREDDER_SEXY_CASINGS,
            cue(GunAnimation.CYCLE, 2, ModGunSounds.SHREDDER_CYCLE, 0.25F, 1.5F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.INSPECT, 2, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 28, ModGunSounds.MAG_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_QUADRO =
            timeline(Muzzle.CYCLE, null, cue(GunAnimation.RELOAD, 30, ModGunSounds.CANISTER_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MINIGUN = timeline(Muzzle.CYCLE,
            MINIGUN_CASINGS,
            cue(GunAnimation.CYCLE, 1, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 1, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MINIGUN_DUAL = timeline(Muzzle.CYCLE,
            MINIGUN_DUAL_CASINGS,
            cue(GunAnimation.CYCLE, 1, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 1, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.REVOLVER_SPIN, 1F, 0.75F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MISSILE_LAUNCHER = timeline(Muzzle.CYCLE,
            null,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1.25F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.BOLT_OPEN, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 30, ModGunSounds.CANISTER_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 42, ModGunSounds.BOLT_CLOSE, 1F, 0.9F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.BOLT_OPEN, 1F, 0.9F),
            cue(GunAnimation.INSPECT, 27, ModGunSounds.BOLT_CLOSE, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 0, ModGunSounds.BOLT_OPEN, 1F, 0.9F),
            cue(GunAnimation.JAMMED, 27, ModGunSounds.BOLT_CLOSE, 1F, 0.9F));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_TESLA = timeline(
            cue(GunAnimation.CYCLE, 2, ModGunSounds.SHREDDER_CYCLE, 0.25F, 1.25F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.CYCLE_DRY, 2, ModGunSounds.SHREDDER_CYCLE, 0.25F, 1.25F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_LASER_PISTOL = timeline(
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1.5F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 10, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1.25F),
            cue(GunAnimation.RELOAD, 34, ModGunSounds.MAG_SMALL_INSERT, 1F, 1.25F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.REVOLVER_CLOSE, 1F, 1.25F),
            cue(GunAnimation.JAMMED, 10, ModGunSounds.REVOLVER_COCK, 1F, 1F),
            cue(GunAnimation.JAMMED, 15, ModGunSounds.REVOLVER_CLOSE, 1F, 1.25F),
            cue(GunAnimation.JAMMED, 30, ModGunSounds.IMPACT, 0.25F, 1.5F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_STG77 = timeline(Muzzle.CYCLE,
            STG77_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 5, ModGunSounds.PISTOL_COCK, 1F, 0.9F),
            cue(GunAnimation.CYCLE_DRY, 40, ModSounds.DRY_FIRE_CLICK, 0.25F, 1.25F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 16, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.IMPACT, 0.25F, 1.25F),
            cue(GunAnimation.RELOAD, 38, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 43, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.REVOLVER_CLOSE, 1F, 0.9F),
            cue(GunAnimation.INSPECT, 11, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 72, ModGunSounds.MAG_SMALL_INSERT, 1F, 1F),
            cue(GunAnimation.INSPECT, 84, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_TAU = timeline(
            cue(GunAnimation.CYCLE, 0, ModGunSounds.FIRE_TAU, 0.5F, 1F),
            cue(GunAnimation.ALT_CYCLE, 0, ModGunSounds.FIRE_TAU, 0.5F, 0.8F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FATMAN = timeline(
            cue(GunAnimation.RELOAD, 0, ModGunSounds.FATMAN_RELOAD, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_LASRIFLE = timeline(
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1.5F),
            cue(GunAnimation.RELOAD, 2, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 18, ModGunSounds.IMPACT, 0.25F, 1F),
            cue(GunAnimation.RELOAD, 30, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 38, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.INSPECT, 2, ModGunSounds.MAG_SMALL_REMOVE, 1F, 1F),
            cue(GunAnimation.INSPECT, 12, ModGunSounds.MAG_INSERT, 1F, 1F),
            cue(GunAnimation.INSPECT, 20, ModGunSounds.REVOLVER_CLOSE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_COILGUN = timeline(Muzzle.NI4NI_CYCLE,
            null,
            cue(GunAnimation.RELOAD, 0, ModGunSounds.COIL_RELOAD, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_HANGMAN = timeline(Muzzle.CYCLE,
            HANGMAN_CASINGS,
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 1F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 5, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.8F),
            cue(GunAnimation.RELOAD, 25, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.RELOAD, 35, ModGunSounds.REVOLVER_COCK, 1F, 0.75F),
            cue(GunAnimation.JAMMED, 10, ModGunSounds.REVOLVER_COCK, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 15, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.8F),
            cue(GunAnimation.JAMMED, 20, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 25, ModGunSounds.REVOLVER_COCK, 1F, 0.75F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_BOLTER = timeline(Muzzle.NONE,
            BOLTER_CASINGS,
            cue(GunAnimation.RELOAD, 5, ModGunSounds.MAG_REMOVE, 1F, 1F),
            cue(GunAnimation.RELOAD, 26, ModGunSounds.MAG_INSERT, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FOLLY = timeline(
            cue(GunAnimation.RELOAD, 20, ModGunSounds.SCREW, 1F, 1F),
            cue(GunAnimation.RELOAD, 80, ModGunSounds.ROCKET_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 120, ModGunSounds.SCREW, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_DOUBLE_BARREL = timeline(Muzzle.CYCLE,
            DOUBLE_BARREL_CASINGS,
            cue(GunAnimation.RELOAD, 5, ModGunSounds.REVOLVER_COCK, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 19, ModGunSounds.MAG_SMALL_INSERT, 1F, 0.9F),
            cue(GunAnimation.RELOAD, 29, ModGunSounds.REVOLVER_CLOSE, 1F, 0.8F),
            cue(GunAnimation.INSPECT, 5, ModGunSounds.REVOLVER_COCK, 1F, 0.75F),
            cue(GunAnimation.INSPECT, 19, ModGunSounds.REVOLVER_CLOSE, 1F, 0.8F),
            cue(GunAnimation.CYCLE_DRY, 2, ModSounds.DRY_FIRE_CLICK, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_ABERRATOR = timeline(Muzzle.CYCLE,
            ABERRATOR_CASINGS,
            cue(GunAnimation.RELOAD, 5, ModGunSounds.MAG_SMALL_REMOVE, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 32, ModGunSounds.MAG_SMALL_INSERT, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 42, ModGunSounds.PISTOL_COCK, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 1, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.CYCLE_DRY, 9, ModGunSounds.PISTOL_COCK, 1F, 0.75F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_MAS36 = timeline(Muzzle.CYCLE,
            MAS36_CASINGS,
            cue(GunAnimation.EQUIP, 10, ModGunSounds.LATCH_OPEN, 1F, 1F),
            cue(GunAnimation.EQUIP, 18, ModGunSounds.REVOLVER_CLOSE, 1F, 1F),
            cue(GunAnimation.CYCLE, 7, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.CYCLE, 16, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.CYCLE_DRY, 0, ModSounds.DRY_FIRE_CLICK, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 0, ModGunSounds.BOLT_OPEN, 1F, 1F),
            cue(GunAnimation.RELOAD, 20, ModGunSounds.RIFLE_COCK, 1F, 1F),
            cue(GunAnimation.RELOAD, 36, ModGunSounds.BOLT_CLOSE, 1F, 1F),
            cue(GunAnimation.JAMMED, 5, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 12, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 16, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.JAMMED, 23, ModGunSounds.BOLT_CLOSE, 0.5F, 1F),
            cue(GunAnimation.INSPECT, 0, ModGunSounds.BOLT_OPEN, 0.5F, 1F),
            cue(GunAnimation.INSPECT, 17, ModGunSounds.BOLT_CLOSE, 0.5F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_FIREEXT = timeline(
            cue(GunAnimation.RELOAD, 0, ModGunSounds.VALVE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_CHARGE_THROWER = timeline(
            cue(GunAnimation.RELOAD, 30, ModGunSounds.ROCKET_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 40, ModGunSounds.BOLT_CLOSE, 1F, 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_DRILL = timeline(
            cue(GunAnimation.RELOAD, 15, ModGunSounds.LATCH_OPEN, 1F, 1F),
            cue(GunAnimation.RELOAD, 35, ModGunSounds.IMPACT, 0.5F, 1F),
            cue(GunAnimation.RELOAD, 60, ModGunSounds.REVOLVER_CLOSE, 1F, 0.75F),
            cue(GunAnimation.RELOAD, 70, ModGunSounds.CANISTER_INSERT, 1F, 1F),
            cue(GunAnimation.RELOAD, 85, ModGunSounds.VALVE, 1F, 1F));
}
