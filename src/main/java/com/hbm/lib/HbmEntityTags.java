package com.hbm.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class HbmEntityTags {

    /** Types radiation never affects (1.7 {@code isRadImmune}'s class list); {@code IRadiationImmune} needs no entry. */
    public static final TagKey<EntityType<?>> RADIATION_IMMUNE = TagKey.create(
            Registries.ENTITY_TYPE,
            Library.id("radiation_immune"));

    private HbmEntityTags() {
    }
}
