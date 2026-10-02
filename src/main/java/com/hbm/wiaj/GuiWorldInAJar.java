package com.hbm.wiaj;

import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ISpecialActor;
import com.hbm.wiaj.actors.JarRenderers;
import com.hbm.wiaj.cannery.CanneryBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.GuiScenes;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.Jars;
import dev.golem.core.render.SubmitSink;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * The presentation screen: the jar in the middle, the actors' boxes over it, play/rewind/forward at the
 * bottom, the subject's icon top left and further presentations below it.
 *
 * <p>Every coordinate, sprite and colour is 1.7's. The jar is drawn through {@link GuiScenes} with 1.7's
 * transform — {@code scale(-10)}, Z flattened by half, zoom, pitch, yaw, centre, offset — minus the
 * {@code translate(width / 2, height / 2, 400)} that only placed it on screen.
 */
//krass
public class GuiWorldInAJar extends CompatScreen {

    private static final Identifier guiUtil = dev.golem.core.Ids.of("hbm", "textures/gui/gui_utility.png");

    JarScript jarScript;
    ItemStack icon;
    ActorFancyPanel titlePanel;
    CanneryBase[] seeAlso;
    ActorFancyPanel[] seeAlsoTitles;
    Font fontRendererObj;

    public GuiWorldInAJar(JarScript script, String title, ItemStack icon, CanneryBase... seeAlso) {
        super(Component.empty());
        this.fontRendererObj = Minecraft.getInstance().font;

        this.jarScript = script;
        this.icon = icon;
        this.seeAlso = seeAlso;

        this.titlePanel = new ActorFancyPanel(fontRendererObj, 40, 27, new Object[][] {{I18n.get(title)}}, 0).setColors(CanneryBase.colorGold).setOrientation(Orientation.LEFT);
        this.seeAlsoTitles = new ActorFancyPanel[seeAlso.length];

        for(int i = 0; i < seeAlso.length; i++) {
            this.seeAlsoTitles[i] = new ActorFancyPanel(fontRendererObj, 40, 27 + 36 * (i + 1), new Object[][] {{I18n.get(seeAlso[i].getName())}}, 0).setColors(CanneryBase.colorGrey).setOrientation(Orientation.LEFT);
        }
        //SKY BLUE: 0xffA5D9FF, 0xff39ACFF, 0xff1A6CA7, 0xff1A1F22
    }

    /** Opens {@code cannery}'s presentation, as the tooltip hotkey and the see-also icons do. */
    public static void open(CanneryBase cannery) {
        Minecraft.getInstance().setScreen(new GuiWorldInAJar(cannery.createScript(), cannery.getName(), cannery.getIcon(), cannery.seeAlso()));
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // drawDefaultBackground: the in-world gradient.
        GuiDraw.fillGradient(graphics, 0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        try {
            if(jarScript != null) {
                jarScript.run();
            }
            this.drawGuiContainerBackgroundLayer(graphics);
        } catch(Exception ex) {
            fail(ex);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try {
            this.drawGuiContainerForegroundLayer(graphics, mouseX, mouseY);
        } catch(Exception ex) {
            fail(ex);
        }
    }

    /** Upstream printed the stack trace to chat and closed the screen. */
    private void fail(Exception ex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for(StackTraceElement line : ex.getStackTrace()) {
                mc.player.sendSystemMessage(Component.literal(line.toString()).withStyle(ChatFormatting.RED));
            }
        }
        com.hbm.Constants.LOG.error("World in a jar failed", ex);
        mc.setScreen(null);
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if(width / 2 - 12 <= mouseX && width / 2 - 12 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {
            click();

            if(this.jarScript.isPaused()) {
                this.jarScript.unpause();
            } else {
                this.jarScript.pause();
            }
        }

        if(width / 2 - 12 - 36 <= mouseX && width / 2 - 12 - 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {

            if(this.jarScript.sceneNumber > 0) {
                this.jarScript.rewindOne();
                click();
            }
        }

        if(width / 2 - 12 + 36 <= mouseX && width / 2 - 12 + 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY) {

            if(this.jarScript.sceneNumber < this.jarScript.scenes.size()) {
                this.jarScript.forwardOne();
                click();
            }
        }

        for(int i = 0; i < seeAlso.length; i++) {

            if(15 <= mouseX && 39 > mouseX && 15 + 36 * (i + 1) < mouseY && 39 + 36 * (i + 1) >= mouseY) {
                CanneryBase cannery = seeAlso[i];
                click();
                open(cannery);
                return true;
            }
        }
        return false;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void drawGuiContainerForegroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

        for(Entry<Integer, ISpecialActor> actor : this.jarScript.actors.entrySet()) {
            actor.getValue().drawForegroundComponent(graphics, this.width, this.height, this.jarScript.ticksElapsed, this.jarScript.interp);
        }

        if(KeyMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT)) {
            List<Object[]> list = new ArrayList<>();
            list.add(new Object[] { (mouseX - width / 2) + " / " + (mouseY - height / 2) });
            new com.hbm.wiaj.actors.ActorBasicPanel(mouseX - width / 2, mouseY - height / 2, list.toArray(new Object[0][]))
                    .drawForegroundComponent(graphics, this.width, this.height, 0, 0F);
        }

        int playButton = this.jarScript.isPaused() ? 64 : 40;

        if(width / 2 - 12 <= mouseX && width / 2 - 12 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
            this.drawTexturedModalRect(graphics, width / 2 - 12, height - 36, playButton, 24, 24, 24);
        else
            this.drawTexturedModalRect(graphics, width / 2 - 12, height - 36, playButton, 48, 24, 24);

        if(this.jarScript.sceneNumber == 0)
            this.drawTexturedModalRect(graphics, width / 2 - 12 - 36, height - 36, 88, 72, 24, 24);
        else if(width / 2 - 12 - 36 <= mouseX && width / 2 - 12 - 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
            this.drawTexturedModalRect(graphics, width / 2 - 12 - 36, height - 36, 88, 24, 24, 24);
        else
            this.drawTexturedModalRect(graphics, width / 2 - 12 - 36, height - 36, 88, 48, 24, 24);

        if(this.jarScript.sceneNumber >= this.jarScript.scenes.size())
            this.drawTexturedModalRect(graphics, width / 2 - 12 + 36, height - 36, 112, 72, 24, 24);
        else if(width / 2 - 12 + 36 <= mouseX && width / 2 - 12 + 36 + 24 > mouseX && height - 36 < mouseY && height - 36 + 24 >= mouseY)
            this.drawTexturedModalRect(graphics, width / 2 - 12 + 36, height - 36, 112, 24, 24, 24);
        else
            this.drawTexturedModalRect(graphics, width / 2 - 12 + 36, height - 36, 112, 48, 24, 24);

        this.drawTexturedModalRect(graphics, 15, 15, 136, 48, 24, 24);
        GuiDraw.item(graphics, this.icon, 19, 19);
        GuiDraw.itemDecorations(graphics, this.fontRendererObj, this.icon, 19, 19);

        if(15 <= mouseX && 39 > mouseX && 15 < mouseY && 39 >= mouseY) {
            this.titlePanel.drawForegroundComponent(graphics, 0, 0, this.jarScript.ticksElapsed, this.jarScript.interp);
        }

        for(int i = 0; i < seeAlso.length; i++) {
            CanneryBase also = seeAlso[i];

            this.drawTexturedModalRect(graphics, 15, 15 + 36 * (i + 1), 136, 72, 24, 24);
            GuiDraw.item(graphics, also.getIcon(), 19, 19 + 36 * (i + 1));
            GuiDraw.itemDecorations(graphics, this.fontRendererObj, also.getIcon(), 19, 19 + 36 * (i + 1));

            if(15 <= mouseX && 39 > mouseX && 15 + 36 * (i + 1) < mouseY && 39 + 36 * (i + 1) >= mouseY) {
                this.seeAlsoTitles[i].drawForegroundComponent(graphics, 0, 0, this.jarScript.ticksElapsed, this.jarScript.interp);
            }
        }
    }

    private void drawTexturedModalRect(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        GuiDraw.blit(graphics, guiUtil, x, y, u, v, w, h, 256, 256);
    }

    /**
     * The jar and its actors. Both passes run inside one scene so they share one depth buffer, as the 1.7
     * block pass and actor pass shared the screen's.
     */
    private void drawGuiContainerBackgroundLayer(GuiGraphicsExtractor graphics) {
        final JarScript script = this.jarScript;
        final WorldInAJar world = script.world;
        final int ticks = script.ticksElapsed;
        final float interp = script.interp;
        final double zoom = script.zoom();
        final double pitch = script.pitch();
        final double yaw = script.yaw();
        final double offsetX = script.offsetX();
        final double offsetY = script.offsetY();
        final double offsetZ = script.offsetZ();
        final List<ISpecialActor> actors = new ArrayList<>(script.actors.values());

        GuiScenes.draw(graphics, 0, 0, this.width, this.height, (pose, sink, camera) -> {
            pose.pushPose();
            setupRotation(pose, world, zoom, pitch, yaw, offsetX, offsetY, offsetZ);
            Jars.submit(world.blocks(), pose, sink);
            for (int x = 0; x < world.sizeX; x++) {
                for (int y = 0; y < world.sizeY; y++) {
                    for (int z = 0; z < world.sizeZ; z++) {
                        Object tile = world.getTileEntity(x, y, z);
                        if (tile instanceof BlockEntity be) {
                            // a tile an actor draws (basin, mold, crucible) is left to that actor
                            if (JarRenderers.drawnWithBlocks(be)) JarRenderers.drawTile(world, be, x, y, z, pose, sink, camera, interp);
                        } else {
                            BlockEntity standIn = world.standIn(x, y, z);
                            if (standIn != null) JarRenderers.drawTile(world, standIn, x, y, z, pose, sink, camera, interp);
                        }
                    }
                }
            }
            pose.popPose();

            pose.pushPose();
            setupRotation(pose, world, zoom, pitch, yaw, offsetX, offsetY, offsetZ);
            for(ISpecialActor actor : actors) {
                pose.pushPose();
                actor.drawBackgroundComponent(world, pose, sink, camera, ticks, interp);
                pose.popPose();
            }
            pose.popPose();
        });
    }

    private static void setupRotation(PoseStack pose, WorldInAJar world, double zoom, double pitch, double yaw,
                                      double offsetX, double offsetY, double offsetZ) {

        float scale = -10;

        pose.scale(scale, scale, scale);
        pose.scale(1, 1, 0.5F); //incredible flattening power

        pose.scale((float) zoom, (float) zoom, (float) zoom);

        pose.mulPose(Axis.XP.rotationDegrees((float) pitch));
        pose.mulPose(Axis.YP.rotationDegrees((float) yaw));
        pose.translate(world.sizeX / -2D, -world.sizeY / 2D, world.sizeZ / -2D);
        pose.translate(offsetX, offsetY, offsetZ);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
