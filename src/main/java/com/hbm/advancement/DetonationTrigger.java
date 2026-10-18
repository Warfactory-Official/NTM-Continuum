package com.hbm.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The detonation event kinds ntm-next's {@code DetonationTrigger} criterion observes. The criterion
 * itself (a {@code SimpleCriterionTrigger} with a codec-backed instance) belongs to the advancement
 * subsystem, which this port has not brought over yet; the kinds are carried so the entity call sites
 * ({@link HbmCriteria#detonation}) keep their exact ntm-next shape until the triggers land.
 */
public final class DetonationTrigger {

    private DetonationTrigger() {}

    public enum Kind implements StringRepresentable {
        /**
         * 1.7 EntityNukeExplosionMK3:127 / MK5:60 - every player in the world, every tick the blast lives.
         */
        NUKE("nuke"),
        /**
         * 1.7 TileEntityRBMKBase:552 - a column melting down.
         */
        RBMK("rbmk"),
        /**
         * 1.7 TileEntityReactorZirnox:396.
         */
        ZIRNOX("zirnox"),
        /**
         * 1.7 TileEntityWatz:529-533 - a Watz Power Plant drowning in its own mud, 50 blocks.
         */
        WATZ("watz"),
        /**
         * 1.7 MachineRefinery:125 - and only when a napalm bomblet was the exploder.
         */
        REFINERY("refinery"),
        FLUID_TANK("fluid_tank"),
        /**
         * 1.7 EntitySpear - the digamma spear's impact.
         */
        DIGAMMA_SPEAR("digamma_spear"),
        /**
         * 1.7 EntityCreeperNuclear:81 (MainRegistry.bossCreeper) - every player within 50 blocks of its death.
         */
        NUCLEAR_CREEPER("nuclear_creeper"),
        /**
         * 1.7 EntityRocketSoyuz (MainRegistry.achSoyuz) - awarded for standing in the launch plume.
         */
        SOYUZ_EXHAUST("soyuz_exhaust");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
