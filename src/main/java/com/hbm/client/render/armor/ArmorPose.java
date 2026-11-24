package com.hbm.client.render.armor;

import dev.golem.core.render.HumanoidPose;
import dev.golem.core.world.ArmorSlot;

/** What a worn suit's declaration reads: one model for all four slots, replayed once per slot. */
public final class ArmorPose {

    public ArmorSlot slot;
    public HumanoidPose wearer;
    /**
     * Set for a first-person draw of the arms alone ({@link ArmorRenderer#submitArms}): per side (right 0, left 1) the frames that take a vanilla arm
     * part's local space (blocks) to the hand pass, upper arm at {@code [side * 2]}, forearm at {@code [side * 2 + 1]}. Null otherwise.
     */
    public org.joml.Matrix4fc @org.jspecify.annotations.Nullable [] arms;
}
