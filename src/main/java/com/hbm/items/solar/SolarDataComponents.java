package com.hbm.items.solar;

import com.hbm.Constants;
import dev.golem.core.data.GolemComponent;
import net.minecraft.core.BlockPos;

/** Persistent coordinates recorded by the solar mirror adjustment tool. */
public final class SolarDataComponents {

    private SolarDataComponents() {}

    /** The old mirror-tool {@code posX}, {@code posY}, and {@code posZ} tag, as one typed position. */
    public static final GolemComponent<BlockPos> MIRROR_TARGET =
            GolemComponent.of(Constants.MOD_ID, "mirror_target", BlockPos.CODEC);

    public static void init() {}
}
