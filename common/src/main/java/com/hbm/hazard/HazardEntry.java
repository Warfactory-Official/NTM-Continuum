package com.hbm.hazard;

import java.util.ArrayList;
import java.util.List;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A single hazard on an item: type + base level + modifiers. Ported 1:1 from 1.12.2. */
public class HazardEntry implements Cloneable {

    public final IHazardType type;
    public final double baseLevel;

    /** Modifiers are evaluated in application order. Shared across cloned instances. */
    public List<IHazardModifier> mods = new ArrayList<>();

    public HazardEntry(final IHazardType type) {
        this(type, 1D);
    }

    public HazardEntry(final IHazardType type, final double level) {
        this.type = type;
        this.baseLevel = level;
    }

    public HazardEntry addMod(final IHazardModifier mod) {
        this.mods.add(mod);
        return this;
    }

    public void applyHazard(final ItemStack stack, final LivingEntity entity) {
        type.onUpdate(entity, IHazardModifier.evalAllModifiers(stack, entity, baseLevel, mods), stack);
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
        final HazardEntry clone = new HazardEntry(type, baseLevel * mult);
        clone.mods = this.mods;
        return clone;
    }
}
