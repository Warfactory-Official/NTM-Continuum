package com.hbm.extprop;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.world.entity.player.Player;

/**
 * Player-state facade (1.7 {@code HbmPlayerProps}). The port keeps the fields on every living entity's
 * {@link EntityHbmProps}; this class names the player-only ones and carries 1.7's respawn copy.
 */
public final class HbmPlayerProps {
    private HbmPlayerProps() {}

    /** 1.7's reputation bounds: a lantern repair raises it below 25, a destroyed lantern lowers it above -25. */
    public static final int REPUTATION_MAX = 25;
    public static final int REPUTATION_MIN = -25;

    private static boolean registered;

    public static EntityHbmProps getData(Player player) {
        return HbmLivingProps.getData(player);
    }

    /** 1.7 {@code ModEventHandler.onPlayerClone}: the player-props half survives death and the End return. */
    public static void init() {
        if (registered) return;
        registered = true;
        Lifecycle.onPlayerCopy((oldPlayer, newPlayer, alive) ->
                getData(newPlayer).copyPlayerPropsFrom(getData(oldPlayer)));
        com.hbm.blocks.generic.BlockLanternBehemoth.initReputationHook();
    }

    public static int getReputation(Player player) {
        return getData(player).reputation;
    }

    public static void setReputation(Player player, int reputation) {
        getData(player).reputation = reputation;
    }
}
