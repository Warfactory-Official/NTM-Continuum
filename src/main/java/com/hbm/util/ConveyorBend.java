package com.hbm.util;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import com.hbm.blocks.network.BlockConveyorBendable;

/** 1.7 {@code BlockConveyorBendable.getPathDirection}: metadata 2-5 is straight, 6-9 a left bend, 10-13 a right bend. */
public enum ConveyorBend implements StringRepresentable {
    STRAIGHT,
    LEFT,
    RIGHT;

    public static final ConveyorBend[] VALUES = values();

    private final String id = name().toLowerCase(Locale.ROOT);

    /** 1.7 sneak-screw: {@code meta += 4} walks straight to left to right. */
    public ConveyorBend next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public ConveyorBend mirrored() {
        return switch (this) {
            case STRAIGHT -> STRAIGHT;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
