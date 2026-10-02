package com.hbm.inventory.fluid.trait;

import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The 1.7 {@code FT_Toxin}: what breathing or touching a fluid does to a living thing — direct damage on
 * a clock, potion effects, or both — unless the victim wears the right gas-mask filter (and, for
 * full-body entries, a hazmat suit). Applied by {@code EntityMist} and {@code EntityChemical} with the
 * cloud's remaining intensity, exactly where 1.7 calls {@code affect}.
 *
 * <p>Like the port's other traits it is a plain object in the {@code FluidType} trait map (no
 * {@code FluidTrait} base class). The JSON (de)serialisation for 1.7's custom-fluid dump is not carried:
 * this port has no custom-fluid file.
 */
public final class FT_Toxin {

    public final List<ToxinEntry> entries = new ArrayList<>();

    public FT_Toxin addEntry(ToxinEntry entry) {
        entries.add(entry);
        return this;
    }

    /** 1.7 {@code addInfoHidden}: the light-purple {@code [Toxin]} tag, then one block per entry. */
    public void addInfoHidden(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.toxin")).append("]")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        for (ToxinEntry entry : entries) entry.addInfo(info);
    }

    public void affect(LivingEntity entity, double intensity) {
        for (ToxinEntry entry : entries) entry.poison(entity, intensity);
    }

    public abstract static class ToxinEntry {

        public final HazardClass clazz;
        public final boolean fullBody;

        protected ToxinEntry(HazardClass clazz, boolean fullBody) {
            this.clazz = clazz;
            this.fullBody = fullBody;
        }

        /**
         * 1.7 {@code isProtected}. The mask check wears the filter by one point on every call that finds
         * it — including calls where the missing hazmat suit then lets the toxin through anyway. That is
         * 1.7's order of operations and is kept.
         */
        public boolean isProtected(LivingEntity entity) {
            boolean hasMask = clazz == null;
            boolean hasSuit = !fullBody;

            // 1.7 armor slot 3 is the helmet.
            if (clazz != null && ArmorRegistry.hasAllProtection(entity, EquipmentSlot.HEAD, clazz)) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                hasMask = true;
            }

            if (fullBody && ArmorUtil.checkForHazmat(entity)) hasSuit = true;

            return hasMask && hasSuit;
        }

        public abstract void poison(LivingEntity entity, double intensity);

        public abstract void addInfo(List<Component> info);

        /** {@code "- Chemical Gas"}, plus the red {@code (requires hazmat suit)} for full-body entries. */
        protected MutableComponent header() {
            MutableComponent line = Component.literal("- ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(clazz.lang).withStyle(ChatFormatting.YELLOW));
            if (fullBody) {
                line.append(Component.literal(" (").withStyle(ChatFormatting.RED)
                        .append(Component.translatable("hbmfluid.trait.hazmat"))
                        .append(")"));
            }
            return line;
        }
    }

    public static class ToxinDirectDamage extends ToxinEntry {

        public final ResourceKey<DamageType> damage;
        public final float amount;
        public final int delay;

        public ToxinDirectDamage(ResourceKey<DamageType> damage, float amount, int delay, HazardClass clazz,
                boolean fullBody) {
            super(clazz, fullBody);
            this.damage = damage;
            this.amount = amount;
            this.delay = delay;
        }

        /** 1.7: an ordinary {@code attackEntityFrom} (invulnerability frames apply) on the world clock. */
        @Override
        public void poison(LivingEntity entity, double intensity) {
            if (!(entity.level() instanceof ServerLevel level)) return;
            if (isProtected(entity)) return;

            if (delay == 0 || level.getGameTime() % delay == 0) {
                Compat.hurt(entity, level, ModDamageSources.source(level, damage), (float) (amount * intensity));
            }
        }

        /** {@code "- Chemical Gas: 4.0 DPS"}; the rate is 1.7's {@code amount * 20 / delay}. */
        @Override
        public void addInfo(List<Component> info) {
            // 1.7 does not reset the colour after the red hazmat note, so its ": " comes out red too.
            info.add(header().append(Component.literal(": ")
                            .withStyle(fullBody ? ChatFormatting.RED : ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(Locale.US, "%,.1f", amount * 20 / delay) + " ")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("hbmfluid.trait.perDamage").withStyle(ChatFormatting.YELLOW)));
        }
    }

    public static class ToxinEffects extends ToxinEntry {

        /**
         * The effect templates. Suppliers, not instances: the fluid registry initialises before the
         * effect registry is bound, so an HBM effect such as {@code death} cannot be resolved here yet.
         */
        public final List<Supplier<MobEffectInstance>> effects = new ArrayList<>();

        public ToxinEffects(HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
        }

        @SafeVarargs
        public final ToxinEffects add(Supplier<MobEffectInstance>... effs) {
            for (Supplier<MobEffectInstance> eff : effs) this.effects.add(eff);
            return this;
        }

        /** 1.7: each template re-issued with its duration scaled by the intensity. */
        @Override
        public void poison(LivingEntity entity, double intensity) {
            if (isProtected(entity)) return;

            for (Supplier<MobEffectInstance> template : effects) {
                MobEffectInstance eff = template.get();
                entity.addEffect(Effects.instance(Effects.kind(eff), (int) (eff.getDuration() * intensity),
                        eff.getAmplifier()));
            }
        }

        /** {@code "- Corrosive Fumes:"} then {@code "   - Wither II 0:05"} per effect. */
        @Override
        public void addInfo(List<Component> info) {
            info.add(header().append(Component.literal(":").withStyle(ChatFormatting.YELLOW)));

            for (Supplier<MobEffectInstance> template : effects) {
                MobEffectInstance eff = template.get();
                MutableComponent line = Component.literal("   - ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(eff.getDescriptionId()));
                // 1.7 appends the potency numeral only above amplifier 0.
                if (eff.getAmplifier() > 0) {
                    line.append(" ").append(Component.translatable("potion.potency." + eff.getAmplifier()));
                }
                line.append(" " + ticksToElapsedTime(eff.getDuration()));
                info.add(line);
            }
        }
    }

    /**
     * 1.7 {@code StringUtils.ticksToElapsedTime}: {@code m:ss} at 20 ticks a second, minutes unbounded
     * (an hour is {@code 60:00}). Ported rather than delegated: vanilla's modern formatter adds an hour
     * field and changed signature between versions.
     */
    static String ticksToElapsedTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return seconds < 10 ? minutes + ":0" + seconds : minutes + ":" + seconds;
    }
}
