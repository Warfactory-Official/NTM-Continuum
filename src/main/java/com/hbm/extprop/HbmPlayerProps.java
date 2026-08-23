package com.hbm.extprop;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import net.minecraft.world.entity.player.Player;

/** Player-state facade retained for systems, such as the RBMK crane, that use live player input. */
public final class HbmPlayerProps {
    private HbmPlayerProps() {}

    public static EntityHbmProps getData(Player player) {
        return HbmLivingProps.getData(player);
    }
}
