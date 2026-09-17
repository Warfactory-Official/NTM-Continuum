package com.hbm.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The boss kinds ntm-next's {@code BossKilledTrigger} criterion observes. See {@link DetonationTrigger}
 * for why only the kinds are here.
 */
public final class BossKilledTrigger {

    private BossKilledTrigger() {}

    public enum Kind implements StringRepresentable {
        /** 1.7 EntityMaskMan:98 (MainRegistry.bossMaskman) - every player within 100 blocks. */
        MASKMAN("maskman"),
        /** 1.7 EntityRADBeast:174 (MainRegistry.bossMeltdown) - leader only, every player within 50 blocks. */
        MELTDOWN("meltdown"),
        /** 1.7 EntityBOTPrimeHead:139 (MainRegistry.bossWorm) - every player within 200 blocks. */
        WORM("worm"),
        /** 1.7 EntityUFO:311 (MainRegistry.bossUFO) - every player within 200 blocks of the death blast. */
        UFO("ufo");

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
