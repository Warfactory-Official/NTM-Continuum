package com.hbm.hazard;

import java.util.ArrayList;
import java.util.List;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.HazardType;
import com.hbm.hazard.type.IHazardType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A single hazard on an item: type + base level + modifiers + payload. Ported from 1.12.2, with the
 * payload added.
 *
 * <p>The payload is whatever the {@link HazardType} that produced this entry declared — a fuse
 * length, a damage source, a table of effects — and is handed back to that type's lambdas on every
 * application. It is fixed at registration, shared by every stack that resolves to this entry, and
 * therefore never per-stack state: a hazard that needs to remember something about one stack still
 * writes it into that stack's components. Build a typed one with {@link HazardType#entry}; the raw
 * constructor exists for entries that carry none.
 *
 * <p>The three {@code apply}/{@code addInformation} methods are the only places the payload is
 * threaded into a type, so a caller cannot half-use the feature by reaching for
 * {@link IHazardType#onUpdate} directly.
 */
public class HazardEntry implements Cloneable {

    public final IHazardType type;
    public final double baseLevel;

    /** Registration-time data for {@link #type}, or null. Shared across cloned instances. */
    public final @Nullable Object payload;

    /** Modifiers are evaluated in application order. Shared across cloned instances. */
    public List<IHazardModifier> mods = new ArrayList<>();

    public HazardEntry(final IHazardType type) {
        this(type, 1D, null);
    }

    public HazardEntry(final IHazardType type, final double level) {
        this(type, level, null);
    }

    public HazardEntry(final IHazardType type, final double level, final @Nullable Object payload) {
        this.type = type;
        this.baseLevel = level;
        this.payload = payload;
    }

    public HazardEntry addMod(final IHazardModifier mod) {
        this.mods.add(mod);
        return this;
    }

    public void applyHazard(final ItemStack stack, final LivingEntity entity) {
        type.onUpdate(entity, IHazardModifier.evalAllModifiers(stack, entity, baseLevel, mods), stack, payload);
    }

    /** The dropped-item half of {@link #applyHazard}; modifiers see a null holder, as they always have. */
    public void applyDropped(final ItemEntity item) {
        final ItemStack stack = item.getItem();
        type.updateEntity(item, IHazardModifier.evalAllModifiers(stack, null, baseLevel, mods), payload);
    }

    /** Tooltip lines for this entry. The base level is passed through — types evaluate the mods themselves. */
    public void addInformation(final Player player, final List<Component> list, final ItemStack stack) {
        type.addHazardInformation(player, list, baseLevel, stack, mods, payload);
    }

    @Override
    public HazardEntry clone() {
        try {
            return (HazardEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public HazardEntry clone(final double mult) {
        final HazardEntry clone = new HazardEntry(type, baseLevel * mult, payload);
        clone.mods = this.mods;
        return clone;
    }
}
