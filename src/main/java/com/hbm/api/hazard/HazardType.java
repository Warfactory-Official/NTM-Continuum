package com.hbm.api.hazard;

import java.util.List;

import com.hbm.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A hazard assembled from lambdas instead of from a class, carrying a payload of the author's own
 * type. This is the extension point for "a hazard the mod does not have": nothing about radiation,
 * heat or asbestos is privileged, and a hazard declared here is indistinguishable from the built-in
 * ones to {@link com.hbm.hazard.HazardSystem} — it registers on items and tags the same way, folds
 * through the same override/mutex rules, takes the same {@link IHazardModifier}s, and rides the same
 * carried-item scan.
 *
 * <pre>{@code
 * record Acid(int seconds, boolean eatsArmour) {}
 *
 * static final Identifier ID = dev.golem.core.Ids.of("mymod", "corrosive");
 * public static final HazardType<Acid> CORROSIVE = HazardRegistry.registerType(ID,
 *         HazardType.builder(ID, Acid.class)
 *                 .trait(ChatFormatting.DARK_GREEN)
 *                 .onCarried((target, stack, level, acid) -> burn(target, level, acid))
 *                 .onDropped((item, level, acid) -> etch(item, level))
 *                 .build());
 *
 * HazardSystem.register(MyItems.VIAL, new HazardData().addEntry(CORROSIVE, 2D, new Acid(8, true)));
 * }</pre>
 *
 * <p>Every lambda is optional; an omitted one is simply not called, so a hazard that only matters on
 * the ground is a single {@link Builder#onDropped} and a hazard that only annotates a tooltip is a
 * single {@link Builder#tooltip}. With no tooltip lambda the type prints {@code [trait.<path>]} in
 * the colour given to {@link Builder#trait} — {@code trait.<namespace>.<path>} outside this mod's
 * namespace, so a third-party hazard cannot collide with an HBM translation key.
 *
 * <p>Payloads are read through {@link Class#isInstance}, not an unchecked cast: an entry carrying
 * someone else's payload degrades to the declared default instead of throwing inside a hazard tick.
 *
 * <p>{@link #build()} does not register anything. Naming the type is a separate, explicit step
 * ({@link com.hbm.hazard.HazardRegistry#registerType}) so a type built for a test or a probe does
 * not end up in the shared table.
 */
public final class HazardType<P> implements IHazardType {

    /** The hazard's effect on whoever is carrying the stack, run every {@code hazardRate} ticks. */
    @FunctionalInterface
    public interface Carried<P> {
        void apply(LivingEntity target, ItemStack stack, double level, @Nullable P payload);
    }

    /** The hazard's effect while the stack lies on the ground, run on the dropped-item cadence. */
    @FunctionalInterface
    public interface Dropped<P> {
        void apply(ItemEntity item, double level, @Nullable P payload);
    }

    /** The hazard's tooltip lines. {@code level} is the base level; {@code modifiers} are not yet applied. */
    @FunctionalInterface
    public interface Info<P> {
        void addInfo(Player player, List<Component> tooltip, double level, ItemStack stack,
                List<IHazardModifier> modifiers, @Nullable P payload);
    }

    private final Identifier id;
    private final Class<P> payloadType;
    private final @Nullable P defaultPayload;
    private final @Nullable Carried<P> carried;
    private final @Nullable Dropped<P> dropped;
    private final @Nullable Info<P> info;
    private final ChatFormatting trait;

    private HazardType(Builder<P> builder) {
        this.id = builder.id;
        this.payloadType = builder.payloadType;
        this.defaultPayload = builder.defaultPayload;
        this.carried = builder.carried;
        this.dropped = builder.dropped;
        this.info = builder.info;
        this.trait = builder.trait;
    }

    /** A hazard with no payload — every lambda receives {@code null} for it. */
    public static Builder<Void> builder(final Identifier id) {
        return new Builder<>(id, Void.class);
    }

    public static <P> Builder<P> builder(final Identifier id, final Class<P> payloadType) {
        return new Builder<>(id, payloadType);
    }

    public Identifier id() {
        return id;
    }

    public Class<P> payloadType() {
        return payloadType;
    }

    /** An entry of this hazard carrying the type's default payload. */
    public HazardEntry entry(final double level) {
        return new HazardEntry(this, level, defaultPayload);
    }

    /** An entry of this hazard carrying {@code payload}, which its lambdas then receive verbatim. */
    public HazardEntry entry(final double level, final @Nullable P payload) {
        return new HazardEntry(this, level, payload);
    }

    /** This type's reading of an entry's payload: the entry's own, or the declared default. */
    public @Nullable P payloadOf(final HazardEntry entry) {
        return cast(entry.payload);
    }

    private @Nullable P cast(final @Nullable Object payload) {
        return payloadType.isInstance(payload) ? payloadType.cast(payload) : defaultPayload;
    }

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        onUpdate(target, level, stack, defaultPayload);
    }

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack, final @Nullable Object payload) {
        if (carried != null) carried.apply(target, stack, level, cast(payload));
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
        updateEntity(item, level, defaultPayload);
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level, final @Nullable Object payload) {
        if (dropped != null) dropped.apply(item, level, cast(payload));
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level,
            final ItemStack stack, final List<IHazardModifier> modifiers) {
        addHazardInformation(player, list, level, stack, modifiers, defaultPayload);
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level,
            final ItemStack stack, final List<IHazardModifier> modifiers, final @Nullable Object payload) {
        if (info != null) {
            info.addInfo(player, list, level, stack, modifiers, cast(payload));
            return;
        }
        list.add(Component.literal("[").append(Component.translatable(traitKey())).append(Component.literal("]"))
                .withStyle(trait));
    }

    private String traitKey() {
        return Constants.MOD_ID.equals(id.getNamespace())
                ? "trait." + id.getPath()
                : "trait." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public String toString() {
        return "HazardType[" + id + "]";
    }

    public static final class Builder<P> {

        private final Identifier id;
        private final Class<P> payloadType;
        private @Nullable P defaultPayload;
        private @Nullable Carried<P> carried;
        private @Nullable Dropped<P> dropped;
        private @Nullable Info<P> info;
        private ChatFormatting trait = ChatFormatting.GRAY;

        private Builder(final Identifier id, final Class<P> payloadType) {
            this.id = id;
            this.payloadType = payloadType;
        }

        /** The payload used by entries registered without one, and by any entry carrying a foreign one. */
        public Builder<P> defaultPayload(final @Nullable P payload) {
            this.defaultPayload = payload;
            return this;
        }

        public Builder<P> onCarried(final Carried<P> carried) {
            this.carried = carried;
            return this;
        }

        public Builder<P> onDropped(final Dropped<P> dropped) {
            this.dropped = dropped;
            return this;
        }

        public Builder<P> tooltip(final Info<P> info) {
            this.info = info;
            return this;
        }

        /** Formatting of the generated {@code [trait.…]} line, used only when no tooltip lambda is given. */
        public Builder<P> trait(final ChatFormatting trait) {
            this.trait = trait;
            return this;
        }

        public HazardType<P> build() {
            return new HazardType<>(this);
        }
    }
}
