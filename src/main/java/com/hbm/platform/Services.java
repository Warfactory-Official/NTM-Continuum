package com.hbm.platform;

import com.hbm.Constants;
import com.hbm.platform.services.IPlatformHelper;

/**
 * Access point for the loader-specific implementations of the platform services.
 *
 * <p>This used to resolve implementations through {@link java.util.ServiceLoader}, which the
 * MultiLoader layout required: each loader subproject shipped its own
 * {@code META-INF/services/...IPlatformHelper} file. In the flat single-source-tree layout both
 * loaders' service files would occupy the same resource path and cannot coexist, so selection now
 * happens at compile time instead — Stonecutter keeps exactly one branch below and discards the rest,
 * which is cheaper at runtime and removes two resource files and the reflective lookup.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = createPlatformHelper();

    private Services() {}

    private static IPlatformHelper createPlatformHelper() {
        //? if fabric {
        /*IPlatformHelper helper = new com.hbm.platform.fabric.FabricPlatformHelper();
        *///?} elif neoforge {
        IPlatformHelper helper = new com.hbm.platform.neoforge.NeoForgePlatformHelper();
        //?}
        Constants.LOG.debug("Loaded {} for service {}", helper, IPlatformHelper.class);
        return helper;
    }
}
