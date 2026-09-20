package com.hbm.util;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import com.hbm.blocks.network.BlockConveyorLift;

/** 1.7 {@code BlockConveyorLift}: the isBottom / isTop pair its bounds, snapping, travel and renderer all read. */
public enum ConveyorLiftPart implements StringRepresentable {
    BOTTOM,
    MIDDLE,
    TOP;

    private final String id = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return id;
    }
}
