package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A block that can be detonated. Ported from 1.12.2 ({@code World}→{@code Level}). Used by the
 * detonation paths (right-click detonator, redstone, remote triggers) to fire a bomb and report
 * whether it went off, was missing parts, etc.
 */
public interface IBomb {

    BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator);

    enum BombReturnCode {
        UNDEFINED(false, ""),                                    // non-null type for clients that don't process the return code
        DETONATED(true, "bomb.detonated"),                       // success for blowing up bombs
        TRIGGERED(true, "bomb.triggered"),                       // success for triggering other things
        LAUNCHED(true, "bomb.launched"),                         // success for launching missiles
        ERROR_MISSING_COMPONENT(false, "bomb.missingComponent"), // error for bomb parts missing
        ERROR_INCOMPATIBLE(false, "bomb.incompatible"),          // error for an incompatible target
        ERROR_NO_BOMB(false, "bomb.nobomb");                     // generic error for triggering a no-bomb block

        private final String unloc;
        private final boolean success;

        BombReturnCode(boolean success, String unloc) {
            this.unloc = unloc;
            this.success = success;
        }

        public String getUnlocalizedMessage() {
            return this.unloc;
        }

        public boolean wasSuccessful() {
            return this.success;
        }
    }
}
