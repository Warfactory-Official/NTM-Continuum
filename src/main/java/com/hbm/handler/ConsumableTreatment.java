package com.hbm.handler;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.EnchantmentUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.data.Codecs;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What using or eating an item does, as data ({@code hbm:consumable_treatment}): the triggers that run it, whether
 * potion sickness blocks it, and an ordered list of actions. Actions run on the server against the target (the eater,
 * or the entity a syringe hits); item actions work on the user's hand and inventory.
 */
public record ConsumableTreatment(List<Trigger> triggers, boolean blockWhenPotionSick, List<Action> actions) {

    public static final Codec<ConsumableTreatment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Trigger.CODEC.listOf().optionalFieldOf("triggers", List.of(Trigger.CONSUME)).forGetter(ConsumableTreatment::triggers),
            Codec.BOOL.optionalFieldOf("block_when_potion_sick", false).forGetter(ConsumableTreatment::blockWhenPotionSick),
            Action.CODEC.listOf().fieldOf("actions").forGetter(ConsumableTreatment::actions)
    ).apply(i, ConsumableTreatment::new));

    public enum Trigger {
        /** After the item was eaten or drunk. */
        CONSUME,
        /** On right-click, instead of the item's own use. */
        USE,
        /** When the item hits an entity in melee (HBM syringes); the hit entity is the target. */
        HIT;

        public static final Codec<Trigger> CODEC = named(Trigger.class);
    }

    public boolean has(Trigger trigger) {
        return triggers.contains(trigger);
    }

    /** Whether potion sickness on {@code target} stops the treatment, and the item's use with it. */
    public boolean blocked(LivingEntity target) {
        return blockWhenPotionSick && VersatileConfig.hasPotionSickness(target);
    }

    public void run(Context context) {
        RandomSource random = context.level.getRandom();
        for (Action action : actions) {
            Gate gate = action.gate();
            if (gate.survivalOnly() && context.user != null && context.user.getAbilities().instabuild) continue;
            if (gate.chance() < 1F && random.nextFloat() >= gate.chance()) continue;
            action.apply(context);
        }
    }

    /**
     * One run. {@code user} is the player holding the item (null for a non-player eater), {@code hand} the hand it is
     * in for a use or hit, {@code stack} the stack {@code finishUsingItem} is finishing when the item runs a consume
     * itself. {@link #result} is what that {@code finishUsingItem} returns.
     */
    public static final class Context {
        final ServerLevel level;
        final Player user;
        final LivingEntity target;
        final InteractionHand hand;
        final ItemStack stack;
        Item fallbackRemainder;
        public ItemStack result;

        private Context(ServerLevel level, Player user, LivingEntity target, InteractionHand hand, ItemStack stack) {
            this.level = level;
            this.user = user;
            this.target = target;
            this.hand = hand;
            this.stack = stack;
            this.result = stack;
        }

        /** A right-click or melee hit with the item in {@code hand}. */
        public static Context use(ServerLevel level, Player user, LivingEntity target, InteractionHand hand) {
            return new Context(level, user, target, hand, null);
        }

        /** An item finishing {@code stack} itself; {@code consume_item} takes from it. */
        public static Context consume(ServerLevel level, LivingEntity eater, ItemStack stack) {
            return new Context(level, eater instanceof Player player ? player : null, eater, null, stack);
        }

        /** Another item, already eaten by its own logic; {@code consume_item} only hands out the remainder. */
        public static Context eaten(ServerLevel level, LivingEntity eater) {
            return consume(level, eater, null);
        }

        /** The remainder of a {@code consume_item} that names none. */
        public Context fallbackRemainder(Item item) {
            this.fallbackRemainder = item;
            return this;
        }
    }

    /** Fields every action has: a chance to run at all, and whether a creative-mode user skips it. */
    public record Gate(float chance, boolean survivalOnly) {
        public static final Gate ALWAYS = new Gate(1F, false);
        static final MapCodec<Gate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("chance", 1F).forGetter(Gate::chance),
                Codec.BOOL.optionalFieldOf("survival_only", false).forGetter(Gate::survivalOnly)
        ).apply(i, Gate::new));
    }

    public sealed interface Action {

        Codec<Action> CODEC = Codecs.partialDispatch(Codec.STRING, "type", action -> DataResult.success(action.type()),
                type -> {
                    MapCodec<? extends Action> codec = codecFor(type);
                    return codec != null ? DataResult.success(codec)
                            : DataResult.error(() -> "Unknown treatment action " + type);
                });

        static MapCodec<? extends Action> codecFor(String type) {
            return switch (type) {
                case "counter" -> Counter.CODEC;
                case "effect" -> AddEffect.CODEC;
                case "remove_effects" -> RemoveEffects.CODEC;
                case "clear_effects" -> ClearEffects.CODEC;
                case "damage" -> Damage.CODEC;
                case "heal" -> Heal.CODEC;
                case "restore_health" -> RestoreHealth.CODEC;
                case "drain_health" -> DrainHealth.CODEC;
                case "potion_sickness" -> PotionSickness.CODEC;
                case "sound" -> Sound.CODEC;
                case "consume_item" -> ConsumeItem.CODEC;
                case "give_item" -> GiveItem.CODEC;
                case "ignite" -> Ignite.CODEC;
                case "motion" -> Motion.CODEC;
                case "experience" -> Experience.CODEC;
                default -> null;
            };
        }

        String type();

        Gate gate();

        void apply(Context context);
    }

    public enum CounterType {
        RADIATION, DIGAMMA, ASBESTOS, BLACK_LUNG, CONTAGION;

        static final Codec<CounterType> CODEC = named(CounterType.class);

        double get(LivingEntity entity) {
            return switch (this) {
                case RADIATION -> HbmLivingProps.getRadiation(entity);
                case DIGAMMA -> HbmLivingProps.getDigamma(entity);
                case ASBESTOS -> HbmLivingProps.getAsbestos(entity);
                case BLACK_LUNG -> HbmLivingProps.getBlackLung(entity);
                case CONTAGION -> HbmLivingProps.getContagion(entity);
            };
        }

        void set(LivingEntity entity, double value) {
            switch (this) {
                case RADIATION -> HbmLivingProps.setRadiation(entity, value);
                case DIGAMMA -> HbmLivingProps.setDigamma(entity, value);
                case ASBESTOS -> HbmLivingProps.setAsbestos(entity, (int) value);
                case BLACK_LUNG -> HbmLivingProps.setBlackLung(entity, (int) value);
                case CONTAGION -> HbmLivingProps.setContagion(entity, (int) value);
            }
        }

        /** The same increments the rest of HBM uses; digamma and contagion only floor at zero. */
        void add(LivingEntity entity, double value) {
            switch (this) {
                case RADIATION -> HbmLivingProps.incrementRadiation(entity, value);
                case ASBESTOS -> HbmLivingProps.incrementAsbestos(entity, (int) value);
                case BLACK_LUNG -> HbmLivingProps.incrementBlackLung(entity, (int) value);
                default -> set(entity, Math.max(get(entity) + value, 0D));
            }
        }

        /** The lung diseases' maximum, for {@code limit_divisor}; 0 for the others. */
        int max() {
            return switch (this) {
                case ASBESTOS -> EntityHbmProps.MAX_ASBESTOS;
                case BLACK_LUNG -> EntityHbmProps.MAX_BLACKLUNG;
                default -> 0;
            };
        }
    }

    public enum Operation {
        ADD, SET, MIN, MAX;

        static final Codec<Operation> CODEC = named(Operation.class);
    }

    /** Changes an HBM counter: {@code add}, {@code set}, {@code min} (cap at) or {@code max} (raise to). */
    public record Counter(Gate gate, CounterType counter, Operation operation, double amount,
                          Optional<Integer> limitDivisor) implements Action {
        static final MapCodec<Counter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Counter::gate),
                CounterType.CODEC.fieldOf("counter").forGetter(Counter::counter),
                Operation.CODEC.optionalFieldOf("operation", Operation.ADD).forGetter(Counter::operation),
                Codec.DOUBLE.optionalFieldOf("amount", 0D).forGetter(Counter::amount),
                Codec.INT.optionalFieldOf("limit_divisor").forGetter(Counter::limitDivisor)
        ).apply(i, Counter::new));

        public Counter(CounterType counter, Operation operation, double amount) {
            this(Gate.ALWAYS, counter, operation, amount, Optional.empty());
        }

        @Override
        public String type() {
            return "counter";
        }

        @Override
        public void apply(Context context) {
            LivingEntity entity = context.target;
            // integer division, as 1.7's maxBlacklung / 5
            double value = limitDivisor.isPresent() && counter.max() > 0 ? counter.max() / limitDivisor.get() : amount;
            switch (operation) {
                case ADD -> counter.add(entity, value);
                case SET -> counter.set(entity, value);
                case MIN -> counter.set(entity, Math.min(counter.get(entity), value));
                case MAX -> counter.set(entity, Math.max(counter.get(entity), value));
            }
        }
    }

    /** Adds a mob effect; {@code extend} adds the remaining time of the active one first. */
    public record AddEffect(Gate gate, String effect, int duration, int amplifier, boolean extend) implements Action {
        static final MapCodec<AddEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(AddEffect::gate),
                Codec.STRING.fieldOf("effect").forGetter(AddEffect::effect),
                Codec.INT.fieldOf("duration").forGetter(AddEffect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(AddEffect::amplifier),
                Codec.BOOL.optionalFieldOf("extend", false).forGetter(AddEffect::extend)
        ).apply(i, AddEffect::new));

        public AddEffect(String effect, int duration, int amplifier) {
            this(Gate.ALWAYS, effect, duration, amplifier, false);
        }

        @Override
        public String type() {
            return "effect";
        }

        @Override
        public void apply(Context context) {
            MobEffect kind = mobEffect(effect);
            if (kind == null) return;
            int ticks = duration;
            if (extend) {
                MobEffectInstance active = Effects.getRegistered(context.target, kind);
                if (active != null) ticks += active.getDuration();
            }
            Effects.addRegistered(context.target, kind, ticks, amplifier);
        }
    }

    public record RemoveEffects(Gate gate, List<String> effects) implements Action {
        static final MapCodec<RemoveEffects> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(RemoveEffects::gate),
                Codec.STRING.listOf().fieldOf("effects").forGetter(RemoveEffects::effects)
        ).apply(i, RemoveEffects::new));

        @Override
        public String type() {
            return "remove_effects";
        }

        @Override
        public void apply(Context context) {
            for (String id : effects) {
                MobEffect kind = mobEffect(id);
                if (kind != null) Effects.removeRegistered(context.target, kind);
            }
        }
    }

    public record ClearEffects(Gate gate) implements Action {
        static final MapCodec<ClearEffects> CODEC = Gate.CODEC.xmap(ClearEffects::new, ClearEffects::gate);

        @Override
        public String type() {
            return "clear_effects";
        }

        @Override
        public void apply(Context context) {
            Effects.clear(context.target);
        }
    }

    /**
     * {@code repeat} hits of {@code amount}, each of a damage type picked at random from {@code sources}. When the
     * target is not the user and {@code other_sources} is given, those are used instead, credited to the user.
     */
    public record Damage(Gate gate, List<String> sources, float amount, int repeat, List<String> otherSources)
            implements Action {
        static final MapCodec<Damage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Damage::gate),
                Codec.STRING.listOf().fieldOf("sources").forGetter(Damage::sources),
                Codec.FLOAT.fieldOf("amount").forGetter(Damage::amount),
                Codec.INT.optionalFieldOf("repeat", 1).forGetter(Damage::repeat),
                Codec.STRING.listOf().optionalFieldOf("other_sources", List.of()).forGetter(Damage::otherSources)
        ).apply(i, Damage::new));

        @Override
        public String type() {
            return "damage";
        }

        @Override
        public void apply(Context context) {
            boolean other = context.user != null && context.target != context.user && !otherSources.isEmpty();
            List<String> pool = other ? otherSources : sources;
            if (pool.isEmpty()) return;
            for (int n = 0; n < repeat; n++) {
                String id = pool.size() == 1 ? pool.get(0) : pool.get(context.level.getRandom().nextInt(pool.size()));
                ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, Ids.parse(id));
                DamageSource source;
                try {
                    source = other ? ModDamageSources.source(context.level, key, context.user, context.user)
                            : ModDamageSources.source(context.level, key);
                } catch (IllegalStateException missing) {
                    // a pack named a damage type the server does not have
                    Constants.LOG.warn("Consumable treatment: unknown damage type {}", id);
                    return;
                }
                Compat.hurt(context.target, context.level, source, amount);
            }
        }
    }

    public record Heal(Gate gate, float amount) implements Action {
        static final MapCodec<Heal> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Heal::gate),
                Codec.FLOAT.fieldOf("amount").forGetter(Heal::amount)
        ).apply(i, Heal::new));

        public Heal(float amount) {
            this(Gate.ALWAYS, amount);
        }

        @Override
        public String type() {
            return "heal";
        }

        @Override
        public void apply(Context context) {
            context.target.heal(amount);
        }
    }

    /** Sets health to the maximum, past any heal handlers. */
    public record RestoreHealth(Gate gate) implements Action {
        static final MapCodec<RestoreHealth> CODEC = Gate.CODEC.xmap(RestoreHealth::new, RestoreHealth::gate);

        @Override
        public String type() {
            return "restore_health";
        }

        @Override
        public void apply(Context context) {
            context.target.setHealth(context.target.getMaxHealth());
        }
    }

    /** Takes health past armor and resistances; the target dies of magic at zero. */
    public record DrainHealth(Gate gate, float amount) implements Action {
        static final MapCodec<DrainHealth> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(DrainHealth::gate),
                Codec.FLOAT.fieldOf("amount").forGetter(DrainHealth::amount)
        ).apply(i, DrainHealth::new));

        @Override
        public String type() {
            return "drain_health";
        }

        @Override
        public void apply(Context context) {
            LivingEntity target = context.target;
            target.setHealth(Math.max(target.getHealth() - amount, 0F));
            if (target.getHealth() <= 0) target.die(target.damageSources().magic());
        }
    }

    /** {@code hbm.cfg} {@code 8.S0} decides the length: none, the seconds, or twelve times them. */
    public record PotionSickness(Gate gate, int seconds) implements Action {
        static final MapCodec<PotionSickness> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(PotionSickness::gate),
                Codec.INT.fieldOf("seconds").forGetter(PotionSickness::seconds)
        ).apply(i, PotionSickness::new));

        public PotionSickness(int seconds) {
            this(Gate.ALWAYS, seconds);
        }

        @Override
        public String type() {
            return "potion_sickness";
        }

        @Override
        public void apply(Context context) {
            VersatileConfig.applyPotionSickness(context.target, seconds);
        }
    }

    /** A sound at the target, in the players category. */
    public record Sound(Gate gate, String sound, float volume, float pitch) implements Action {
        static final MapCodec<Sound> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Sound::gate),
                Codec.STRING.fieldOf("sound").forGetter(Sound::sound),
                Codec.FLOAT.optionalFieldOf("volume", 1F).forGetter(Sound::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1F).forGetter(Sound::pitch)
        ).apply(i, Sound::new));

        public Sound(String sound) {
            this(Gate.ALWAYS, sound, 1F, 1F);
        }

        @Override
        public String type() {
            return "sound";
        }

        @Override
        public void apply(Context context) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(Ids.parse(sound)).orElse(null);
            if (event == null) return;
            LivingEntity target = context.target;
            context.level.playSound(null, target.getX(), target.getY(), target.getZ(), event, SoundSource.PLAYERS,
                    volume, pitch);
        }
    }

    /**
     * Takes one of the used item and leaves {@code remainder}: in the emptied hand, else in the inventory or dropped.
     * After another item's own eating it only hands out the remainder.
     */
    public record ConsumeItem(Gate gate, Optional<String> remainder) implements Action {
        static final MapCodec<ConsumeItem> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(ConsumeItem::gate),
                Codec.STRING.optionalFieldOf("remainder").forGetter(ConsumeItem::remainder)
        ).apply(i, ConsumeItem::new));

        @Override
        public String type() {
            return "consume_item";
        }

        @Override
        public void apply(Context context) {
            Item item = remainder.map(ConsumableTreatment::item).orElse(context.fallbackRemainder);
            ItemStack left = item == null ? ItemStack.EMPTY : new ItemStack(item);
            if (context.stack != null) {
                context.stack.shrink(1);
                if (left.isEmpty()) return;
                if (context.stack.isEmpty()) context.result = left;
                else giveOrDrop(context.user, left, true);
            } else if (context.hand != null && context.user != null) {
                ItemStack held = context.user.getItemInHand(context.hand);
                held.shrink(1);
                if (left.isEmpty()) return;
                if (held.isEmpty()) context.user.setItemInHand(context.hand, left);
                else giveOrDrop(context.user, left, true);
            } else if (!left.isEmpty()) {
                giveOrDrop(context.user, left, true);
            }
        }
    }

    /** Puts items in the user's inventory; with {@code drop_if_full} false what does not fit is lost, as 1.7's cans. */
    public record GiveItem(Gate gate, String item, int count, boolean dropIfFull) implements Action {
        static final MapCodec<GiveItem> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(GiveItem::gate),
                Codec.STRING.fieldOf("item").forGetter(GiveItem::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(GiveItem::count),
                Codec.BOOL.optionalFieldOf("drop_if_full", true).forGetter(GiveItem::dropIfFull)
        ).apply(i, GiveItem::new));

        @Override
        public String type() {
            return "give_item";
        }

        @Override
        public void apply(Context context) {
            Item given = ConsumableTreatment.item(item);
            if (given != null) giveOrDrop(context.user, new ItemStack(given, count), dropIfFull);
        }
    }

    /** Sets the target on fire for {@code seconds}. */
    public record Ignite(Gate gate, int seconds) implements Action {
        static final MapCodec<Ignite> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Ignite::gate),
                Codec.INT.fieldOf("seconds").forGetter(Ignite::seconds)
        ).apply(i, Ignite::new));

        @Override
        public String type() {
            return "ignite";
        }

        @Override
        public void apply(Context context) {
            Effects.ignite(context.target, seconds);
        }
    }

    /** Replaces the velocity components given, in blocks per tick; the client is told. */
    public record Motion(Gate gate, Optional<Double> x, Optional<Double> y, Optional<Double> z) implements Action {
        static final MapCodec<Motion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Motion::gate),
                Codec.DOUBLE.optionalFieldOf("x").forGetter(Motion::x),
                Codec.DOUBLE.optionalFieldOf("y").forGetter(Motion::y),
                Codec.DOUBLE.optionalFieldOf("z").forGetter(Motion::z)
        ).apply(i, Motion::new));

        @Override
        public String type() {
            return "motion";
        }

        @Override
        public void apply(Context context) {
            Vec3 motion = context.target.getDeltaMovement();
            context.target.setDeltaMovement(x.orElse(motion.x), y.orElse(motion.y), z.orElse(motion.z));
            context.target.hurtMarked = true;
        }
    }

    /** Gives (or, negative, takes) experience points from the user, never below zero. */
    public record Experience(Gate gate, int amount) implements Action {
        static final MapCodec<Experience> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Gate.CODEC.forGetter(Experience::gate),
                Codec.INT.fieldOf("amount").forGetter(Experience::amount)
        ).apply(i, Experience::new));

        @Override
        public String type() {
            return "experience";
        }

        @Override
        public void apply(Context context) {
            Player player = context.user;
            if (player == null) return;
            if (amount >= 0) EnchantmentUtil.addExperience(player, amount, false);
            else EnchantmentUtil.setExperience(player, Math.max(EnchantmentUtil.getTotalExperience(player) + amount, 0));
        }
    }

    private static MobEffect mobEffect(String id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(Ids.parse(id)).orElse(null);
        if (effect == null) Constants.LOG.warn("Consumable treatment: unknown effect {}", id);
        return effect;
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(null);
        if (item == null) Constants.LOG.warn("Consumable treatment: unknown item {}", id);
        return item;
    }

    private static void giveOrDrop(Player player, ItemStack stack, boolean drop) {
        if (player == null) return;
        if (!player.getInventory().add(stack) && drop) player.drop(stack, false);
    }

    private static <E extends Enum<E>> Codec<E> named(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException unknown) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + " " + name);
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
