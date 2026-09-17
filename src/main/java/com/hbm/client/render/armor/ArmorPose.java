package com.hbm.client.render.armor;

import dev.golem.core.render.HumanoidPose;
import dev.golem.core.world.ArmorSlot;

/** What a worn suit's declaration reads: one model for all four slots, replayed once per slot. */
public final class ArmorPose {

    public ArmorSlot slot;
    public HumanoidPose wearer;
}
