package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/**
 * Actors, anything that can receive ticks (for rendering movement for example) and renders on screen
 * Can be tile entity models, faux entities or tooltips
 *
 * <p>The two draw calls carry what 1.7 kept in global GL state: the screen's graphics for the foreground,
 * and the scene's pose and sink for the background, already set up with the jar's rotation and zoom.
 * @author hbm
 */
public interface ISpecialActor {

    /** Draws things in the foreground like text boxes */
    public void drawForegroundComponent(GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp);
    /** Draws things in the background, fitted to the world renderer like TESRs */
    public void drawBackgroundComponent(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp);
    /** Update ticks to emulate serverside ticking */
    public void updateActor(JarScene scene);
    /** Sets the data object to the passed NBT */
    public void setActorData(CompoundTag data);
    /** Auto-detects the passed object's type and sets the specified NBT tag */
    public void setDataPoint(String tag, Object o);
}
