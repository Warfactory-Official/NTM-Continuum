package com.hbm.wiaj.actors;

import com.hbm.wiaj.WorldInAJar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.nbt.CompoundTag;

/**
 * A machine model drawn as an actor rather than as a tile in the jar. 1.7 had the machine TESRs implement
 * this directly; here the implementations fill the machine's own render state from the actor's data and
 * replay the port's renderer, see {@link JarRenderers}.
 */
public interface ITileActorRenderer {

    public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data);
    public void updateActor(int ticks, CompoundTag data);
}
